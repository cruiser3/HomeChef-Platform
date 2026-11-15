# 家庭菜品库模块（DISH_MODULE）

**范围：** 记录**已经实际实现并验证**的家庭菜品库模块（M-04）。
**依据：** 当前源码 + 本机实际执行的 `mvn clean test` 与启动后 HTTP 实测结果。未执行的项一律标注「未执行」。

---

## 1. 新增文件

| 层 | 文件 |
|---|---|
| Entity | `entity/Dish.java` |
| Entity | `entity/DishIngredient.java` |
| Entity | `entity/DishStep.java` |
| Entity | `entity/DishCategory.java` |
| Repository | `repository/DishRepository.java` |
| Repository | `repository/DishIngredientRepository.java` |
| Repository | `repository/DishStepRepository.java` |
| Repository | `repository/DishCategoryRepository.java` |
| DTO | `dto/DishRequest.java`（含内部类 `DishIngredientRequest`、`DishStepRequest`） |
| DTO | `dto/DishResponse.java`（含内部类 `IngredientItem`、`StepItem`） |
| DTO | `dto/DishCategoryRequest.java` |
| DTO | `dto/DishCategoryResponse.java` |
| Service | `service/DishService.java` |
| Service | `service/impl/DishServiceImpl.java` |
| Controller | `controller/DishController.java` |
| Test | `src/test/java/com/fooddelivery/service/DishServiceImplTest.java` |
| Skill | `.codebuddy/skills/dish-module/SKILL.md`、`_meta.json` |

**未修改任何既有 Java 文件**（认证、JWT、家庭模块、`MenuItem`、`Order`、`SecurityConfig` 全部未动）。

### 1.1 与旧 `MenuItem` 的关系（重要）

原项目存在 `MenuItem`（`menu_items`，挂在 `restaurant` 下），它仍被未改造的 `Order`、`Restaurant` 模块引用。为避免改动已完成的家庭模块、也避免破坏未改造的订单模块，菜品库**没有复用 `MenuItem`**，而是新建独立的 `Dish` 实体，归属 `family_id`。

因此当前存在两套并存结构：

- `dishes` 等 4 张表：新版家庭菜品库（本模块）
- `menu_items`：旧餐厅菜品，未被家庭模块使用

订单模块（M-06）改造时应改用 `Dish`，并在改造完成后移除旧 `MenuItem`。这一点是待跟进事项，不是本模块缺陷。

---

## 2. 数据结构

### 2.1 `dishes`

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | bigint | PK，自增 | |
| `family_id` | bigint | not null | 所属家庭，隔离依据 |
| `name` | varchar(100) | not null | 菜品名称 |
| `description` | varchar(500) | 可空 | 描述 |
| `image_url` | varchar(500) | 可空 | 第一版仅存 URL |
| `category_id` | bigint | 可空 | 家庭分类 ID |
| `cooking_minutes` | integer | 可空 | 制作耗时（分钟），>= 0 |
| `price` | numeric(10,2) | not null | `BigDecimal`，>= 0 |
| `created_by` | bigint | not null | 创建者用户 ID |
| `is_available` | boolean | not null | 上下架状态，默认 true |
| `created_at` / `updated_at` | timestamp | not null | 创建/更新时间 |

### 2.2 `dish_ingredients`

| 列 | 类型 | 约束 |
|---|---|---|
| `id` | bigint | PK |
| `dish_id` | bigint | not null |
| `name` | varchar(100) | not null |
| `amount` | numeric(10,2) | not null，>= 0 |
| `unit` | varchar(20) | not null |
| `sort_no` | integer | not null，展示顺序 |

### 2.3 `dish_steps`

| 列 | 类型 | 约束 |
|---|---|---|
| `id` | bigint | PK |
| `dish_id` | bigint | not null |
| `step_no` | integer | not null，从 1 开始连续 |
| `content` | varchar(500) | not null |
| `image_url` | varchar(500) | 可空 |

### 2.4 `dish_categories`

| 列 | 类型 | 约束 |
|---|---|---|
| `id` | bigint | PK |
| `family_id` | bigint | not null |
| `name` | varchar(50) | not null |
| `sort_no` | integer | not null |

唯一约束：`uk_dish_category_family_name (family_id, name)` —— 分类按家庭隔离，不同家庭可以同名。

