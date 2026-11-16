# 项目文档总索引（docs/README）

> 本文件是 `docs/` 目录及项目级文档的统一导航，帮助快速定位"某任务该读哪份文档"。
> 状态图例：**已完成**（内容已落实）、**草稿/持续更新**（随开发补充）、**待验证**（结论尚未实测）、**待创建**（尚未建立，暂不预建空文件）。

---

## 1. 文档总览

| 文档 | 作用 | 当前状态 | 性质 |
|------|------|----------|------|
| `../AGENTS.md`（项目根目录） | 项目级 AI 开发规则（权限边界、流程、安全约束、交付格式） | 已完成 | **项目级重要依据** |
| `docs/README.md`（本文件） | 文档总索引与导航 | 已完成 | 导航 |
| `requirements/REQUIREMENT_CONTEXT.md` | 需求上下文：项目背景、业务目标、已确认/待确认需求（V1.2） | 已完成（持续补充） | 需求依据 |
| `requirements/REQUIREMENT_CODE_GAP_ANALYSIS.md` | 需求与现有项目代码差距分析（V1.2，只读静态分析） | 已完成 | 需求分析依据 |
| `requirements/USER_STORIES_AND_ACCEPTANCE.md` | 第一版需求规格：Epic → User Story → 验收标准 → 90 条测试用例（V1.2） | 需求规格初稿（90 条用例均「待执行」） | 需求规格（SDD） |
| `project-analysis/PROJECT_ANALYSIS.md` | 原项目全面静态分析（架构、数据库、安全、代码质量、Java 学习地图） | 已完成（部分结论待验证；原 44 文件快照，遗留外卖模型已删除） | 项目级现状认知依据 |
| `project-analysis/RUN_VERIFICATION.md` | 构建/启动运行验证记录（真实命令输出） | 已完成（H2 时代快照） | 阶段性验证记录 |
| `testing/BASELINE_API_TEST_CASES.md` | 原项目基线接口测试用例（注册/登录/餐厅/菜品查询） | 已完成（用例为推导，待实测；覆盖的旧接口已删除） | 阶段性验证记录 |
| `report/DECISION_LOG.md` | 关键技术决策记录（D-001 ~ D-025，含 MySQL 迁移、家庭订单、两套状态分离、邀请码加入等） | 草稿/持续更新 | 项目级决策依据 + 历史记录 |
| `report/COURSE_REPORT_WORKLOG.md` | 课程报告事实素材库（14 个章节） | 草稿/持续更新（已覆盖 MySQL 迁移、后端 M-01~M-09 与前端阶段 0~7 + 对齐入口） | 项目级事实依据 |
| `report/AI_INTERACTION_LOG.md` | AI 辅助开发过程记录（统一模板） | 草稿/持续更新（已记录至任务 021） | 历史记录 |
| `report/PROJECT_REPORT.md` | 课程设计最终报告的持续完善版 | 草稿/持续更新 | 最终交付产出 |
| `database/MYSQL_DATABASE_DESIGN.md` | 当前 11 个实体的 MySQL 9.6.0 设计、ER 图、DDL、约束/索引、迁移执行、回滚和验收结果 | Flyway V1 已在 `homechef_platform` 成功迁移，11 张表/24 外键/10 唯一约束；Hibernate validate 通过；核心 API 冒烟通过；备份恢复与并发专项待完成 | 数据库设计与迁移记录 |
| `database/MYSQL_MIGRATION_ISOLATED_TEST_REPORT.md` | 迁移前 MySQL 9.6、Connector/J、Flyway 的隔离兼容性测试及真实错误 | 历史报告；Flyway 13.12.2 不兼容已改用 BOM 管理的 10.10.0 规避并完成迁移 | 历史阶段测试报告 |
| `backend/BACKEND_PROGRESS.md` | 后端进度总览：技术栈、模块状态、数据表、实测结果、遗留问题 | 已完成（2026-11-15 已同步 MySQL + 遗留代码删除 + 前端阶段 0~7 + 对齐入口） | 后端进度导航 |
| `backend/API_FRONTEND_CONTRACT.md` | 前后端交接文档（API 契约、认证、家庭隔离、错误处理规则） | 已完成（H2 时代编写，含旧外卖接口描述，字段以源码为准） | 前端对接依据 |
| `backend/AUTH_FAMILY.md` | M-01/M-02 认证与家庭模块说明 | 已完成（含已删除的 Service 单测文件名，见一致性说明） | 模块说明 |
| `backend/DISH_MODULE.md` | M-04 家庭菜品模块说明（分类/食材/步骤/图片/上下架） | 已完成（同上，见一致性说明） | 模块说明 |
| `backend/CART_MODULE.md` | M-05 个人独立购物车模块说明 | 已完成（同上） | 模块说明 |
| `backend/ORDER_MODULE.md` | M-06/M-07/M-08 家庭订单、备菜协作、采购清单模块说明 | 已完成（含"保留旧订单"等已过时描述，见一致性说明） | 模块说明 |
| `frontend/FRONTEND_DESIGN.md` | 前端视觉方向、技术栈与主导航方案 | 技术栈与主导航已确认，页面细节按阶段细化 | 前端设计依据 |
| `frontend/FRONTEND_DEVELOPMENT_ROADMAP.md` | 前端开发路线图（阶段 0 ~ 9，每阶段交付/验收/测试） | 阶段 0~7 及 3 个对齐入口（成员移除/改角色、采购清单编辑、菜品删除）已实现，`npm run build` 通过；阶段 8/9 待推进 | 前端实施依据 |
| `frontend/FRONTEND_DEVELOPMENT_GUIDE.md` | 前端项目结构、真实后端能力、前端约束与历史文档差异 | 持续更新（阶段 0~7 及 3 个对齐入口已实现，视觉待人工验收） | 前端开发与联调导航 |

