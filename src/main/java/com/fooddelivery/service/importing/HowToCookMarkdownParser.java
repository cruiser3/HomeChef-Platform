package com.fooddelivery.service.importing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HowToCook 菜谱 Markdown 的纯规则解析器（无 Spring 外部依赖，便于单元测试）。
 *
 * <p>职责：把一个菜谱 Markdown 文本解析成结构化的 {@link ParsedDish}，不涉及任何数据库读写。
 * 解析失败一律「降级保留原文」而非报错：</p>
 * <ul>
 *   <li>「## 计算」用量无法量化（份数公式 / 区间 / 多选 / 比例 / 约数 / 无数字）时，
 *       amount、unit 置空，原文存入 amountText，由「复制到我家」时人工补齐；</li>
 *   <li>「## 操作」子步骤合并进父步骤，步骤图与 Markdown 强调标记一并丢弃；</li>
 *   <li>标题、难度、卡路里、描述按规范格式提取，缺失则对应字段为 null。</li>
 * </ul>
 */
@Component
public class HowToCookMarkdownParser {

    /** HTML 注释（跨行），先整体剔除。 */
    private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private static final Pattern H1 = Pattern.compile("^#\\s+(.*)$");
    private static final Pattern H2 = Pattern.compile("^##\\s+(.*)$");
    private static final Pattern DIFFICULTY = Pattern.compile("预估烹饪难度[：:]\\s*([★☆]+)");
    private static final Pattern CALORIES = Pattern.compile("预估卡路里[：:]\\s*(\\d+)");
    private static final Pattern IMAGE = Pattern.compile("^!\\[([^\\]]*)\\]\\(([^)]*)\\)$");

    /** 顶层操作步骤：`1. xxx` 或 `1、xxx`。 */
    private static final Pattern TOP_STEP = Pattern.compile("^(\\d+)[.、]\\s*(.*)$");
    /** 嵌套子步骤（缩进列表项，trim 后以 - 或 * 开头）。 */
    private static final Pattern SUB_STEP = Pattern.compile("^[-*]\\s+(.*)$");

    /**
     * 用量主体：数字（可小数）+ 单位。单位按「多字在前、单字在后」排序，
     * 避免 `ml` 被拆成 `m`、`kg` 被拆成 `g` 等歧义。
     */
    private static final Pattern AMOUNT = Pattern.compile(
            "^([0-9]+(?:\\.[0-9]+)?)\\s*(毫升|汤匙|茶匙|大匙|小匙|千克|公斤|毫米|克|升|斤|两|瓣|只|根|片|勺|杯|块|朵|颗|把|段|条|张|滴|个|kg|ml|mm|cm|g|l)");

