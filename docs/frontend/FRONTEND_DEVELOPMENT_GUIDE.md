# 前端开发前置说明与项目结构梳理

> **版本：** V1.0
> **整理日期：** 2026-11-15
> **状态：** 前端阶段 0 已通过验收；阶段 1 基础框架已实现，自动化构建/测试通过，响应式视觉效果待人工浏览器验收。
> **用途：** 在开始前端编码前，统一说明当前项目文件构成、真实后端能力、已确认的前端方案、接口边界和文档差异。
> **范围：** 本文是项目导航和前端实施依据，不替代需求规格、接口契约或逐页面设计；当前进度以本次检出的源码和实测结果为准。

## 1. 结论与当前基线

1. 项目目标是熟人小群体使用的家庭私厨点单与饮食协作系统，不是面向公众的外卖平台。
2. 当前仓库实际包含 Java / Spring Boot REST 后端、MySQL 迁移脚本、需求与设计文档、测试资料和界面参考截图。
3. 当前工作区已创建 `frontend/` Vue 3 + Vite JavaScript 工程；阶段 1 已加入 Vue Router、五个主导航、移动/桌面布局、路由守卫骨架及通用展示组件。Pinia、真实登录和业务页面尚未实现。`docs/frontend/` 是设计文档目录；`screenshots/` 是参考图。
4. 后端当前配置以源码为准：Java 17、Spring Boot 3.3.2、Spring Data JPA / Hibernate、Spring Security + JWT、MySQL 数据源、Flyway、Hibernate `ddl-auto=validate`。本机目标库和迁移进度见[MySQL 数据库设计](../database/MYSQL_DATABASE_DESIGN.md)。
5. 本文编写前实际运行了 `mvn -B clean verify`：构建成功，现有 3 个自动化测试通过并完成 JAR 打包。该结果不代表前端已存在，也不代表 MySQL 完整验收或生产准备已完成。
6. 项目历史记录曾写“前端阶段 0、1、2 已完成”，但本次检出时没有这些前端文件。历史记录保留为过程记录；**当前实现状态以当前文件为准，本次从阶段 0 重新初始化**。

### 1.1 资料冲突时的采用顺序

发生冲突时，按以下顺序核对，不要直接复制旧文档内容：

1. 当前后端源码、`pom.xml`、`application.properties` 和实际执行结果。
2. 当前需求上下文中的“已确认”规则及当前有效决策记录。
3. [API 前后端契约](../backend/API_FRONTEND_CONTRACT.md)和[前端开发路线图](./FRONTEND_DEVELOPMENT_ROADMAP.md)。
4. 项目分析、课程报告和 README 等阶段性快照。

已发现的主要历史差异：

- 根目录 [README.md](../../README.md)、[AGENTS.md](../../AGENTS.md)、`BACKEND_PROGRESS.md`、`AUTH_FAMILY.md`、`API_FRONTEND_CONTRACT.md` 和路线图中仍有 H2、旧测试数量或旧模块状态描述；当前 `pom.xml`、`application.properties` 和 V1 迁移脚本显示项目已经切换到 MySQL。
- 部分旧文档提到餐厅、旧菜单、旧订单、Gemini 聊天模块；这些相关 Java 文件在当前 `src/main/java` 清单中已不存在，不属于当前前端可接入的家庭业务。
- [COURSE_REPORT_WORKLOG.md](../report/COURSE_REPORT_WORKLOG.md) 和 [AI_INTERACTION_LOG.md](../report/AI_INTERACTION_LOG.md) 记录过前端阶段 0~2 的工作，但当前 checkout 没有对应工程文件。
- `docs/backend/API_FRONTEND_CONTRACT.md` 中有基于旧代码阶段的测试类和数据库描述；接口字段应在联调时再对照当前 Controller、DTO 与真实响应核实。

## 2. 项目文件构成

以下清单依据 2026-11-15 当前工作区的文件扫描整理，忽略 `.git/` 和构建输出 `target/`。

### 2.1 根目录

