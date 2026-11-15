# 家庭私厨点单与饮食协作系统 · 课程设计报告

> 本文档是课程设计最终报告的持续完善版，随项目推进逐步补充。
> 填写规则：已确认内容如实记录；尚未实施的内容标注「待实施」；尚未验证的内容标注「待验证」；不虚构功能实现、测试结果、性能指标或 AI 执行记录。
> 当前进度更新：家庭业务后端和 H2→MySQL 9.6.0 首轮迁移已实施；旧章节中关于“尚未开始新业务”或“H2 为当前数据库”的文字属于早期基线，最新数据库执行事实见 `docs/database/MYSQL_DATABASE_DESIGN.md` 与 `docs/report/COURSE_REPORT_WORKLOG.md`。

---

## 1. 项目概述

### 1.1 项目背景

课程设计要求同时体现传统软件工程开发流程与 AI 大模型工程化实践。项目选取一个已有的开源后端项目 `manishxydev/food-delivery-backend`（外卖点餐后端）作为改造基础，将其二次开发为面向家庭内部使用的「家庭私厨点单与饮食协作系统」。

原项目是一个纯后端 REST API，采用经典分层架构，具备用户认证（JWT）、餐厅/菜品/订单管理以及基于 Google Gemini 的 AI 聊天能力，但安全与权限体系不完整。这为课程设计提供了既可直接复用、又需补齐完善的真实改造场景。

### 1.2 项目目标

- 将"外卖平台"业务模型重构为"家庭内部私厨"业务模型。
- 覆盖家庭成员、菜品、购物车、下单、厨房备菜/开饭、采购清单、评论、饮食偏好等核心场景。
- 在改造过程中补齐角色授权与资源归属校验，建立可运行的自动化测试集。
- 完整记录传统软件工程流程与 AI 辅助开发的工程化实践。

### 1.3 项目范围

- **当前已完成**：原项目分析和基线验证、第一版需求规格、家庭业务后端核心模块、MySQL 9.6.0 首轮迁移及核心 API 冒烟测试。
- **当前未完成**：前端业务页面、邀请码与饮食偏好模块、数据库并发/约束负向/备份恢复专项验收。
- **第一阶段范围（待实施）**：优先围绕家庭成员、菜品、购物车、订单、厨房与采购业务设计；AI 菜谱与饮食建议暂缓。

### 1.4 开发环境与技术栈

以下信息来自本地真实文件（`pom.xml`、`application.properties`、`Dockerfile`、`README.md`）。

| 类别 | 技术 | 说明 |
|------|------|------|
| 语言 | Java 17 | `pom.xml:21` |
| 框架 | Spring Boot 3.3.2 | 应用框架 |
| 构建 | Maven（Docker 用 Maven 3.9 + temurin-17） | `Dockerfile:2` |
| Web | spring-boot-starter-web | REST 接口、内嵌 Tomcat |
| 持久化 | Spring Data JPA / Hibernate | ORM，`ddl-auto=validate` |
| 校验 | spring-boot-starter-validation | DTO 参数校验 |
| 安全 | Spring Security + JWT（jjwt 0.12.5） | 认证、BCrypt、过滤器链 |
| 数据库 | MySQL 9.6.0 | `homechef_platform`；Flyway 10.10.0 管理 schema |
| 工具库 | Lombok 1.18.32 | 减少样板代码 |
| 外部 API | Google Gemini | AI 聊天（暂缓启用） |
| 测试 | spring-boot-starter-test | 当前 3 项单元测试通过；业务 API 已进行部分 MySQL 实测 |

---

## 2. 需求分析

### 2.1 用户角色分析

> 已产出第一版需求规格（`docs/requirements/USER_STORIES_AND_ACCEPTANCE.md` V1.0），角色约定见其第 2 节。

- 未登录用户：仅可注册、登录。
- 家庭管理员：管理家庭成员、生成/撤销邀请码、管理（含他人）菜品、取消家庭订单。
- 家庭普通成员：浏览菜品、使用个人购物车、下单、参与备菜协作、记录食材不足。

