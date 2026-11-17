# 系统菜谱（美食广场）模块 API 与模块设计

> **文档状态**：已实施（② 编码完成，编译 + 单元测试通过）。
> **设计日期**：2026-11-17。
> **前置文档**：[SYSTEM_RECIPE_DATABASE_DESIGN.md](./SYSTEM_RECIPE_DATABASE_DESIGN.md)、[HOWTOCOOK_DATA_ANALYSIS.md](./HOWTOCOOK_DATA_ANALYSIS.md)。
> **事实口径**：本文接口路径、请求头、状态码、异常、权限方法均参照当前**已实现**的家庭菜品模块源码（`DishController`/`DishServiceImpl`/`CurrentUserService`/`Dish`/`DishRepository`/`DishCategoryRepository`），不是凭空猜测。

---

## 1. 模块定位

系统公共菜谱（美食广场）是**面向所有登录用户可读**的公共菜谱库，与家庭私有菜品（`dishes`）物理隔离。核心能力只有两类：

1. **只读浏览**：分类列表、菜谱列表（筛选）、菜谱详情。
2. **复制到我家**：把一条系统菜谱复制为当前家庭的私有菜品（含食材/步骤），复制后与系统菜谱**无任何关联**。

普通用户**不能**修改、下架、删除系统菜谱（维护权限与家庭菜品完全分离）。

---

## 2. 新增文件清单（规划，实施时创建）

| 层 | 文件 | 说明 |
|---|---|---|
| Entity | `entity/SystemDishCategory.java` | 对应 `system_dish_categories` |
| Entity | `entity/SystemDish.java` | 对应 `system_dishes` |
| Entity | `entity/SystemDishIngredient.java` | 对应 `system_dish_ingredients` |
| Entity | `entity/SystemDishStep.java` | 对应 `system_dish_steps` |
| Repository | `repository/SystemDishCategoryRepository.java` | 分类查询 |
| Repository | `repository/SystemDishRepository.java` | 菜谱查询 + 来源去重 |
| Repository | `repository/SystemDishIngredientRepository.java` | 食材查询 |
| Repository | `repository/SystemDishStepRepository.java` | 步骤查询 |
| DTO | `dto/SystemDishCategoryResponse.java` | 分类响应 |
| DTO | `dto/SystemDishSummaryResponse.java` | 列表项响应 |
| DTO | `dto/SystemDishDetailResponse.java` | 详情响应（含食材/步骤/来源） |
| DTO | `dto/CopyToFamilyRequest.java` | 复制请求（价格 + 食材补齐） |
| Service | `service/SystemDishService.java` | 接口 |
| Service | `service/impl/SystemDishServiceImpl.java` | 实现 |
| Service | `service/FamilyDishCategoryResolver.java` | 家庭分类「获取或创建」独立事务组件（`REQUIRES_NEW` 并发兜底） |
| Controller | `controller/SystemDishController.java` | REST 接口 |

**复用（不新建）**：`CurrentUserService`（成员资格校验）、`Dish`/`DishIngredient`/`DishStep`/`DishCategory` + 对应 Repository（复制落库到家庭表）、`DishResponse`（复制结果复用）、`BadRequestException`/`ForbiddenException`/`ResourceNotFoundException`。

**不改动**：现有 `Dish` 模块源码、认证/JWT、家庭模块、订单模块、`dish_ingredients`/`dish_steps` 表结构。

---

## 3. 数据结构（回顾）

4 张表已由 `V12__create_system_recipe_schema.sql` 建立，分类种子数据由 `V13__seed_system_dish_categories.sql` 初始化 10 个分类。字段、约束、索引详见数据库设计文档第 3~5 节，此处不重复。

关键约束（影响接口设计）：
- `system_dishes.source_type + original_path` 复合唯一键（导入去重）。
- `system_dish_ingredients.amount/unit` **可空** + `amount_text` 保留原始文本（无法量化用量）。
- `system_dish_steps.content` 为 `VARCHAR(500)`。

---

## 4. Entity / Repository 设计

### 4.1 Entity 风格

遵循现有 `Dish` 实体的风格：`@Data @Builder @NoArgsConstructor @AllArgsConstructor`，普通 `Long` 字段关联（**不用** `@ManyToOne`），`@GeneratedValue(strategy = IDENTITY)`。

- `SystemDish`：`@Table(name = "system_dishes", uniqueConstraints = @UniqueConstraint(name = "uk_system_dishes_source", columnNames = {"source_type", "original_path"}))`；`difficulty`/`calories`/`cookingMinutes` 用 `Integer`（可空）；`coverImageUrl`/`description`/`sourceUrl`/`originalPath`/`sourceCommit`/`license` 用 `String`（长度对齐 DDL）；`available` 用 `boolean`（默认 `true`）。
- `SystemDishIngredient`：`amount` 用 `BigDecimal`（可空，`precision = 10, scale = 2`）；`amountText` 用 `String`（100）；`optional` 用 `boolean`（默认 `false`）。
- `SystemDishStep`：`stepNo` `Integer`、`content` `String`（500）、`imageUrl` `String`（500）。
- 时间字段：`LocalDateTime`（UTC），与 `Dish` 一致。