| 文件 / 目录 | 作用与注意事项 |
|---|---|
| `AGENTS.md` | 项目级 AI 开发规则、需求边界、权限审批和工作流程；其中数据库现状文字仍有 H2 旧描述，不能覆盖当前配置。 |
| `README.md` | 项目概述、启动方式和目录导航；部分数据库及测试说明已过时。 |
| `pom.xml` | Java / Spring Boot Maven 依赖、Java 版本和构建插件；当前包括 MySQL Connector/J、Flyway 和后端测试依赖。 |
| `Dockerfile`、`.dockerignore` | 后端容器化相关文件；不代表本机已运行 Docker，也不是前端工程配置。 |
| `.gitignore` | 忽略 Maven 输出、IDE 文件、本地数据库、AI 工具目录和本地配置等。 |
| `application-local.properties` | 本机忽略配置文件；可能含数据库或应用凭据，不应复制到文档、前端环境变量或 Git。 |
| `暂时性说明文档.md` | 早期项目文件构成说明；应结合当前源码与本文件复核。 |
| `.github/modernize/` | Java 升级/现代化工具的配置和 hook 脚本，不属于业务运行代码。 |
| `.codebuddy/` | 项目专用 AI 规则和 Skills。前端相关文件包括 `rules/project-workflow.mdc`、`skills/frontend-design-cn/SKILL.md`、`skills/frontend-spec/SKILL.md`、`skills/frontend-design-optimizer/`。其中通用规范对 Token 存储的建议与历史 D-023 决策存在冲突，见第 8 节。 |
| `data/orderdb.mv.db` | 本地遗留 H2 数据库文件；当前应用配置指向 MySQL，不应把它当成当前运行数据库，也不要在未确认前删除。 |
| `target/` | Maven 生成的 `.class`、JAR、测试报告和资源拷贝；构建产物，不是源代码。 |

### 2.2 后端源代码 `src/main/java/com/fooddelivery/`

当前扫描到 72 个 Java 文件。整体结构采用 Controller → Service → Repository 的 REST 分层。

| 目录 / 文件 | 作用与当前内容 |
|---|---|
| `FoodDeliveryApplication.java` | Spring Boot 应用入口。 |
| `controller/` | REST 路由及 HTTP 请求/响应适配：`AuthController`、`CartController`、`DishController`、`FamilyController`、`FamilyOrderController`、`HomeController`、`ShoppingListController`。 |
| `dto/` | API 输入输出结构：`AddCartItemRequest`、`AddShoppingItemRequest`、`AssignResponsibleRequest`、`AuthResponse`、`CartResponse`、`CreateFamilyOrderRequest`、`CreateFamilyRequest`、`DishCategoryRequest`、`DishCategoryResponse`、`DishRequest`、`DishResponse`、`FamilyMemberResponse`、`FamilyOrderResponse`、`FamilyResponse`、`LoginRequest`、`RegisterRequest`、`ShoppingItemResponse`、`UpdateCartItemRequest`、`UpdateCookStatusRequest`。前端字段以 DTO 和实际 JSON 为准，不以数据库列名猜字段名。 |
| `entity/` | 持久化模型：`CartItem`、`Dish`、`DishCategory`、`DishIngredient`、`DishStep`、`Family`、`FamilyMember`、`FamilyOrder`、`FamilyOrderItem`、`FamilyShoppingItem`、`User`。 |
| `exception/` | 业务异常和统一错误响应：`BadRequestException`、`ConflictException`、`ErrorResponse`、`ForbiddenException`、`GlobalExceptionHandler`、`ResourceNotFoundException`。 |
| `repository/` | JPA 数据访问接口：`CartItemRepository`、`DishCategoryRepository`、`DishIngredientRepository`、`DishRepository`、`DishStepRepository`、`FamilyMemberRepository`、`FamilyOrderItemRepository`、`FamilyOrderRepository`、`FamilyRepository`、`FamilyShoppingItemRepository`、`UserRepository`。 |
| `security/` | 登录用户加载、JWT 过滤、Token 工具和 Spring Security 配置：`CustomUserDetailsService`、`JwtAuthFilter`、`JwtUtil`、`SecurityConfig`。 |
| `service/` | 服务接口：`AuthService`、`CartService`、`CurrentUserService`、`DishService`、`FamilyOrderService`、`FamilyService`、`ShoppingListService`。 |
| `service/impl/` | 业务实现：`CartServiceImpl`、`DishServiceImpl`、`FamilyOrderServiceImpl`、`FamilyServiceImpl`、`ShoppingListServiceImpl`。 |
| `util/` | `EmailNormalizer`：邮箱规范化工具。 |

当前 Controller 清单不含 `RestaurantController`、`MenuItemController`、旧 `OrderController` 或 `ChatController`；相关旧外卖模型/聊天类也不在当前 Java 文件树内。前端只应使用当前家庭业务接口。