    /**
     * 解析单个菜谱 Markdown。
     *
     * @param markdown 菜谱 Markdown 全文
     * @return 结构化结果（name 可能为空，表示不是合法菜谱）
     */
    public ParsedDish parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new ParsedDish();
        }

        ParsedDish dish = new ParsedDish();
        List<String> descLines = new ArrayList<>();
        boolean inDescription = false;
        String section = "";
        String firstImage = null;
        String preferredCover = null;

        String cleaned = HTML_COMMENT.matcher(markdown).replaceAll("");
        for (String rawLine : cleaned.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher h1 = H1.matcher(line);
            if (h1.matches()) {
                String title = h1.group(1).trim();
                dish.name = title.replaceFirst("的做法$", "").trim();
                inDescription = true;
                section = "";
                continue;
            }

            Matcher h2 = H2.matcher(line);
            if (h2.matches()) {
                section = h2.group(1).trim();
                inDescription = false;
                continue;
            }

            Matcher dm = DIFFICULTY.matcher(line);
            if (dm.find()) {
                dish.difficulty = countStars(dm.group(1));
                inDescription = false;
                continue;
            }

            Matcher cm = CALORIES.matcher(line);
            if (cm.find()) {
                dish.calories = Integer.parseInt(cm.group(1));
                inDescription = false;
                continue;
            }

            Matcher im = IMAGE.matcher(line);
            if (im.matches()) {
                String alt = im.group(1);
                String path = cleanImagePath(im.group(2));
                if (!path.isEmpty()) {
                    if (firstImage == null) {
                        firstImage = path;
                    }
                    if (preferredCover == null && (alt.contains("成品") || alt.contains("预览"))) {
                        preferredCover = path;
                    }
                }
                continue;
            }

            if (section.startsWith("计算")) {
                // 食材必须是列表项（- / * / + 开头）；普通段落（如「每次制作前需要确定计划做几份」等说明文字）跳过，避免误解析成食材。
                if (line.matches("[-*+]\\s+.*")) {
                    parseIngredientLine(line, dish);
                }
            } else if (section.startsWith("操作")) {
                parseStepLine(line, dish);
            } else if (inDescription) {
                descLines.add(line);
            }
        }

        if (!descLines.isEmpty()) {
            dish.description = String.join(" ", descLines).trim();
            if (dish.description.isEmpty()) {
                dish.description = null;
            }
        }
        dish.coverImage = preferredCover != null ? preferredCover : firstImage;
        return dish;
    }

    private void parseIngredientLine(String line, ParsedDish dish) {
        String text = line.replaceFirst("^[-*+]\\s+", "").trim();
        if (text.isEmpty() || !text.matches(".*\\d.*")) {
            return; // 空行或说明文字（无数字）
        }

        // 1. 分割食材名与用量：优先显式分隔符，其次按首个数字切分。
        String name;
        String usage;
        int eq = text.indexOf('=');
        int colonCn = text.indexOf('：');
        int colonEn = text.indexOf(':');
        int sep = -1;
        if (eq >= 0) {
            sep = eq;
        } else if (colonCn >= 0) {
            sep = colonCn;
        } else if (colonEn >= 0) {
            sep = colonEn;
        }

        if (sep >= 0) {
            name = text.substring(0, sep).trim();
            usage = text.substring(sep + 1).trim();
        } else {
            int digitIdx = firstDigitIndex(text);
            if (digitIdx <= 0) {
                return;
            }
            name = text.substring(0, digitIdx).trim();
            usage = text.substring(digitIdx).trim();
        }

        if (name.isEmpty() || usage.isEmpty()) {
            return;
        }

        // 2. 可选标记：仅识别「可选」字样，其余如「（依个人口味）」不影响是否可选。
        boolean optional = name.contains("可选") || usage.contains("可选");
        name = name.replaceAll("[（(]?可选[）)]?", "").trim();
        if (name.isEmpty()) {
            return;
        }

        // 3. 尝试提取「数字 + 单位」主体。
        Matcher m = AMOUNT.matcher(usage);
        if (m.find()) {
            String rest = usage.substring(m.end()).trim();
            boolean uncertain = rest.contains("份数")      // 份数公式
                    || rest.contains("/")                  // 多选 / 比例
                    || rest.matches("[-~].*\\d.*");        // 区间剩余
            if (uncertain) {
                dish.ingredients.add(new Ingredient(name, null, null, usage, optional));
            } else {
                dish.ingredients.add(new Ingredient(name, new BigDecimal(m.group(1)), m.group(2), null, optional));
            }
        } else {
            dish.ingredients.add(new Ingredient(name, null, null, usage, optional));
        }
    }

    private void parseStepLine(String line, ParsedDish dish) {
        Matcher top = TOP_STEP.matcher(line);
        if (top.matches()) {
            // 序号按解析顺序重排（1,2,3…），避免源文件序号重复破坏唯一约束。
            int stepNo = dish.steps.size() + 1;
            dish.steps.add(new Step(stepNo, cleanInline(top.group(2))));
            return;
        }

        Matcher sub = SUB_STEP.matcher(line);
        if (sub.matches()) {
            if (!dish.steps.isEmpty()) {
                Step last = dish.steps.get(dish.steps.size() - 1);
                last.content = last.content + "\n" + cleanInline(sub.group(1));
            }
            return;
        }

        // 步骤续行（多行说明），追加到上一步。
        if (!dish.steps.isEmpty()) {
            Step last = dish.steps.get(dish.steps.size() - 1);
            last.content = last.content + " " + cleanInline(line);
        }
    }

    /** 去掉 Markdown 行内强调标记（**、*、反引号），保留可读文本。 */
    private String cleanInline(String s) {
        return s.replace("**", "").replace("*", "").replace("`", "").trim();
    }

    /** 提取图片路径，去掉可选的标题部分（`path "title"`）。 */
    private String cleanImagePath(String raw) {
        if (raw == null) {
            return "";
        }
        String path = raw.trim();
        int titleIdx = path.indexOf(" \"");
        if (titleIdx > 0) {
            path = path.substring(0, titleIdx);
        }
        return path.trim();
    }

    private int countStars(String s) {
        int count = 0;
        for (char c : s.toCharArray()) {
            if (c == '★' || c == '☆') {
                count++;
            }
        }
        return count;
    }

    private int firstDigitIndex(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /** 单个菜谱的结构化结果。 */
    public static class ParsedDish {
        public String name;
        public String description;
        public Integer difficulty;
        public Integer calories;
        /** 封面图在 Markdown 中的原始相对路径（成品/预览图优先，否则第一张整行图片）。 */
        public String coverImage;
        public final List<Ingredient> ingredients = new ArrayList<>();
        public final List<Step> steps = new ArrayList<>();
    }

    /** 单个食材的解析结果。amount/unit 为 null 时，用量原文在 amountText 中。 */
    public static class Ingredient {
        public final String name;
        public final BigDecimal amount;
        public final String unit;
        public final String amountText;
        public final boolean optional;

        public Ingredient(String name, BigDecimal amount, String unit, String amountText, boolean optional) {
            this.name = name;
            this.amount = amount;
            this.unit = unit;
            this.amountText = amountText;
            this.optional = optional;
        }
    }

    /** 单个步骤的解析结果。 */
    public static class Step {
        public int stepNo;
        public String content;

        public Step(int stepNo, String content) {
            this.stepNo = stepNo;
            this.content = content;
        }
    }
}
