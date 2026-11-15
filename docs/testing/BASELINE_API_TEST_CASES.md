# 原项目基线接口测试用例（BASELINE_API_TEST_CASES）

> 本轮仅做接口测试准备（分析代码 + 编写用例），**未修改任何 Java 源码、pom.xml、数据库配置或其他业务文件**。
> 所有「预期响应」均根据代码**推导**得出，**尚未实际执行验证**；凡标注「待实测」的结论不得当作已通过的结果。

---

## 1. 测试环境与前置条件

| 项 | 内容 |
|----|------|
| 项目 | food-delivery-backend（原项目，纯后端 REST API） |
| 启动方式 | `mvn clean package -DskipTests` 后 `java -jar target\food-delivery-backend-0.0.1-SNAPSHOT.jar`（已确认可启动） |
| 访问地址 | `http://localhost:8080` |
| 数据库 | H2 内存库（`jdbc:h2:mem:food_delivery`），**重启后数据清空，无任何初始化数据** |
| 认证方式 | JWT（注册/登录接口返回 `token`，受保护接口需 `Authorization: Bearer <token>`） |
| 测试工具 | Postman 或 Thunder Client（VS Code 插件） |

**前置条件**：
1. 应用已启动，`GET http://localhost:8080/` 返回 `{"service":"Food Delivery API","status":"running",...}`。
2. 因 H2 为内存库且无初始化数据，首次查询 `GET /api/restaurants` 大概率返回空数组 `[]`。若要验证"非空查询结果"，需先通过 `POST /api/restaurants`、`POST /api/menu-items` 造数（见 2.3、2.4，均需 JWT）。
3. 环境变量需已设置 `JWT_SECRET`（≥ 32 字节）与 `GEMINI_API_KEY`（占位值即可通过启动）。

---

## 2. 接口清单（准确路径 / 方法 / 权限 / 请求体）

### 2.0 权限总览（依据 `SecurityConfig.java:48-51`）

| 路径 | 是否需要 JWT |
|------|--------------|
| `GET /` | 否（permitAll） |
| `POST /api/auth/register` | 否（permitAll） |
| `POST /api/auth/login` | 否（permitAll） |
| `/h2-console/**` | 否（permitAll，但 console 默认关闭） |
| 其余全部（含 `/api/restaurants/**`、`/api/menu-items/**`） | **是**（`anyRequest().authenticated()`） |

> 注意：`SecurityConfig` 仅有 `permitAll` 与 `authenticated` 两类，**没有任何 `hasRole`**。因此只要登录拿到 token，任何角色都能访问所有受保护接口（角色字段当前不产生实际权限差异）。

---

### 2.1 注册 `POST /api/auth/register`（无需 JWT）

**请求体来源**：`dto/RegisterRequest.java`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `name` | String | `@NotBlank` | 必填 |
| `email` | String | `@NotBlank` + `@Email` | 必填，需合法邮箱格式 |
| `password` | String | `@NotBlank` + `@Size(min=6)` | 必填，至少 6 字符 |
| `role` | String（枚举） | `@NotNull` | 必填，取值 `CUSTOMER` / `RESTAURANT_OWNER` / `ADMIN` |

**请求体示例（来自真实 DTO 字段）**：

```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "secret123",
  "role": "CUSTOMER"
}
```

**预期响应（代码推导，尚未实测）**：

- 成功：HTTP **201**，返回 `AuthResponse`（`dto/AuthResponse.java`）：

```json
{
  "token": "<JWT 字符串>",
  "email": "alice@example.com",
  "name": "Alice",
  "role": "CUSTOMER"
}
```

- 校验失败（HTTP 400，`ErrorResponse` + `fieldErrors`）：

```json
{
  "timestamp": "2026-11-11T...",
  "status": 400,
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": { "email": "Email should be valid" }
}
```

- 邮箱已存在（HTTP **500**，推导值，待实测）：`AuthService.register` 抛 `RuntimeException("Email already registered: ...")`，被 `GlobalExceptionHandler.handleGeneric` 捕获 → 500。

