# 课程设计报告事实素材库（COURSE_REPORT_WORKLOG）

> 本文件是课程报告的**事实素材库**，记录项目全过程中已确认的真实信息。
> 维护规则：已开展 → 记录结果；尚未开展 → 标记「待开展」；尚未验证 → 标记「待验证」；信息未知 → 标记「待确认」。
> 不得把计划功能写成已实现，不得把未执行的测试写成已通过。

---

## 1. 项目基本信息

| 项 | 内容 |
|----|------|
| 项目名称 | 家庭私厨点单与饮食协作系统（二次开发目标） |
| 基础仓库 | `manishxydev/food-delivery-backend`（外卖点餐后端） |
| 项目形态 | 纯后端 REST API（原项目无前端） |
| 二次开发方向 | 家庭内部使用的私厨点单与饮食协作系统 |
| 当前阶段 | 后端核心模块及 H2→MySQL 9.6.0 首轮迁移已完成；前端阶段 0 已验收，阶段 1 基础框架已实现并通过自动化测试/构建，响应式视觉待人工确认；阶段 2 尚未实现 |
| 分析文档 | `docs/project-analysis/PROJECT_ANALYSIS.md`（已生成） |

## 2. 选题背景与目标

- **背景**：课程设计需要同时体现传统软件工程开发流程与 AI 大模型工程化实践；选择一个已有开源后端项目作为改造基础，可降低"从零搭建"成本，聚焦业务建模与二次开发。
- **目标**：将面向"外卖平台"的通用点餐后端，改造为面向"家庭内部"的私厨点单与饮食协作系统，覆盖家庭成员、菜品、购物车、下单、厨房备菜/开饭、采购清单、评论、饮食偏好等场景，并保留可选的 AI 菜谱能力。
- **当前状态**：目标已明确（见第 5、6 节），但具体功能尚未实现。

## 3. 开发环境与技术栈

以下信息来自本地真实文件（`pom.xml`、`application.properties`、`Dockerfile`、`README.md`），非推测。

| 项 | 值 | 来源 |
|----|----|----|
| 语言 | Java 17 | `pom.xml:21` |
| 框架 | Spring Boot 3.3.2 | `pom.xml:10` |
| 构建工具 | Maven（Docker 用 Maven 3.9 + temurin-17） | `Dockerfile:2` |
| Web | spring-boot-starter-web | `pom.xml:27` |
| 持久化 | Spring Data JPA / Hibernate | `pom.xml:31` |
| 校验 | spring-boot-starter-validation | `pom.xml:35` |
| 安全 | Spring Security + JWT（jjwt 0.12.5） | `pom.xml:38-68` |
| 数据库 | MySQL 9.6.0，库名 `homechef_platform`；Flyway 10.10.0 管理 schema，Hibernate `validate` | `pom.xml`、`application.properties`、`db/migration/V1__create_homechef_schema.sql` |
| 工具库 | Lombok 1.18.32 | `pom.xml:48` |
| 外部 API | Google Gemini（AI 聊天） | `application.properties:18` |
| 测试框架 | spring-boot-starter-test；当前有邮箱规范化和购物清单金额精度单元测试 | `pom.xml`、`src/test/` |

### 3.1 MySQL 迁移实施事实

- 目标实例：MySQL 9.6.0，本机 `127.0.0.1:3306`；数据库 `homechef_platform`；应用账号使用 `homechef_app`，密码通过环境变量提供。
- 保持 Java 17、Spring Boot 3.3.2、Hibernate 6.5.2.Final；Connector/J 9.7.0；Flyway Core/MySQL 10.10.0（Spring Boot BOM 管理版本）。
- Flyway V1 成功创建 11 张业务表；核验到 24 个外键、10 个唯一约束和 1 条成功 migration history。应用在 `ddl-auto=validate` 下成功启动。
- 不迁移 H2 临时数据；迁移后由真实 API 请求在 MySQL 准备新的测试记录。
- Flyway 10.10.0 的日志提示 MySQL 9.6.0 超过其已验证版本范围；本地 V1 迁移成功，但不表述为 Flyway 官方已验证支持。

