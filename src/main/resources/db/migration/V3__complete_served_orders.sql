-- 修复历史数据：已开饭（上桌）但订单状态仍停留在进行中的订单，统一修正为「已完成」
UPDATE family_orders
SET status = 'COMPLETED'
WHERE meal_served = TRUE
  AND status NOT IN ('CANCELLED', 'COMPLETED');
