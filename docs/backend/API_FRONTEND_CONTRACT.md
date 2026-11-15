# 前后端交接文档（API_FRONTEND_CONTRACT）

> **文档版本**：V1.0（2026-11-13）
> **用途**：为即将开发的 Vue 前端提供准确的前后端对接说明。
> **依据**：本文所有接口、字段、状态码均来自当前实际源码（`controller/`、`dto/`、`entity/`、`service/`、`security/`、`exception/`），不是根据类名猜测。
> **事实口径**：「已实现」表示对应 Controller/Service/DTO/Entity 已存在且逻辑可读；「是否通过测试」以 `BACKEND_PROGRESS.md` 第 4 节实测结果为准。前端页面与技术选型为**设计建议（待最终确认）**，不代表现状。
> **配套文档**：页面与技术方案见 `docs/frontend/FRONTEND_DESIGN.md`。

---

## 0. 模块总览：新家庭业务 vs 旧外卖遗留

| 类别 | 路径前缀 | 说明 | 前端是否使用 |
|---|---|---|---|
| 认证 | `/api/auth` | 注册、登录、签发 JWT | **使用** |
| 家庭 | `/api/families` | 创建家庭、我的家庭列表、成员查询 | **使用** |
| 菜品 | `/api/dishes` | 菜品库、分类、食材、步骤、上下架 | **使用** |
| 购物车 | `/api/cart` | 个人独立购物车（用户+家庭隔离） | **使用** |
| 家庭订单/备菜 | `/api/family-orders` | 下单、订单、备菜协作、开饭 | **使用** |
| 采购清单 | `/api/shopping-list` | 记录实际缺少食材（最小范围） | **使用** |
| 旧外卖 | `/api/restaurants`、`/api/menu-items`、`/api/orders`、`/api/chat` | 原项目遗留，无家庭隔离 | **不使用**（见第 8 节） |

---

## 1. 全局约定（前端必须先理解）

### 1.1 基础地址
- 无 `context-path`；端口由 `PORT` 环境变量控制，默认 `8080`。
- 本地基础地址：`http://localhost:8080`；局域网手机访问：`http://<电脑局域网IP>:8080`。
- 数据库为 H2 内存库（`ddl-auto=update`），**重启即清空数据**。

### 1.2 认证方式（JWT）
- 注册/登录成功返回 `token`，之后所有受保护请求必须带请求头：
  ```
  Authorization: Bearer <token>
  ```
- JWT：`subject` 为邮箱，有效期 24 小时（`jwt.expiration=86400000`），**不含角色 claim**。
- 放行（无需登录）路径只有：`/`、`/api/auth/**`、`/h2-console/**`，其余全部需登录。
- 启动后端需环境变量：`JWT_SECRET`、`GEMINI_API_KEY`（缺失会导致上下文加载失败，见 `BACKEND_PROGRESS.md` G-03）。

### 1.3 家庭隔离请求头 `X-Family-Id`
- 所有家庭业务接口（菜品/购物车/家庭订单/采购清单，及 `GET /api/families/current/members`）必须带：
  ```
  X-Family-Id: <familyId>
  ```
- 缺失该头 → `400`，message `X-Family-Id header is required`。
- 后端在 Service 层校验当前登录用户是否为该家庭成员，非成员 → `403`，message `You are not a member of this family`。
- **前端不得只靠隐藏按钮做隔离**：必须始终携带当前家庭 `X-Family-Id`，并妥善处理 403。

### 1.4 统一错误响应结构（`ErrorResponse`）
所有非 2xx 响应体统一为：
```json
{
  "timestamp": "2026-11-13T10:20:30.123",
  "status": 400,
  "message": "具体错误信息",
  "path": "/api/dishes",
  "fieldErrors": { "name": "Dish name is required" }
}
```
- `fieldErrors` **仅参数校验失败时出现**，其余情况为 `null`。
- 校验失败时 `message = "Validation failed"`，字段级原因在 `fieldErrors`。

