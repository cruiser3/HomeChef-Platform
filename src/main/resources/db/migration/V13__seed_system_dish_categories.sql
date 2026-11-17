-- =====================================================================
-- V13: 系统菜谱分类种子数据
-- 设计依据：docs/food-square/SYSTEM_RECIPE_DATABASE_DESIGN.md（第 4.1 节）
--
-- 为 system_dish_categories 初始化 10 个分类，对应 HowToCook/dishes 目录：
--   素菜 / 荤菜 / 水产 / 早餐 / 主食 / 汤与粥 / 饮料 / 酱料 / 甜品 / 半成品加工
--
-- 说明：Flyway 版本化保证本脚本只执行一次；若需重跑，须先删除
--       flyway_schema_history 中 version = '13' 的记录（并清空已插入数据）。
-- =====================================================================

INSERT INTO system_dish_categories (name, sort_no, is_available) VALUES
    ('素菜', 1, TRUE),
    ('荤菜', 2, TRUE),
    ('水产', 3, TRUE),
    ('早餐', 4, TRUE),
    ('主食', 5, TRUE),
    ('汤与粥', 6, TRUE),
    ('饮料', 7, TRUE),
    ('酱料', 8, TRUE),
    ('甜品', 9, TRUE),
    ('半成品加工', 10, TRUE);