> 原项目角色（`User.Role`：CUSTOMER / RESTAURANT_OWNER / ADMIN）仅作为字段存在，未真正参与授权，改造时需重新定义角色与权限模型（见 `REQUIREMENT_CODE_GAP_ANALYSIS.md`）。

### 2.2 功能需求

> 已产出第一版功能规格（`USER_STORIES_AND_ACCEPTANCE.md` V1.0），划分为 8 个 Epic、33 个 User Story：

1. E-01 用户注册、登录与身份认证。
2. E-02 家庭空间与成员管理。
3. E-03 邀请码邀请与加入家庭。
4. E-04 家庭菜品库管理。
5. E-05 个人独立购物车与家庭点单。
6. E-06 订单管理与备菜协作。
7. E-07 食材不足记录与家庭采购清单。
8. E-08 个人资料与饮食偏好。

订单评论、AI 菜谱整理、AI 饮食建议等属于后续扩展，未纳入第一版核心验收范围。

### 2.3 非功能需求

> **待实施**。初步关注点（待细化）：

- **安全**：补齐全角色授权、资源归属校验、异常信息脱敏。
- **性能**：解决现有 N+1 查询，合理使用懒加载。
- **数据持久化**：已迁移到 MySQL 9.6.0；备份恢复演练和 Flyway 版本范围风险仍需跟进。
- **可维护性**：统一接口风格、抽取公共 Mapper。

### 2.4 业务流程分析

> 目标核心流程（已体现在需求规格与差距分析中，尚未进入系统设计）：家庭成员选菜 → 个人购物车 → 提交订单 → 厨房接单 → 备菜协作 → 开饭 → 食材不足触发采购清单。（订单评论为后续扩展）

### 2.5 需求验收标准

> 已产出验收标准与测试规格（`USER_STORIES_AND_ACCEPTANCE.md` V1.0）：33 个 User Story 均含 Given/When/Then 验收标准，并设计 90 个测试用例（正常 38 / 异常 10 / 权限 14 / 家庭隔离 14 / 边界 7 / 事务 6 / 并发 1）。全部用例当前「待执行」，预期结果均为「源码推导」。

---

## 3. 系统设计

### 3.1 系统总体架构

- 沿用原项目的**分层架构**（已确定，见 `DECISION_LOG.md` D-002）：

```
Controller（HTTP 接入） → Service（业务/事务） → Repository（数据访问） → Entity（表映射）
```

- 通过 **DTO**（`*Request` / `*Response`）隔离接口契约与内部实体。
- 安全侧：`JwtAuthFilter` + `SecurityConfig` 组成无状态 JWT 认证链。

### 3.2 功能模块设计

> 新业务功能模块**待实施**。原项目已有模块如下（供复用参考）：

| 模块 | 主要类 |
|------|--------|
| 用户认证 | `AuthController`、`AuthService` |
| 餐厅管理 | `RestaurantController`、`RestaurantServiceImpl` |
| 菜品管理 | `MenuItemController`、`MenuItemServiceImpl` |
| 订单管理 | `OrderController`、`OrderService` |
| AI 聊天 | `ChatController`、`ChatOrchestratorService`、`ChatTools`、`GeminiClient` |

### 3.3 数据库设计

原项目实体关系已梳理（详见 `PROJECT_ANALYSIS.md` 第 5 节）：

- 5 张表：`users`、`restaurants`、`menu_items`、`orders`、`order_items`。
- 关联：餐厅 1—N 菜品；订单 1—N 订单明细；订单多对一 用户/餐厅；订单明细多对一 菜品。
- `restaurants.owner_id` 为普通列（非外键），餐厅与用户是弱关联。

> 家庭私厨核心实体数据库设计与 MySQL 迁移已实施；ER 图、完整设计和 V1 执行结果见 `docs/database/MYSQL_DATABASE_DESIGN.md`。评论、饮食偏好等后续业务不在当前 11 个实体范围内。

### 3.4 API 接口设计

> 新业务 API **待实施**。原项目已实现的 REST 接口（见 `README.md`）可作参考：`/api/auth/**`、`/api/restaurants/**`、`/api/menu-items/**`、`/api/orders/**`、`/api/chat`。