## 4. 原项目来源与改造原因

- **来源**：GitHub 开源项目 `manishxydev/food-delivery-backend`（外卖点餐后端）。
- **原项目现状**：纯后端、分层清晰（Controller/Service/Repository/Entity + DTO）、含 JWT 认证与 Gemini AI 聊天模块；但角色授权未落地、资源归属校验缺失（详见 `PROJECT_ANALYSIS.md` 第 6 节）。
- **改造原因**：
  1. 已有可复用的工程骨架（分层、JWT、BCrypt、异常处理、BigDecimal 计价），避免从零搭建。
  2. 原始"餐厅外卖"模型与"家庭私厨"目标差异大，需重新建模核心业务。
  3. 原项目的安全与权限不完整，需在改造中补齐。

## 5. 需求分析过程

- **状态**：**进行中**（已产出需求上下文 V1.2、代码差距分析 V1.2、第一版需求规格 V1.0；部分需求已确认、部分待确认）。
- 已产出的需求文档：
  - `docs/requirements/REQUIREMENT_CONTEXT.md`（需求上下文，V1.2）。
  - `docs/requirements/REQUIREMENT_CODE_GAP_ANALYSIS.md`（需求与代码差距分析，V1.2）。
  - `docs/requirements/USER_STORIES_AND_ACCEPTANCE.md`（第一版需求规格：Epic/User Story/验收标准/测试规格，V1.0）。
- 本次已确认的关键需求（2026-11-12）：
  - 被历史订单引用的菜品只能下架、不得真正删除。
  - 管理员可生成、撤销邀请码。
  - 第一版采用个人独立购物车（按「用户 + 家庭」隔离，不共享）。
- 需求规格细化结果（2026-11-12）：
  - 划分为 8 个 Epic、33 个 User Story，逐条编写 Given/When/Then 验收标准。
  - 设计 90 个测试用例（正常 38 / 异常 10 / 权限 14 / 家庭隔离 14 / 边界 7 / 事务 6 / 并发 1），全部「待执行」。
- 目标场景（已细化为验收规格）：
  - 家庭成员与家庭管理
  - 菜品创建、分类与管理
  - 菜品制作步骤与食材
  - 家庭成员浏览菜品、个人独立购物车、下单
  - 厨房订单管理、备菜状态、开饭状态
  - 食材不足与家庭采购清单
  - 订单评论（后续扩展，未纳入本次 8 个 Epic）
  - 用户资料与饮食偏好
  - 可选：AI 菜谱整理与饮食建议（后续扩展）

## 6. 功能拆分与优先级

- **状态**：**待开展**（尚未正式拆分与排期）。
- 暂定（后续需细化）：第一阶段优先围绕家庭成员、菜品、订单、厨房与采购业务；AI 菜谱与饮食建议暂缓。

## 7. 系统设计过程

- **状态**：数据库设计及 MySQL 首轮迁移已完成；总体架构设计仍需结合后续需求持续完善。
- 数据库方案与真实迁移结果见 `docs/database/MYSQL_DATABASE_DESIGN.md`；ER 图、DDL、约束/索引和差异分析均在该文档。

## 8. 核心模块实现情况

- **后端状态**：家庭私厨核心业务已完成编码，并通过自动化测试与 HTTP 实测（详见 `docs/backend/BACKEND_PROGRESS.md` 第 2、4 节）：
  - 用户注册与登录 JWT（M-01）、家庭管理（M-02）：见 `docs/backend/AUTH_FAMILY.md`。
  - 家庭菜品库（M-04，含分类/食材/步骤/上下架）：见 `docs/backend/DISH_MODULE.md`。
  - 个人独立购物车（M-05）：见 `docs/backend/CART_MODULE.md`。
  - 家庭下单与订单明细（M-06）、备菜协作与开饭（M-07）、最小采购清单（M-08）：见 `docs/backend/ORDER_MODULE.md`。
  - 未开始：邀请码加入家庭（M-03）、用户资料与饮食偏好（M-09）。