---

### 2.2 登录 `POST /api/auth/login`（无需 JWT）

**请求体来源**：`dto/LoginRequest.java`

| 字段 | 类型 | 约束 |
|------|------|------|
| `email` | String | `@NotBlank` |
| `password` | String | `@NotBlank` |

**请求体示例**：

```json
{
  "email": "alice@example.com",
  "password": "secret123"
}
```

**预期响应（推导，尚未实测）**：

- 成功：HTTP **200**，返回 `AuthResponse`（结构同注册，`role` 为登录用户实际角色）。
- 密码错误 / 用户不存在：`authenticationManager.authenticate` 抛 `BadCredentialsException` 或 `UsernameNotFoundException`，被 `handleGeneric` 捕获 → **500**（推导值，待实测；本应返回 401，属已知缺陷）。

---

### 2.3 创建餐厅 `POST /api/restaurants`（需 JWT，造数辅助）

**请求体来源**：`dto/RestaurantRequest.java`

| 字段 | 类型 | 约束 |
|------|------|------|
| `name` | String | `@NotBlank` |
| `address` | String | `@NotBlank` |
| `ownerId` | Long | 可选（可空） |

```json
{
  "name": "家常小厨",
  "address": "幸福路 1 号",
  "ownerId": null
}
```

**预期响应**：HTTP **201**，`RestaurantResponse`（含 `id`、`name`、`address`、`ownerId`、`active=true`、`menuItemCount=0`）。

---

### 2.4 创建菜品 `POST /api/menu-items`（需 JWT，造数辅助）

**请求体来源**：`dto/MenuItemRequest.java`

| 字段 | 类型 | 约束 |
|------|------|------|
| `name` | String | `@NotBlank` |
| `description` | String | 可选 |
| `price` | BigDecimal | `@NotNull` + `@DecimalMin(value="0.0", inclusive=false)`，必须 > 0 |
| `restaurantId` | Long | `@NotNull`，必须是已存在的餐厅 id |

```json
{
  "name": "红烧肉",
  "description": "家常做法",
  "price": 38.50,
  "restaurantId": 1
}
```

**预期响应**：HTTP **201**，`MenuItemResponse`（含 `id`、`name`、`description`、`price`、`available=true`、`restaurantId`、`restaurantName`）。若 `restaurantId` 不存在 → HTTP **404**。

---

### 2.5 查询餐厅列表 `GET /api/restaurants`（需 JWT）

- **URL**：`http://localhost:8080/api/restaurants`
- **方法**：GET
- **路径参数 / 查询参数**：无
- **请求 Header**：`Authorization: Bearer <token>`

**预期响应（推导，尚未实测）**：HTTP **200**，`RestaurantResponse` 数组：

```json
[
  {
    "id": 1,
    "name": "家常小厨",
    "address": "幸福路 1 号",
    "ownerId": null,
    "active": true,
    "menuItemCount": 0
  }
]
```

- 新库无数据时返回 `[]`。

---

### 2.6 查询单个餐厅 `GET /api/restaurants/{id}`（需 JWT）

- **URL**：`http://localhost:8080/api/restaurants/1`
- **路径参数**：`id`（Long，餐厅主键）
- **请求 Header**：`Authorization: Bearer <token>`

**预期响应（推导，尚未实测）**：
- 成功：HTTP **200**，单个 `RestaurantResponse`（结构同 2.5 的数组元素）。
- 不存在：HTTP **404**，`ErrorResponse`：

```json
{
  "timestamp": "2026-11-11T...",
  "status": 404,
  "message": "Restaurant not found with id: 999",
  "path": "/api/restaurants/999",
  "fieldErrors": null
}
```

---

### 2.7 查询单个菜品 `GET /api/menu-items/{id}`（需 JWT）

- **URL**：`http://localhost:8080/api/menu-items/1`
- **路径参数**：`id`（Long，菜品主键）
- **请求 Header**：`Authorization: Bearer <token>`

