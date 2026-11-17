package com.fooddelivery.service.importing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * HowToCook 数据导入的启动入口（ApplicationRunner）。
 *
 * <p>默认关闭，不参与正常启动；仅在配置 {@code app.system-dish.import.enabled=true} 时，
 * 于应用启动后执行一次幂等导入。导入失败仅记录日志，不阻断应用启动。</p>
 */
@Slf4j
@Component
public class SystemDishImportRunner implements ApplicationRunner {

    private final boolean enabled;
    private final Path howToCookPath;
    private final SystemDishImportService importService;

    public SystemDishImportRunner(
            @Value("${app.system-dish.import.enabled:false}") boolean enabled,
            @Value("${app.system-dish.import.howtocook-path:./HowToCook}") String howToCookPath,
            SystemDishImportService importService) {
        this.enabled = enabled;
        this.howToCookPath = Path.of(howToCookPath);
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("系统菜谱导入已关闭（app.system-dish.import.enabled=false），跳过 HowToCook 导入");
            return;
        }
        log.info("开始导入 HowToCook 菜谱，目录：{}", howToCookPath.toAbsolutePath());
        try {
            SystemDishImportService.ImportResult result = importService.importAll(howToCookPath);
            log.info("HowToCook 导入完成：新增 {}，回填封面 {}，跳过(已存在) {}，跳过(未知分类目录) {}，失败 {}",
                    result.getImported(), result.getCoverBackfilled(), result.getSkippedExisting(), result.getSkippedCategory(), result.getFailed());
        } catch (Exception e) {
            log.error("HowToCook 导入异常：{}", e.getMessage(), e);
        }
    }
}
