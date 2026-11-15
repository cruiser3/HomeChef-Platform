# 订单与备菜协作模块（M-06 / M-07 / M-08）

> 最后更新：2026-11-12
> 状态：核心主流程已完成（下单 / 查询 / 状态流转 / 逐道菜备菜协作 / 最小家庭采购清单）
> 相关决策：D-014（新建家庭订单表，保留旧订单）、D-015（两套状态分离）、D-016（最小采购清单范围）

## 1. 模块范围

本模块在购物车（M-05）之后，把「个人购物车」转化为「家庭订单」，并支持家庭成员协作备菜。

**本轮实现：**

1. 提交当前用户在当前家庭的购物车，创建个人订单（M-06）。
2. 订单列表与详情查询，含明细快照（M-06）。
3. 订单整体状态流转：`CREATED → PREPARING → COMPLETED`，可 `CANCELLED`（M-06）。
4. 逐道菜备菜状态：`PENDING / COOKING / READY / SERVED / BLOCKED`（M-07）。
5. 备菜负责人指定（必须是当前家庭成员）（M-07）。
6. 订单级开饭状态 `mealServed` / `mealServedAt`（M-07）。
7. 最小家庭待采购清单：只登记成员实际标记缺少的食材 + 查看（M-08）。

**本轮明确不做（见第 10 节）：**

- 不实现管理员代他人取消订单。
- 不实现采购清单已购买标记、误加纠正、取消订单联动。
- 不实现支付、配送、物流、库存锁定。
- 不实现旧 `Order`/`OrderItem`（餐厅外卖模型）的任何改造。

## 2. 与旧订单实现的关系

旧实现基于 `Restaurant` + `MenuItem`（餐厅外卖语义），与新家庭 `Dish` 模型不兼容。按已批准方案 B（调整版）：

| 项目 | 旧实现 | 新实现 |
|---|---|---|
| 实体 | `Order` / `OrderItem` | `FamilyOrder` / `FamilyOrderItem` |
| 数据表 | `orders` / `order_items` | `family_orders` / `family_order_items` |
| 菜品引用 | `MenuItem` | `Dish`（另存名称与单价快照） |
| 接口路径 | `/api/orders` | `/api/family-orders` |
| 状态 | `PENDING/CONFIRMED/PREPARING/OUT_FOR_DELIVERY/DELIVERED/CANCELLED` | `CREATED/PREPARING/COMPLETED/CANCELLED` |
| 采购清单 | 无 | `family_shopping_items` |

**关键点：旧 `Order`、`OrderItem`、`OrderService`、`OrderController`、`ChatTools` 一行未改**，旧 `/api/orders` 路由仍然存在（实测对 CUSTOMER 角色返回 403，与改动前一致）。新模块是纯增量新增，无任何数据迁移风险。

## 3. 新增文件清单

| 层 | 文件 |
|---|---|
| Entity | `entity/FamilyOrder.java`、`entity/FamilyOrderItem.java`、`entity/FamilyShoppingItem.java` |
| Repository | `repository/FamilyOrderRepository.java`、`repository/FamilyOrderItemRepository.java`、`repository/FamilyShoppingItemRepository.java` |
| DTO | `dto/CreateFamilyOrderRequest.java`、`dto/FamilyOrderResponse.java`、`dto/UpdateCookStatusRequest.java`、`dto/AssignResponsibleRequest.java`、`dto/AddShoppingItemRequest.java`、`dto/ShoppingItemResponse.java` |
| Service | `service/FamilyOrderService.java`、`service/impl/FamilyOrderServiceImpl.java`、`service/ShoppingListService.java`、`service/impl/ShoppingListServiceImpl.java` |
| Controller | `controller/FamilyOrderController.java`、`controller/ShoppingListController.java` |
| Test | `src/test/java/com/fooddelivery/service/FamilyOrderServiceImplTest.java`（23 例）、`ShoppingListServiceImplTest.java`（6 例） |

**修改的既有文件：无。** 未改动认证、家庭、菜品、购物车模块，也未改动旧订单模块。

## 4. 数据表结构

### 4.1 `family_orders`