---

## 2. 各文档详细说明与使用场景

### 2.1 `../AGENTS.md`（项目根目录）

- **作用**：规定 AI 助手在本项目的统一行为规则（基本信息、技术基线、业务方向、开发流程、文档读取规则、权限边界、安全约束、教学要求、记录要求、交付格式）。
- **何时读**：**每次任务开始前必读**。
- **性质**：项目级最高优先级规则。

### 2.2 `docs/README.md`（本文件）

- **作用**：文档导航，说明"有哪些文档、各自做什么、何时读"。
- **何时读**：任务开始定位专业文档时；文档新增/变更后需同步更新本索引。
- **性质**：导航。

### 2.3 `requirements/REQUIREMENT_CONTEXT.md`

- **作用**：统一项目背景、业务目标、已确认需求与待确认事项（V1.2）。
- **何时读**：开展任何需求相关设计/开发前；需要确认"哪些已确认、哪些待确认"时。
- **性质**：需求依据。标记「待确认」的内容不得擅自实现。

### 2.4 `requirements/REQUIREMENT_CODE_GAP_ANALYSIS.md`

- **作用**：需求与现有项目代码的差距分析（V1.2，只读静态分析），标注「源码依据 / 待验证 / 待确认」。
- **何时读**：开展新业务设计、判断"哪些已有、哪些要改"时。
- **性质**：需求分析依据。

### 2.5 `requirements/USER_STORIES_AND_ACCEPTANCE.md`

- **作用**：第一版需求规格（SDD），按 Epic → User Story → 验收标准 → 测试用例分解，共 90 条用例。
- **何时读**：设计、AI 编码、人工验收、接口测试前。
- **性质**：需求规格。本文档 90 条用例当前均为「待执行」，不声称任何用例已通过。

### 2.6 `project-analysis/PROJECT_ANALYSIS.md`

- **作用**：对原项目 44 个 Java 源文件与配置的静态分析，覆盖架构、业务模块、数据库、安全、事务/代码质量、Java 学习地图等。
- **何时读**：理解原项目现状、技术栈、已知缺陷时。
- **性质**：项目级现状认知依据（H2 时代快照，其中原外卖遗留模块已删除）。

### 2.7 `project-analysis/RUN_VERIFICATION.md`

- **作用**：记录 JDK/Maven 版本、构建、测试、启动的真实命令与输出。
- **何时读**：构建、启动、排查环境问题，或确认"哪些已验证"时。
- **性质**：阶段性验证记录（H2 时代快照）。

### 2.8 `testing/BASELINE_API_TEST_CASES.md`

- **作用**：原项目注册、登录、餐厅/菜品查询等接口的基线测试用例（API-001 ~ API-116）。
- **何时读**：追溯原项目接口基线时。
- **性质**：阶段性验证记录（预期结果为推导；其覆盖的旧外卖接口已删除）。

### 2.9 `report/DECISION_LOG.md`

- **作用**：记录项目关键技术决策（D-001 ~ D-025）。
- **何时读**：理解"为什么这么选"时；做出新决策前避免与历史冲突。
- **性质**：项目级决策依据 + 历史记录（不覆盖，只追加）。

### 2.10 `report/COURSE_REPORT_WORKLOG.md`

- **作用**：课程报告的事实素材库，按 14 个章节汇总已确认信息。
- **何时读**：撰写或更新课程报告、汇总真实进度时。
- **性质**：项目级事实依据。

### 2.11 `report/AI_INTERACTION_LOG.md`

- **作用**：按统一模板记录每次重要 AI 辅助任务。
- **何时读**：回顾 AI 工程化实践、撰写报告第 5 章时。
- **性质**：历史记录。

