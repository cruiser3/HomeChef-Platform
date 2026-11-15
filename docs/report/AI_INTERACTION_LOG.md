# AI 辅助开发过程记录（AI_INTERACTION_LOG）

> 本文件按统一模板记录每次重要 AI 辅助任务。信息未知处标记「待确认」，不猜测；未执行的测试不写"通过"。

---

## 任务模板

```
### 任务 NNN：任务标题
- 日期：
- 所处开发阶段：
- 使用的 AI 工具：
- 模型名称和版本：
- 调用方式：
- 任务目标：
- 输入的需求/设计文档与 Skill：
- 使用的提示词/任务规格（摘要）：
- AI 输出与代码修改范围：
- 人工审查发现的问题：
- 是否经过人工审批：
- 实际测试命令与测试结果：
- 最终是否接受 AI 结果：
- 经验总结：
```

---

## 任务 001：原项目全面静态分析

- **日期**：2026-11-11
- **所处开发阶段**：项目理解 / 现状梳理（编码前）
- **使用的 AI 工具**：CodeBuddy（IDE 内置 AI 编程助手）
- **模型名称和版本**：DeepSeek-V4 Pro（模型名来自当前运行环境声明；精确版本号「待确认」）
- **调用方式**：IDE 集成调用（本地 IDE 内通过云端模型服务）
- **任务目标**：全面理解现有 food-delivery-backend 项目，为二次开发建立认知基础，不修改任何代码。
- **输入的需求/设计文档与 Skill**：
  - 用户提供的任务规格（分析范围、输出结构、严格限制共 10 条）。
  - 本地真实文件：`pom.xml`、`Dockerfile`、`README.md`、`application.properties`、`src/test.http` 及全部 44 个 Java 源文件。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：要求按 10 章结构分析（项目概况/目录/架构/业务模块/数据库/安全/事务质量/Java 学习地图/家庭私厨适配/运行环境），并输出到 `docs/project-analysis/PROJECT_ANALYSIS.md`；约束"只分析、不改代码、不删文件、不升级依赖、不确定结论标注待验证"。
- **AI 输出与代码修改范围**：
  - 输出：新建 `docs/project-analysis/PROJECT_ANALYSIS.md`（含目录、12 章节、Mermaid ER 图、待验证问题清单）。
  - 代码修改：**无**（未改任何 Java 代码、配置、pom.xml、README）。
- **人工审查发现的问题**：
  - 首次整篇写入超长被截断，改为分两次写入完成（先写前半部分，再追加后半部分）。
  - 文档初稿中部分措辞需与真实文件核对（已核对实体、DTO、安全配置等真实代码）。
- **是否经过人工审批**：本任务为只读分析 + 新建文档，未触发高风险操作审批；任务完成后等待人工确认。
- **实际测试命令与测试结果**：未运行任何命令（分析为纯静态阅读，不涉及编译/测试）。
- **最终是否接受 AI 结果**：已产出文档，待人工复核确认。
- **经验总结**：
  - 大型文档应分段写入，避免单次输出超限。
  - 静态分析结论必须逐条对照真实文件，不能仅凭 README 推测。
  - 明确区分"已确认/待验证/待开展"，防止把计划写成已实现。

---

## 任务 002：课程报告素材库初始化（本文件所属任务）

- **日期**：2026-11-11
- **所处开发阶段**：文档建设 / 报告素材准备
- **使用的 AI 工具**：CodeBuddy（IDE 内置 AI 编程助手）
- **模型名称和版本**：DeepSeek-V4 Pro（精确版本号「待确认」）
- **调用方式**：IDE 集成调用
- **任务目标**：建立课程设计报告素材库（WORKLOG、AI 日志、决策日志三份文档），不修改业务代码。
- **输入的需求/设计文档与 Skill**：
  - 用户提供的三份文档模板与 6 项既定决策。
  - 参考已生成的 `docs/project-analysis/PROJECT_ANALYSIS.md` 中的真实结论。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：创建 `docs/report/` 下三份 Markdown，按给定章节与模板填写；事实约束 9 条；未知信息标"待确认"，未开展标"待开展"，未验证标"待验证"。
- **AI 输出与代码修改范围**：
  - 输出：`COURSE_REPORT_WORKLOG.md`、`AI_INTERACTION_LOG.md`、`DECISION_LOG.md`。
  - 代码修改：**无**。
- **人工审查发现的问题**：待人工复核（当前刚完成初稿）。
- **是否经过人工审批**：本任务为文档新建，未触发高风险操作审批。
- **实际测试命令与测试结果**：未运行命令。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：待后续补充。

---

## 任务 003：原项目运行环境与启动验证

- **日期**：2026-11-11（运行验证实际发生日，见 `RUN_VERIFICATION.md` 检查日期；本记录补录于 2026-11-12）
- **所处开发阶段**：基线运行验证（编码前）
- **使用的 AI 工具**：CodeBuddy（IDE 内置 AI 编程助手）
- **模型名称和版本**：DeepSeek-V4 Pro（模型名来自当前运行环境声明；精确版本号「待确认」）
- **调用方式**：IDE 集成调用
- **任务目标**：核实项目的 Java、Maven、构建与启动情况，为二次开发确认"可运行"这一事实。
- **输入的需求/设计文档与 Skill**：
  - 用户提供的任务规格（只验证、不改代码的约束）。
  - 本地真实文件：`pom.xml`、`application.properties`、`Dockerfile`。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：检查 Java/Maven 版本、执行构建与测试、验证启动方式；约束"不修改 Java 源码、pom.xml、数据库配置、Dockerfile"。
- **AI 输出与代码修改范围**：
  - 输出：`docs/project-analysis/RUN_VERIFICATION.md`（真实命令与结果记录）。
  - 代码修改：**无**。
- **人工审查发现的问题**：
  - `mvn spring-boot:run` 启动失败（`NoClassDefFoundError: SpringApplication`），改用 `java -jar` 方式成功启动。
  - 浏览器访问 `http://localhost:8080/` 返回服务状态 JSON，确认 `GET /` 可响应。
- **是否经过人工审批**：本任务为只读检查 + 新建文档，未触发高风险操作审批；启动应用涉及本地运行，未改动业务代码。
- **实际测试命令与测试结果**：
  - `java -version` → Temurin 17.0.20.1。
  - `mvn -version` → Maven 3.9.15。
  - `mvn clean test` → BUILD SUCCESS，Surefire 输出 `No tests to run.`（无测试代码）。
  - `mvn spring-boot:run` → BUILD FAILURE（`NoClassDefFoundError: SpringApplication`）。
  - `mvn clean package -DskipTests` + `java -jar target\food-delivery-backend-0.0.1-SNAPSHOT.jar` → 启动成功。
- **最终是否接受 AI 结果**：已产出文档，待人工复核确认。
- **经验总结**：
  - 区分"构建成功 / 启动成功 / 接口测试通过"三个概念，避免把编译通过误当成测试通过。
  - `mvn spring-boot:run` 失败不意味着应用不可运行，改用 `java -jar` 可绕开 fork classpath 问题。

---

## 任务 004：需求澄清与文档同步

- **日期**：2026-11-12
- **所处开发阶段**：需求分析（编码前）
- **使用的 AI 工具**：CodeBuddy（IDE 内置 AI 编程助手）
- **模型名称和版本**：DeepSeek-V4 Pro（精确版本号「待确认」）
- **调用方式**：IDE 集成调用
- **任务目标**：根据最新人工确认结果，同步更新需求上下文与代码差距分析文档，并记录决策与工作日志。
- **输入的需求/设计文档与 Skill**：
  - 用户提供的最新人工确认结果（菜品删除、订单取消、邀请码三类）。
  - 既有文档：`REQUIREMENT_CONTEXT.md`、`REQUIREMENT_CODE_GAP_ANALYSIS.md`、`DECISION_LOG.md`、`COURSE_REPORT_WORKLOG.md`、`AI_INTERACTION_LOG.md`。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：将已确认内容写入「已确认」，未确认内容继续标记「待确认」；不把「被引用的菜品不能物理删除」扩大为「所有菜品只能下架」；区分产品范围问题与技术设计问题；约束仅修改 Markdown。
- **AI 输出与代码修改范围**：
  - 输出：更新 5 份 Markdown（需求上下文、差距分析、决策日志、工作日志、AI 交互日志）。
  - 代码修改：**无**。
- **人工审查发现的问题**：待人工复核。
- **是否经过人工审批**：本次为文档同步，未修改代码/配置/数据库，未触发高风险操作审批。
- **实际测试命令与测试结果**：未运行任何命令。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：待后续补充。

**本次任务的真实事实区分（不伪造）**：

- **用户实际确认的内容**：
  - 被历史订单引用的菜品只能下架、不得真正删除；历史订单必须保留原有菜品信息。
  - 管理员可生成邀请码、可撤销邀请码。
- **AI 提出的建议（非用户确认）**：
  - 将「购物车、当前家庭传递机制、订单快照字段范围、`User.Role` 设计、食材复用粒度」区分为产品范围问题或后续技术设计问题。
  - 在差距分析第 8 节补充「引入逻辑删除 + 名称快照」的改造建议。