| 列 | 类型 | 约束 / 说明 |
|---|---|---|
| `id` | bigint | PK 自增 |
| `family_id` | bigint | not null，家庭隔离键 |
| `user_id` | bigint | not null，下单人 |
| `total_amount` | numeric(12,2) | not null，服务端计算 |
| `item_count` | integer | not null，明细条数 |
| `status` | varchar(20) | not null，`CREATED/PREPARING/COMPLETED/CANCELLED` |
| `meal_period` | varchar(20) | 可空，就餐时段（如 DINNER） |
| `remark` | varchar(500) | 可空，备注 |
| `meal_served` | boolean | not null，开饭状态，独立字段 |
| `meal_served_at` | timestamp | 可空 |
| `cancelled_at` | timestamp | 可空 |
| `cancelled_by` | bigint | 可空，取消人 |
| `created_at` / `updated_at` | timestamp | not null |

### 4.2 `family_order_items`

| 列 | 类型 | 约束 / 说明 |
|---|---|---|
| `id` | bigint | PK 自增 |
| `order_id` | bigint | not null，FK → `family_orders(id)` |
| `dish_id` | bigint | 可空，仅作来源追溯，不参与金额计算 |
| `dish_name_snapshot` | varchar(100) | not null，**下单时刻菜名快照** |
| `unit_price_snapshot` | numeric(10,2) | not null，**下单时刻单价快照** |
| `quantity` | integer | not null |
| `subtotal` | numeric(12,2) | not null，**下单时刻小计快照** |
| `cook_status` | varchar(20) | not null，`PENDING/COOKING/READY/SERVED/BLOCKED` |
| `responsible_user_id` | bigint | 可空，备菜负责人 |
| `blocked_reason` | varchar(200) | 可空，无法备菜的原因 |
| `cook_status_updated_by` | bigint | 可空 |
| `cook_status_updated_at` | timestamp | 可空 |
| `created_at` | timestamp | not null |

### 4.3 `family_shopping_items`

| 列 | 类型 | 约束 / 说明 |
|---|---|---|
| `id` | bigint | PK 自增 |
| `family_id` | bigint | not null，**家庭所有，非个人私有** |
| `name` | varchar(100) | not null，食材名 |
| `amount` | numeric(10,2) | not null，数量 |
| `unit` | varchar(20) | not null，单位 |
| `order_id` | bigint | 可空，来源订单 |
| `order_item_id` | bigint | 可空，来源订单明细 |
| `added_by` | bigint | not null，登记人 |
| `created_at` | timestamp | not null |

> 注意：采购清单**只有成员显式添加的记录**，系统不会从 `DishIngredient` 自动展开菜谱全部食材。

## 5. 状态设计（两套状态严格分离）

### 5.1 订单整体状态（`FamilyOrder.status`）

| 状态 | 含义 | 进入方式 |
|---|---|---|
| `CREATED` | 已下单，待开始备菜 | 下单成功自动进入 |
| `PREPARING` | 已在备菜 | **仅**通过 `POST /{orderId}/start-preparing` 显式触发 |
| `COMPLETED` | 已完成 | 见 5.4 的待确认说明 |
| `CANCELLED` | 已取消 | 下单人取消 |

**约束**：单道菜的备菜状态变化**绝不**自动改写订单整体状态（自动化测试 `cookStatusChangeDoesNotAlterOrderStatus`、HTTP 断言 A31/A40 均验证）。

### 5.2 单道菜备菜状态（`FamilyOrderItem.cookStatus`）

```
PENDING ──→ COOKING ──→ READY ──→ SERVED
   │           │          │
   └───────────┴──────────┴──────→ BLOCKED
BLOCKED ──→ PENDING / COOKING / READY   （恢复）
SERVED  ──→ SERVED                       （终态，不可再流转）
```

- 非法流转返回 400（如 `BLOCKED → SERVED`、`READY → COOKING`）。
- 置为 `BLOCKED` 必须带 `blockedReason`，否则 400。
- 恢复为非 `BLOCKED` 时自动清空 `blockedReason`。
- 每次变更记录 `cookStatusUpdatedBy` 与 `cookStatusUpdatedAt`。

代码位置：`FamilyOrderItem.CookStatus.canTransitionTo`。

### 5.3 开饭状态

`mealServed`（boolean）+ `mealServedAt` 独立存储，通过 `POST /{orderId}/meal-served` 触发，**不改变**订单整体状态，也**不推导**自任何菜的状态。实测 A41~A44 验证。

### 5.4 待确认事项（不擅自认定）