### 2.12 `report/PROJECT_REPORT.md`

- **作用**：课程设计最终报告的持续完善版。
- **何时读**：查看/续写最终报告时。
- **性质**：最终交付产出。

### 2.13 `database/MYSQL_DATABASE_DESIGN.md`

- **作用**：当前 11 个 JPA 实体的 MySQL 9.6.0 设计、ER 图、DDL、约束/索引、迁移执行、回滚与验收状态。
- **何时读**：审查迁移结果、规划后续数据库测试时。
- **性质**：迁移已实施记录。

### 2.14 `database/MYSQL_MIGRATION_ISOLATED_TEST_REPORT.md`

- **作用**：迁移前 MySQL 9.6 / Connector/J / Flyway 隔离兼容性测试与真实错误。
- **何时读**：追溯迁移前依赖选型时。
- **性质**：历史阶段测试报告。

### 2.15 `backend/BACKEND_PROGRESS.md`

- **作用**：后端进度总览（技术栈、模块状态、数据表、实测结果、遗留问题、前端对接）。
- **何时读**：需要了解后端当前进度、下一步待办时。
- **性质**：后端进度导航（仅维护总览，不写单模块细节）。

### 2.16 `backend/API_FRONTEND_CONTRACT.md`

- **作用**：面向前端开发的逐接口契约（方法/URL、请求头、参数、JSON 示例、状态码与业务错误），并汇总认证、家庭隔离、错误处理规则。
- **何时读**：开发或对接前端、核对接口结构时。
- **性质**：前端对接依据。注意：本文档 H2 时代编写，其中"H2 内存库""旧外卖接口仍存在"等描述已过时，接口字段仍以当前 Controller/DTO 为准。

### 2.17 `backend/AUTH_FAMILY.md`

- **作用**：M-01（注册/登录/JWT）与 M-02（家庭与成员管理）模块说明。
- **何时读**：理解认证与家庭模块实现、排查相关接口时。
- **性质**：模块说明（H2 时代编写，含已删除的 Service 单测文件名，见第 6 节）。

### 2.18 `backend/DISH_MODULE.md`

- **作用**：M-04 家庭菜品模块说明（分类/食材/步骤/图片 URL/上下架）。
- **何时读**：理解菜品模块实现时。
- **性质**：模块说明（同上）。

### 2.19 `backend/CART_MODULE.md`

- **作用**：M-05 个人独立购物车模块说明（按「用户 + 家庭」隔离）。
- **何时读**：理解购物车模块实现时。
- **性质**：模块说明（同上）。

### 2.20 `backend/ORDER_MODULE.md`

- **作用**：M-06/M-07/M-08 家庭订单、备菜协作、最小采购清单模块说明。
- **何时读**：理解订单、备菜、采购清单实现时。
- **性质**：模块说明（含"保留旧订单""旧订单表"等已过时描述，见第 6 节）。

### 2.21 `frontend/FRONTEND_DESIGN.md`

- **作用**：已确认的 Vue 3 + Vite + Vant + Axios 方案、移动端优先与桌面端响应式策略、五个主导航。
- **何时读**：开展前端设计/开发前。
- **性质**：前端设计依据。

### 2.22 `frontend/FRONTEND_DEVELOPMENT_ROADMAP.md`

- **作用**：前端开发路线图，划分阶段 0 ~ 9，每阶段含交付物、验收标准、测试要求与依赖。
- **何时读**：每个前端阶段开始前、验收时。
- **性质**：前端实施依据。阶段 0~7 及 3 个对齐入口已实现，阶段 8/9（整体联调、测试部署演示）待推进。

### 2.23 `frontend/FRONTEND_DEVELOPMENT_GUIDE.md`

- **作用**：汇总当前仓库真实文件结构、后端可对接接口、已确认的前端需求与技术方案、不可实现接口边界、历史文档冲突与建议实施顺序。
- **何时读**：前端工程初始化前，以及每个前端阶段开始前。
- **性质**：前端开发导航；技术细节仍以源码和 API 实测为准。

---

## 3. 文档关系与分工

```
AGENTS.md（规则）──指导──> docs/README.md（导航）
                              │
     ┌────────────────────────┼────────────────────────┐
     │                        │                        │
 requirements/           project-analysis/ + testing/   database/
 （需求上下文/差距/规格）    （静态分析/运行验证/接口测试）  （MySQL 设计/迁移报告）
     │                        │                        │
     └────────────┬───────────┴────────────┬───────────┘
                  ▼                        ▼
           backend/（模块说明 + API 契约 + 进度总览）
                  │
                  ▼
          frontend/（设计 + 路线图 + 实施指南）
                  │
                  ▼
           report/（WORKLOG + DECISION_LOG + AI_INTERACTION_LOG → PROJECT_REPORT）
```

