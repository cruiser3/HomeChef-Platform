# 个人独立购物车模块（CART_MODULE）

**范围：** 记录**已经实际实现并验证**的个人独立购物车模块（对应需求中的购物车部分，本轮不含下单、结算、支付、备菜协作）。
**依据：** 当前源码 + 本机实际执行的 `mvn clean test` 与启动后真实 HTTP 实测结果。自动化测试与实际 HTTP 测试分开记录，未执行项一律标注「未执行」。

---

## 1. 新增文件

| 层 | 文件 |
|---|---|
| Entity | `entity/CartItem.java` |
| Repository | `repository/CartItemRepository.java` |
| DTO | `dto/AddCartItemRequest.java` |
| DTO | `dto/UpdateCartItemRequest.java` |
| DTO | `dto/CartResponse.java`（含内部类 `CartItemResponse`） |
| Service | `service/CartService.java` |
| Service | `service/impl/CartServiceImpl.java` |
| Controller | `controller/CartController.java` |
| Test | `src/test/java/com/fooddelivery/service/CartServiceImplTest.java` |

**未修改任何既有 Java 文件**：认证（`AuthController`/`AuthService`/`JwtUtil`/`JwtAuthFilter`/`SecurityConfig`）、家庭模块（`FamilyController`/`FamilyService`/`FamilyMember`）、菜品模块（`DishController`/`DishService`/`Dish`/`DishRepository`）、`CurrentUserService`、`GlobalExceptionHandler`、旧 `MenuItem`/`Order` 全部未改动。

购物车只依赖既有能力完成三件事：
1. 复用 `CurrentUserService.getCurrentUserId()` 取当前用户；
2. 复用 `CurrentUserService.requireMembership(familyId)` 做家庭成员校验；
3. 复用 `DishRepository.findByIdAndFamilyId(...)` 与 `Dish.isAvailable` 做菜品校验和价格读取。

### 1.1 与旧 `MenuItem` 的关系

购物车**不读取 `MenuItem`**，唯一菜品来源是家庭菜品库 `Dish`。`CartItem` 只存 `dishId`，展示时回查 `Dish` 取当前价格与上下架状态，因此上架/改价不需要同步购物车行数据。

---

## 2. 数据结构

### 2.1 `cart_items`

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | bigint | PK，自增 | 购物车项目 ID |
| `user_id` | bigint | not null | 购物车归属用户，**隔离依据之一** |
| `family_id` | bigint | not null | 购物车所属家庭，**隔离依据之一** |
| `dish_id` | bigint | not null | 关联 `Dish.id`，不存价格 |
| `quantity` | integer | not null | 购买数量，业务约束 1~99 |
| `created_at` | timestamp | not null，不可更新 | 用于购物车条目稳定排序 |
| `updated_at` | timestamp | not null | 最近修改时间 |

唯一约束：

```
uk_cart_item_user_family_dish UNIQUE (user_id, family_id, dish_id)
```

该约束保证「同一用户在**同一家庭**中的**同一菜品**只有一条记录」，重复添加时合并数量，同时不影响其他用户或其他家庭的同名菜品记录。

未建立外键约束（与项目既有 `FamilyMember` 等实体风格一致，均使用裸 ID 关联并在服务层校验）。

### 2.2 与其他表的关系

| 关系 | 实现方式 |
|---|---|
| `user_id` → `users.id` | 服务层通过 `CurrentUserService` 保证是登录用户 |
| `family_id` → `families.id` | 服务层通过 `requireMembership` 保证用户属于该家庭 |
| `dish_id` → `dishes.id` | 服务层通过 `findByIdAndFamilyId` + `isAvailable` 校验 |

---

## 3. 接口清单

所有接口都要求 `Authorization: Bearer <token>` 与 `X-Family-Id: <familyId>` 请求头。缺少 `X-Family-Id` 返回 400。

| 方法 | 路径 | 请求头 | 请求体 | 说明 |
|---|---|---|---|---|
| GET | `/api/cart` | `X-Family-Id` | 无 | 查看当前用户在当前家庭的购物车 |
| POST | `/api/cart/items` | `X-Family-Id` | `AddCartItemRequest` | 添加菜品（重复菜品合并数量） |
| PATCH | `/api/cart/items/{itemId}` | `X-Family-Id` | `UpdateCartItemRequest` | 修改购物车某项数量 |
| DELETE | `/api/cart/items/{itemId}` | `X-Family-Id` | 无 | 删除购物车某项 |
| DELETE | `/api/cart` | `X-Family-Id` | 无 | 清空当前用户在当前家庭的购物车 |

