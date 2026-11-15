# 后端开发进度总览

**适用范围：** 仅记录后端（Java / Spring Boot）的开发进度。
**更新原则：** 内容以当前实际源码和已执行过的构建、测试结果为准；未实际验证的内容必须标注为"未执行"。

---

## 1. 当前技术栈（以实际文件为准）

| 项目 | 当前状态 | 依据 |
|---|---|---|
| Java 版本 | 17 | `pom.xml` → `<java.version>17</java.version>` |
| Spring Boot | 3.3.2 | `pom.xml` → `spring-boot-starter-parent` |
| 构建工具 | Maven | `pom.xml` |
| Web / 校验 | `spring-boot-starter-web`、`spring-boot-starter-validation` | `pom.xml` |
| 持久层 | Spring Data JPA / Hibernate（`ddl-auto=validate`） | `pom.xml`、`application.properties` |
| 安全 | Spring Security + JWT（jjwt 0.12.5） | `pom.xml`、`security/` 包 |
| 数据库 | MySQL 9.6.0（库 `homechef_platform`），Connector/J 9.7.0 | `pom.xml`、`application.properties` |
| 表结构管理 | Flyway（Spring Boot BOM 管理的 10.10.0），V1 建表 | `pom.xml`、`db/migration/V1__create_homechef_schema.sql` |
| Lombok | 1.18.32（provided） | `pom.xml` |

**已移除原项目遗留代码**：`Restaurant`/`MenuItem`/`Order`/`Chat`（Gemini）等旧外卖/聊天模块及其 Controller/Service/Entity 在当前 `src/main/java` 中已不存在，`GEMINI_API_KEY` 启动依赖也随之消失。

---

## 2. 模块完成状态

状态含义：

- **已完成**：代码已落地，且已在本机执行过编译与测试（或 HTTP 实测）。
- **未开始**：尚无对应实体/服务/接口。

| 编号 | 模块 | 状态 | 说明 |
|---|---|---|---|
| M-01 | 用户注册与登录（JWT） | 已完成 | 详见 `AUTH_FAMILY.md` |
| M-02 | 家庭管理与家庭成员管理 | 已完成 | 详见 `AUTH_FAMILY.md` |
| M-03 | 邀请码加入家庭 | 未开始 | 需求中"有效期、使用次数"等仍待确认 |
| M-04 | 家庭菜品库（含食材/步骤/分类） | 已完成 | 独立 `Dish` 实体体系，含分类、食材、步骤、图片 URL、上下架；详见 `DISH_MODULE.md` |
| M-05 | 个人独立购物车（用户 + 家庭） | 已完成 | 按 `(user_id, family_id, dish_id)` 归属与唯一约束双重隔离；详见 `CART_MODULE.md` |
| M-06 | 家庭内部下单与订单明细 | 已完成 | `FamilyOrder`/`FamilyOrderItem`，下单事务内校验、服务端计费、快照落库、清空当前用户当前家庭购物车；详见 `ORDER_MODULE.md` |
| M-07 | 备菜协作与开饭状态 | 已完成 | 单菜状态 `PENDING/COOKING/READY/SERVED/BLOCKED`，订单整体状态独立；详见 `ORDER_MODULE.md` |
| M-08 | 食材不足与家庭采购清单 | 已完成（最小范围） | `family_shopping_items` 按家庭隔离，只登记实际缺少食材并提供查看；见 `ORDER_MODULE.md` 第 11 节 |
| M-09 | 用户资料与饮食偏好 | 未开始 | — |

### 2.0 当前数据表（11 个 JPA 实体）

| 实体 | 对应表 | 说明 |
|---|---|---|
| `User` | `users` | 系统账号、密码、邮箱、系统角色 |
| `Family` | `families` | 家庭 |
| `FamilyMember` | `family_members` | 家庭成员与家庭角色（ADMIN/MEMBER） |
| `Dish` | `dishes` | 家庭菜品库 |
| `DishCategory` | `dish_categories` | 菜品分类（`(family_id, name)` 唯一约束） |
| `DishIngredient` | `dish_ingredients` | 菜品食材 |
| `DishStep` | `dish_steps` | 菜品制作步骤 |
| `CartItem` | `cart_items` | 购物车（`(user_id, family_id, dish_id)` 唯一约束） |
| `FamilyOrder` | `family_orders` | 家庭订单主表 |
| `FamilyOrderItem` | `family_order_items` | 订单明细（菜名/单价/小计快照、单菜备菜状态、负责人） |
| `FamilyShoppingItem` | `family_shopping_items` | 家庭待购清单（按 `family_id` 隔离） |

以上表由 Flyway V1 在 MySQL `homechef_platform` 建立（11 张表、24 个外键、10 个唯一约束），应用在 Hibernate `ddl-auto=validate` 下启动通过。完整 ER 图与 DDL 见 `docs/database/MYSQL_DATABASE_DESIGN.md`。

### 2.1 原项目遗留代码

原外卖模型（餐厅、旧菜单、旧订单、Gemini 聊天）已在 MySQL 迁移阶段移除，当前源码树中不存在，前端不应再接入 `/api/restaurants`、`/api/menu-items`、`/api/orders`、`/api/chat`。此前针对旧 `/api/orders` 的 IDOR 修复（D-017）所涉及的代码已随旧模块一并移除，该决策保留为历史记录。

