-- =====================================================================
-- V17: 菜系排序调整 + 新增「未分类」占位类别
--
-- 目的：
--   1. 将「其他」移到倒数第二（sort_no = 1000）。
--   2. 新增「未分类」类别（sort_no = 1001），代表 cuisine_id IS NULL 的菜品。
--
-- 说明：
--   - 正常菜系（含后续新建）sort_no 从 1 开始递增，始终 < 1000，排在「其他」之前。
--   - 「未分类」是虚拟归类，仅用于筛选列表展示与排序占位，不用于给菜品赋值
--     cuisine_id；后端筛选时把「未分类」转成 cuisine_id IS NULL 查询。
--   - 外键按 id 关联，修改「其他」的 sort_no 不影响已有菜品的菜系归属。
--
-- 回滚方式（如确需撤销，须人工审批）：
--   1. DELETE FROM cuisines WHERE name = '未分类';
--   2. UPDATE cuisines SET sort_no = 12 WHERE name = '其他';
--   3. DELETE FROM flyway_schema_history WHERE version = '17';
-- =====================================================================

UPDATE cuisines SET sort_no = 1000 WHERE name = '其他';

INSERT INTO cuisines (name, sort_no) VALUES ('未分类', 1001);