所有接口（含写操作）统一返回**变更后的完整购物车** `CartResponse`，前端一次请求即可拿到最新状态，避免额外再查一次。

### 3.1 `AddCartItemRequest`

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `dishId` | Long | 必填 | 菜品 ID |
| `quantity` | Integer | `@Min(1)` `@Max(99)` | 本次添加数量 |

**请求体中没有价格字段**，价格只从服务端 `Dish.price` 读取，从结构上杜绝前端传价。

### 3.2 `UpdateCartItemRequest`

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `quantity` | Integer | `@Min(1)` `@Max(99)` | 修改后的目标数量（覆盖写，不是增量） |

---

## 4. 响应结构 `CartResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | Long | 当前用户 ID |
| `familyId` | Long | 当前家庭 ID |
| `itemCount` | int | 购物车条目数（不同菜品条数，不是数量总和） |
| `totalAmount` | BigDecimal | 全部条目的 `subtotal` 之和 |
| `items` | List<CartItemResponse> | 条目列表，按 `createdAt` 升序 |

`CartItemResponse`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 购物车项目 ID，用于修改/删除 |
| `dishId` | Long | 菜品 ID |
| `dishName` | String | 菜品名称（实时取自 `Dish`） |
| `imageUrl` | String | 菜品图片（实时取自 `Dish`） |
| `unitPrice` | BigDecimal | 单价，实时取自 `Dish.price` |
| `quantity` | Integer | 当前数量 |
| `subtotal` | BigDecimal | `unitPrice × quantity` |
| `dishAvailable` | boolean | 当前上下架状态，菜品下架后条目仍在购物车并显示 `false` |
| `createdAt` | LocalDateTime | 加入时间 |

金额全程 `BigDecimal`，未出现 `double`/`float`。

---

## 5. 个人隔离机制（核心）

### 5.1 双重隔离的实现

隔离依赖三个层次的组合，缺一不可：

**第一层：数据模型同时存 `user_id` 和 `family_id`。**
`CartItem` 不是「按家庭共享的购物车」，而是「用户 × 家庭」二维归属。购物车行上同时写死了两个维度。

**第二层：仓储查询一律带 `userId + familyId` 两个条件。**
`CartItemRepository` 不对外提供「按 familyId 单独查购物车」的方法，所有方法都强制带 `userId`：

| 仓储方法 | 用途 | 隔离作用 |
|---|---|---|
| `findByUserIdAndFamilyIdOrderByCreatedAtAsc(userId, familyId)` | 构建购物车视图 | 只返回当前用户自己的行 |
| `findByIdAndUserIdAndFamilyId(id, userId, familyId)` | 按 itemId 定位 | 即使拿到别人的 itemId 也查不到，等价 404 |
| `findByUserIdAndFamilyIdAndDishId(userId, familyId, dishId)` | 重复添加时合并 | 不会与其他用户的同菜品行合并 |
| `deleteByUserIdAndFamilyId(userId, familyId)` | 清空购物车 | 只清自己的行，不影响同家庭成员 |

**第三层：服务层统一从 `CurrentUserService.getCurrentUserId()` 取 userId。**
userId **从不来自请求参数或请求体**，只来自已校验的 JWT。因此伪造参数无法冒充他人。

三层叠加后可证明：

- 同一家庭不同用户：`family_id` 相同但 `user_id` 不同 → 查询结果互不可见；唯一约束允许两个用户各持一份同菜品记录。
- 同一用户不同家庭：`user_id` 相同但 `family_id` 不同 → 查询结果互不可见；唯一约束允许同一用户在不同家庭各持一份同菜品记录。
- 跨家庭访问：`requireMembership(familyId)` 直接拦截，返回 403。
- 越权操作他人条目：`findByIdAndUserIdAndFamilyId` 查不到，返回 404。

### 5.2 隔离与「个人订单」的关系

购物车按 `(user_id, family_id)` 归属，因此后续提交订单时可直接以当前用户自己的购物车为数据源，生成**该用户自己的订单**，而不是家庭共享订单。这是本模块与后续订单模块的衔接约定，本轮未实现订单。

---

## 6. 菜品与数量校验

### 6.1 校验顺序（添加 / 修改均相同）

1. `requireMembership(familyId)` — 当前用户必须属于该家庭，否则 403。
2. 参数校验 — `dishId` / `quantity` 必填，`quantity` 由 Bean Validation 限制在 1~99，越界 400。
3. `findByIdAndFamilyId(dishId, familyId)` — 菜品必须存在且属于当前家庭，否则 400「Dish does not belong to the current family」。
4. `dish.isAvailable()` — 下架菜品禁止加入/修改，否则 400「Dish is not available for ordering」。