### 3.5 安全设计

> 基于原项目分析，安全设计需重点补齐（当前为**待实施**的改造项）：

- 角色授权：原项目 `SecurityConfig` 仅有 `authenticated()`，无 `hasRole`。
- 资源归属：订单/餐厅/菜品接口缺当前用户归属校验。
- 注册权限：注册接口允许自选角色，需限制默认角色。
- 异常脱敏：全局异常处理泄露 `ex.getMessage()`。

---

## 4. 系统编码实现

### 4.1 开发框架与技术选型

- 沿用 Java 17、Spring Boot 3.3.2、JPA、Security 和 JWT；持久层已从 H2 迁移到 MySQL 9.6.0。
- 家庭业务后端核心模块已实现；前端业务页面和部分后续业务仍待开展。

### 4.2 核心业务实现

> **待实施**。原项目可供参考的优质实现（`OrderService.createOrder`）：

- 事务一致性：类级 `@Transactional`，多表写入同事务。
- 金额精度：全程 `BigDecimal`。
- 价格快照：`OrderItem.priceAtOrder` 保存下单时单价。

### 4.3 关键 Java 技术分析

> 本报告涉及的核心 Java 技术及对应真实代码（详见 `PROJECT_ANALYSIS.md` 第 8 节）：

- 接口与实现：`service/MenuItemService` ↔ `service/impl/MenuItemServiceImpl`。
- 泛型与集合：`JpaRepository<User, Long>`、`List`/`Stream`。
- 注解：`@Entity`、`@Service`、`@RestController`、`@Transactional`、`@Value`。
- Spring IOC/DI：`@RequiredArgsConstructor` + `final` 字段构造器注入。
- JPA/Hibernate：`@OneToMany`、`@ManyToOne`、`cascade`、`mappedBy`。
- Spring Security/JWT：`security/` 包全部 4 个类。

### 4.4 前后端交互实现

> **待实施**。原项目为纯后端，无前端；新系统是否提供前端（Web 页面或 API 客户端）待定。

---

## 5. AI 工程化开发实践

> 本节基于实际已发生的 AI 辅助开发过程记录（详见 `docs/report/AI_INTERACTION_LOG.md`）。

### 5.1 AI 工具与模型选择

- 使用的 AI 工具：CodeBuddy（IDE 内置 AI 编程助手）。
- 模型名称：DeepSeek-V4 Pro（来自运行环境声明）；精确版本号**待确认**。
- 调用方式：IDE 集成调用。

### 5.2 需求规约与 SDD 实践

- **已开展**：以结构化任务规格驱动分析（明确分析范围、输出结构、约束条件）。
- **待实施**：正式的新业务需求规约、SDD（Spec-Driven Development）规格文档与可验收测试集尚未建立。

### 5.3 AI Agent 辅助开发流程

- **已开展**：通过自然语言任务规格，驱动 AI 完成项目静态分析与文档生成。
- 使用能力：文件读取、代码搜索、目录列举、文档写入等工具（Agent 能力包）。
- **待实施**：AI 参与新业务编码的流程尚未开始。

### 5.4 Skill 设计与使用

- **当前状态**：尚未为本项目设计或使用自定义 Skill（**待实施**）。
- 后续可考虑：沉淀"项目分析""报告模板""安全审查"等可复用 Skill。

### 5.5 AI 生成代码的人工审查

- **已开展**：对 AI 产出的分析文档进行人工审查（核对真实文件、修正措辞、区分"已确认/待验证"）。
- **待实施**：AI 生成业务代码后的代码审查。

### 5.6 高风险操作人工审批

- **当前状态**：已发生的任务均为只读分析 + 新建文档，未触发高风险操作审批。
- 后续涉及删除文件、改数据库、改配置等操作时，需人工审批后再执行。

### 5.7 AI 迭代与错误修正

- **已开展**：首次整篇文档写入超长被截断，改为分段写入解决（见任务 001 经验总结）。
- **待实施**：AI 生成代码出错后的需求修订与迭代。

---

## 6. 系统测试与验证

### 6.1 测试环境