- `COMPLETED` 的触发规则需求标注为待确认。当前代码接受该状态值，但**未实现自动进入 `COMPLETED` 的接口**，也没有「全部菜 SERVED 即完成」的隐藏推导。是否需要 `POST /{orderId}/complete` 或自动完成，需人工决策。
- 管理员代他人取消（U-002/U-003）：未实现。

## 6. 接口清单

所有接口需 `Authorization: Bearer <token>` + `X-Family-Id` 请求头。缺失 `X-Family-Id` 返回 403（`requireMembership(null)` 先拒）。

### 6.1 订单

| 方法 | 路径 | 请求体 | 成功码 | 说明 |
|---|---|---|---|---|
| POST | `/api/family-orders` | `{mealPeriod?, remark?}` | 201 | 提交当前用户当前家庭购物车创建订单 |
| GET | `/api/family-orders` | 无 | 200 | 当前家庭订单列表（按创建时间倒序） |
| GET | `/api/family-orders/{orderId}` | 无 | 200 | 订单详情，含明细快照 |
| POST | `/api/family-orders/{orderId}/cancel` | 无 | 200 | 下单人取消未开始备菜的订单 |
| POST | `/api/family-orders/{orderId}/start-preparing` | 无 | 200 | 显式开始备菜 |
| POST | `/api/family-orders/{orderId}/meal-served` | 无 | 200 | 标记开饭 |
| PATCH | `/api/family-orders/items/{orderItemId}/cook-status` | `{status, blockedReason?}` | 200 | 单菜备菜状态 |
| PATCH | `/api/family-orders/items/{orderItemId}/responsible` | `{responsibleUserId}` | 200 | 指定备菜负责人 |

> 所有修改类接口返回**变更后的完整订单响应**，便于前端一次性刷新。

### 6.2 采购清单

| 方法 | 路径 | 请求体 | 成功码 | 说明 |
|---|---|---|---|---|
| GET | `/api/shopping-list` | 无 | 200 | 查看家庭待购清单 |
| POST | `/api/shopping-list/items` | `{name, amount, unit, orderId?, orderItemId?}` | 201 | 登记实际缺少的食材 |

`amount` 必须 > 0（`@DecimalMin("0.01")`），`name`/`unit` 非空且限长。

### 6.3 响应示例（订单详情）

```json
{
  "id": 1,
  "familyId": 2,
  "userId": 3,
  "userName": "Alice",
  "totalAmount": 88.00,
  "itemCount": 2,
  "status": "PREPARING",
  "mealPeriod": "DINNER",
  "remark": "less spicy",
  "mealServed": true,
  "mealServedAt": "2026-11-12T23:38:00",
  "cancelledAt": null,
  "cancelledBy": null,
  "createdAt": "2026-11-12T23:37:00",
  "updatedAt": "2026-11-12T23:38:00",
  "items": [
    {
      "id": 1,
      "dishId": 5,
      "dishName": "红烧肉",
      "unitPrice": 38.00,
      "quantity": 2,
      "subtotal": 76.00,
      "cookStatus": "READY",
      "responsibleUserId": 3,
      "responsibleUserName": "Alice",
      "blockedReason": null,
      "cookStatusUpdatedBy": 3,
      "cookStatusUpdatedAt": "2026-11-12T23:37:30"
    }
  ]
}
```

## 7. 事务边界与回滚机制

`FamilyOrderServiceImpl.createOrder` 标注 `@Transactional`（类级 + 方法级），单个事务内完成：

```
1. requireMembership(familyId)                    ← 非成员直接抛异常
2. 读取当前用户当前家庭购物车                        ← 空则 400
3. 重新查询每个 Dish：归属当前家庭 + 仍上架          ← 任一失败 400
4. 服务端计算 subtotal / totalAmount（BigDecimal）
5. 保存 family_orders + family_order_items（快照）
6. deleteByUserIdAndFamilyId(userId, familyId)     ← 清空该用户该家庭购物车
```

**失败回滚**：第 3、4 步任何异常抛出后，整个事务回滚，第 5 步的订单不落库、第 6 步的购物车不清空。这是本次最关键的事务要求。

实测证据：
- 自动化测试 `failedOrderKeepsCartIntact`：下架菜品后下单抛 400，购物车仍有 2 条，订单表为空。
- HTTP A82~A85：下架后下单 400 → 购物车 `itemCount` 仍为 1 → 订单列表仍为 1 条（未多出订单）。