- **尚未确认的内容（仍标记待确认）**：
  - 未被历史订单引用的菜品是否允许真正删除。
  - 订单取消时间边界（部分菜品开始备菜后能否取消、管理员取消已开始备菜订单、取消后采购清单处理）。
  - 邀请码有效期、使用次数、撤销后的具体影响。

---

## 任务 005：个人独立购物车需求确认与文档同步

- **日期**：2026-11-12
- **所处开发阶段**：需求分析（编码前）
- **使用的 AI 工具**：CodeBuddy（IDE 内置 AI 编程助手）
- **模型名称和版本**：DeepSeek-V4 Pro（精确版本号「待确认」）
- **调用方式**：IDE 集成调用
- **任务目标**：将「个人独立购物车」作为正式需求同步到项目文档，不再将购物车列为待确认。
- **输入的需求/设计文档与 Skill**：
  - 用户提供的购物车确认结果（归属、功能、与订单关系、权限隔离、暂不扩展项）。
  - 既有文档：`REQUIREMENT_CONTEXT.md`、`REQUIREMENT_CODE_GAP_ANALYSIS.md`、`AGENTS.md`、`DECISION_LOG.md`、`COURSE_REPORT_WORKLOG.md`、`AI_INTERACTION_LOG.md`。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：将购物车写入「已确认」并移除「待确认」标记；明确个人独立购物车按「用户 + 家庭」隔离；修正采购清单描述；仅修改 Markdown，不改 Java/pom/数据库。
- **AI 输出与代码修改范围**：
  - 输出：更新 6 份 Markdown（需求上下文、差距分析、AGENTS、决策日志、工作日志、AI 交互日志）。
  - 代码修改：**无**（未修改 Java 源码、pom.xml、application.yml、数据库配置，未执行数据库迁移）。
- **人工审查发现的问题**：待人工复核。
- **是否经过人工审批**：本次为文档同步，未修改代码/配置/数据库，未触发高风险操作审批。
- **实际测试命令与测试结果**：未运行任何命令，未伪造测试结果。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：待后续补充。

**本次任务的真实事实区分（不伪造）**：

- **用户实际确认的内容**：
  - 第一版需要购物车，采用个人独立购物车（不共享）。
  - 购物车属于「用户 + 家庭」组合，同一用户在不同家庭各自独立。
  - 每个成员只能查看和修改自己的购物车；后端校验身份与成员资格。
  - 加入购物车不建正式订单；提交时后端重新计价、事务一致、保存快照；成功才清空、失败不清空。
  - 第一版暂不做多人共享、协同编辑、库存锁定、自动过期清理、优惠券/促销/折扣。
- **AI 提出的建议（非用户确认）**：
  - 在差距分析中补充购物车并发一致性与「提交订单原子性」的技术风险提示。
  - 购物车具体表结构、字段、接口路径留待系统设计阶段确定。
- **尚未确认的内容（仍标记待确认）**：
  - 未被历史订单引用的菜品是否允许真正删除。
  - 订单取消时间边界（部分菜品开始备菜后能否取消、管理员取消已开始备菜订单、取消后采购清单处理）。
  - 邀请码有效期、使用次数、撤销后的具体影响。

---

## 任务 006：需求文档修正与第一版 SDD 需求细化

- **日期**：2026-11-12
- **所处开发阶段**：需求分析 / 需求规格细化（编码前）
- **使用的 AI 工具**：CodeBuddy（IDE 内置 AI 编程助手）
- **模型名称和版本**：DeepSeek-V4 Pro（精确版本号「待确认」）
- **调用方式**：IDE 集成调用
- **任务目标**：修正需求文档中的设计歧义，并将第一版需求拆分为 Epic / User Story / Acceptance Criteria / 测试规格。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`、`REQUIREMENT_CONTEXT.md`、`REQUIREMENT_CODE_GAP_ANALYSIS.md`、`DECISION_LOG.md`、`COURSE_REPORT_WORKLOG.md`、`AI_INTERACTION_LOG.md`、`PROJECT_ANALYSIS.md`、`BASELINE_API_TEST_CASES.md`、`PROJECT_REPORT.md`。
  - 用户提供的任务规格（修正项、Epic 划分要求、测试类型、待确认项管理）。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：修正订单状态与备菜状态表述、调整购物车与结算职责划分、处理购物车边界；新建需求规格文档（Epic→US→AC→TC）；约束仅改 Markdown，不改 Java/pom/数据库。
- **AI 输出与代码修改范围**：
  - 输出：新建 `docs/requirements/USER_STORIES_AND_ACCEPTANCE.md`（8 Epic / 33 User Story / 90 测试用例）；修正 `REQUIREMENT_CONTEXT.md` 3.2、`REQUIREMENT_CODE_GAP_ANALYSIS.md` 第 5 节；更新 `COURSE_REPORT_WORKLOG.md`、`AI_INTERACTION_LOG.md`、`PROJECT_REPORT.md`。
  - 代码修改：**无**（未改 Java 源码、pom.xml、application.yml、数据库配置，未执行数据库迁移）。
- **人工审查发现的问题**：待人工复核。
- **是否经过人工审批**：本次为文档修正与新建，未修改代码/配置/数据库，未触发高风险操作审批。
- **实际测试命令与测试结果**：未运行任何命令，未伪造测试结果；90 个测试用例均「待执行」，预期结果均为「源码推导」。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：待后续补充。

**本次任务的真实事实区分（不伪造）**：

- **用户实际确认的内容**：
  - 购物车与结算职责划分：购物车负责添加/改数量/查看/移除/查看总金额；提交订单时负责确认内容/选时段/填备注/提交。
  - 订单整体状态与单道菜备菜状态分别管理；联动规则留待业务设计阶段明确，不预设状态机。
- **AI 提出的建议（非用户确认，标记「建议待人工确认」）**：
  - 购物车边界（CART-01~05）：重复添加累加数量、下架菜品标记并禁提交、空购物车禁提交、数量上限 99 下限 1、退出家庭后购物车保留但不可访问。
  - 备菜状态合法集合与非法转换规则留待设计阶段定义状态机。
- **尚未确认的内容（仍标记待确认）**：
  - U-001~U-008 八项需求待确认（菜品删除限制、订单取消边界、邀请码策略等）。
  - 购物车边界 CART-01~05 五项（建议待人工确认）。

---

## 任务 007：家庭菜品库模块（M-04）实现与验证

- **日期**：2026-11-12
- **所处开发阶段**：编码实现 / 单元测试 / 接口实测
- **使用的 AI 工具**：CodeBuddy（IDE 内置 AI 编程助手）
- **模型名称和版本**：DeepSeek-V4 Pro（精确版本号「待确认」）
- **调用方式**：IDE 集成调用
- **任务目标**：实现家庭菜品库：菜品创建、查询（分类筛选/名称搜索/上下架区分）、修改、上下架，并强制家庭隔离与创建者/管理员权限。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`
  - `USER_STORIES_AND_ACCEPTANCE.md` 中 US-04-01 ~ US-04-06 及 TC-04 相关条目
  - `REQUIREMENT_CODE_GAP_ANALYSIS.md` 菜品章节
  - `DECISION_LOG.md`（D-007 菜品只下架、D-010 新建 Dish）
  - `docs/backend/AUTH_FAMILY.md`（复用 `CurrentUserService`）
  - `docs/backend/BACKEND_PROGRESS.md`、`docs/project-analysis/RUN_VERIFICATION.md`
  - 新建 Skill：`.codebuddy/skills/dish-module/SKILL.md`（本模块业务约束与验证要求）
- **使用的提示词/任务规格（摘要）**：按当前登录用户所选家庭实现菜品增删改查与上下架；金额用 `BigDecimal`；食材/步骤结构化；复用既有家庭权限；不修改已完成模块；完成 `mvn clean test`；提供 Postman 步骤并区分已执行与未执行。
- **AI 输出与代码修改范围**：
  - 新增 4 个实体（`Dish`、`DishIngredient`、`DishStep`、`DishCategory`）、4 个仓储、4 个 DTO、`DishService` + `DishServiceImpl`、`DishController`、`DishServiceImplTest`。
  - 修改：`DishServiceImpl`（补 Service 层入参校验、分类名回填）。
  - 未修改：认证、JWT、家庭模块、`MenuItem`、`Order`、`SecurityConfig`、`pom.xml`、`application.properties`。
- **人工审查发现的问题**：
  - 实测发现「创建菜品返回 `categoryName` 为空」（仅列表/详情正确），已修复并在测试中加断言。
  - 决策：不复用旧 `MenuItem`，改为独立 `Dish`（见 D-010），避免破坏未改造的订单模块。
- **是否经过人工审批**：未触发高风险操作（未改安全配置、未迁移数据库、未升级依赖）；新实体与表结构变更已由用户任务规格预先授权（「直接开始实际开发」）。
- **实际测试命令与测试结果**：
  - `mvn clean test` → `BUILD SUCCESS`，`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`（其中菜品 14 个、家庭 5 个）。
  - `mvn -q package -DskipTests` → 成功。
  - `java -jar` 启动成功，HTTP 实测：创建 201、分类筛选/搜索正确、修改与上下架正常、非成员 4 类操作均 403、缺请求头 400、非法输入 400、详情不存在 404。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：AI 生成的分层代码可直接落地，但**接口返回字段的完整性**（如 `categoryName`）容易在「创建路径」遗漏，必须在启动后实测验证，而不能只依赖编译通过与 Service 单测。