### 4.2 Repository 派生查询（命名风格与 `DishRepository` 一致）

> 注：Entity 属性名为 `available`（列名才是 `is_available`），Spring Data 派生查询按**属性名**解析，故为 `findByAvailableTrue...` 而非 `findByIsAvailableTrue...`（后者会启动失败）。

| Repository | 方法 | 用途 |
|---|---|---|
| `SystemDishCategoryRepository` | `findByAvailableTrueOrderBySortNoAsc()` | 分类列表（仅启用） |
| `SystemDishRepository` | `findByIdAndAvailableTrue(Long id)` | 详情（仅上架） |
| | `findByAvailableTrueOrderByIdDesc()` | 全部列表 |
| | `findByCategoryIdAndAvailableTrueOrderByIdDesc(Long categoryId)` | 按分类筛选 |
| | `findByNameContainingIgnoreCaseAndAvailableTrueOrderByIdDesc(String keyword)` | 关键字搜索 |
| | `existsBySourceTypeAndOriginalPath(String sourceType, String originalPath)` | 导入去重（先查） |
| | `findBySourceTypeAndOriginalPath(String sourceType, String originalPath)` | 导入时取已有记录 |
| `SystemDishIngredientRepository` | `findByDishIdOrderBySortNoAsc(Long dishId)` | 食材列表 |
| `SystemDishStepRepository` | `findByDishIdOrderByStepNoAsc(Long dishId)` | 步骤列表 |

---

## 5. DTO 设计

### 5.1 `SystemDishCategoryResponse`

```
id, name, sortNo
```

### 5.2 `SystemDishSummaryResponse`（列表项）

```
id, name, description, coverImageUrl, categoryId, categoryName,
cookingMinutes, difficulty, calories
```

### 5.3 `SystemDishDetailResponse`（详情）

列表项字段基础上追加：

```
sourceType, sourceUrl, originalPath, sourceCommit, license,
ingredients[], steps[]
```

- `ingredients[]`：`name`、`amount`（可空）、`unit`（可空）、`amountText`（可空）、`optional`、`sortNo`
- `steps[]`：`stepNo`、`content`、`imageUrl`（可空）

### 5.4 `CopyToFamilyRequest`（复制到我家）

```json
{
  "price": 38.00,
  "ingredientOverrides": [
    { "sortNo": 3, "amount": 3.0, "unit": "g" }
  ]
}
```

- `price`：`BigDecimal`，**必填**、`>= 0`（不允许默认 0，由用户定价）。
- `ingredientOverrides`：**可空**。仅当系统菜谱存在「无法量化」的食材（`amount` 或 `unit` 为空）时，才需要前端回传补齐值。每项以 `sortNo` 定位到对应食材，提供补齐后的 `amount` + `unit`。

---

## 6. API 接口契约

所有接口需 `Authorization: Bearer <token>`。**浏览接口不依赖 `X-Family-Id`；复制接口必须携带 `X-Family-Id`**。

| 方法 | 路径 | 用途 | 权限 | 成功码 |
|---|---|---|---|---|
| GET | `/api/system-dish-categories` | 系统分类列表（仅启用） | 登录用户 | 200 |
| GET | `/api/system-dishes` | 菜谱列表，支持 `categoryId`、`keyword` 筛选 | 登录用户 | 200 |
| GET | `/api/system-dishes/{id}` | 菜谱详情（含食材/步骤/来源） | 登录用户 | 200 |
| POST | `/api/system-dishes/{id}/copy-to-family` | 复制到目标家庭 | 家庭成员 | 201 |

### 6.1 列表接口

```
GET /api/system-dishes?categoryId=1&keyword=红烧
```

返回 `List<SystemDishSummaryResponse>`，按 `id` 倒序；`categoryId` 与 `keyword` 可组合或单独使用（组合时先按关键字过滤再按分类过滤，与 `DishServiceImpl.list` 的语义一致）。**不返回已下架（`is_available=false`）菜谱**。

### 6.2 详情接口

```
GET /api/system-dishes/{id}
```

返回 `SystemDishDetailResponse`。不存在或已下架 → `404`（`ResourceNotFoundException`，不区分「不存在」与「已下架」）。

### 6.3 复制接口（核心）

```
POST /api/system-dishes/{id}/copy-to-family
X-Family-Id: <familyId>
Content-Type: application/json
```

请求体见 5.4。返回 `DishResponse`（复用现有家庭菜品响应，`familyId`/`createdBy`/`price` 已由服务端填写）。

错误场景：
- 缺 `X-Family-Id` → `400`（`X-Family-Id header is required`）。
- 非家庭成员 → `403`（`You are not a member of this family`）。
- 系统菜谱不存在或已下架 → `404`。
- `price` 缺失或 `< 0` → `400`。
- 存在无法量化的食材但前端未在 `ingredientOverrides` 补齐 → `400`（明确提示缺哪些食材，允许前端取消本次添加，**不静默丢弃**）。

### 6.4 管理接口（本阶段不实现）