### 1.5 HTTP 状态码速查
| 状态码 | 触发来源 | 前端处理建议 |
|---|---|---|
| 400 | `BadRequestException` / 参数校验 | 展示 `message`；表单错误读 `fieldErrors` |
| 401 | 未认证 / 登录失败 | 未登录统一 `Authentication required` → 跳登录；登录接口失败统一 `Invalid email or password` |
| 403 | `ForbiddenException` | 已登录但无权限（非家庭成员/非创建者或管理员等）→ 提示无权限 |
| 404 | `ResourceNotFoundException` | 资源不存在或不属于当前家庭 |
| 409 | `ConflictException` | 邮箱已注册 |
| 500 | 其他异常兜底 | 统一 `Something went wrong` → 通用错误提示 |

> 注意：未认证访问受保护资源时，`SecurityConfig` 的 `AuthenticationEntryPoint` 返回 **401**。但 `BACKEND_PROGRESS.md` G-01 记录了历史"未认证返回 403"的问题，前端应同时兼容 401/403 的未登录跳转逻辑，并以实测为准。

### 1.6 两套角色，勿混淆
- **系统角色 `User.Role`**：`CUSTOMER` / `RESTAURANT_OWNER` / `ADMIN`。注册固定 `CUSTOMER`，客户端不能指定。**与家庭权限无关**，不要用它判断家庭操作权限。
- **家庭角色 `FamilyMember.FamilyRole`**：`ADMIN` / `MEMBER`。创建家庭者自动成为该家庭 `ADMIN`。判断"能否改菜品/管理家庭"用这个（见 `FamilyResponse.currentUserRole`）。

---

## 2. 认证模块（`/api/auth`，无需登录）

### 2.1 注册
`POST /api/auth/register`

请求体：
```json
{ "name": "张三", "email": "zhang@example.com", "password": "123456" }
```
校验：`name` 必填≤50、`email` 必填且合法、`password` 必填≥6。

响应 `201`：
```json
{ "token": "<jwt>", "email": "zhang@example.com", "name": "张三", "role": "CUSTOMER" }
```
错误：`409`（`Email already registered: ...`）、`400`（校验）。

### 2.2 登录
`POST /api/auth/login`

请求体：
```json
{ "email": "zhang@example.com", "password": "123456" }
```
响应 `200`（结构同注册）。错误：`401`（`Invalid email or password`）、`400`（校验）。

> 实现：`AuthService`（单一 `@Service` 类）。测试：`AuthServiceRegisterTest`、`AuthSecurityIntegrationTest`。

---

## 3. 家庭模块（`/api/families`，需登录；除 `/current/members` 外不需要 `X-Family-Id`）

| 接口 | 说明 |
|---|---|
| `POST /api/families` | 创建家庭，创建者成为 ADMIN |
| `GET /api/families` | 我加入的家庭列表 |
| `GET /api/families/{familyId}` | 我的某家庭详情（非成员 403） |
| `GET /api/families/{familyId}/members` | 成员列表（非成员 403） |
| `GET /api/families/current/members` | 成员列表（改用 `X-Family-Id` 头，与上一条等价） |

### 3.1 创建家庭
`POST /api/families`，请求体：
```json
{ "name": "我家" }
```
响应 `201`（`FamilyResponse`）：
```json
{ "id": 1, "name": "我家", "createdBy": 2, "createdAt": "2026-11-13T10:00:00",
  "currentUserRole": "ADMIN", "memberCount": 1 }
```

### 3.2 我的家庭列表
`GET /api/families` → `200`，返回 `FamilyResponse[]`。`currentUserRole` 为该用户在此家庭角色（`ADMIN`/`MEMBER`）。

### 3.3 家庭详情 / 成员列表
- `GET /api/families/{familyId}` → `200`（`FamilyResponse`）。
- `GET /api/families/{familyId}/members` → `200`（`FamilyMemberResponse[]`）：
```json
[ { "userId": 2, "name": "张三", "role": "ADMIN" } ]
```

> 实现：`FamilyServiceImpl`。测试：`FamilyServiceImplTest`。**未实现邀请码加入、成员移除、改角色。**

---

## 4. 菜品模块（`/api/dishes`，需登录 + `X-Family-Id`）