- **已确认**（2026-11-11 实测，见 `RUN_VERIFICATION.md`）：
  - 操作系统：Windows 11 + PowerShell。
  - JDK：Temurin **17.0.20.1**（满足 Java 17 要求）。
  - Maven：**3.9.15**。
  - 数据库：MySQL 9.6.0（`homechef_platform`）；H2 内容不迁移。
  - 启动方式：`mvn clean package -DskipTests` 后 `java -jar target\food-delivery-backend-0.0.1-SNAPSHOT.jar`。
  - 访问地址：`http://localhost:8080`。

### 6.2 测试方案

- **自动化测试**：原项目当时无测试；当前已有少量单元测试，完整业务集成与并发测试仍需补充。
- **运行验证方案（已执行）**：
  1. `mvn clean test` 验证编译与测试框架可运行（结果：BUILD SUCCESS，无测试可运行）。
  2. `mvn clean package -DskipTests` + `java -jar` 验证应用可启动。
  3. 浏览器访问 `GET /` 验证服务响应。
- **接口功能测试**：已对新家庭业务部分 API 执行 MySQL 冒烟验证；旧基线用例及其余边界场景仍待逐项实测。

### 6.3 测试用例

> 已形成需求验收测试规格；自动化覆盖范围仍有限，待逐步落实。

### 6.4 实际测试结果

- **Maven 构建**：`mvn -B clean test` → **BUILD SUCCESS**，3 项测试通过；`mvn -B -DskipTests package` → **BUILD SUCCESS**。
- **数据库迁移**：Flyway V1 在 MySQL 9.6.0 成功执行；11 张表、24 个外键、10 个唯一约束；应用在 `ddl-auto=validate` 下成功启动。
- **应用启动**：MySQL-backed 应用成功启动，`GET /` 返回 HTTP 200。
- **启动方式对比**：`mvn spring-boot:run` 曾失败（`NoClassDefFoundError: SpringApplication`），未作为成功依据。
- **接口功能测试**：注册/登录/家庭/菜品及子表/购物车合并与计价/下单快照/购物车清理/家庭隔离 403/采购清单新增与查询已通过 HTTP 冒烟；约束拒绝、并发冲突、事务失败回滚和备份恢复待测。

### 6.5 性能分析

- **静态分析已确认的问题**：订单/餐厅查询存在 N+1、AI 聊天会话无过期清理。
- **运行期性能指标**：**待验证**（尚未压测）。

---

## 7. 项目总结

### 7.1 项目完成情况

- **已完成**：原项目静态分析和运行基线；第一版需求规格；家庭业务后端核心实现；MySQL 9.6.0 首轮迁移；部分 API 和单元测试。
- **未完成**：前端业务页面、数据库并发和约束专项测试、备份恢复演练及演示发布。

### 7.2 遇到的问题及解决方案

| 问题 | 解决方案 | 状态 |
|------|----------|------|
| 大型分析文档单次写入超长被截断 | 分段写入 | 已解决 |
| 原项目角色授权未落地 | 规划改造为真实角色授权 | 待实施 |
| 原项目资源归属校验缺失 | 规划补充越权防护 | 待实施 |
| Flyway 10.10.0 对 MySQL 9.6 的验证范围警告 | 保持 Spring Boot 3.3.2/BOM 管理版本并记录实际迁移风险 | 待持续评估 |

### 7.3 项目不足

- 当前单元测试数量较少，接口和数据库约束/并发测试尚未全面覆盖。
- 原项目安全与权限体系不完整，需大量补齐。
- 当前 HTTP 测试为迁移后冒烟验证，不代表完整验收或性能测试。

### 7.4 后续优化方向

1. 补齐角色授权与资源归属校验。
2. 补充 MySQL 约束、隔离、并发和事务测试，并完成备份恢复演练。
3. 按已确认业务规则完善订单状态与取消权限。
4. 持续扩展自动化测试集。
5. 抽取公共 Mapper，统一接口风格。

---

> 维护说明：本文档随开发推进持续补充，对应事实素材见 `docs/report/COURSE_REPORT_WORKLOG.md`、`docs/report/AI_INTERACTION_LOG.md`、`docs/report/DECISION_LOG.md`。