**只清空自己**：`deleteByUserIdAndFamilyId(userId, familyId)` 双条件删除，同家庭其它成员、同用户其它家庭的购物车不受影响（测试 `successfulOrderClearsOnlyCurrentUserCurrentFamilyCart`、HTTP A28~A30）。

## 8. 价格快照机制

1. 请求 DTO `CreateFamilyOrderRequest` **只有 `mealPeriod` 与 `remark`**，结构上不存在价格字段。
2. `unitPriceSnapshot` 来自 `Dish.getPrice()`（数据库当前值），`subtotal = unitPrice × quantity`，`totalAmount` 逐项累加，全部 `BigDecimal`。
3. 快照写入 `family_order_items` 独立列，**不引用** `Dish.price`。
4. 后续菜品改名或改价，历史订单不变。测试 `orderItemStoresSnapshotIndependentOfLaterDishChanges`：下单后把菜名改为「改名后的红烧肉」、价格改为 99.00，重查订单仍是「红烧肉」/ 38.00 / 38.00。

## 9. 权限与隔离

| 校验 | 实现 |
|---|---|
| 必须是家庭成员 | `CurrentUserService.requireMembership(familyId)` → 非成员 403 |
| 订单只能按家庭查 | `FamilyOrderRepository.findByIdAndFamilyId` → 跨家庭 404 |
| 订单列表只能看本家庭 | `findByFamilyIdOrderByCreatedAtDesc` |
| 明细只能在本家庭内操作 | `FamilyOrderItemRepository.findByIdAndOrderFamilyId` → 跨家庭 404 |
| 取消仅限下单人 | `order.getUserId().equals(currentUserId)` → 否则 403 |
| 备菜负责人必须是家庭成员 | `familyMemberRepository.existsByFamilyIdAndUserId` → 否则 403 |
| 采购清单属家庭 | `findByFamilyIdOrderByCreatedAtDesc`，家庭间隔离 |
| 采购清单关联订单必须同家庭 | `findByIdAndFamilyId` / `findByIdAndOrderFamilyId` → 否则 400 |

`userId` 一律取自 `CurrentUserService.getCurrentUserId()`（JWT 解析），不接受请求参数传入。

**仓储层设计上不提供任何仅按 `orderId` 或无家庭条件的查询方法**，与购物车模块保持一致的防越权思路。

## 10. 测试结果

### 10.1 自动化测试（真实执行）

```bash
# 必须先注入环境变量（application.properties 中 jwt.secret=${JWT_SECRET}、gemini.api.key=${GEMINI_API_KEY}）
export JWT_SECRET=...   # 本机实际使用 order-verify-secret-key-for-local-testing-0123456789
export GEMINI_API_KEY=...   # 本机实际使用 test-key
mvn clean test
```

