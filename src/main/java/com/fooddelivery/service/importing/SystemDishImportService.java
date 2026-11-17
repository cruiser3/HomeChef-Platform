package com.fooddelivery.service.importing;

import com.fooddelivery.entity.SystemDish;
import com.fooddelivery.entity.SystemDishCategory;
import com.fooddelivery.entity.SystemDishIngredient;
import com.fooddelivery.entity.SystemDishStep;
import com.fooddelivery.repository.SystemDishCategoryRepository;
import com.fooddelivery.repository.SystemDishIngredientRepository;
import com.fooddelivery.repository.SystemDishRepository;
import com.fooddelivery.repository.SystemDishStepRepository;
import com.fooddelivery.service.importing.HowToCookMarkdownParser.Ingredient;
import com.fooddelivery.service.importing.HowToCookMarkdownParser.ParsedDish;
import com.fooddelivery.service.importing.HowToCookMarkdownParser.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HowToCook 菜谱批量导入服务。
 *
 * <p>职责：遍历 HowToCook/dishes 目录 → 按目录名映射到系统菜谱分类 → 解析 Markdown →
 * 幂等写入 system_dishes 表族。</p>
 *
 * <p>幂等与一致性：以 {@code (source_type, original_path)} 复合唯一键去重；
 * 已存在且缺封面图的菜谱会「回填」封面图，其余已存在记录跳过；
 * 每道菜的「菜谱 + 食材 + 步骤」在同一个独立事务中写入（借助 {@link TransactionTemplate}），
 * 单道菜失败不影响其它菜，也不污染已成功的数据。</p>
 *
 * <p>封面图：解析 Markdown 中的成品/预览图（否则第一张整行图片），把图片文件复制到
 * {@code app.upload.dir}（复用 {@code /uploads} 静态映射），并将可访问的
 * {@code /uploads/sysdish-cover-<id>.<ext>} 写入 {@code cover_image_url}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemDishImportService {

    private static final String SOURCE_TYPE = "HOWTOCOOK";

    /** HowToCook 分类目录名 → 系统菜谱分类名（与 V13 种子数据一致）。 */
    private static final Map<String, String> CATEGORY_MAP = Map.ofEntries(
            Map.entry("vegetable_dish", "素菜"),
            Map.entry("meat_dish", "荤菜"),
            Map.entry("aquatic", "水产"),
            Map.entry("breakfast", "早餐"),
            Map.entry("staple", "主食"),
            Map.entry("soup", "汤与粥"),
            Map.entry("drink", "饮料"),
            Map.entry("condiment", "酱料"),
            Map.entry("dessert", "甜品"),
            Map.entry("semi-finished", "半成品加工")
    );

    private final SystemDishRepository systemDishRepository;
    private final SystemDishCategoryRepository categoryRepository;
    private final SystemDishIngredientRepository ingredientRepository;
    private final SystemDishStepRepository stepRepository;
    private final HowToCookMarkdownParser parser;
    private final TransactionTemplate transactionTemplate;

    /** 封面图复制目标目录（复用 /uploads 静态映射与 vite 代理）。 */
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * 全量导入 HowToCook 菜谱。
     *
     * @param howToCookRoot HowToCook 仓库根目录（含 dishes 子目录）
     * @return 导入统计
     * @throws IOException 读取目录或文件失败时
     */
    public ImportResult importAll(Path howToCookRoot) throws IOException {
        Path dishesDir = howToCookRoot.resolve("dishes");
        if (!Files.isDirectory(dishesDir)) {
            throw new IllegalStateException("HowToCook dishes 目录不存在：" + dishesDir.toAbsolutePath());
        }

        Map<String, Long> categoryIdByName = categoryRepository.findByAvailableTrueOrderBySortNoAsc().stream()
                .collect(Collectors.toMap(SystemDishCategory::getName, SystemDishCategory::getId));
        if (categoryIdByName.isEmpty()) {
            throw new IllegalStateException("system_dish_categories 为空，请先启动一次后端执行 V13 迁移后再导入");
        }

        Path rootAbs = howToCookRoot.toAbsolutePath().normalize();
        ImportResult result = new ImportResult();
        try (Stream<Path> dirs = Files.list(dishesDir)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                String dirName = dir.getFileName().toString();
                if ("template".equals(dirName)) {
                    continue;
                }
                Long categoryId = categoryIdByName.get(CATEGORY_MAP.get(dirName));
                if (categoryId == null) {
                    log.warn("跳过无法映射分类的目录：{}", dirName);
                    result.skippedCategory++;
                    continue;
                }
                try (Stream<Path> files = Files.walk(dir)) {
                    for (Path md : files.filter(p -> p.toString().endsWith(".md")).toList()) {
                        importOne(md, rootAbs, categoryId, result);
                    }
                }
            }
        }
        return result;
    }

    private void importOne(Path md, Path rootAbs, Long categoryId, ImportResult result) {
        // originalPath 统一为相对 HowToCook 根目录的 `/` 分隔路径（如 dishes/vegetable_dish/xxx.md）
        String normalized = rootAbs.relativize(md.toAbsolutePath().normalize())
                .toString().replace('\\', '/');

        SystemDish existing = systemDishRepository
                .findBySourceTypeAndOriginalPath(SOURCE_TYPE, normalized).orElse(null);

        try {
            String markdown = Files.readString(md, StandardCharsets.UTF_8);
            ParsedDish parsed = parser.parse(markdown);

            if (existing != null) {
                // 已存在：仅当缺封面图时回填，否则跳过。
                if (existing.getCoverImageUrl() == null || existing.getCoverImageUrl().isBlank()) {
                    String url = storeCoverImage(existing.getId(), md, parsed.coverImage);
                    if (url != null) {
                        existing.setCoverImageUrl(url);
                        systemDishRepository.save(existing);
                        result.coverBackfilled++;
                        return;
                    }
                }
                result.skippedExisting++;
                return;
            }

            if (parsed.name == null || parsed.name.isBlank()) {
                log.warn("菜谱缺少标题，跳过：{}", normalized);
                result.failed++;
                return;
            }
            transactionTemplate.executeWithoutResult(status -> persist(parsed, md, categoryId, normalized));
            result.imported++;
        } catch (Exception e) {
            log.warn("导入失败：{} -> {}", normalized, e.getMessage());
            result.failed++;
        }
    }

    private void persist(ParsedDish parsed, Path md, Long categoryId, String originalPath) {
        SystemDish dish = SystemDish.builder()
                .categoryId(categoryId)
                .name(truncate(parsed.name, 100))
                .description(truncate(parsed.description, 500))
                .difficulty(parsed.difficulty)
                .calories(parsed.calories)
                .sourceType(SOURCE_TYPE)
                .originalPath(originalPath)
                .build();
        SystemDish saved = systemDishRepository.save(dish);

        String coverUrl = storeCoverImage(saved.getId(), md, parsed.coverImage);
        if (coverUrl != null) {
            saved.setCoverImageUrl(coverUrl);
            systemDishRepository.save(saved);
        }

        int sortNo = 1;
        for (Ingredient ing : parsed.ingredients) {
            ingredientRepository.save(SystemDishIngredient.builder()
                    .dishId(saved.getId())
                    .name(truncate(ing.name, 100))
                    .amount(ing.amount)
                    .unit(truncate(ing.unit, 20))
                    .amountText(truncate(ing.amountText, 100))
                    .optional(ing.optional)
                    .sortNo(sortNo++)
                    .build());
        }

        for (Step step : parsed.steps) {
            stepRepository.save(SystemDishStep.builder()
                    .dishId(saved.getId())
                    .stepNo(step.stepNo)
                    .content(truncate(step.content, 500))
                    .build());
        }
    }

    /**
     * 把封面图从 HowToCook 仓库复制到 uploads 目录，返回可访问的 /uploads 路径。
     * 文件名按菜谱 id 确定（sysdish-cover-<id>.<ext>），重复导入时覆盖同一文件，保持幂等。
     * 无封面图或图片文件缺失/复制失败时返回 null（不阻断导入）。
     */
    private String storeCoverImage(Long dishId, Path md, String coverImage) {
        if (coverImage == null || coverImage.isBlank()) {
            return null;
        }
        try {
            Path source = md.getParent().resolve(coverImage).normalize();
            if (!Files.isRegularFile(source)) {
                return null;
            }
            String filename = "sysdish-cover-" + dishId + "." + extensionOf(source.getFileName().toString());
            Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Files.copy(source, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (Exception e) {
            log.warn("封面图复制失败：dishId={} cover={} -> {}", dishId, coverImage, e.getMessage());
            return null;
        }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "jpg";
        }
        String ext = filename.substring(dot + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "webp" -> ext;
            default -> "jpg";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 导入统计。 */
    @lombok.Data
    public static class ImportResult {
        private int imported;
        private int skippedExisting;
        private int skippedCategory;
        private int failed;
        private int coverBackfilled;
    }
}