### 2.3 后端资源、迁移与测试

| 路径 | 作用 |
|---|---|
| `src/main/resources/application.properties` | MySQL 数据源、JWT、Flyway 和 Hibernate 配置入口。默认端口为 8080；凭据由环境变量提供，不能写进前端代码。 |
| `src/main/resources/db/migration/V1__create_homechef_schema.sql` | 当前 MySQL 初始迁移，创建用户、家庭、菜品、购物车、家庭订单和采购清单等表及约束。 |
| `src/test/java/com/fooddelivery/dto/AddShoppingItemRequestValidationTest.java` | 采购项请求校验的单元测试。 |
| `src/test/java/com/fooddelivery/util/EmailNormalizerTest.java` | 邮箱规范化的单元测试。 |

本次扫描到 2 个测试源文件；最近一次 Maven 验证为 3 个测试通过。后端 HTTP 接口的更完整历史实测结果见数据库设计文档和相关测试记录，不能把单元测试数量等同于业务覆盖率。

### 2.4 `docs/` 文档目录

| 子目录 | 当前文件与用途 |
|---|---|
| `docs/README.md` | 文档索引；本次需同步登记本文件。 |
| `docs/backend/` | `API_FRONTEND_CONTRACT.md`（接口契约）、`AUTH_FAMILY.md`（认证/家庭模块）、`BACKEND_PROGRESS.md`（后端进度）、`CART_MODULE.md`（购物车）、`DISH_MODULE.md`（菜品）、`ORDER_MODULE.md`（订单/备菜）。其中若干说明仍是 H2 时期快照。 |
| `docs/database/` | `MYSQL_DATABASE_DESIGN.md`（MySQL 结构与迁移记录）、`MYSQL_MIGRATION_ISOLATED_TEST_REPORT.md`（依赖兼容性历史报告）。 |
| `docs/frontend/` | `FRONTEND_DESIGN.md`（交互和视觉方向）、`FRONTEND_DEVELOPMENT_ROADMAP.md`（分阶段路线图）、本文件（项目现状与前端开工约束）。 |
| `docs/requirements/` | `REQUIREMENT_CONTEXT.md`（已确认/待确认业务规则）、`REQUIREMENT_CODE_GAP_ANALYSIS.md`（旧项目与目标差距）、`USER_STORIES_AND_ACCEPTANCE.md`（用户故事及 90 条测试规格）。 |
| `docs/project-analysis/` | `PROJECT_ANALYSIS.md`（早期项目静态分析）、`RUN_VERIFICATION.md`（阶段性构建/启动记录）。 |
| `docs/testing/` | 原项目 API 基线用例、订单 HTTP 测试结果与 PowerShell 回归脚本；注意不同脚本对应不同历史代码/数据库阶段。 |
| `docs/report/` | `DECISION_LOG.md`（技术决策）、`COURSE_REPORT_WORKLOG.md`、`AI_INTERACTION_LOG.md`、`PROJECT_REPORT.md`（课程报告与过程记录）。 |

### 2.5 参考图片与当前前端工程状态

`screenshots/` 保留桌面/移动端菜品、家庭、订单和登录页面截图，可用于视觉走查。图片是参考资料，不是页面实现。

当前目录扫描结果（2026-11-15 阶段 0 实施后）：

- `frontend/`：已存在 Vue 3 + Vite 工程，当前只有工程启动占位页。
- `frontend/package.json` / `frontend/package-lock.json`：已存在；包含 Vue、Axios、Vite 和 Vue 插件。
- `docs/frontend/`：设计、路线图文档及本说明，不是源码目录。
- `src/`：目前是 Java 后端及 2 个 Java 测试源文件。

阶段 0 骨架位于仓库根目录 `frontend/`，与后端 `src/`、`docs/` 平级；不要把占位页误认为业务功能已实现。

## 3. 当前后端能力与前端可用范围

以下是当前家庭系统的前端对接范围。完整请求/响应字段以[API 契约](../backend/API_FRONTEND_CONTRACT.md)和当前 Controller/DTO 为准。