- **需求类**（`requirements/`）：定义"要做什么、如何验收"。
- **分析/验证类**（`project-analysis/`、`testing/`）：产出事实与结论（H2 时代快照）。
- **数据库类**（`database/`）：记录 MySQL 设计与迁移。
- **后端类**（`backend/`）：模块实现说明、API 契约与进度总览。
- **前端类**（`frontend/`）：设计、路线图与实施指南。
- **记录类**（`report/`）：沉淀过程与决策，最终汇总成正式报告。

---

## 4. 按任务类型推荐阅读顺序

| 任务类型 | 推荐阅读顺序 |
|----------|--------------|
| 理解项目现状 | `AGENTS.md` → 本文件 → `backend/BACKEND_PROGRESS.md` → `PROJECT_ANALYSIS.md` |
| 构建/启动/环境排查 | `AGENTS.md` → `RUN_VERIFICATION.md`（H2 时代）→ `database/MYSQL_DATABASE_DESIGN.md` |
| 接口实测 | `AGENTS.md` → `backend/API_FRONTEND_CONTRACT.md` → `backend/BACKEND_PROGRESS.md` |
| 做新设计/决策前 | `AGENTS.md` → `DECISION_LOG.md` → `requirements/REQUIREMENT_CONTEXT.md` |
| 更新课程报告 | `COURSE_REPORT_WORKLOG.md` → `AI_INTERACTION_LOG.md` → `DECISION_LOG.md` → `PROJECT_REPORT.md` |
| 后端开发 | `requirements/USER_STORIES_AND_ACCEPTANCE.md` → 对应 `backend/` 模块说明 → 当前源码 |
| 前端开发/对接 | `AGENTS.md` → `frontend/FRONTEND_DEVELOPMENT_GUIDE.md` → `frontend/FRONTEND_DEVELOPMENT_ROADMAP.md` → `requirements/REQUIREMENT_CONTEXT.md` → `backend/API_FRONTEND_CONTRACT.md` → `frontend/FRONTEND_DESIGN.md` → 当前阶段相关源码 |

---

## 5. 尚未创建（暂不预建空文件）

以下文档在对应阶段真正开展后再创建，**当前不建立空占位**：

- 自动化测试代码与测试报告（当前仅 2 个测试类、3 项单测，系统化集成测试待测试阶段）
- 性能分析报告（待性能分析阶段）

> 说明：需求规格（`requirements/USER_STORIES_AND_ACCEPTANCE.md`）与 MySQL 数据库设计（`database/MYSQL_DATABASE_DESIGN.md`）已建立，不再是"尚未创建"项。

---

## 6. 已知的一致性问题（仅提示，不修改）

以下为当前文档间存在的**过时/待补充**之处，后续更新对应文档时处理：

1. 后端模块文档（`AUTH_FAMILY.md`、`DISH_MODULE.md`、`CART_MODULE.md`、`ORDER_MODULE.md`）与 `API_FRONTEND_CONTRACT.md` 编写于 H2 时代：仍引用已删除的 Service 层单测文件名（如 `FamilyServiceImplTest`、`CartServiceImplTest`、`DishServiceImplTest`）与旧外卖遗留接口（餐厅/旧菜单/旧订单），且"保留旧订单"等表述已过时（旧 `Order` 表已随遗留代码删除）。**接口字段仍以当前 Controller/DTO 为准**，建议后续逐个校正。
2. 旧测试结果（66/76 项 Service 单测）属 H2 时代，对应测试类当前已不在工作区；当前实际为 2 个测试类、3 项单测。引用旧测试数的文档应更新为当前 3 项。
3. `PROJECT_ANALYSIS.md`、`RUN_VERIFICATION.md`、`BASELINE_API_TEST_CASES.md` 为原项目 H2 时代快照，其"原外卖遗留模型仍存在"的结论已过时，仅作历史参考。

> 已在本轮解决：根目录 `README.md` 与 `backend/BACKEND_PROGRESS.md` 已从 H2/无前端/遗留代码时代更新为 MySQL + Flyway + 前端阶段 0/1 状态，消除此前的 H2 与前端进度不一致。

> 内容重复说明：`PROJECT_ANALYSIS.md` 与 `RUN_VERIFICATION.md` 有部分技术栈重叠，但前者是"静态分析结论"、后者是"实测验证结果"，职责不同，均保留；`COURSE_REPORT_WORKLOG.md`（素材）与 `PROJECT_REPORT.md`（报告）同理，不删除、不合并。

---

> 维护说明：新增文档后请在本索引登记；文档状态变化时更新"当前状态"列；本索引仅做导航，不复制各文档正文。