- **前端历史记录**：2026-11-13 的过程记录记载阶段 0（工程初始化）、阶段 1（基础框架）和阶段 2（认证与家庭上下文）曾完成；本次检出时对应源码不在工作区，不能把历史记录当作当前代码状态：
  - 阶段 0：技术栈定稿 Vue3 + Vite + Vant + Axios；创建 `frontend/` 工程并安装依赖（`vue`/`axios`/`vite`/`@vitejs/plugin-vue`）；配置 `/api` 代理（`vite.config.js`，转发到 `localhost:8080`）；实测代理链路「前端→代理→后端」贯通（受保护接口返回 401）。
  - 阶段 1：引入 `vue-router@4` 并建立嵌套路由（`/` → `MainLayout` 父布局 + 五个子路由 `dishes/orders/cart/shopping/family` + 404）；落地设计令牌（`src/styles/tokens.css`：奶油白 `#FAF6EF`、浅绿主色 `#6B9E7C`、暖黄点缀 `#E8B04B`）与全局样式（`base.css`）；设计公共布局 `MainLayout.vue`（电脑端顶部导航 + 手机端底部五栏 TabBar，由单一数据源 `navItems` 驱动）；内置 SVG 图标组件 `NavIcon.vue` 与统一占位组件 `PagePlaceholder.vue`；创建五个空白占位页。实测：`npm run build` 成功、dev server 五个子路由与未知路由全部 200（history fallback 正常）、无头截图像素抽样确认移动端底栏/桌面端顶栏/背景色正确。业务页面（菜品/购物车/订单等）尚未编码，按 `FRONTEND_DEVELOPMENT_ROADMAP.md` 阶段 3 起逐模块开展。前后端交接文档见 `docs/backend/API_FRONTEND_CONTRACT.md`，前端设计见 `docs/frontend/FRONTEND_DESIGN.md`。
  - 阶段 2：引入 `pinia` 状态管理（`store/auth.js` 管 token/用户、`store/family.js` 管家庭列表与当前家庭，均持久化 localStorage）；`api/request.js` axios 实例 + 请求拦截器（自动附 `Authorization` 与 `X-Family-Id`）+ 响应拦截器（非登录接口 401 统一清空登录态并跳 `/login`）；`LoginView.vue` 登录/注册切换（字段级 + 通用错误）；`FamilySelect.vue` 家庭列表/切换/创建；`FamilyView.vue` 由占位页改为家庭选择 + 创建 + 退出；路由新增 `/login` 独立路由（脱离 MainLayout）与前置守卫（未登录 → `/login`，已登录访问 `/login` → `/dishes`）；`MainLayout.vue` 桌面顶导航加用户区 + 退出。实测：`npm run build` 成功、后端真实 HTTP 联调通过（注册 201 / 重复邮箱 409 / 错误密码 401 / 登录 200 / 创建家庭 201 / 带 `X-Family-Id` 访问 200 / 缺失 400）、未登录访问 `/dishes` 被重定向到 `/login`（无头截图哈希一致）。
- **当前工作区前端状态（2026-11-15）**：阶段 0 已验收（构建、首页和用户实测 `/api/families` 经代理返回 HTTP 401）；阶段 1 已实现 Vue Router、五导航、设计令牌、响应式布局、通用展示组件和登录守卫骨架。`npm test` 3 项通过、`npm run build` 成功，七个路径的 SPA fallback 均返回 HTTP 200；因本机缺少 Playwright Chromium，手机/桌面视觉仍待人工确认。真实登录、Pinia 与业务页面尚未实现。
- 原项目已实现模块（仅供复用参考，非本次新实现）：
  - 用户认证（`AuthService`、`AuthController`）
  - 餐厅管理（`RestaurantServiceImpl`，写接口已冻结）
  - 菜品管理（`MenuItemServiceImpl`，写接口已冻结）
  - 订单管理（`OrderService`，IDOR 越权已修复，见 D-017）
  - AI 聊天（`ChatOrchestratorService`、`ChatTools`、`GeminiClient`，按 D-004 暂缓）