---

## 任务 008：个人独立购物车模块（M-05）实现与验证

- **日期**：2026-11-12
- **所处开发阶段**：编码实现 / 单元测试 / 真实 HTTP 接口实测 / 文档记录
- **使用的 AI 工具**：Trae IDE 内置 AI 编程助手（Kimi-K2.6）
- **模型名称和版本**：Kimi-K2.6（精确版本号「待确认」）
- **调用方式**：IDE 集成调用
- **任务目标**：实现个人独立购物车：查看、添加菜品、修改数量、删除条目、清空；按「用户 + 家庭」双重隔离；菜品归属/上下架/数量校验；价格只用服务端 `Dish` 数据并以 `BigDecimal` 计算。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`
  - `REQUIREMENT_CONTEXT.md`、`USER_STORIES_AND_ACCEPTANCE.md`、`REQUIREMENT_CODE_GAP_ANALYSIS.md` 中购物车相关内容（按需读取，未全文通读）
  - `DECISION_LOG.md`（D-009 个人独立购物车、D-011/D-012/D-013 本轮新增）
  - `docs/backend/AUTH_FAMILY.md`（复用 `CurrentUserService.requireMembership`）
  - `docs/backend/DISH_MODULE.md`（`Dish` 与 `DishRepository.findByIdAndFamilyId`）
  - `docs/backend/BACKEND_PROGRESS.md`、`docs/project-analysis/RUN_VERIFICATION.md`（环境变量与启动方式）
  - 未使用 Skill（本轮未新建 Skill 文件）。
- **使用的提示词/任务规格（摘要）**：用户任务书四部分——核心业务规则（双重隔离、五项功能、菜品校验）、实现约束（复用现有栈与模块、不用 `MenuItem`、不做订单/支付/备菜、不改已完成模块、不擅自扩展、不加依赖）、测试验证（10 类必覆盖场景 + `mvn clean test` + 两用户两家庭真实 HTTP）、文档记录与最终汇报格式。
- **AI 输出与代码修改范围**：
  - 新增 9 个文件：`entity/CartItem.java`、`repository/CartItemRepository.java`、`dto/AddCartItemRequest.java`、`dto/UpdateCartItemRequest.java`、`dto/CartResponse.java`、`service/CartService.java`、`service/impl/CartServiceImpl.java`、`controller/CartController.java`、`src/test/java/com/fooddelivery/service/CartServiceImplTest.java`。
  - 修改：无（未改任何既有 Java 文件、`pom.xml`、`application.properties`）。
  - 文档新增/更新：`docs/backend/CART_MODULE.md`（新建）、`BACKEND_PROGRESS.md`、`DECISION_LOG.md`（D-011~D-013）、本文件。
- **人工审查发现的问题**：
  1. **脚本层问题（非业务缺陷）**：PowerShell 5.1 直接向 `curl.exe` 传内联 JSON 会被引号处理破坏，导致大量 POST/PATCH 返回 500；改为「先写 JSON 文件再 `--data-binary @file`」后全部恢复正常。此过程一度被误判为接口缺陷，需强调「先验证测试工具本身」。
  2. **环境变量缺失**：首次 `mvn clean test` 全部 37 个用例报 `ApplicationContext failure threshold (1) exceeded`，根因是缺 `JWT_SECRET`（`Could not resolve placeholder 'JWT_SECRET'`），属既有项目约束，非购物车代码问题；设置环境变量后 37/37 通过。
  3. **H2 控制台无法访问应用内存库**：`jdbc:h2:mem:food_delivery` 为应用进程私有内存库，外部 H2 控制台看不到数据；改为以 `--spring.datasource.url=jdbc:h2:file:...;AUTO_SERVER=TRUE` 启动后，才可用 H2 工具读取同一库。
  4. **M-03 未实现导致的测试准备缺口**：为验证「同一家庭不同用户」隔离，需第二个用户加入同一家庭，但邀请码功能未实现。采用 H2 `RunScript`/`Shell` 直接向 `family_members` 插入一条 Bob 的 MEMBER 记录作为测试准备，**未改动任何业务代码**，并在文档中明确标注该缺口。
  5. **自写断言错误**：HTTP 脚本中两处期望值写错（Alice 删除唯一条目后期望 1、应为 0），已修正后重跑通过——属于脚本 bug，不是接口 bug。
- **是否经过人工审批**：未触发高风险操作（未改安全配置、未迁移数据库、未新增依赖、未改已完成模块）。任务书明确要求「直接进入编码」，新表 `cart_items` 的创建已由任务规格预先授权。
- **实际测试命令与测试结果**：
  - **自动化测试**：`mvn clean test` → `BUILD SUCCESS`，`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`（Auth 1、Cart 18、Dish 13、Family 5）。
  - **真实 HTTP 测试**：`java -jar`（文件库 + `AUTO_SERVER`）启动 8080 后用 `curl.exe` 实测，两用户三家庭：
    - 主场景 **30/30 通过**（含跨家庭 403、跨家庭菜品 400、下架加购/改量 400、数量 0/-2/100 均 400、缺请求头 400、不存在条目 404）。
    - 同一家庭双用户隔离 **20/20 通过**（同菜品各持一条、金额分别 76.00/38.00、越权改删 404、清空互不影响）。
  - 上述两类结果均在文档 `CART_MODULE.md` 第 7、8 节逐条记录，未虚构。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：
  - 隔离类需求要落到**数据模型 + 仓储方法签名**两层，仅靠 Service 判断容易被后续新增查询绕过；仓储层「不提供只按 familyId 的查询方法」是最有效的防越权设计。
  - 写接口**请求体不设价格字段**，比「服务端忽略前端价格」更可靠。
  - 在 Windows PowerShell 环境做 HTTP 实测，应固定使用「JSON 落盘 + `curl @file`」流程，否则会把工具问题误判成接口缺陷，浪费大量时间。
  - 涉及「同家庭多用户」验证时，若缺少成员加入入口，应明确区分「测试准备手段」与「业务实现」，并在文档中如实标注缺口。

---

## 任务 009：订单与备菜协作模块开发（M-06 / M-07 / M-08）

- **日期**：2026-11-12
- **所处开发阶段**：阶段 4 收尾与阶段 5（家庭下单 + 备菜协作 + 最小采购清单）
- **使用的 AI 工具**：Trae IDE 内置 AI 编程助手
- **模型名称和版本**：Kimi-K2.6（模型名来自当前运行环境声明；精确版本号「待确认」）
- **调用方式**：IDE 集成调用，多轮交互式编码 + 命令执行 + 文档更新
- **任务目标**：以可运行、可测试、可演示为目标完成家庭订单主流程，包含下单、查询、状态流转、逐道菜备菜协作、最小家庭采购清单；同时不改动既有认证/家庭/菜品/购物车模块与旧订单模块。
- **输入的需求/设计文档与 Skill**：
  - 用户两轮任务规格：第一轮为完整模块要求（九条），第二轮为**人工审批后的调整方案**（六条，确定 Q1/Q2/Q3 与事务、隔离、合法性校验要求）。
  - 本地真实文件：`AGENTS.md`、`docs/backend/CART_MODULE.md`、`DISH_MODULE.md`、`AUTH_FAMILY.md`、`BACKEND_PROGRESS.md`、`DECISION_LOG.md`，以及 `CartItem`、`CartServiceImpl`、`Dish`、`CurrentUserService`、`GlobalExceptionHandler`、旧 `Order`/`OrderItem`/`OrderController`、`SecurityConfig`、`ChatTools` 源码。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：方案 B 调整版——新建 `FamilyOrder`/`FamilyOrderItem` 与独立表，新路径 `/api/family-orders`，旧 `/api/orders` 不迁移不修改；订单状态 `CREATED/PREPARING/COMPLETED/CANCELLED`，单菜状态 `PENDING/COOKING/READY/SERVED/BLOCKED`，两套状态独立且开始备菜由独立操作触发；`mealServed` 独立存字段；采购清单只做「实际缺少食材添加 + 查看」；负责人须为家庭成员；状态转换须校验合法性；下单+明细+清空购物车同事务，明细存菜名/单价/数量/小计快照；不破坏旧订单、不重构无关模块；完成后跑自动化测试与真实 HTTP 端到端并如实更新四份文档。
- **AI 输出与代码修改范围**：
  - 新增 Java（18 个）：`entity/FamilyOrder.java`、`entity/FamilyOrderItem.java`、`entity/FamilyShoppingItem.java`、`repository/FamilyOrderRepository.java`、`repository/FamilyOrderItemRepository.java`、`repository/FamilyShoppingItemRepository.java`、`dto/CreateFamilyOrderRequest.java`、`dto/FamilyOrderResponse.java`、`dto/UpdateCookStatusRequest.java`、`dto/AssignResponsibleRequest.java`、`dto/AddShoppingItemRequest.java`、`dto/ShoppingItemResponse.java`、`service/FamilyOrderService.java`、`service/impl/FamilyOrderServiceImpl.java`、`service/ShoppingListService.java`、`service/impl/ShoppingListServiceImpl.java`、`controller/FamilyOrderController.java`、`controller/ShoppingListController.java`。
  - 新增测试（2 个类 29 例）：`FamilyOrderServiceImplTest.java`（23 例）、`ShoppingListServiceImplTest.java`（6 例）。
  - 修改：**无**（未改任何既有 Java 文件、`pom.xml`、`application.properties`、安全配置）。
  - 文档：新建 `docs/backend/ORDER_MODULE.md`；更新 `BACKEND_PROGRESS.md`（M-06~M-08 状态、新表、验证结果、G-04/G-05）、`DECISION_LOG.md`（D-014~D-016）、本文件。
  - 验证脚本：`target/order-http-verify.ps1`（非源码，仅本地验证用）。
- **人工审查发现的问题**：
  1. **方案冲突先审批后编码**：旧 `Order` 基于 `Restaurant`/`MenuItem`，与家庭 `Dish` + 购物车模型不兼容；AI 先暂停并列出 Q1/Q2/Q3 待决项与影响面，待人工批准调整版方案后才动手，未擅自改造旧代码。
  2. **HTTP 脚本工具问题（非业务缺陷）**：PowerShell 5.1 内联中文 JSON 被引号/编码破坏，导致早期大量 500；改为「JSON 写临时文件 + `curl.exe --data-binary @file`」后恢复正常。此过程一度被误判为接口缺陷，需强调**先验证测试工具本身**。
  3. **脚本断言逻辑错误（非接口缺陷）**：首轮 85 项中 3 项失败，全部是「重复注册已存在用户」的预期码写错（脚本预期 400，实际既有认证模块抛通用 `RuntimeException` 被兜底成 500）。这属于**既有认证模块行为**，按「不重构无关模块」约束**本轮未修改**，只修正脚本断言。已记入文档说明。
  4. **测试断言细节**：`BigDecimal` 比较因 scale 不同失败，改用 `compareTo`；缺 `X-Family-Id` 时实际抛 `ForbiddenException` 而非 `BadRequestException`，修正断言并补齐成员校验。
  5. **文档编辑回归**：更新 `DECISION_LOG.md` 尾部状态记录时一度误删两行历史更新记录，已立即核对并恢复，最终用 `### D-0..` 与 `> 状态更新` 检索确认 D-001~D-016 结构与历史记录完整。