---

## 3. 后续开发顺序

后端剩余待办：

1. **邀请码加入家庭**（M-03）——邀请码有效期与使用次数需先确认。
2. **用户资料与饮食偏好**（M-09）。

前端开发进度见 `docs/frontend/FRONTEND_DEVELOPMENT_ROADMAP.md`（阶段 0 已验收、阶段 1 已实现、阶段 2 起待逐阶段推进）。

---

## 4. 最近一次实测结果（已记录）

以下为本机**已实际执行并记录**的结果（来源：`AI_INTERACTION_LOG.md` 任务 015/016/017 与 `docs/database/MYSQL_DATABASE_DESIGN.md`），未执行的项目不列入"通过"。

| 验证项 | 命令 / 方式 | 实际结果 |
|---|---|---|
| 编译 + 单元测试 | `mvn -B clean test` | `BUILD SUCCESS`，3 项测试通过（`AddShoppingItemRequestValidationTest`、`EmailNormalizerTest`） |
| 打包 | `mvn -B -DskipTests package` | 成功 |
| 数据库迁移 | Flyway V1 在空 `homechef_platform` 执行 | 11 张业务表、24 个外键、10 个唯一约束、1 条迁移记录 |
| 应用启动 + ORM 校验 | `java -jar` 启动 | MySQL 连接成功，Hibernate `ddl-auto=validate` 通过，`GET /` 返回 200 |
| HTTP 冒烟测试 | 启动后真实请求 | 注册/登录/创建家庭/菜品（含食材步骤）/购物车合并计价/下单快照/提交后清空购物车/历史快照不受改名改价影响/家庭越权 403/采购清单新增与查询 均通过 |

启动后端前必须设置环境变量（`application.properties`）：

- `JWT_SECRET`（必需，JWT 签名密钥）
- `DB_PASSWORD`（必需，MySQL 应用账号密码）
- `DB_URL`、`DB_USERNAME`（可选，已有默认值：`jdbc:mysql://127.0.0.1:3306/homechef_platform` 与 `homechef_app`）
- `PORT`（可选，默认 `8080`）

> 注意：此前的 `GEMINI_API_KEY` 依赖已随 Gemini 聊天模块移除，不再需要。旧的 66/76 项 Service 层单测结果属于 H2 时代，对应测试类当前已不在工作区，不作为当前测试覆盖依据。

---

## 5. 已知遗留问题

| 编号 | 问题 | 影响 | 状态 |
|---|---|---|---|
| G-01 | 未认证请求返回 401 | `SecurityConfig` 当前 `AuthenticationEntryPoint` 返回 401（历史曾记录 403，现已为 401）；前端对未登录兼容 401/403，以实测为准 | 已确认返回 401 |
| G-02 | "当前家庭"仅通过请求头 `X-Family-Id` 表达，未在服务端持久化 | 客户端自行记住所选家庭，切换家庭后由前端重新拉取 | 待设计确认（前端用 store + localStorage 处理） |
| G-03 | ~~`GeminiClient` 依赖 `GEMINI_API_KEY`~~ | ~~缺失即启动失败~~ | 已消除（Gemini 模块已删除） |
| G-04 | ~~旧 `Restaurant`/`MenuItem`/`Order` 无家庭归属校验~~ | ~~家庭数据隔离未覆盖旧接口~~ | 已消除（旧模块已删除，家庭业务由 `Family*` 体系承担） |
| G-05 | ~~旧 `MenuItem` 与 `Dish` 两套结构并存~~ | ~~旧订单引用 `MenuItem`~~ | 已消除（`MenuItem` 已删除，菜品来源统一为 `Dish`） |

---

## 6. 前端对接

- 前后端交接文档：`docs/backend/API_FRONTEND_CONTRACT.md`（逐接口 API 契约、认证、家庭隔离、错误处理规则）。
- 前端设计：`docs/frontend/FRONTEND_DESIGN.md`（技术选型、视觉方向、主导航）。
- 前端实施依据：`docs/frontend/FRONTEND_DEVELOPMENT_ROADMAP.md`（阶段 0~9）与 `docs/frontend/FRONTEND_DEVELOPMENT_GUIDE.md`（当前文件构成与边界）。
- 前端当前状态：阶段 0（工程初始化）已验收；阶段 1（路由/五导航/响应式布局/通用组件）已实现并通过 `npm test`、`npm run build` 与 SPA 路由 HTTP 检查，响应式视觉待人工确认；阶段 2（认证与家庭上下文）尚未实现。

> 注意：`API_FRONTEND_CONTRACT.md` 及各后端模块文档编写于 H2 时代，其中"H2 内存库""旧外卖模块仍存在""`GEMINI_API_KEY` 必需"等描述已过时，接口字段仍应以当前 Controller/DTO 为准，联调时逐字段核实。

---

> 维护记录：本文件只维护总览与进度，不记录单模块细节；单模块细节写入对应模块文档。
> 维护记录：2026-11-15 更新技术栈（H2→MySQL + Flyway）、模块状态（移除遗留代码描述）、测试结果（66/76→3 项）、遗留问题（G-03/G-04/G-05 消除）与前端对接状态（阶段 0/1 已实现），使其与当前源码及 MySQL 迁移结果一致。