**预期响应（推导，尚未实测）**：
- 成功：HTTP **200**，单个 `MenuItemResponse`：

```json
{
  "id": 1,
  "name": "红烧肉",
  "description": "家常做法",
  "price": 38.50,
  "available": true,
  "restaurantId": 1,
  "restaurantName": "家常小厨"
}
```

- 不存在：HTTP **404**，`message = "Menu item not found with id: <id>"`。

---

### 2.8 按餐厅查询菜品 `GET /api/menu-items/restaurant/{restaurantId}`（需 JWT）

- **URL**：`http://localhost:8080/api/menu-items/restaurant/1`
- **路径参数**：`restaurantId`（Long，餐厅主键）
- **请求 Header**：`Authorization: Bearer <token>`

**预期响应（推导，尚未实测）**：HTTP **200**，`MenuItemResponse` 数组（结构同 2.7 元素）；该餐厅无菜品时返回 `[]`。

---

## 3. 测试用例

> 说明：以下「预期结果」均为**代码推导值**，标注「待实测」的用例在真正执行前不得记为通过。

### 3.1 正常测试用例

| 编号 | 接口 | 请求要点 | 预期结果（推导，待实测） |
|------|------|----------|--------------------------|
| API-001 | `POST /api/auth/register` | 完整合法 body（`role="CUSTOMER"`） | 201，返回含 `token/email/name/role` 的 JSON |
| API-002 | `POST /api/auth/login` | 用 API-001 注册的邮箱+正确密码 | 200，返回 `AuthResponse` |
| API-003 | `GET /api/restaurants` | 携带有效 token | 200，返回数组（可能为空 `[]`） |
| API-004 | `GET /api/restaurants/{id}` | 携带 token，id 为已存在的餐厅 | 200，返回 `RestaurantResponse` |
| API-005 | `GET /api/menu-items/restaurant/{restaurantId}` | 携带 token，restaurantId 为已存在餐厅 | 200，返回菜品数组（可能为空） |
| API-006 | `POST /api/restaurants` + `GET /api/restaurants` | 先造数再查询 | 造数 201；查询 200 且能查到新餐厅 |
| API-007 | `POST /api/menu-items` + `GET /api/menu-items/restaurant/{id}` | 先造菜品再查询 | 造数 201；查询 200 且能查到新菜品 |

### 3.2 异常测试用例

| 编号 | 接口 | 请求要点 | 预期结果（推导，待实测） |
|------|------|----------|--------------------------|
| API-101 | `POST /api/auth/register` | 缺少 `name` | 400，`fieldErrors.name = "Name is required"` |
| API-102 | `POST /api/auth/register` | 缺少 `email` | 400，`fieldErrors.email = "Email is required"` |
| API-103 | `POST /api/auth/register` | `email` 非法（如 `"abc"`） | 400，`fieldErrors.email = "Email should be valid"` |
| API-104 | `POST /api/auth/register` | `password` 为 5 位（`"12345"`） | 400，`fieldErrors.password = "Password must be at least 6 characters"` |
| API-105 | `POST /api/auth/register` | 缺少 `role` | 400，`fieldErrors.role = "Role is required"` |
| API-106 | `POST /api/auth/register` | `role` 传枚举外值（如 `"SUPER_ADMIN"`） | **500**（反序列化异常被 `handleGeneric` 捕获，待实测） |
| API-107 | `POST /api/auth/register` | 重复注册同一邮箱 | **500**（`Email already registered`，待实测；本应 409） |
| API-108 | `POST /api/auth/login` | 密码错误 | **500**（`BadCredentialsException`，待实测；本应 401） |
| API-109 | `POST /api/auth/login` | 未注册邮箱 | **500**（`UsernameNotFoundException`，待实测；本应 401） |
| API-110 | `POST /api/auth/login` | 缺少 `password` | 400，`fieldErrors.password = "Password is required"` |
| API-111 | `GET /api/restaurants` | 不带 token | **401 或 403**（待实测确认具体值） |
| API-112 | `GET /api/restaurants/{id}` | 带 token，id 不存在（如 999） | 404，`Restaurant not found with id: 999` |
| API-113 | `GET /api/menu-items/{id}` | 带 token，id 不存在 | 404，`Menu item not found with id: <id>` |
| API-114 | `GET /api/menu-items/restaurant/{id}` | 带 token，餐厅不存在 | **200 + `[]`**（`findByRestaurantId` 无记录返回空，待实测；注意：不是 404） |
| API-115 | `GET /api/restaurants` | 携带非法/被篡改的 token | **500**（`JwtAuthFilter` 未捕获解析异常，待实测；本应 401） |
| API-116 | `GET /api/restaurants` | 携带过期 token | **500**（同上，`ExpiredJwtException` 未被捕获，待实测） |