- **是否经过人工审批**：**是**。Q1（方案 B 调整版）、Q2（两套状态取值与独立触发）、Q3（最小采购清单范围）均经人工批准后方编码；未新增依赖、未修改安全配置、未迁移数据、未改动已完成模块，故未出现超出批准范围的高风险操作。
- **实际测试命令与测试结果**：
  - **自动化测试**：注入 `JWT_SECRET` / `GEMINI_API_KEY` 后 `mvn clean test` → `BUILD SUCCESS`，`Tests run: 66, Failures: 0, Errors: 0, Skipped: 0`（Auth 1、Cart 18、Dish 13、Family 5、**FamilyOrder 23**、**ShoppingList 6**）。未注入环境变量时全部 66 个用例报 `ApplicationContext failure threshold (1) exceeded`（`Could not resolve placeholder 'JWT_SECRET'`），属原项目既有约束，已在 `ORDER_MODULE.md` 第 10.1 节如实记录。
  - **真实 HTTP 端到端**：`java -jar`（H2 文件库 `AUTO_SERVER=TRUE`）启动 8080，`curl.exe` 实测，脚本 `target/order-http-verify.ps1`：**PASS=85 FAIL=0**。覆盖下单/金额/快照/清空购物车/空车下单 400/单菜状态互不影响/非法流转 400/`BLOCKED` 必填原因/开始备菜与开饭独立/负责人非成员 403/跨家庭 403 与 404/采购清单隔离与跨家庭关联 400/下架菜品下单 400 且购物车保留/重新上架后下单成功。
  - **旧功能回归确认**：旧 `/api/orders` 路由仍存在，实测对 CUSTOMER 返回 403（与改动前一致）；`Order.java` 状态枚举 `PENDING/CONFIRMED/PREPARING/OUT_FOR_DELIVERY/DELIVERED/CANCELLED` 未改动。
  - **未执行**：并发下单测试、`COMPLETED` 流转测试（规则待确认）、旧订单管理员流程回归。以上未虚构为通过。
  - 测试准备说明：HTTP 验证中「同一家庭多成员协作」由 Alice 完成，跨用户隔离由独立家庭的 Eve 验证；期间**未**用 SQL 直插成员关系，M-03 邀请码仍未实现，此点已在 `ORDER_MODULE.md` 第 10.2 节如实标注。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：
  - 遇到既有结构与新模型根本冲突时，应先**暂停并给出带影响分析的候选方案**，等人工批准再编码，比「先改后解释」成本更低、风险更小。
  - 「两套状态互不推导」这类要求，最可靠的做法是把推导通道从代码里去掉（订单状态只由专用接口写），而不是加判断去压制。
  - HTTP 实测脚本在 Windows PowerShell 下必须固定「JSON 落盘 + `curl @file`」，并让脚本对「幂等已有数据」有明确预期，否则工具噪声会掩盖真实缺陷。
  - 失败结果要区分「接口缺陷 / 工具缺陷 / 脚本断言缺陷 / 既有模块已知行为」，逐项定性后再决定是否修代码，避免违反「不重构无关模块」约束。
  - 更新长文档尾部时先核对原有行，避免误删历史记录；改完用结构化检索复核。

---

> 后续每次重要 AI 任务按模板在下方追加（任务 010 …），不覆盖历史记录。

---

## 任务 010：修复旧订单模块 IDOR 越权漏洞（最小范围安全修复）