## 9. 测试与验证结果

- **状态**：原项目基线验证与本轮 MySQL 迁移验证均已进行；剩余并发、约束负向和备份恢复专项待执行。
- 原项目当时没有自动化测试代码；迁移后新增了少量单元测试，勿将原项目历史状态误作当前状态。

**三个不同概念须区分清楚：**

| 概念 | 实际状态 |
|------|----------|
| Maven 构建/测试 | **成功**（本轮 `mvn -B clean test`：3 项测试通过；`mvn -B -DskipTests package`：BUILD SUCCESS） |
| MySQL 迁移与 ORM 校验 | **成功**（Flyway V1 成功；应用启动时 Hibernate `ddl-auto=validate` 通过） |
| HTTP 冒烟测试 | **部分通过**（注册/登录/家庭/菜品/购物车/下单快照/购物车清理/家庭隔离/采购清单已实测；并发与数据库约束负向测试待补） |

- **自动化测试**：历史原项目 `mvn clean test` 无测试类；迁移后当前测试集有 3 项并已通过，覆盖范围有限，不代表业务集成全覆盖。
- **启动方式**：`mvn spring-boot:run` 曾失败（`NoClassDefFoundError: SpringApplication`），已改用 `java -jar` 方式成功启动；详见 `RUN_VERIFICATION.md`。
- **接口验证**：`GET /` 已实测返回服务状态 JSON；注册、登录、餐厅、菜品等受保护接口**待实测**，不得记为通过。

## 10. 性能和安全分析

- **已完成静态分析**（见 `PROJECT_ANALYSIS.md` 第 6、7 节），主要结论：
  - 安全：角色授权未实现、资源归属校验缺失、注册可自选角色（权限提升风险）、异常信息泄露、JWT 过滤器无异常处理。
  - 性能：订单/餐厅查询存在 N+1 问题；AI 聊天会话无过期清理。
  - 优点：金额用 `BigDecimal`、下单价格快照、事务边界合理。
- **运行期性能**：待验证（尚未运行压测）。

## 11. AI 工程化实践摘要

- 已记录 17 项 AI 辅助任务；任务 017 记录当前工作区前端阶段 1 基础布局与路由实现，模型标识未由运行环境提供，响应式视觉待人工确认，详见 `AI_INTERACTION_LOG.md`。
- 后续开发过程中的 AI 使用情况将按模板逐条追加。

## 12. 项目成果与不足

- **当前成果**：原项目分析与基线记录、需求规格、家庭业务后端及 MySQL 9.6.0 首轮迁移已完成；前端阶段 0 已验收，阶段 1 基础框架实现并通过自动化测试/构建，视觉待人工确认；阶段 2 及后续业务模块尚未实现。
- **当前不足**：前端业务页面尚未编码；邀请码加入家庭（M-03）、资料与饮食偏好（M-09）未实现；MySQL 并发、约束负向及备份恢复测试待补；订单管理员取消权限与需求有差异；Flyway 对 MySQL 9.6.0 的验证范围警告待评估。

## 13. 后续优化方向

- 补齐安全基线（角色授权 + 资源归属校验）。
- 完成 MySQL 9.6.0 的备份与恢复演练，并评估 Flyway 10.10.0 版本范围警告。
- 补齐乐观锁冲突、家庭隔离、数据库约束拒绝及事务失败自动化测试。
- 按业务决策完善订单状态与取消权限行为。

## 14. 最终演示和发布信息

- **状态**：**待开展**（尚未到演示与发布阶段）。

---