修改数量时同样完整执行 3、4 两步，即**状态变化后不能绕过检查**。

### 6.2 数量规则

| 场景 | 规则 |
|---|---|
| 数量范围 | 1 ~ 99（`MIN_QUANTITY=1`，`MAX_QUANTITY=99`） |
| 添加已存在菜品 | 合并为 `原数量 + 本次数量`，合并结果同样要 ≤ 99，否则 400 |
| 修改数量 | 覆盖为请求值，不再累加 |
| 数量为 0 或负数 | 400。删除应使用 DELETE 接口，而不是把数量改成 0 |

### 6.3 价格规则

- 请求 DTO 无价格字段，`AddCartItemRequest` / `UpdateCartItemRequest` 只含 `dishId` 与 `quantity`。
- `unitPrice` 与 `subtotal` 在 `buildCart` 中由 `dish.getPrice()` 实时计算，`Dish` 改价后购物车立即体现新价。
- 菜品被删除时，`buildCart` 通过 `filter(item -> dishes.containsKey(item.getDishId()))` 跳过无对应菜品的行，保证响应不出现悬空引用。

### 6.4 错误码约定

| 场景 | HTTP 状态 | 异常类型 |
|---|---|---|
| 缺少 `X-Family-Id` | 400 | `BadRequestException` |
| 数量越界 / 缺少必填 | 400 | Bean Validation → `GlobalExceptionHandler` |
| 非家庭成员 | 403 | `CurrentUserService.requireMembership` |
| 菜品不属于当前家庭 | 400 | `BadRequestException` |
| 菜品已下架 | 400 | `BadRequestException` |
| 购物车条目不存在或不属于当前用户 | 404 | `ResourceNotFoundException` |

---

## 7. 自动化测试（真实执行）

命令（需先设置环境变量，见第 9 节）：

```
mvn clean test
```

真实结果：

```
Tests run: 1  ... AuthServiceRegisterTest
Tests run: 18 ... CartServiceImplTest
Tests run: 13 ... DishServiceImplTest
Tests run: 5  ... FamilyServiceImplTest
Tests run: 37  Failures: 0  Errors: 0  Skipped: 0
BUILD SUCCESS
```

`CartServiceImplTest` 共 18 个用例，覆盖：添加成功、查看购物车、修改数量、删除条目、清空购物车、同一家庭不同用户隔离、同一用户不同家庭隔离、非家庭成员拒绝、下架菜品拒绝（添加与修改）、其他家庭菜品拒绝、非法数量拒绝、重复添加合并及上限、价格始终取自菜品。

---

## 8. 真实 HTTP 测试（已执行）

在 `java -jar` 启动的真实服务（端口 8080）上用 `curl.exe` 实测，共 **50** 项断言，全部通过。为区分自动化测试与手工测试，下表只记录 HTTP 结果。

### 8.1 主场景（30/30 通过）

两个用户（Alice、Bob）与三个家庭（AliceFamilyA / AliceFamilyB / BobFamilyC），四个菜品。

| 编号 | 场景 | 期望 | 实际 |
|---|---|---|---|
| A1 | Alice 在家庭A 添加菜品 qty2 | 200 | 200 |
| A2 | 金额由服务端计算 | 76.00 | 76.00 |
| A3 | 重复添加同菜品合并数量 | 3 | 3 |
| A4 | 合并后金额 | 114.00 | 114.00 |
| A5 | 查看购物车条目数 | 1 | 1 |
| A6 | 返回 familyId 正确 | 家庭A | 家庭A |
| A7 | 家庭B 添加金额 | 60.00 | 60.00 |
| A8 | 家庭B 操作后家庭A 金额不变 | 114.00 | 114.00 |
| A9 | 家庭A 条目数不变 | 1 | 1 |
| A10 | Bob 在自己家庭初始为空 | 0 | 0 |
| A11 | Bob 在自己家庭添加 | 200 | 200 |
| A12 | Bob 金额 | 15.00 | 15.00 |
| A13 | Bob 跨家庭 GET 家庭A | 403 | 403 |
| A14 | Bob 跨家庭 添加 家庭A | 403 | 403 |
| A15 | Alice 跨家庭 GET 家庭C | 403 | 403 |
| A16 | 家庭B 菜品加入 家庭A | 400 | 400 |
| A17 | 下架菜品添加 | 400 | 400 |
| A18 | 下架菜品修改数量 | 400 | 400 |
| A19 | 下架后购物车保留条目并标记不可用 | false | false |
| A20 | 数量 0 | 400 | 400 |
| A21 | 数量 -2 | 400 | 400 |
| A22 | 数量 100 | 400 | 400 |
| A23 | 非法请求后数量未被改动 | 3 | 3 |
| A24 | 改为 4 后金额 | 152.00 | 152.00 |
| A25 | Bob 删除自己条目 | 0 | 0 |
| A26 | Alice 清空家庭A | 0 | 0 |
| A27 | Alice 家庭B 未受影响 | 1 | 1 |
| A28 | 缺少 X-Family-Id | 400 | 400 |
| A29 | 删除不存在条目 | 404 | 404 |