- **日期**：2026-11-13
- **所处开发阶段**：只读审计后的安全问题修复（安全 / 回归验证）
- **使用的 AI 工具**：CodeBuddy IDE 内置 AI 编程助手
- **模型名称和版本**：DeepSeek-V4 Pro（模型名来自当前运行环境声明；精确版本号「待确认」）
- **调用方式**：IDE 集成调用，多轮交互式编码 + 命令执行 + 文档更新
- **任务目标**：修复旧 `/api/orders` 三个接口（`GET /api/orders/{id}`、`GET /api/orders/restaurant/{restaurantId}`、`PUT /api/orders/{id}/status`）的 IDOR 越权：普通用户只能读/改自己创建的订单，不能凭订单 ID 越权访问他人订单；不新增管理员体系、不改新家庭订单模块、不重构无关代码。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`（第 5.3 节高危审批、第 6 节安全约束）
  - 上一轮《后端只读审计报告》（S-1 旧 `/api/orders` IDOR 为 P0）
  - 源码：`OrderService`、`Order`、`OrderItem`、`OrderRepository`、`User`、`Restaurant`、`CurrentUserService`、`SecurityConfig`、`GlobalExceptionHandler`、`OrderController`、`RestaurantController`、`RestaurantServiceImpl`、相关 DTO
  - 测试参考：`FamilyServiceImplTest`、`AuthServiceRegisterTest`（复用 `authenticateAs`/`@SpringBootTest` + `@Transactional` 模式）
  - `docs/testing/order-http-verify-rebuilt.ps1`、`risk1-legacy-order-idor-verify.ps1`、`order-http-result-rebuilt.txt`
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：任务书八节——背景与已确认问题、最小范围修复目标（从 `SecurityContext` 取当前用户、不信任前端 userId、单查/改状态须校验创建者、餐厅查询须校验餐厅访问权、不存在与他人订单统一拒绝不泄露）、实施前先读代码、必补自动化测试（T1~T7 + 餐厅越权，禁 mock 恒真校验）、真实 HTTP Alice/Bob 验证、回归（`mvn test` + 两个 HTTP 脚本）、严格禁止事项（不删旧模块、不改 SecurityConfig/JWT/新家庭订单/购物车业务、不 commit/reset）、完成后十项汇报 + 更新三份文档。
- **AI 输出与代码修改范围**：
  - 修改（1 个 Java 文件）：`service/OrderService.java`——注入 `CurrentUserService`，`getById`/`updateStatus` 改为经 `requireOwnOrder`（校验 `order.customer.id == 当前用户`，否则 `ForbiddenException` 403），`getByRestaurant` 增加 `restaurant.ownerId == 当前用户` 一致性校验（ownerId 为 null 或不一致则 403），新增私有方法 `requireOwnOrder`。共 +28 行，未改任何 Controller/SecurityConfig/新家庭订单/数据库。
  - 新增测试（1 个类 9 例）：`src/test/java/com/fooddelivery/service/OrderServiceTest.java`（T1~T7 + 餐厅越权 + 不存在订单 404 + 餐厅不存在 404）。
  - 新增验证脚本：`docs/testing/risk1-supplementary-idor-verify.ps1`（补充 Alice 改自己状态、餐厅越权、404、my-orders、ownerId 正/负向）。
  - 修正脚本（1 处笔误）：`docs/testing/order-http-verify-rebuilt.ps1` 第 180/183/187 行路径 `availabilityavailable=` → `availability?available=`（缺 `?`，与历史 result 文件第 54 行契约一致）。
  - 文档更新：`BACKEND_PROGRESS.md`（旧订单行 + G-04）、`DECISION_LOG.md`（D-017）、本文件。
- **人工审查发现的问题**：
  1. **回归脚本路径笔误（非接口缺陷、非本次引入）**：`order-http-verify-rebuilt.ps1` 第 15/16 节 8 项 FAIL，根因是路径 `availabilityavailable=false` 缺 `?`，Spring 将整段当作静态资源抛出 `NoResourceFoundException`，落入 `GlobalExceptionHandler.handleGeneric` 返回 500。历史 result 文件第 54 行记录的契约是 `availability?available=false`，证明这是脚本笔误；修正后 63/63 通过。此问题涉及 Dish 接口，与本次 `OrderService` 修改完全无关。
  2. **无可信管理员/餐厅 owner 授权体系**：`User.Role.RESTAURANT_OWNER`/`ADMIN` 全库无授权逻辑消费、`SecurityConfig` 无 `hasRole`，故不新增管理员权限；`getByRestaurant` 仅做 ownerId 一致性校验，ownerId 为 null（旧数据/未指定）时一律 403，采用最小权限，不放大权限。
  3. **PowerShell 脚本汇总变量显示瑕疵**：`$script:Pass++` 在 `-File` 模式下汇总行显示 `PASS=0 FAIL=0`，但逐条 `Check` 输出真实（63 个 PASS 均对应真实状态码），不影响断言真实性。
- **是否经过人工审批**：**是**。用户任务书明确「我已经批准修复该漏洞」，并限定最小范围与禁止事项；本轮未改 SecurityConfig、JWT、数据库、新家庭订单模块，未触发额外高危操作。
- **实际测试命令与测试结果**：
  - **自动化测试**：注入 `JWT_SECRET`/`GEMINI_API_KEY` 后 `mvn test` → `BUILD SUCCESS`，`Tests run: 76, Failures: 0, Errors: 0, Skipped: 0`（原 67 + 新增 OrderService 9）。
  - **IDOR 专项 HTTP**：`risk1-legacy-order-idor-verify.ps1`——Bob 读/改 Alice 订单均 403，owner 正常 200，匿名 403。
  - **补充 HTTP**：`risk1-supplementary-idor-verify.ps1`——Alice 改自己状态 200、Bob 读餐厅订单 403、不存在订单 404、my-orders 200、ownerId 正向 200 / 负向 403，共 8 项 PASS。
  - **家庭订单回归**：`order-http-verify-rebuilt.ps1`（修正笔误后）——63 项断言全部 PASS、0 FAIL，证明新家庭订单模块不受影响。
  - **未执行**：未改旧 `/api/orders` 的 `getMyOrders` 等未在本次范围内的端点；未做并发越权测试。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：
  - 归属校验应落在 **Service 层**（`requireOwnOrder` 复用），而不是只在 Controller 加判断，避免后续新增入口绕过。
  - 修复边界要严格按「最小权限」：系统没有可信管理员体系时，不做「隐式放行管理员」的假设，ownerId 为空一律拒绝。
  - 回归失败要先区分「接口缺陷 / 脚本笔误 / 工具问题」；本次 8 项 FAIL 是脚本缺 `?`，查历史 result 文件的契约记录即可定位，避免误判为本次修改引入回归。
  - 记录真实测试结果时，汇总变量的显示瑕疵要单独说明，但逐条断言结果才是有效证据。

---

## 任务 011：前端开发准备审查与前后端交接文档、前端设计启动

- **日期**：2026-11-13
- **所处开发阶段**：前端开发准备 / 前后端对接设计（前端编码前）
- **使用的 AI 工具**：CodeBuddy IDE 内置 AI 编程助手
- **模型名称和版本**：Kimi-K3.1-Pro（模型名来自当前运行环境声明；精确版本号「待确认」）
- **调用方式**：IDE 集成调用，多轮交互（只读源码审查 + 文档编写）
- **任务目标**：为即将开发的 Vue 前端整理准确的前后端对接说明；编写前后端交接文档；启动前端设计（页面规划、设计风格参考），并同步更新项目文档与课程报告素材。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`（文档读取规则、审批边界、汇报要求）
  - `docs/requirements/REQUIREMENT_CONTEXT.md`、`USER_STORIES_AND_ACCEPTANCE.md`
  - `docs/backend/BACKEND_PROGRESS.md`、`docs/report/DECISION_LOG.md`、`COURSE_REPORT_WORKLOG.md`、`AI_INTERACTION_LOG.md`
  - 实际源码：全部新家庭模块与旧外卖模块的 `controller/`、`dto/`、`entity/`、`service/`，以及 `security/`（`SecurityConfig`/`JwtAuthFilter`/`JwtUtil`）、`exception/`（`GlobalExceptionHandler` 等）、`application.properties`
  - 用户提供的三张微信小程序界面截图（菜品库首页 / 创建菜品 / 安排下单），作为风格参考
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：先做只读的前端开发准备审查，逐接口列出方法/URL、请求头、参数、请求/响应 JSON、状态码、是否实现与测试；随后新建前后端交接文档与前端设计初稿，更新索引/进度/决策/AI 日志/素材库；要求以实际源码为依据、区分新旧模块与已冻结写接口、明确未实现功能、不伪造测试结果、文档用 Markdown。
- **AI 输出与代码修改范围**：
  - 新建文档：`docs/backend/API_FRONTEND_CONTRACT.md`（前后端交接）、`docs/frontend/FRONTEND_DESIGN.md`（前端设计初稿）。
  - 更新文档：`docs/README.md`（登记与推荐阅读顺序）、`docs/report/DECISION_LOG.md`（D-018/D-019）、`docs/report/COURSE_REPORT_WORKLOG.md`、`docs/backend/BACKEND_PROGRESS.md`、本文件。
  - 代码修改：**无**（未改任何 Java 源码、`pom.xml`、`application.properties`、数据库配置）。
- **人工审查发现的问题**：待人工复核。
- **是否经过人工审批**：本任务为只读源码审查 + 文档新建/更新，未修改代码/配置/数据库，未触发高风险操作审批；前端技术选型为 AI 建议，已标记「待确认」，待用户最终确认。
- **实际测试命令与测试结果**：未运行任何命令（纯文档与源码静态阅读）；接口测试通过情况以 `BACKEND_PROGRESS.md` 第 4 节既有实测为准，本任务未新增任何"测试通过"结论。
- **最终是否接受 AI 结果**：待人工确认。
- **经验总结**：
  - 前后端交接文档必须逐字段对照真实 DTO/Controller，不能凭类名或记忆猜测字段。
  - 长会话中工具结果可能被上下文压缩清理，需重新读取源码后再落笔，避免写出过时或臆测的接口结构。
  - 必须清晰区分「已实现 / 已测试 / 待确认」，前端不得把后端未实现功能（邀请码、资料偏好、评论等）当作可对接能力。
  - 将"前端设计风格参考"与"技术选型建议"分别标注为「已确定」与「待确认」，避免把 AI 建议误记为用户已拍板的决策。

---

## 任务 012：前端阶段 0 工程初始化与代理链路验证