> 维护记录：2026-11-11 创建，录入当前已确认事实；其余阶段标记「待开展/待验证」。2026-11-12 更新第 1/9/11/12 节，同步原项目构建与启动验证结果，区分「构建成功 / 启动成功 / 接口测试通过」三个概念。
> 维护记录：2026-11-12 更新第 5/11/12 节，记录需求澄清与文档同步（菜品删除规则、邀请码管理权限已确认；邀请码有效期/使用次数、订单取消边界等仍待确认）。
> 维护记录：2026-11-12 更新第 5/11/12 节，确认第一版采用个人独立购物车（按「用户 + 家庭」隔离），购物车不再列为待确认。
> 维护记录：2026-11-12 更新第 5/11/12 节，记录需求文档修正（订单状态与备菜状态分离、购物车与结算职责划分）及第一版 SDD 需求细化（8 Epic / 33 User Story / 90 测试用例）。
> 维护记录：2026-11-13 更新第 1/8/11/12 节，记录后端核心模块完成、前端开发准备审查、前后端交接文档（`API_FRONTEND_CONTRACT.md`）与前端设计初稿（`FRONTEND_DESIGN.md`）产出，以及决策 D-018（前端技术栈，待确认）/D-019（设计风格参考，已确定）。
> 维护记录：2026-11-13 更新第 1/8/11/12 节，记录前端阶段 0（技术选型与工程初始化）完成：Vue3+Vite+Vant+Axios 技术栈定稿（D-018 已确定）、主导航保留五个独立模块（D-020）、第一版暂不接入 AI 聊天模块（D-021）；`frontend/` 工程初始化、依赖安装、`/api` 代理配置，实测 `npm run build` 成功、dev server 200 OK、代理链路受保护接口返回 401（链路贯通）。
> 维护记录：2026-11-13 更新第 1/8/11/12 节，记录前端阶段 1（基础框架与整体布局）完成：引入 Vue Router 嵌套路由、落地设计令牌与全局样式、公共布局（桌面顶部导航 + 移动底部五栏 TabBar）、五个主导航与空白占位页、响应式适配（决策 D-022：暂缓引入 Vant/Pinia）；实测 `npm run build` 成功、五个子路由与未知路由均 200（history fallback 正常）、无头截图像素抽样确认移动端底栏/桌面端顶栏/背景色正确。
> 维护记录：2026-11-13 更新第 1/8/11/12 节，记录前端阶段 2（用户认证与家庭上下文）完成：引入 `pinia` 状态管理（`auth`/`family` store，localStorage 持久化）、axios 双拦截器（自动附 `Authorization` 与 `X-Family-Id`、401 统一跳转）、登录/注册页、路由前置守卫、家庭选择与创建（决策 D-023：Vant 仍暂缓）；实测 `npm run build` 成功、后端真实 HTTP 联调通过（注册/登录/家庭/`X-Family-Id` 联动）、未登录访问 `/dishes` 被重定向到 `/login`（无头截图哈希一致）。
> 维护记录：2026-11-15 更新第 1/8/11/12 节：核对当前检出中没有历史前端阶段 0~2 源码；按用户要求重建阶段 0 Vue/Vite JavaScript 骨架。构建和首页 200 已实测；后端未运行导致代理返回 502，未将阶段 0 标记为完成，阶段 1/2 未开始。
> 维护记录：2026-11-15 更新第 1/8/12 节：用户启动前后端后提供 `/api/families` 经 Vite 代理返回 401 `Authentication required` 的实测结果；阶段 0 验收通过，阶段 1/2 尚未开始。
> 维护记录：2026-11-15 更新第 1/8/11/12 节：实现前端阶段 1 Vue Router、五导航、响应式布局和通用组件；`npm test` 3 项通过、`npm run build` 成功、SPA 路由 HTTP 检查 7 路径均 200。Playwright Chromium 缺失，视觉验收待人工确认；真实登录和业务功能仍属后续阶段。