| 接口 | 权限 | 说明 |
|---|---|---|
| `POST /api/dishes` | 家庭成员 | 创建菜品 |
| `GET /api/dishes` | 家庭成员 | 列表（支持筛选） |
| `GET /api/dishes/{dishId}` | 家庭成员 | 详情（含食材/步骤） |
| `PUT /api/dishes/{dishId}` | 创建者本人 或 家庭 ADMIN | 修改（整体替换食材/步骤） |
| `PATCH /api/dishes/{dishId}/availability?available=true` | 创建者本人 或 家庭 ADMIN | 上/下架 |
| `GET /api/dishes/categories` | 家庭成员 | 分类列表 |
| `POST /api/dishes/categories` | 家庭成员 | 新建分类 |

**没有删除菜品接口**（与 D-007"被引用菜品只能下架"一致）。

### 4.1 创建 / 修改菜品
`POST /api/dishes`、`PUT /api/dishes/{dishId}`，请求体（`DishRequest`）：
```json
{
  "name": "番茄炒蛋",
  "description": "家常菜",
  "imageUrl": "https://...",
  "categoryId": 3,
  "cookingMinutes": 15,
  "price": 18.50,
  "ingredients": [
    { "name": "番茄", "amount": 2.0, "unit": "个" },
    { "name": "鸡蛋", "amount": 3.0, "unit": "个" }
  ],
  "steps": [
    { "stepNo": 1, "content": "打蛋", "imageUrl": null },
    { "stepNo": 2, "content": "下锅翻炒", "imageUrl": null }
  ]
}
```
- `stepNo` 必须从 1 连续递增，否则 `400`（`Step numbers must be continuous starting from 1`）。
- 食材的 `name`/`amount`/`unit` 均必填；`categoryId` 必须是本家庭分类。

响应 `201`（创建）/ `200`（修改），返回 `DishResponse`（见 4.3）。

### 4.2 列表
`GET /api/dishes?categoryId=3&keyword=番茄&available=true`
- 三个 query 参数均可选：`categoryId`、`keyword`（名称模糊，忽略大小写）、`available`。
- 返回 `200`（`DishResponse[]`）。**列表为摘要，不含 `ingredients`/`steps`**。

### 4.3 详情
`GET /api/dishes/{dishId}` → `200`（`DishResponse` 完整结构）：
```json
{
  "id": 10, "familyId": 1, "name": "番茄炒蛋", "description": "家常菜",
  "imageUrl": "https://...", "categoryId": 3, "categoryName": "热菜",
  "cookingMinutes": 15, "price": 18.50, "createdBy": 2, "createdByName": "张三",
  "available": true, "createdAt": "2026-11-13T10:00:00", "updatedAt": "2026-11-13T10:00:00",
  "ingredients": [ { "name": "番茄", "amount": 2.0, "unit": "个" } ],
  "steps": [ { "stepNo": 1, "content": "打蛋", "imageUrl": null } ]
}
```
- `categoryName`/`createdByName` 为后端补充的关联名称，无分类时 `categoryName` 为 `null`。

### 4.4 上/下架
`PATCH /api/dishes/{dishId}/availability?available=false` → `200`（`DishResponse`）。

### 4.5 分类
- `GET /api/dishes/categories` → `200`：`[{ "id":3, "familyId":1, "name":"热菜", "sortNo":0 }]`
- `POST /api/dishes/categories`，请求体 `{ "name": "热菜", "sortNo": 0 }` → `201`。重名 `400`。
- **没有分类修改/删除接口。**

> 实现：`DishServiceImpl`。修改/上下架权限由 `requireModifyPermission` 判定（创建者本人或家庭 ADMIN，否则 `403`）。测试：`DishServiceImplTest`。

---

## 5. 购物车模块（`/api/cart`，需登录 + `X-Family-Id`）

购物车按「用户 + 家庭」隔离，只能操作自己的购物车。

| 接口 | 说明 |
|---|---|
| `GET /api/cart` | 查看我的购物车 |
| `POST /api/cart/items` | 添加菜品（同菜品自动合并数量） |
| `PATCH /api/cart/items/{itemId}` | 修改数量 |
| `DELETE /api/cart/items/{itemId}` | 删除单项 |
| `DELETE /api/cart` | 清空购物车 |