| 模块 | 当前接口范围 | 前端功能边界 |
|---|---|---|
| 认证 | `POST /api/auth/register`、`POST /api/auth/login` | 注册、登录；成功返回 JWT。 |
| 家庭 | `POST /api/families`、`GET /api/families`、`GET /api/families/{familyId}`、`GET /api/families/{familyId}/members`、`GET /api/families/current/members` | 创建/选择家庭、查看家庭信息和成员；“当前家庭”由前端选择并通过请求头传递，不在服务端持久化。 |
| 菜品与分类 | `GET/POST /api/dishes`、`GET/PUT /api/dishes/{dishId}`、`PATCH /api/dishes/{dishId}/availability`、`GET/POST /api/dishes/categories` | 浏览、筛选、详情、创建/编辑、上架/下架、分类列表/新增。修改及上下架须考虑创建者/家庭管理员权限。 |
| 个人购物车 | `GET/DELETE /api/cart`、`POST /api/cart/items`、`PATCH/DELETE /api/cart/items/{itemId}` | 查看、加购、改数量、删除、清空；每个用户在每个家庭有独立购物车。 |
| 家庭订单与备菜 | `POST/GET /api/family-orders`、`GET /api/family-orders/{orderId}`、订单取消/开始备菜/开饭接口、订单明细备菜状态及负责人接口 | 提交购物车、查看订单、协作备菜、指派负责人、标记开饭。具体方法与路径见 API 契约。 |
| 采购清单 | `GET /api/shopping-list`、`POST /api/shopping-list/items` | 仅查看与新增实际缺少的食材。 |
| 首页探活 | `GET /` | 可用于开发环境检查后端是否启动，不是业务首页数据 API。 |

## 4. 产品与业务要求

### 4.1 产品定位和第一版目标

- 用户为家庭、情侣、室友等熟人小群体；家庭之间的数据隔离。
- 第一版主闭环：登录/注册 → 创建或选择家庭 → 维护菜品 → 个人购物车 → 提交家庭订单 → 查看订单并协作备菜 → 登记实际缺少的食材。
- 不做公开菜品广场、支付、配送、商家入驻或平台抽佣。
- AI 菜谱整理、AI 饮食建议和外部美食数据源均为后续选项，不作为当前已实现能力。

### 4.2 用户、家庭和权限

- 一个用户可以属于多个家庭；创建家庭的用户成为该家庭管理员。
- 家庭角色是 `ADMIN` / `MEMBER`，与 `AuthResponse.role` 的系统账号角色不是一回事。
- 家庭业务请求必须使用 `X-Family-Id`；后端会验证登录用户是否属于该家庭。前端的路由守卫或隐藏按钮只是 UX，不是权限边界。
- 家庭切换后，菜品、订单、采购清单及购物车都应重新加载；不得复用上一个家庭的页面数据。
- 购物车按“用户 + 家庭”隔离，订单则是家庭成员可共同查看/协作的数据。

### 4.3 菜品、金额与订单

- 菜品详情包括简介、图片 URL、分类、耗时、食材/用量/单位和有序步骤。
- 当前没有图片上传 API；`imageUrl` 只是字符串输入/展示，不得伪造上传功能。
- 加入购物车不创建订单；提交时前端只发送当前后端约定的餐次/备注等字段，不发送价格或总金额。
- 服务端负责重新计价、创建订单明细快照并在成功后清空已提交购物车。前端以 API 返回的 `unitPrice`、`subtotal`、`totalAmount` 为准，不负责最终计价。
- 订单整体状态、单菜备菜状态和开饭标记是不同概念，不要混为一个进度字段。
- 备菜状态按后端允许的转移展示；标记 `BLOCKED` 时必须收集原因。`SERVED` 为终态。
- 前端只在后端当前允许时展示取消操作。当前实现为下单者本人可取消 `CREATED` 且所有菜仍为 `PENDING` 的订单；需求上下文还写有管理员取消家庭订单的基本权限，两者存在差异，见第 8 节。

### 4.4 采购清单

- 清单只记录备菜过程中实际发现缺少的食材，不自动把菜谱全部食材加入。
- 当前只有查看和新增；没有已购买状态、编辑、删除或误添加纠正接口。

## 5. 已确认的前端方案与页面结构

### 5.1 技术方案

根据 [FRONTEND_DESIGN.md](./FRONTEND_DESIGN.md) 和 [DECISION_LOG.md](../report/DECISION_LOG.md) 的 D-018、D-020、D-022、D-023：