### 8.2 同一家庭不同用户隔离（20/20 通过）

Alice 与 Bob 同属家庭A（因邀请码 M-03 未实现，Bob 的成员关系通过数据库脚本写入 `family_members`，仅用于测试准备，未改动任何业务代码），两者在**同一家庭**下添加**同一菜品**：

| 编号 | 场景 | 期望 | 实际 |
|---|---|---|---|
| B1 / B2 | 双方先清空共享家庭购物车 | 0 | 0 |
| B3 | Alice 添加同菜品 qty2 | 200 | 200 |
| B4 | Alice 金额 | 76.00 | 76.00 |
| B5 | 响应含 userId | true | true |
| B6 | Bob 在同家庭添加同菜品 qty1 | 200 | 200 |
| B7 | Bob 金额 38.00（**未与 Alice 合并**） | 38.00 | 38.00 |
| B8 | Bob 只看到自己 1 条 | 1 | 1 |
| B9 | Bob 金额仍 38.00 | 38.00 | 38.00 |
| B10 | Alice 仍为 76.00 | 76.00 | 76.00 |
| B11 | Alice 条目数 | 1 | 1 |
| B12 | Alice 数量 | 2 | 2 |
| B13 | Bob 数量 | 1 | 1 |
| B14 | 两人 itemId 不同 | true | true |
| B15 | Alice 删除自己唯一条目后自身为 0 | 0 | 0 |
| B16 | Bob 条目不受影响 | 1 | 1 |
| B17 | Bob 修改 Alice 的 itemId | 404 | 404 |
| B18 | Bob 删除 Alice 的 itemId | 404 | 404 |
| B19 | Alice 清空自身购物车 | 0 | 0 |
| B20 | Bob 条目仍未受影响 | 1 | 1 |

结论：同一家庭内，两人添加同一菜品产生**两条独立记录**，数量、金额、删除、清空互相不影响；越权操作他人条目返回 404。

---

## 9. 运行与验证注意事项

启动或运行测试前必须设置环境变量（项目 `application.properties` 中 `${JWT_SECRET}`、`${GEMINI_API_KEY}` 无默认值，缺失会导致 Spring 上下文加载失败，从而所有测试报错）：

```
JWT_SECRET=<至少 32 字节>
GEMINI_API_KEY=<任意占位值即可>
```

PowerShell 下若直接 `mvn clean test` 未设置这两个变量，会出现 `Could not resolve placeholder 'JWT_SECRET'` 并导致全部测试 `ApplicationContext failure threshold (1) exceeded`，这是环境问题而非测试失败。

---

## 10. 尚未解决的问题

1. **跨用户加入同一家庭缺少正式入口**：M-03（邀请码加入家庭）未实现，本节 8.2 的「同一家庭不同用户」真实验证使用了数据库脚本写入 `family_members` 作为测试准备。业务代码未改动，但这条链路的端到端完整验证依赖 M-03 完成后才能完全通过 HTTP 自然完成。
2. **购物车未做依赖与订单衔接**：本轮按任务要求不实现提交购物车、生成订单、结算、支付、备菜协作。`CartItem` 与 `Order` 之间目前无任何代码关联。
3. **菜品删除后的购物车行**：`buildCart` 会跳过无对应菜品的行，但这些行仍保留在 `cart_items` 表中，未做自动清理。若后续需要严格一致性，可在菜品删除时级联删除购物车行，本轮未实现（避免修改菜品模块）。
4. **数量上限 99 为自定规则**：需求文档未明确上限，采用简单一致的 1~99，已记录于此。
5. **旧 `MenuItem` 仍存在**：购物车完全使用 `Dish`，未触碰旧 `MenuItem`；旧结构的清理应在订单模块改造时统一处理。