### 5.1 查看购物车
`GET /api/cart` → `200`（`CartResponse`）：
```json
{
  "userId": 2, "familyId": 1, "itemCount": 2, "totalAmount": 37.00,
  "items": [
    { "id": 5, "dishId": 10, "dishName": "番茄炒蛋", "imageUrl": "https://...",
      "unitPrice": 18.50, "quantity": 2, "subtotal": 37.00,
      "dishAvailable": true, "createdAt": "2026-11-13T10:00:00" }
  ]
}
```
- `dishAvailable=false` 表示菜品已下架（前端需提示，下单会被拒绝）。

### 5.2 添加 / 修改数量
`POST /api/cart/items`：
```json
{ "dishId": 10, "quantity": 2 }
```
`PATCH /api/cart/items/{itemId}`：
```json
{ "quantity": 3 }
```
两者返回 `200`（更新后的完整 `CartResponse`）。`quantity` 范围 1~99。

### 5.3 删除 / 清空
`DELETE /api/cart/items/{itemId}`、`DELETE /api/cart` → `200`（`CartResponse`）。

错误：
- 菜品不属于本家庭 → `400`；菜品已下架 → `400`（`Dish is not available for ordering`）。
- 数量越界 → `400`（`Quantity must be between 1 and 99`）。
- 购物车项不存在 → `404`。

> 实现：`CartServiceImpl`。测试：`CartServiceImplTest`（含跨用户/跨家庭隔离）。

---

## 6. 家庭订单 + 备菜协作（`/api/family-orders`，需登录 + `X-Family-Id`）

订单由"用户从自己的购物车提交"创建，成功即清空该用户当前家庭购物车。**订单全家共享**（列表/详情任何成员可见）。

| 接口 | 权限 | 说明 |
|---|---|---|
| `POST /api/family-orders` | 成员 | 从购物车提交订单 |
| `GET /api/family-orders` | 成员 | 订单列表（按创建倒序） |
| `GET /api/family-orders/{orderId}` | 成员 | 订单详情 |
| `POST /api/family-orders/{orderId}/cancel` | 仅下单人 | 取消订单 |
| `POST /api/family-orders/{orderId}/start-preparing` | 成员 | 开始备菜（CREATED→PREPARING） |
| `POST /api/family-orders/{orderId}/meal-served` | 成员 | 标记开饭（仅置 `mealServed=true`，不改状态） |
| `PATCH /api/family-orders/items/{orderItemId}/cook-status` | 成员 | 改单个菜品备菜状态 |
| `PATCH /api/family-orders/items/{orderItemId}/responsible` | 成员 | 指定单菜负责人 |

### 6.1 提交订单
`POST /api/family-orders`，请求体（`CreateFamilyOrderRequest`，**可为空 body**）：
```json
{ "mealPeriod": "晚餐", "remark": "少辣" }
```
响应 `201`（`FamilyOrderResponse`）。错误：购物车为空 `400`；购物车内菜品已下架 `400`（`Dish is no longer available: <name>`）。

### 6.2 订单列表 / 详情
`GET /api/family-orders` → `200`（数组）；`GET /api/family-orders/{orderId}` → `200`：
```json
{
  "id": 1, "familyId": 1, "userId": 2, "userName": "张三",
  "totalAmount": 37.00, "itemCount": 2, "status": "CREATED",
  "mealPeriod": "晚餐", "remark": "少辣",
  "mealServed": false, "mealServedAt": null,
  "cancelledAt": null, "cancelledBy": null,
  "createdAt": "2026-11-13T10:00:00", "updatedAt": "2026-11-13T10:00:00",
  "items": [
    { "id": 11, "dishId": 10, "dishName": "番茄炒蛋", "unitPrice": 18.50,
      "quantity": 2, "subtotal": 37.00, "cookStatus": "PENDING",
      "responsibleUserId": null, "responsibleUserName": null,
      "blockedReason": null, "cookStatusUpdatedBy": null, "cookStatusUpdatedAt": null }
  ]
}
```