- **日期**：2026-11-13
- **所处开发阶段**：前端开发 / 阶段 0（技术选型与工程初始化）
- **使用的 AI 工具**：CodeBuddy IDE 内置 AI 编程助手
- **模型名称和版本**：DeepSeek-V4 Pro（模型名来自当前运行环境声明；精确版本号「待确认」）
- **调用方式**：IDE 集成调用，多轮交互（环境检查 + 工程搭建 + 命令执行验证 + 文档归档）
- **任务目标**：按 `FRONTEND_DEVELOPMENT_ROADMAP.md` 阶段 0，检查 Node/npm 环境、确定技术选型、搭建最小前端工程（Vue3+Vite）、安装依赖、配置后端代理、编写基本首页，并实测验证前端启动与「前端→代理→后端」链路；不开发业务功能、不改后端与数据库，阶段 0 完成即停等待审查。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`、`docs/frontend/FRONTEND_DEVELOPMENT_ROADMAP.md`、`docs/frontend/FRONTEND_DESIGN.md`。
  - 源码核对：`SecurityConfig.java`（放行 `/`、`/api/auth/**`、`/h2-console/**`，其余未认证返回 401）、`HomeController.java`（`GET /` 返回 running）、`application.properties`、`GeminiClient.java`。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：用户任务书明确——主导航保留五个独立模块、前端优先 Vue3+Vite、组件库暂定 Vant 但须评估双端体验、Node 用兼容所选 Vite 的 LTS、第一版不接入聊天模块（不要求配置 GEMINI_API_KEY）；要求先检查 Node/npm、给出技术选型与理由、说明目录结构、创建工程并装依赖、配置代理、写基本首页、验证代理访问后端；禁止提前开发登录/菜品/购物车等业务、禁止改后端与数据库；阶段 0 完成即停、等待审查验收。
- **AI 输出与代码修改范围**：
  - 新增前端工程 `frontend/`：`package.json`、`vite.config.js`、`index.html`、`.env.development`、`.gitignore`、`src/main.js`、`src/App.vue`、`src/style.css`、`src/views/HomeView.vue`。
  - 安装依赖：`vue`、`axios`、`vite`、`@vitejs/plugin-vue`。
  - 代码修改：**无**（未改任何 Java 源码、`pom.xml`、`application.properties`、数据库配置）。
- **人工审查发现的问题**：
  1. `npm create vite` 脚手架命令被工具判定为 watch 并超时，其后台异步补全导致遗留一批未引用的模板文件（`HelloWorld.vue`、`public/*`、`src/assets/*`、`README.md`、`.vscode/*`），已清理，保持最小工程。
  2. **后端启动强依赖 `gemini.api.key`**：`GeminiClient` 为 `@Component` + `@Value("${gemini.api.key}")` 无默认值，即使前端不接入聊天模块，后端启动仍必须提供该变量；本次用命令行参数 `--gemini.api.key=dummy` 覆盖启动成功，记为技术债务（D-021）。
- **是否经过人工审批**：**是**。用户任务书明确批准从阶段 0 开始并限定范围（只做工程初始化与验证、不改后端/数据库、阶段 0 完成即停）；本次未改 Java/配置/数据库，未触发高风险操作。
- **实际测试命令与测试结果**：
  - `node -v` → v24.16.0（LTS）；`npm -v` → 11.13.0。
  - `npm run build` → 成功（Vite 8.3.8，72 模块转换，120ms）。
  - `npm run dev` → dev server 启动成功（ready in 454ms）；`curl http://localhost:5173/` → 200 OK。
  - 后端未运行时 `curl :5173/api/families` → 502（证明代理规则生效，请求被转发到 8080）。
  - 启动后端后 `curl http://localhost:8080/` → `{"status":"running","service":"Food Delivery API"}`；`curl :5173/api/families` → **401 + `{"status":401,"message":"Authentication required","path":"/api/families"}`**（证明「前端→代理→后端」链路贯通，认证机制生效）。
  - 验证用后台进程与临时日志/pid 文件均已清理（8080/5173 端口已释放）。
- **最终是否接受 AI 结果**：待人工确认（用户已审查阶段 0 交付报告，原则通过验收）。
- **经验总结**：
  - Windows PowerShell 下脚手架/长驻命令易被工具误判为 watch 并超时，用「后台启动 + 显式完成标记」或直接手动创建工程文件更可控。
  - 验证代理链路用「受保护接口返回 401」比「放行接口返回 200」更能证明认证机制与代理同时生效。
  - 技术选型评估要落到「本项目主场景是手机」这一事实上：Vant 桌面端偏弱，但本项目无复杂桌面组件需求，响应式增强即可，无需引入第二套组件库。

---

## 任务 013：前端阶段 1 基础框架与整体布局

- **日期**：2026-11-13
- **所处开发阶段**：前端开发 / 阶段 1（基础框架与整体布局）
- **使用的 AI 工具**：CodeBuddy IDE 内置 AI 编程助手
- **模型名称和版本**：DeepSeek-V4 Pro（模型名来自当前运行环境声明；精确版本号「待确认」）
- **调用方式**：IDE 集成调用，多轮交互（环境与源码核对 + 编码 + 命令执行验证 + 截图验证 + 文档归档）
- **任务目标**：按 `FRONTEND_DEVELOPMENT_ROADMAP.md` 阶段 1，搭建完整、统一、响应式的前端视觉框架：引入 Vue Router；落地设计令牌与全局样式；设计公共布局（电脑端宽屏顶部导航 + 手机端底部五栏导航）；实现五个主导航模块与五个空白占位页；响应式适配；保持 Vite 代理与启动能力。只做框架与视觉布局，不开发业务逻辑。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`（文档读取规则、审批边界、汇报要求）
  - `docs/frontend/FRONTEND_DEVELOPMENT_ROADMAP.md`（阶段 1 任务、第 3.2 节设计令牌与配色、第 4 节信息架构）
  - `docs/frontend/FRONTEND_DESIGN.md`（设计风格参考）
  - `docs/report/DECISION_LOG.md`（D-022 本轮新增）
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：用户任务书明确——引入并配置 Vue Router；建立全局样式与设计规范（颜色/字体/间距/圆角/卡片/按钮）；设计公共页面布局；实现五个主导航模块（小家/厨房/购物车/采购清单/家庭）；电脑端宽屏导航、手机端底部五栏导航；创建五个空白占位页；响应式适配（手机窄屏/普通手机/电脑宽屏）；保持 Vite 代理与启动能力。视觉方向温馨生活风为主、现代简约为辅（奶油白/浅绿/柔和暖色、圆角、留白、轻量阴影，不做外卖平台或后台）。边界：只做框架与视觉布局，不开发登录/业务逻辑，不调家庭接口，不改 Java 后端/数据库，不引入阶段 2 状态管理（Pinia/Vant），不升级已有依赖版本。完成后汇报、输出截图与真实测试结果、更新进度记录，停在阶段 1 等待审查。
- **AI 输出与代码修改范围**：
  - 新增前端文件（9 个）：
    - `src/styles/tokens.css`（全部设计令牌：`--bg`/`--card`/`--primary`/`--accent`/`--radius`/`--shadow-card`/`--tabbar-height`/`--bp-*` 等）
    - `src/styles/base.css`（全局 reset、`body`、`.container`、`.card`、`.btn-primary`、`.app-content`）
    - `src/router/index.js`（导出单一数据源 `navItems` 五模块；嵌套路由 `/` → `MainLayout` 父布局 + 子路由 `dishes/orders/cart/shopping/family` + `:pathMatch(.*)*` 404；`beforeEach` 仅设 `document.title`，阶段 1 不做鉴权）
    - `src/components/NavIcon.vue`（内联 SVG 线性图标，`currentColor` 随激活变色）
    - `src/components/PagePlaceholder.vue`（统一占位组件）
    - `src/layouts/MainLayout.vue`（桌面顶部导航 + 移动端固定底部 tabbar + 内容区预留安全区 padding）
    - `src/views/DishListView.vue`、`OrderListView.vue`、`CartView.vue`、`ShoppingListView.vue`、`FamilyView.vue`、`NotFoundView.vue`
  - 修改前端文件（3 个）：`package.json`（新增 `vue-router` 依赖）、`src/main.js`（`createApp(App).use(router)` 并引入两个全局样式）、`src/App.vue`（仅 `<RouterView />`）。
  - 删除前端文件（2 个）：`src/style.css`（旧全局样式，被 `tokens.css`/`base.css` 替代）、`src/views/HomeView.vue`（阶段 0 检测页，被占位页替代）。
  - 文档更新：`docs/report/DECISION_LOG.md`（新增 D-022：嵌套路由 + 纯 CSS 响应式 + 阶段 1 暂缓引入 Vant/Pinia）。
  - 代码修改：**无**（未改任何 Java 源码、`pom.xml`、`application.properties`、数据库配置）。
- **人工审查发现的问题**：
  1. **Edge `--dump-dom` 获取渲染后 DOM 为空**：尝试 `--headless=new` 与重定向均未解决，最终改用「无头截图 + 像素抽样」验证渲染结果，未获取到渲染后 DOM。
  2. **模型不支持读取图片内容**：`read_file` 读取截图返回 `model does not support images`；改用 `System.Drawing.Bitmap.GetPixel` 抽样验证尺寸、背景色、卡片白、移动端底栏白/桌面端顶栏白，确认非白屏且配色与设计令牌一致。
  3. **`Remove-Item` 清理日志文件返回退出码 1**：首次删除时日志文件尚未生成导致命令报错，但后台 Vite 实际已成功启动；后处理改用 `-ErrorAction SilentlyContinue` 解决。
- **是否经过人工审批**：**是**。用户任务书明确批准从阶段 1 开始并限定范围（只做框架与视觉布局、不改后端/数据库、不引入 Pinia/Vant、不升级依赖、完成即停）；本轮未改 Java/配置/数据库，未触发高风险操作。
- **实际测试命令与测试结果**：
  - `npm install vue-router@4` → 成功，`package.json` 新增 `"vue-router": "^4.6.4"`。
  - `npm run build` → 成功（Vite 8.3.8，42 modules transformed，约 974ms）。
  - `npm run dev` → 启动成功（端口 5173）；`curl` 验证 `/`、`/dishes`、`/orders`、`/cart`、`/shopping`、`/family`、`/nonexistent` 全部 200，history fallback 正常（`/nonexistent` 命中前端 404 页面而非服务端 404）。
  - 无头 Edge 截图生成 5 张（`screenshots/` 下 320×568 手机窄屏、390×844 手机两页、1440×900 桌面两页）；像素抽样验证：移动端右下角白色 = 底部 TabBar、桌面端左上角白色 = 顶部导航、背景色 `(250,246,239)` 与令牌 `#FAF6EF` 一致，确认非白屏且响应式布局生效。
  - `read_lints` 对 `frontend/src` 返回 0 错误。
  - 验证用后台 dev server 与临时日志均已清理，端口 5173 已释放。
- **最终是否接受 AI 结果**：待人工确认（阶段 1 完成后停在当前阶段，等待用户审查验收）。
- **经验总结**：
  - 模型不支持读图时，「无头截图 + `GetPixel` 像素抽样」是可行的渲染验证手段，能证明非白屏、配色正确、响应式底栏/顶栏切换生效，但无法验证具体文案与视觉细节。
  - 导航菜单做成单一数据源 `navItems`（同时驱动桌面顶部导航与移动底部 Tab），可避免两处导航项不一致。
  - SPA 采用 `createWebHistory` 时必须在 dev server 与生产环境分别验证 history fallback，否则刷新子路由会 404；本阶段通过访问 `/nonexistent` 与各子路由均返回 200 确认 fallback 生效。

---

## 任务 014：前端阶段 2 用户认证与家庭上下文

- **日期**：2026-11-13
- **所处开发阶段**：前端开发 / 阶段 2（用户认证与家庭上下文）
- **使用的 AI 工具**：CodeBuddy IDE 内置 AI 编程助手
- **模型名称和版本**：DeepSeek-V4 Pro（精确版本号待确认）
- **调用方式**：IDE 集成调用，多轮交互（源码核对 + 编码 + 构建 + 真实 HTTP 联调 + 无头截图验证 + 文档归档）
- **任务目标**：按 `FRONTEND_DEVELOPMENT_ROADMAP.md` 阶段 2，实现注册/登录/退出、Token 管理、路由鉴权、家庭列表与选择、`X-Family-Id` 统一处理。只做认证与家庭上下文，不开发后续业务页面。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`、`docs/frontend/FRONTEND_DEVELOPMENT_ROADMAP.md`（阶段 2）、`docs/backend/API_FRONTEND_CONTRACT.md`、`docs/report/DECISION_LOG.md`（D-022/D-023）。
  - 后端源码：`AuthController`、`AuthService`、`FamilyController`、`FamilyService`、`AuthResponse`、`RegisterRequest`、`LoginRequest`、`CreateFamilyRequest`、`FamilyResponse`。
  - 未使用 Skill。
- **使用的提示词/任务规格（摘要）**：用户授权「进行下一阶段」（阶段 2）。任务要点：axios 拦截器（自动带 token 与 `X-Family-Id`）；登录/注册页；路由鉴权；家庭列表与「当前家庭」选择；登录后进入默认家庭或引导创建。完成标准：注册→登录→选择/创建家庭→后续请求自动携带 `X-Family-Id`；未登录访问受保护页被重定向；退出清空状态。
- **AI 输出与代码修改范围**：
  - 新增前端文件（8 个）：`src/store/storage.js`（本地存储键与辅助）、`src/api/request.js`（axios 实例 + 请求/响应双拦截器）、`src/api/auth.js`、`src/api/family.js`、`src/store/auth.js`、`src/store/family.js`、`src/views/LoginView.vue`（登录/注册切换 + 字段级/通用错误）、`src/components/FamilySelect.vue`（家庭列表/切换/创建）。
  - 修改前端文件（5 个）：`package.json`（新增 `pinia`）、`src/main.js`（注册 Pinia）、`src/router/index.js`（`/login` 独立路由 + 前置守卫）、`src/layouts/MainLayout.vue`（桌面用户区 + 退出）、`src/views/FamilyView.vue`（占位页 → 家庭选择 + 创建 + 退出）。
  - 文档更新：`docs/report/DECISION_LOG.md`（D-023）。
  - 代码修改：**无**（未改 Java/配置/数据库）。
- **人工审查发现的问题**：
  1. PowerShell `@(Invoke-RestMethod)` 对空 JSON 数组 `[]` 计数误判（`@($null)` 的 Count=1），导致首轮脚本「新用户家庭列表 count=1」与「创建家庭 id=1」矛盾；核对 `AuthService.register` 源码确认注册不自动建家庭后，改为显式判 `$null`/`is [Array]` 精确计数，复验为 0。
  2. PowerShell 调 curl.exe 传 JSON 时双引号被吞，body 变为 `{name:Bob}` 触发 Jackson `JsonParseException`（500），用户未创建；改用 `Invoke-RestMethod -Body (ConvertTo-Json)` 规避。
  3. Edge `--dump-dom` 仍返回空（与阶段 1 一致），改用「长预算截图 + 哈希对比 + 像素抽样」验证未登录跳转。
- **是否经过人工审批**：**是**。用户明确授权进入阶段 2（「进行下一阶段」），范围由路线图阶段 2 限定；本轮未改 Java/配置/数据库，未触发高风险操作。
- **实际测试命令与测试结果**：
  - `npm install pinia` 成功（`package.json` 新增 `pinia`）。
  - `npm run build` 成功（119 modules transformed）。
  - `read_lints` 对 `frontend/src` 返回 0 错误。
  - 后端真实 HTTP 联调（`Invoke-RestMethod`）：注册 201（token 173 字符，role=CUSTOMER）、重复邮箱 409、错误密码 401、登录 200、新用户家庭列表 0、创建家庭 201（role=ADMIN、members=1）、再列家庭 1、带 `X-Family-Id` 访问菜品 200（空列表）、缺失 `X-Family-Id` 400。
  - 前端 dev server（5173）：`/login`、`/dishes` 均 200；代理链路 `5173/api/auth/login` 返回 200 与 token（代理贯通）。
  - 无头 Edge 截图 + 哈希：未登录访问 `/dishes` 的截图与 `/login` 截图 MD5 一致，且像素抽样（logo 深绿 #558767、登录按钮主绿 #6B9E7C、背景 #FAF6EF）确认渲染的是登录页，证明路由守卫「未登录重定向」生效。
  - 联调用后端/前端进程与临时日志/脚本均已清理，端口 5173/8080 已释放。
- **最终是否接受 AI 结果**：待人工确认（停在阶段 2 等待审查验收）。
- **经验总结**：
  - PowerShell 里 `@(Invoke-RestMethod)` 对空 JSON 数组会因 `@($null)` 把 null 计成 1，判断「列表是否为空」必须显式判 `$null`/`is [Array]`，不能直接 `@(...).Count`。
  - PowerShell 调 curl.exe 传 JSON 时双引号会丢，跨进程传 JSON 用 `Invoke-RestMethod -Body (ConvertTo-Json)` 更可靠。
  - 「未登录跳转」这类浏览器运行时行为，在模型不能读图、`--dump-dom` 返回空的环境下，可用「截图 MD5 一致 + 关键像素色值精确匹配设计令牌」间接确证。

---

## 任务 015：H2 到 MySQL 9.6.0 迁移与验收

- **日期**：2026-11-15
- **所处开发阶段**：数据库实施 / Flyway 迁移与 MySQL HTTP 验证
- **使用的 AI 工具**：Copilot SDK in VS Code
- **模型名称和版本**：待确认（本次记录未取得可验证的精确模型标识）
- **调用方式**：IDE 会话内代码审查、修改、Maven 验证、数据库迁移结果核对和 HTTP 冒烟测试。
- **任务目标**：按用户已确认决策将项目从 H2 切换到本机 MySQL 9.6.0；使用 Flyway 建表、Hibernate validate，并在真实 MySQL 上验证核心业务链路；不迁移 H2 临时数据。
- **输入的需求/设计文档与 Skill**：
  - 用户授权的数据库迁移任务及已确认 DB-01 至 DB-16 决策。
  - `docs/database/MYSQL_DATABASE_DESIGN.md`、`docs/database/MYSQL_MIGRATION_ISOLATED_TEST_REPORT.md`、`docs/README.md`。
  - 实际审查 `pom.xml`、`application.properties`、11 个 Entity、Repository、Service、DTO、Flyway V1 和安全配置。
  - 使用 Java 启动排查 Skill；未使用外部服务。
- **使用的提示词/任务规格（摘要）**：在保持 Java 17、Spring Boot 3.3.2、MySQL 9.6.0 的前提下完成正式迁移；不安装/修改 MySQL 服务、不提交数据库凭据、不导入 H2 数据；验证 V1、Hibernate validate 和家庭业务关键路径，并把未完成的并发/恢复风险如实记录。
- **AI 输出与代码修改范围**：
  - 项目迁移实现（本任务及此前已完成的同一迁移阶段）：Maven 改用 Connector/J 9.7.0 与 Spring Boot BOM 管理的 Flyway Core/MySQL 10.10.0；默认数据源转为 MySQL 环境变量配置；新增 `V1__create_homechef_schema.sql`；调整 Entity、Repository/Service 的成员退出状态、订单明细家庭键、`@Version`、UTC 与历史删除保护；新增邮箱规范化及测试。
  - 本次续验额外修改：移除 H2 Console 的公开放行规则和全局 frame-options 关闭；采购清单请求增加最多两位小数校验并添加单元测试。
  - 文档更新：`MYSQL_DATABASE_DESIGN.md`、`MYSQL_MIGRATION_ISOLATED_TEST_REPORT.md`、`docs/README.md`、本工作日志及决策日志。
- **人工审查发现的问题**：
  1. Flyway 13.12.2 与 Spring Boot 3.3.2 自动配置此前发生 API `NoSuchMethodError`；保留 Boot 3.3.2，最终改用 BOM 管理的 Flyway 10.10.0 后迁移成功。
  2. Flyway 10.10.0 仍对 MySQL 9.6.0 超出已验证范围发出警告；真实迁移成功不等于获得官方兼容保证。
  3. 历史订单取消权限仍只允许下单者，未按需求上下文扩展为家庭管理员可取消；本任务未擅自改变该业务规则。
- **是否经过人工审批**：是。用户明确授权正式数据库迁移、Maven/Java/配置/迁移脚本修改和 MySQL 9.6.0 目标；MySQL 服务配置未修改。
- **实际测试命令与测试结果**：
  - `mvn -B clean test`：成功，3 项测试通过（0 失败、0 错误）。
  - `mvn -B -DskipTests package`：BUILD SUCCESS。
  - Flyway V1：在空 `homechef_platform` 成功迁移；核验 11 张业务表、24 个外键、10 个唯一约束、1 条成功迁移记录。
  - 应用：MySQL 连接成功，Spring Boot 启动成功；Hibernate `ddl-auto=validate` 通过；`GET /` 返回 200。
  - HTTP：注册/登录/创建家庭/创建菜品及食材步骤/购物车同菜合并和计价/下单快照/提交后清空购物车/菜品更名改价不改变历史快照/家庭越权 403/采购清单新增与查询均通过。
  - 最初一次 `mvn clean test` 因测试应用仍持有 JAR 文件而无法清理；确认并停止拥有测试端口 18089 的 Java PID 后重跑，最终 `mvn -B clean test` 成功。
- **最终是否接受 AI 结果**：已完成本轮迁移和实测，等待用户审查；未完成的约束负向、并发冲突、事务故障回滚和备份恢复测试不标记为通过。
- **经验总结**：
  - 兼容性依赖应优先遵循 Spring Boot BOM；升级 Flyway 主版本需同时验证 Boot 自动配置 API，而不仅是独立命令行迁移。
  - 数据库迁移、Hibernate validate、业务 API 冒烟和并发/灾备验收是不同证据层级，不能相互替代。
  - 测试记录应区分新写入的 MySQL 测试数据与 H2 临时数据，并避免输出密码、JWT 或其他凭据。

---

## 任务 016：当前工作区前端阶段 0 重建

- **日期**：2026-11-15
- **所处开发阶段**：前端开发 / 阶段 0（工程初始化；代理验收待完成）
- **使用的 AI 工具**：Copilot SDK in VS Code
- **模型名称和版本**：当前运行环境未提供可验证的模型标识
- **调用方式**：IDE 会话内阅读项目文档、初始化前端、安装依赖、构建与启动验证。
- **任务目标**：按用户要求分阶段启动前端；核对当前实际工作区后，从阶段 0 创建最小 Vue 3 + Vite 工程，不实现业务页面，不修改后端或数据库；完成当前阶段验证后停止。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`、`docs/README.md`、`FRONTEND_DEVELOPMENT_GUIDE.md`、`FRONTEND_DEVELOPMENT_ROADMAP.md`、`FRONTEND_DESIGN.md`、`DECISION_LOG.md`（D-018、D-020、D-022）、`API_FRONTEND_CONTRACT.md`、`REQUIREMENT_CONTEXT.md`。
  - 核对当前 `application.properties`、前端工作区状态及 Vite 脚手架文件。
  - 读取 `frontend-design-cn`、`frontend-spec` 项目规范；加载了 TypeScript setup Skill，但因既有路线图明确采用 JavaScript 文件，未按 TypeScript 模板设置。
- **使用的提示词/任务规格（摘要）**：用户要求“分阶段开始做前端”；遵循路线图每次只推进一个阶段的约定。本次只重建阶段 0 骨架，不进入路由布局、认证或业务模块。
- **AI 输出与代码修改范围**：
  - 新建 `frontend/` Vue 3 + Vite JavaScript 工程；依赖 Vue、Axios、Vite 和 `@vitejs/plugin-vue`。
  - 新增中文阶段 0 占位页、开发环境 `/api` 代理配置、`frontend/README.md`。
  - 更新前端开发指南、路线图、`docs/README.md`、本工作日志及课程报告工作记录，标注当前检出状态与阶段进度。
  - 未修改 Java 源码、Maven 配置、数据库或认证授权代码。
- **人工审查发现的问题**：
  1. 历史记录曾显示阶段 0~2 已完成，但本次检出时前端工程不存在；以当前工作区为准，重建阶段 0，历史记录保留为历史事实。
  2. 本机后端 `127.0.0.1:8080` 未运行，代理请求返回 `502 ECONNREFUSED`；实际前端到后端链路尚未验证，阶段 0 不标记完成。
  3. 浏览器检查工具缺少其所需的 Chromium 可执行文件；尝试 `npx --yes playwright install chromium` 后下载停留在 0%（207.3 MiB），为避免留下长时间后台下载进程而停止。浏览器视觉验收未完成。
- **是否经过人工审批**：用户明确要求开始前端并分阶段推进；本次仅执行低风险前端工程初始化，无后端/数据库高风险操作。
- **实际测试命令与测试结果**：
  - `node --version` / `npm --version`：Node v24.16.0，npm 11.13.0；Vite 8.3.8 的 Node 引擎要求为 `^20.19.0 || >=22.12.0`，当前满足。
  - `npm install axios`：成功，审计 0 个漏洞。
  - `npm run build`：成功，Vite 构建完成。
  - `npm run dev -- --strictPort`：Vite 就绪（290 ms），监听 `0.0.0.0:5173`。
  - Node Fetch `GET http://127.0.0.1:5173/`：200，UTF-8 页面标题正确；PowerShell 自带 HTTP 解码曾显示乱码，不作为页面编码失败依据。
  - `GET http://127.0.0.1:5173/api/families`：502；Vite 日志为 `ECONNREFUSED 127.0.0.1:8080`，与后端未运行一致。
- **阶段结论**：工程骨架、构建与首页启动已验证；代理目标配置已建立，但代理真实后端响应待后端启动后验证。阶段 0 尚未完成，停止于此，不进入阶段 1。
- **验收补充（2026-11-15）**：用户启动前后端后实测 `curl.exe -i http://localhost:5173/api/families` 返回 HTTP 401，响应体为 `{"status":401,"message":"Authentication required","path":"/api/families","fieldErrors":null}`。结合此前构建成功、前后端首页均可访问的结果，阶段 0 验收通过；阶段 1 尚未开始。

---

## 任务 017：前端阶段 1 基础布局与路由

- **日期**：2026-11-15
- **所处开发阶段**：前端开发 / 阶段 1（基础框架与整体布局；响应式视觉待人工确认）
- **使用的 AI 工具**：Copilot SDK in VS Code
- **模型名称和版本**：当前运行环境未提供可验证的模型标识
- **调用方式**：IDE 会话内阅读路线图和前端约束，修改 Vue 工程、编写 Node 内置测试并运行 Vite 构建和路由 HTTP 检查。
- **任务目标**：按用户要求开始阶段一；只实现基础布局、路由、设计令牌、五个主导航、响应式框架及通用展示组件，不提前实现阶段二真实登录、Pinia 状态或业务 API。
- **输入的需求/设计文档与 Skill**：
  - `AGENTS.md`、`docs/README.md`、`FRONTEND_DEVELOPMENT_GUIDE.md`、`FRONTEND_DEVELOPMENT_ROADMAP.md`（阶段 1）、`FRONTEND_DESIGN.md`、`API_FRONTEND_CONTRACT.md`、`REQUIREMENT_CONTEXT.md`。
  - 加载 `implementing-code` Skill；路线图提供了本阶段任务和验收标准，未发现独立的阶段任务分解/REQ 规格文件。
- **AI 输出与代码修改范围**：
  - 在 `frontend/` 引入 Vue Router 4，建立嵌套主布局、五个导航路由、未知路径兜底及 Token 存在性检查的守卫骨架。
  - 增加桌面顶部导航、移动端底部五栏导航、设计令牌、响应式样式、登录占位页和通用卡片/空态/状态标签组件。
  - 增加 `npm test`（Node.js 内置测试运行器）及导航、路由守卫测试；未实现真实登录、Token 签发/验证、Pinia、业务 API。
  - 未修改 Java、Maven、数据库或认证授权代码。
- **人工审查发现的问题**：
  1. 首次依赖安装命令误在仓库根目录执行，随即检测并清理命令创建的根目录 `package.json`、`package-lock.json`、`node_modules/`；之后在 `frontend/` 正确安装 Vue Router。根目录没有保留这些误操作产物。
  2. 浏览器自动化无法启动：本机缺少 Playwright Chromium 可执行文件。没有声称已完成人工视觉验收；移动/桌面实际显示待用户确认。
  3. 阶段二尚无登录页功能；阶段一守卫只检查 `homechef.token` 是否存在，不代表 JWT 有效或获得后端访问权限。
- **实际测试命令与测试结果**：
  - `npm test`：3 项通过，0 失败。
  - `npm run build`：Vite production build 成功。
  - 启动 Vite 开发服务器于 `127.0.0.1:5174` 后，通过 PowerShell `Invoke-WebRequest` 请求 `/`、`/dishes`、`/orders`、`/cart`、`/shopping`、`/family` 和未知路径，均返回 HTTP 200（确认 SPA history fallback）；这不等于页面已在真实浏览器完成视觉验收。
  - Playwright 浏览器启动：失败，原因是 Chromium 可执行文件缺失；未完成移动/桌面截图。
- **最终结论**：阶段一布局与路由代码、自动化测试、构建和 SPA 路由 HTTP 检查已完成；响应式视觉人工验收待用户执行。阶段二未开始。
- **是否经过人工审批**：用户明确要求开始阶段一；依赖及实现范围均在已确认的前端路线图内，无高风险后端或数据库操作。