**设计说明**：食材与步骤各自独立成表，未采用字符串拼接，满足「结构化存储」要求；也未使用 JPA 实体关联映射，改为 `dish_id` 外键列 + 仓储查询，避免 `open-in-view=false` 下的懒加载问题（与已完成家庭模块的做法保持一致）。

---

## 3. 接口

所有接口均需 `Authorization: Bearer <token>`，并需 `X-Family-Id: <家庭ID>` 请求头（缺失返回 400）。

| 方法 | 路径 | 用途 | 权限 | 成功码 |
|---|---|---|---|---|
| POST | `/api/dishes` | 创建菜品 | 该家庭成员 | 201 |
| GET | `/api/dishes` | 菜品列表，支持 `categoryId`、`keyword`、`available` 组合筛选 | 该家庭成员 | 200 |
| GET | `/api/dishes/{dishId}` | 菜品详情（含食材、步骤） | 该家庭成员 | 200 |
| PUT | `/api/dishes/{dishId}` | 修改菜品（整体替换食材与步骤） | 创建者 或 家庭 ADMIN | 200 |
| PATCH | `/api/dishes/{dishId}/availability?available=true|false` | 上架/下架 | 创建者 或 家庭 ADMIN | 200 |
| GET | `/api/dishes/categories` | 家庭分类列表 | 该家庭成员 | 200 |
| POST | `/api/dishes/categories` | 创建家庭分类 | 该家庭成员 | 201 |

### 3.1 创建/修改请求体

```json
{
  "name": "红烧肉",
  "description": "家常做法",
  "imageUrl": "https://img.example.com/a.jpg",
  "categoryId": 1,
  "cookingMinutes": 90,
  "price": 38.00,
  "ingredients": [
    { "name": "五花肉", "amount": 500, "unit": "g" },
    { "name": "冰糖", "amount": 30, "unit": "g" }
  ],
  "steps": [
    { "stepNo": 1, "content": "焯水" },
    { "stepNo": 2, "content": "煸炒", "imageUrl": "https://img.example.com/s2.jpg" },
    { "stepNo": 3, "content": "收汁" }
  ]
}
```

### 3.2 响应体

`DishResponse` 字段：`id`、`familyId`、`name`、`description`、`imageUrl`、`categoryId`、`categoryName`、`cookingMinutes`、`price`、`createdBy`、`createdByName`、`available`、`createdAt`、`updatedAt`、`ingredients[]`（`name`/`amount`/`unit`）、`steps[]`（`stepNo`/`content`/`imageUrl`）。

---

## 4. 权限与家庭隔离实现

复用已完成模块的 `CurrentUserService`，未新建权限体系：

| 场景 | 实现 |
|---|---|
| 家庭隔离 | 所有方法入口调用 `currentUserService.requireMembership(familyId)`，非成员 403 |
| 跨家庭查看特定菜品 | `dishRepository.findByIdAndFamilyId(dishId, familyId)`，查不到返回 404，不泄露存在性 |
| 修改权限 | `requireModifyPermission`：`createdBy == 当前用户` → 放行；否则查成员角色，仅 `ADMIN` 放行，其余 403 |
| 分类归属 | `validateCategory`：`categoryId` 必须能在本家庭找到，否则 400 |
| 创建者身份 | 由服务端 `getCurrentUserId()` 写入，不接受前端传入 |
| 下架约束 | `available=false` 落库；后续购物车需读取该字段重新校验（尚未开发） |

---

## 5. 测试结果（实际执行）

命令：

```
$env:JWT_SECRET='...'; $env:GEMINI_API_KEY='test-dummy-key'; mvn clean test
```

实际输出：