```
Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

明细：Auth 1、Cart 18、Dish 13、Family 5、**FamilyOrder 23**、**ShoppingList 6**。

> **补充说明（如实记录）**：未注入环境变量时，`mvn clean test` 会因 `Could not resolve placeholder 'JWT_SECRET'` 导致 Spring 上下文加载失败，66 个用例全部报 `ApplicationContext failure threshold (1) exceeded`。这属于原项目既有约束，**不是本模块代码问题**；注入上述环境变量后 66/66 通过。

新模块 29 个用例覆盖任务书要求的 10 类场景：

| 要求场景 | 覆盖用例 |
|---|---|
| 正常提交购物车创建订单 | `createOrderFromCartSuccessfully` |
| 订单金额由服务端计算 | `orderAmountIsCalculatedByServer` |
| 明细保存名称/单价/数量快照 | `orderItemStoresSnapshotIndependentOfLaterDishChanges` |
| 提交前下架不能下单 | `unavailableDishCannotBeOrdered` |
| 购物车为空不能下单 | `emptyCartCannotCreateOrder` |
| 只清空当前用户当前家庭购物车 | `successfulOrderClearsOnlyCurrentUserCurrentFamilyCart` |
| 下单失败购物车保留 | `failedOrderKeepsCartIntact` |
| 不同用户/家庭订单隔离 | `ordersAreIsolatedBetweenUsersAndFamilies`、`crossFamilyOrderItemCannotBeModified` |
| 普通成员不能越权修改 | `nonMemberCannotViewOrOperateFamilyOrders`、`nonOwnerNonAdminCannotCancelOthersOrder` |
| 单菜状态不影响其他菜 | `cookStatusChangeDoesNotAlterOrderStatus` |
| 只添加实际缺少的食材 | `addOnlyActuallyMissingIngredient`、`shoppingListIsFamilyScopedNotPersonal` |

补充覆盖：非法状态流转、`BLOCKED` 必填原因、完整流转到 `SERVED`、开饭独立、负责人必须是成员、跨家庭采购关联被拒。

### 10.2 实际 HTTP 端到端测试（真实执行）

方式：`java -jar` 启动（H2 文件库 `AUTO_SERVER=TRUE`），`curl.exe` 真实请求，脚本 `target/order-http-verify.ps1`。

完整流程：注册/登录 → 创建 3 个家庭 → 创建 3 个菜品 → 加入购物车 → 提交订单 → 查看详情/列表 → 逐道菜改备菜状态 → 开始备菜 → 标记开饭 → 指定负责人 → 登记采购 → 隔离与下架校验。

```
=== RESULT: PASS=85 FAIL=0 ===
```

关键断言（节选）：

| 编号 | 断言 | 结果 |
|---|---|---|
| A20 | 下单 201，totalAmount=88.00，itemCount=2，status=CREATED | PASS |
| A24 | 明细快照：菜名与创建时一致、单价 38.00、小计 76.00 | PASS |
| A28~A30 | 下单后 Alice 该家庭购物车 itemCount=0 | PASS |
| A31 | 空购物车下单 400 | PASS |
| A36~A39 | 单菜 PENDING→COOKING→READY，订单整体仍 CREATED，另一道菜仍 PENDING | PASS |
| A40 | 另一道菜置 BLOCKED，原因保存，第一道菜不受影响 | PASS |
| A41 | 非法流转 BLOCKED→SERVED 400 | PASS |
| A42 | 非法流转 READY→COOKING 400 | PASS |
| A43~A45 | 未知状态 400、BLOCKED 无原因 400 | PASS |
| A46~A49 | start-preparing 后订单 PREPARING，而菜仍是 READY（互不推导） | PASS |
| A50~A53 | meal-served 后 mealServed=true、有 mealServedAt、订单仍 PREPARING | PASS |
| A54 | 指定非成员为负责人 403 | PASS |
| A59~A61 | Eve（非成员）查家庭 A 订单 403；家庭 B 查家庭 A 订单 404 | PASS |
| A62~A68 | 采购清单：添加 201、只 1 条、Eve 403、跨家庭关联 400 | PASS |
| A69~A71 | 下架菜品加入购物车 400 | PASS |
| A72~A78 | 下架菜品下单 400，购物车保留 1 条，订单数未增加 | PASS |
| A79~A85 | 重新上架后下单成功，只清空该家庭购物车，家庭 A 共 2 单 | PASS |

**说明**：为验证「同一家庭多成员」协作，脚本中 Bob 的成员关系未通过邀请码建立（M-03 未实现）。本轮 HTTP 的协作类断言（A31~A61）由 Alice 作为家庭成员完成，跨用户隔离由 Eve（独立家庭）验证。这属于**测试准备方式**，不代表邀请码功能已完成。

### 10.3 未执行的测试

- 未做并发下单测试（当前单用户串行流程）。
- 未做 `COMPLETED` 状态相关测试（该流转规则待确认，未实现）。
- 未做旧 `/api/orders` 管理员流程回归（本轮未改动该模块，且无相应测试数据准备）。

## 11. 待确认与未完成事项

| 编号 | 事项 | 当前处理 |
|---|---|---|
| U-101 | `COMPLETED` 何时进入（手动接口？全菜 SERVED 自动？） | 需求待确认，**未实现自动完成**，仅状态值可写 |
| U-102 | 管理员/家庭 ADMIN 是否有权代成员取消订单 | 未实现，仅下单人可取消 |
| U-103 | 取消订单后已登记的采购清单如何处理 | 未实现联动 |
| U-104 | 采购清单已购买标记、误加删除与纠正 | 未实现 |
| U-105 | 采购清单是否需要去重/合并同名食材 | 未实现，每次登记独立一行 |
| U-106 | 备菜负责人是否限制「只有负责人才能改该菜状态」 | 当前允许任意家庭成员改，负责人仅作标记 |