- Vue 3（`<script setup>`）+ Vite。
- Axios 统一封装 HTTP 请求。
- Vue Router 4 组织 SPA 路由。
- Pinia 管理登录态和当前家庭状态。
- Vant 已作为移动端 UI 组件库选定；D-022 将它的实际引入推迟到业务页面需要时，避免空壳阶段引入未使用依赖。
- 一套移动优先的响应式 Web 工程：移动端底部导航，桌面端顶部导航/限宽布局；不另做两套页面。
- 视觉方向参考 `screenshots/`：温馨生活感、奶油白/浅绿、柔和暖色，移动端优先、卡片/列表清晰、按钮便于触控。

当前工程已安装 Vue、Axios、Vue Router、Vite 和 Vue 插件；Vant 按 D-022 暂缓到业务页面需要时再引入，Pinia 留待阶段二。修改依赖前仍应核对兼容性与脚本。

### 5.2 主导航和建议路由

| 主导航 | 建议路由 | 页面范围 |
|---|---|---|
| 小家（菜品库） | `/dishes`、`/dishes/:id`、`/dish-edit/:id?` | 分类/搜索、菜品列表、详情、创建/编辑、上下架。 |
| 厨房（订单与备菜） | `/orders`、`/orders/:id` | 订单列表/详情、备菜状态、负责人、开饭。 |
| 购物车 | `/cart` | 当前用户和家庭的个人购物车、数量编辑、提交订单。 |
| 采购清单 | `/shopping` | 查看清单、新增缺货记录。 |
| 家庭 | `/family` | 家庭选择、创建、家庭信息、成员列表。 |

认证页 `/login`（可包含注册切换或使用独立 `/register`）不占主导航。五个主导航是 D-020 已确认决定，不要擅自把采购清单并入家庭页。

## 6. 前后端联调约定

### 6.1 请求和家庭上下文

- 本地后端基础地址默认 `http://localhost:8080`，没有额外 context path。
- 开发服务器建议用 `/api` 代理到后端；代理目标和环境变量不应包含数据库凭据、JWT 签名密钥等秘密。
- 所有受保护请求带 `Authorization: Bearer <token>`。
- 菜品、购物车、家庭订单、采购清单以及 `GET /api/families/current/members` 需要 `X-Family-Id: <当前家庭 ID>`。
- `/api/families` 下其他路径按 API 契约执行，不能为了统一而无条件添加/遗漏请求头。
- 统一 API 层负责附加 Token 和家庭头、处理错误；页面组件不重复拼请求头。

### 6.2 权限和错误

- 未认证通常返回 `401`；已认证但不属于目标家庭/无操作权限返回 `403`；资源不存在或不属于当前家庭可能返回 `404`。
- 错误响应包括 `status`、`message`、`path`，参数校验失败时还会有 `fieldErrors`。
- 登录接口的 `401` 表示认证失败，不应按“Token 过期”循环跳转。
- 后端是最终权限校验方。前端可根据当前家庭角色和创建者信息改善 UX，但不可把客户端判断当作安全控制。
- `FamilyResponse.currentUserRole` 表示家庭角色；不要用注册/登录响应中的系统角色判断菜品或家庭管理员权限。

### 6.3 数据展示和操作反馈

- 为请求中的页面提供加载中、空状态、错误重试、提交中和重复提交保护。
- 表单先做必要的客户端校验，但服务端校验结果仍需展示；`fieldErrors` 应映射回对应输入项。
- 日期字段以真实 JSON 格式为准；不要仅凭文档示例假定时区或格式。
- 写接口多数会返回变更后的完整购物车/订单响应；按真实返回刷新视图，不要假定需要客户端合成状态。

## 7. 第一版明确不做的前端能力

后端当前没有相应接口或业务实现，第一版不得做成“已可用”：

- 邀请码生成、撤销、加入家庭；成员移除、角色修改。
- 个人资料、头像管理、饮食偏好。
- 订单评论、家庭聊天、AI 菜谱导入或 AI 饮食建议。
- 菜品物理删除、分类修改/删除。
- 采购清单已购买、删除、编辑或撤销。
- 图片文件上传。
- 订单 `COMPLETED` 的显式触发入口。
- 旧 `/api/restaurants`、`/api/menu-items`、`/api/orders`、`/api/chat` 外卖/聊天接口。

## 8. 开工前需保留的差异与待核实项