### 6.3 取消订单
`POST /api/family-orders/{orderId}/cancel` → `200`。
- 仅下单人可取消，否则 `403`。
- 仅当状态为 `CREATED` 且**所有菜品仍为 `PENDING`** 时可取消，否则 `400`。

### 6.4 状态机（前端按钮可用性依据）

**订单整体状态 `status`**：`CREATED` → `PREPARING`（`start-preparing`）；`CANCELLED`（取消）。`COMPLETED` 枚举已定义但**代码中无触发入口**（见第 9 节）。

**单菜备菜状态 `cookStatus`** 允许转移（`FamilyOrderItem.CookStatus`）：

| 当前 | 允许转到 |
|---|---|
| `PENDING` | `COOKING`、`BLOCKED` |
| `COOKING` | `READY`、`BLOCKED` |
| `READY` | `SERVED`、`BLOCKED` |
| `BLOCKED` | `PENDING`、`COOKING`、`READY` |
| `SERVED` | 终态 |

### 6.5 备菜协作操作
改状态：`PATCH /api/family-orders/items/{orderItemId}/cook-status`：
```json
{ "status": "BLOCKED", "blockedReason": "缺鸡蛋" }
```
- `status` 大小写不敏感、自动 trim；非法值 `400`。
- 非法转移 `400`（`Illegal cook status transition: COOKING -> PENDING`）。
- 目标为 `BLOCKED` 时 `blockedReason` 必填，否则 `400`。
- 已取消订单的菜品不能再改状态 `400`。

指定负责人：`PATCH /api/family-orders/items/{orderItemId}/responsible`：
```json
{ "responsibleUserId": 3 }
```
- 负责人必须是本家庭成员，否则 `403`。

> 实现：`FamilyOrderServiceImpl`。金额由后端用 `BigDecimal` 重算，**前端只展示、不计算**。"食材不足"通过"备菜标记 `BLOCKED` + 采购清单"体现，无独立"食材不足"接口。测试：`FamilyOrderServiceImplTest`。

---

## 7. 家庭采购清单（`/api/shopping-list`，需登录 + `X-Family-Id`）

| 接口 | 权限 | 说明 |
|---|---|---|
| `GET /api/shopping-list` | 成员 | 清单列表（按创建倒序） |
| `POST /api/shopping-list/items` | 成员 | 添加采购项 |

### 7.1 添加采购项
`POST /api/shopping-list/items`，请求体（`AddShoppingItemRequest`）：
```json
{ "name": "鸡蛋", "amount": 3.0, "unit": "个", "orderId": 1, "orderItemId": 11 }
```
- `name`/`amount`/`unit` 必填（`amount > 0`）；`orderId`/`orderItemId` 可选，用于关联"哪笔订单哪个菜品发现缺食材"。
- 若提供，二者必须属于当前家庭且指向同一订单，否则 `400`。

响应 `201`（`ShoppingItemResponse`）：
```json
{ "id": 1, "familyId": 1, "name": "鸡蛋", "amount": 3.0, "unit": "个",
  "orderId": 1, "orderItemId": 11, "addedBy": 2, "createdAt": "2026-11-13T10:00:00" }
```

### 7.2 清单列表
`GET /api/shopping-list` → `200`（`ShoppingItemResponse[]`）。

**没有"标记已购买/完成""删除""修改"接口**（最小范围，见 D-016）。

> 实现：`ShoppingListServiceImpl`。测试：`ShoppingListServiceImplTest`。

---

## 8. 旧外卖模块与已冻结写接口（前端不使用）

以下为原项目遗留，无家庭隔离，与新家庭业务无关，**前端不接入**：