```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

其中 `DishServiceImplTest` 14 个用例，`FamilyServiceImplTest` 5 个，`AuthServiceRegisterTest` 1 个。

`DishServiceImplTest` 覆盖：

1. 创建菜品保存创建者、家庭、食材、步骤、图片、耗时。
2. 列表只返回本家庭菜品（另一个家庭的菜品不会出现）。
3. 详情返回食材与步骤。
4. 按分类筛选、按名称搜索，且返回分类名。
5. 修改菜品后食材从 2 条变 1 条、步骤从 2 条变 1 条，旧数据不残留。
6. 上/下架切换，并验证 `available` 筛选结果。
7. 普通成员修改他人菜品、上下架他人菜品均被拒（403）。
8. 管理员修改他人菜品允许。
9. 非成员 list/detail/create/listCategories 全部 403。
10. 跨家庭访问具体菜品返回 404。
11. 非法输入（负价、空价、空名、步骤号不连续、缺单位、非法分类）全部被拒，且**未写入任何菜品**。
12. 家庭内分类重名被拒；不同家庭可同名。
13. 下架菜品在详情中 `available=false`。

### 5.1 手工端到端实测（已执行）

以 `java -jar` 启动应用后，用真实 HTTP 请求验证，实际结果：

| 动作 | 实际结果 |
|---|---|
| 创建分类 | 201，返回 `name` 与 `id` |
| 创建菜品（含分类/食材/步骤） | 201，`categoryName` 正确回填，`ingredients=2`、`steps=3` |
| 列表全部 | 2 条 |
| 按 `categoryId` 筛选 | 1 条 |
| `keyword=Final` 搜索 | 1 条 |
| 详情 | 返回 2 条食材、3 条步骤，分类名正确 |
| 修改（食材 2→1、步骤 3→1、价格 38→42） | 200，分类名保留，食材/步骤数量正确变化 |
| 下架 | `available=false`；`available=true` 列表剩 1 条，`available=false` 列表 1 条 |
| 重新上架 | `available=true` |
| 非成员 LIST / DETAIL / CREATE / UPDATE | 均为 403 |
| 缺 `X-Family-Id` | 400 |
| 负价 / 缺价 / 空名 / 步骤号不连续 / 缺单位 / 非法分类 | 均为 400 |
| 详情不存在 | 404 |
| 分类重名 | 400 |
| 无 token | 403 |

**说明**：无 token 返回 403 属项目既有行为（既有 G-01），本次未修改。

### 5.2 开发中发现并修复的缺陷

| 缺陷 | 现象 | 修复 |
|---|---|---|
| D-01 | 创建菜品接口返回的 `categoryName` 为 null，而列表/详情正常 | `create`/`update`/`changeAvailability` 统一调用 `categoryNameOf(familyId, categoryId)` 回填；并在测试中加断言防回归 |

---

## 6. 遗留问题与待人工决策

| 编号 | 问题 | 说明 | 建议 |
|---|---|---|---|
| DM-01 | 旧 `MenuItem` 与 `Dish` 并存 | 订单模块仍用 `MenuItem`，两套菜品结构同时存在 | 订单模块改造时统一到 `Dish` 并删除 `MenuItem` |
| DM-02 | 物理删除路径未实现 | 需求 U-007「未被订单引用的菜品是否允许真正删除」未确认，按需求只实现下架 | 保持不实现，待需求确认 |
| DM-03 | 食材/步骤更新为整体替换 | 修改菜品时未提交的食材/步骤会被清空 | 前端需回传完整列表；如需要「增量更新」需另立需求 |
| DM-04 | 图片仅存 URL，无上传安全校验 | 差距分析 7.3 要求类型/大小/文件名/路径安全 | 待后续确认是否引入文件上传 |
| DM-05 | 购物车尚未开发 | 下架菜品「不能加入购物车」已在数据层具备条件（`available` 字段），但拦截逻辑尚未落地 | 随 M-05 实现时在加购处校验 `available` |
| DM-06 | 无 token 返回 403 | 与验收期望 401 不符，属既有问题 G-01 | 需人工批准后统一处理 |
| DM-07 | 分类只支持创建与查询 | US-04-05 提到分类增删改权限与菜品归属一致，本版仅实现创建/查询 | 如课堂演示需要，可补充改名与停用 |

---

## 7. 本次 AI 工程化过程记录

| 阶段 | 实际做法 |
|---|---|
| 需求依据 | 只读取 `USER_STORIES_AND_ACCEPTANCE.md` 的 US-04-01~US-04-06、`REQUIREMENT_CODE_GAP_ANALYSIS.md` 菜品章节、`AUTH_FAMILY.md` 的权限实现 |
| 方案决策（人工确认型） | ① 复用 `CurrentUserService` 而不新建权限体系；② 新建 `Dish` 而不改造 `MenuItem`（避免破坏未改造订单模块）；③ 物理删除不实现（U-007 未确认） |
| 代码生成 | Entity/Repository/DTO/Service/Controller 分层生成，全部经人工审阅后编译 |
| 验证 | `mvn clean test` 19 条通过；启动后 HTTP 端到端实测 |
| 缺陷闭环 | 实测发现 D-01（创建返回分类名为空），修复后重跑测试并再次实测确认 |

---

> 维护记录：本文档只覆盖菜品模块；购物车、订单、采购清单模块完成后另建文档。