`PATCH /api/system-dishes/{id}/availability` 等系统菜谱维护接口，需要**系统管理员**（`User.Role.ADMIN`）权限，且与家庭权限体系分离。**本阶段仅规划，不实现**；数据初始化由导入程序（后续阶段）完成。

---

## 7. Service 设计要点

### 7.1 复制事务（`@Transactional`）

完整流程见数据库设计文档第 7 节，核心要点：

1. `currentUserService.requireMembership(familyId)` 校验成员资格。
2. 读取 `system_dishes` + 食材 + 步骤完整内容。
3. **分类映射**：`system_dishes.category_id` 不能直接写入家庭 `dishes.category_id`。规则：目标家庭存在同名分类（`dishCategoryRepository.findByFamilyIdAndName`）则复用，不存在则自动创建同名家庭分类。并发安全见 7.2。
4. 写入家庭 `dishes`（`familyId`、`name`、`description`、`imageUrl = coverImageUrl`、`categoryId`、`cookingMinutes`、`price`、`createdBy = 当前用户`、`available = true`）。`difficulty`/`calories` 不复制（家庭表无字段）。
5. 复制食材（遵循 7.3 补齐规则）与步骤到 `dish_ingredients` / `dish_steps`。
6. 返回 `DishResponse`。

### 7.2 分类自动创建的并发处理

「获取或创建家庭分类」放到**独立方法 + `@Transactional(propagation = REQUIRES_NEW)`**：先 `findByFamilyIdAndName`，未命中则插入；插入撞唯一约束（`DataIntegrityViolationException`）时**重新查询已提交的那条**并复用其 `id`。冲突只回滚这个小事务，不污染外层复制事务。数据库唯一约束 `uk_dish_categories_family_name` 是最终安全网。

### 7.3 食材复制补齐规则

对系统菜谱每个食材：

1. `amount` 与 `unit` **均有效** → 直接复制。
2. **无法量化**（`amount` 或 `unit` 为空）→ 必须在 `ingredientOverrides` 中以 `sortNo` 找到补齐值，且 `amount + unit` 齐全，否则 `400`。
3. **严禁**把「适量」「少许」转成 `amount = 0`。
4. 用户不补齐 → 明确提示并允许取消本次添加，**不静默丢弃**、不产生半成品数据。

---

## 8. 权限与隔离

| 场景 | 实现 |
|---|---|
| 浏览接口 | 仅需登录（`SecurityConfig` 默认 `/api/**` 需认证），**不读 `X-Family-Id`** |
| 复制接口 | `requireMembership(familyId)`，非成员 403 |
| 复制后解耦 | 家庭菜品与系统菜谱无外键关联，订单仍只引用 `dishes` |
| 系统菜谱维护 | 普通用户无权；管理接口与家庭权限分离（本阶段不实现） |

---

## 9. 待确认项

1. **匿名可读**：系统菜谱浏览是否允许未登录访问（`SecurityConfig` 需额外放行 `GET /api/system-dishes/**`、`/api/system-dish-categories`）。建议保持「登录可读」，降低风险。
2. **分页**：`GET /api/system-dishes` 第一版建议不分页（与现有 `DishController.list` 风格一致），但 HowToCook 约 373 道菜谱，一次性返回体量偏大；是否引入 `page`/`size` 分页需确认。
3. **`difficulty`/`calories` 是否复制到家庭菜品**：家庭 `dishes` 无对应字段，默认**不复制**（见数据库设计文档第 11 节）。
4. **图片导入策略**：默认仅文字导入（`coverImageUrl` 置空），次选选择性上传，见数据库设计文档第 8 节。
5. **管理接口**：上下架/维护接口是否纳入本阶段范围（需系统管理员权限体系）。
6. **食材补齐交互**：复制接口采用「后端校验 + 前端预览补齐」的前后端契约（5.4/6.3），前端需先调详情接口识别无法量化食材——该交互是否可接受。

---

## 10. 测试计划（规划，实施时执行）

- **单元/集成测试**（`SystemDishServiceImplTest`，参照 `DishServiceImplTest` 风格）：
  1. 分类列表仅返回启用分类。
  2. 列表按分类/关键字筛选，且不返回已下架菜谱。
  3. 详情返回食材、步骤、来源字段；不存在/已下架返回 404。
  4. 复制成功：生成家庭菜品 + 食材 + 步骤，分类复用/新建正确，`price` 由服务端采用。
  5. 复制时无法量化食材未补齐 → 400，且不产生任何家庭菜品（事务回滚）。
  6. 复制时 `price` 缺失/负数 → 400。
  7. 非成员复制 → 403；缺 `X-Family-Id` → 400。
  8. 分类自动创建并发冲突 → 兜底重查复用（`REQUIRES_NEW`）。
- **端到端实测**：启动后 HTTP 走查「浏览 → 详情 → 复制到我家 → 家庭菜品列表可见新菜品」。

---

> 维护说明：本文档为系统菜谱模块的 API/模块设计稿，编码实施前须经用户审查确认；确认后另按 `docs/report/` 记录实际实现与测试结果。