| 路径 | 冻结情况 |
|---|---|
| `POST/PUT/DELETE /api/restaurants` 及 `/{id}` | **写接口已冻结**，恒 `403` |
| `POST/PUT/DELETE /api/menu-items` 及 `/{id}` | **写接口已冻结**，恒 `403` |
| `GET /api/restaurants`、`GET /api/restaurants/{id}` | 可用（旧全局数据） |
| `GET /api/menu-items/{id}`、`GET /api/menu-items/restaurant/{restaurantId}` | 可用（旧数据） |
| `POST /api/orders`、`GET /api/orders/{id}`、`GET /api/orders/my-orders`、`PUT /api/orders/{id}/status` | 旧订单（无家庭隔离；IDOR 已修复，见 D-017） |
| `GET /api/orders/restaurant/{restaurantId}` | **已冻结**，恒 `403` |
| `POST/DELETE /api/chat` | 旧 AI 聊天（依赖 `GEMINI_API_KEY`，按 D-004 暂缓） |

冻结原因（源码注释）：旧 `Restaurant.ownerId` 由客户端任意传入、无归属校验，存在越权风险。

---

## 9. 暂未实现的功能（前端先不做或留占位）

1. **邀请码加入家庭**（无 `join`/`invite` 接口，M-03 未开始）。
2. **家庭成员管理**（移除成员、修改家庭角色）。
3. **菜品物理删除**（无 `DELETE /api/dishes/{id}`）。
4. **菜品分类的修改/删除**。
5. **采购清单的完成/删除/修改**。
6. **订单评论/协作**。
7. **用户个人资料与饮食偏好**（M-09 未开始）。
8. 订单 `status` 的 `COMPLETED`：枚举已定义，但无触发入口（D-015 留待确认 U-101）。

> 另有需求待确认项 U-001~U-008（菜品删除限制、订单取消边界、邀请码策略等），前端不得擅自按既定规则实现，见 `REQUIREMENT_CONTEXT.md` 第 7 节。

---

## 10. 前端开发必须遵守的规则总结

**认证**
1. 登录/注册后把 `token` 存本地，所有请求带 `Authorization: Bearer <token>`。
2. 收到 `401` 统一跳登录页并清空 token（区分：登录接口 401 = 账号密码错；其余接口 401 = 未登录/过期）。

**家庭隔离**
3. 前端维护"当前家庭 `familyId`"，所有家庭业务请求带 `X-Family-Id: <id>`。
4. 切换家庭即切换整套数据（购物车按 user+family 隔离）；不要信任前端传入的 `userId`/`familyId` 做权限判断，一切以接口返回 + 后端校验为准。
5. 权限按钮：判断"能否改菜品"用 `FamilyResponse.currentUserRole`（`ADMIN`）或菜品 `createdBy == 当前 userId`；**不要用** `AuthResponse.role`。

**错误处理**
6. 统一解析 `ErrorResponse`：`message` 展示给用户，`fieldErrors` 用于表单字段级提示。
7. 区分 400/403/404/409 的提示，不要把所有错误都弹"系统错误"。

**金额与数据**
8. 金额均为 `BigDecimal`，前端只展示后端返回的 `totalAmount`/`subtotal`/`unitPrice`，**不自行计算**。
9. 下单后购物车被后端清空，前端应重新拉取购物车。

**状态机**
10. 备菜状态按钮只提供"当前状态允许的下一状态"（见 6.4），`BLOCKED` 必填 `blockedReason`。
11. 取消订单仅在"下单人本人 + 状态 CREATED + 全部菜品 PENDING"时可用。

---

## 11. 无法确认 / 需注意的点（未自行补全）

1. **日期格式**：`LocalDateTime` 的 JSON 序列化未在 `application.properties` 显式配置，走 Spring Boot 默认（ISO-8601 字符串，如 `2026-11-13T10:20:30.123`）。建议前端写死格式前先抓一次真实响应确认。
2. **未认证状态码**：`SecurityConfig` 当前配置返回 401，但 `BACKEND_PROGRESS.md` G-01 记录过历史 403 行为，前端应兼容并以实测为准。
3. **测试通过情况**：以 `BACKEND_PROGRESS.md` 第 4 节实测为准；本交接文档不新增任何"测试通过"结论。
4. **环境变量**：本地启动需先设置 `JWT_SECRET`、`GEMINI_API_KEY`，否则上下文加载失败。

---

> 维护记录：2026-11-13 创建 V1.0。依据当前源码整理，供前端对接使用；页面与技术方案见 `docs/frontend/FRONTEND_DESIGN.md`。