---

## 4. JWT 验证方法

1. **获取 token**：调用 `POST /api/auth/register` 或 `POST /api/auth/login`，响应体中的 `token` 字段即 JWT。
2. **携带方式**：请求头 `Authorization: Bearer <token>`（注意 `Bearer` 后有一个空格）。
3. **格式说明**：该 token 为 JWS（HS256 签名），三段式 `header.payload.signature`。payload 中 `sub` 为用户 email，含 `iat`、`exp`（`JwtUtil.java:26-33`）。
4. **离线查看（可选）**：可将 token 粘贴到 jwt.io 或本地解码 payload 查看 `sub`/`exp`，确认 token 内容。
5. **有效期**：`jwt.expiration=86400000`（24 小时，`application.properties:2`）。

---

## 5. 当前发现的安全风险（仅记录，不修改）

1. **角色越权注册（高危）**：`RegisterRequest.role` 由客户端任意指定，且 `AuthService.register` 直接落库（`service/AuthService.java:33`）。**用户可自行注册为 `ADMIN`**。当前因 `SecurityConfig` 未启用 `hasRole` 而暂无明显提权效果，但一旦后续加入角色授权即成为权限提升入口。
2. **无角色授权**：`SecurityConfig` 授权规则只有 `permitAll` 和 `authenticated`，`CustomUserDetailsService` 写入的 `ROLE_*` 权限从未被消费。
3. **无资源归属校验（IDOR）**：`GET /api/restaurants/{id}`、`GET /api/menu-items/{id}` 等查询接口未校验"当前用户是否为资源拥有者"，任意登录用户可查任意记录（本基线以查询为主，写接口的越权点见 `PROJECT_ANALYSIS.md`）。
4. **异常信息泄露**：`GlobalExceptionHandler.handleGeneric` 将 `ex.getMessage()` 原样返回（`exception/GlobalExceptionHandler.java:49`），可能泄露内部实现细节。
5. **JWT 过滤器缺异常处理**：`JwtAuthFilter.doFilterInternal` 中 `jwtUtil.extractEmail(token)` 无 try-catch，非法/过期 token 会抛异常（预期返回 500 而非 401，待实测确认）。
6. **登录失败返回码不当**：`BadCredentialsException` 被兜底 `handleGeneric` 捕获返回 500（本应 401/403）。

---

## 6. 测试执行建议顺序

1. `GET /` 确认服务在跑（无需 token）。
2. `POST /api/auth/register`（API-001）注册并**保存返回的 token**。
3. `POST /api/auth/login`（API-002）登录，再次拿到 token。
4. `GET /api/restaurants`（API-003）带 token 查询（新库预期 `[]`）。
5. （可选）`POST /api/restaurants` 造一个餐厅 → 拿返回的 `id`。
6. （可选）`POST /api/menu-items` 造一个菜品（`restaurantId` 用上一步 id）。
7. `GET /api/restaurants/{id}`（API-004）、`GET /api/menu-items/restaurant/{id}`（API-005）验证非空查询。
8. 按 3.2 异常用例逐一验证（API-101 ~ API-116）。

---

> 再次强调：本文档所有预期状态码与响应均为**基于源码的推导**，尚未在真实运行环境中执行。执行后请将「预期结果」列更新为「实际结果」，并单独记录与推导不一致之处。