1. **Token 持久化安全决策冲突**：D-023 历史记录选择将 Token 存入 `localStorage`；`.codebuddy/skills/frontend-spec/SKILL.md` 的通用安全规范又要求敏感信息不要放在 `localStorage`，并禁止明文存储 Token。初始化登录持久化前应明确采用的方案及 XSS 风险防护；不要在实现时静默忽略其中一方。
2. **订单取消权限差异**：[REQUIREMENT_CONTEXT.md](../requirements/REQUIREMENT_CONTEXT.md) 已确认下单者和管理员都有基本取消权限；当前接口实现只允许下单者本人取消。第一版前端按已实现接口做，不展示“管理员代取消”入口；如需满足管理员权限，需先由用户决定是否调整后端/需求。
3. **前端目录状态差异**：历史过程记录显示前端阶段 0~2 曾完成，但当前文件缺失。前端重新启动时应视为新工程初始化，不假设既有组件、路由、状态管理或 API 客户端存在。
4. **后端说明文档过期**：H2、H2 Console、旧模型仍出现在部分文档。当前 MySQL 配置和迁移脚本才是数据库现状；前端无需连接数据库，所有数据通过后端 API 获取。
5. **数据库完整验收未完成**：用户决定暂不执行备份恢复、并发等专项验收；这不妨碍搭建前端或基于已有接口进行开发，但不能据此宣称数据库达到生产上线标准。
6. **订单状态待确认**：`COMPLETED` 没有当前前端可调用的完成接口；不得在界面提供无后端支持的完成按钮。
7. **视觉细节仍会分阶段确认**：风格和主导航方向已确定，具体页面布局、空态文案和交互细节可在各模块实现前结合截图/原型确认。

## 9. 建议的前端实施顺序与验收

根据 [FRONTEND_DEVELOPMENT_ROADMAP.md](./FRONTEND_DEVELOPMENT_ROADMAP.md)，并结合当前前端目录缺失，建议从阶段 0 重新开始：

| 阶段 | 目标 | 验收重点 | 当前状态 |
|---|---|---|---|
| 0 | 初始化 `frontend/`、依赖、脚本、环境变量和 `/api` 开发代理 | `npm run dev` 可访问；代理请求能到达后端；不泄露后端秘密。 | 2026-11-15 验收通过：构建、首页和代理均已验证。 |
| 1 | 布局、嵌套路由、设计令牌、五个导航和响应式骨架 | 移动/桌面导航正确，刷新路由可用，未知路由有兜底。 | 已实现；构建/单元测试/SPA 路由 HTTP 验证通过；响应式视觉待人工确认。 |
| 2 | 注册/登录、认证状态、家庭列表/创建/切换 | Token 与家庭头处理一致；未登录路由受保护；切换家庭后业务数据重新加载。 |
| 3 | 菜品库与分类 | 筛选、详情、创建/编辑、上下架按接口/权限运行。 |
| 4 | 个人购物车与提交订单 | 用户/家庭隔离正确；金额以服务端返回为准；重复提交受控；提交成功后刷新购物车/订单。 |
| 5 | 订单与备菜协作 | 订单状态和单菜状态分开；合法状态操作、负责人和取消规则与后端一致。 |
| 6 | 采购清单 | 仅查看/新增实际缺少食材，不出现未实现的购买/删除功能。 |
| 7 | 家庭信息和成员展示 | 家庭切换联动；只显示当前接口提供的信息和能力。 |
| 8 | 联调、异常态与响应式回归 | 认证/越权/空态/网络失败/防重复提交及手机、桌面布局有实测记录。 |
| 9 | 演示和交付准备 | 演示数据、启动步骤、构建与端到端主流程记录齐全。 |

每次只实现一个明确阶段；先核对需求、接口和源码，再编码。每阶段完成后执行相应构建/测试、审查 Diff、汇报结果，确认后再进入下一阶段。

## 10. 开始前的阅读清单

每个前端阶段至少按以下顺序阅读：

1. 本文件，确认当前代码状态、范围和已知差异。
2. [需求上下文](../requirements/REQUIREMENT_CONTEXT.md)及[用户故事与验收标准](../requirements/USER_STORIES_AND_ACCEPTANCE.md)。
3. [API 前后端契约](../backend/API_FRONTEND_CONTRACT.md)。
4. [前端设计](./FRONTEND_DESIGN.md)和[开发路线图](./FRONTEND_DEVELOPMENT_ROADMAP.md)。
5. 当前阶段涉及的 Controller、DTO、Service 与 Security 源码。
6. 涉及订单/家庭隔离/金额时，核对[数据库设计](../database/MYSQL_DATABASE_DESIGN.md)及对应后端模块文档。

文档字段或状态与源码不符时，以当前源码和真实 HTTP 响应为准，并将差异记录在本文件或阶段记录中。
