# 需求与现有项目代码差距分析（REQUIREMENT_CODE_GAP_ANALYSIS）

- **文档版本**：V1.2
- **分析日期**：2026-11-12
- **分析性质**：只读静态分析，未修改任何源码/配置，未运行测试。
- **主要依据**：`docs/requirements/REQUIREMENT_CONTEXT.md`（需求上下文 V1.2）、`docs/project-analysis/PROJECT_ANALYSIS.md`、`docs/project-analysis/RUN_VERIFICATION.md`、`docs/report/DECISION_LOG.md`，以及 `src/main/java` 下 44 个 Java 文件实际源码。
- **标注规则**：`[源码依据]`＝已核对源码；`[待验证]`＝源码无法确认；`[待确认]`＝需求未定，不得擅自实现。

---

## 1. 项目理解摘要

本项目目标是把通用「外卖点餐后端」改造为面向熟人小群体的「家庭私厨点单与饮食协作系统」。

核心业务目标（需求上下文已确认）：

- **多家庭空间**：多个相互独立家庭，家庭间数据完全隔离。
- **多成员关系**：用户可创建家庭（自动成为管理员）或经邀请码加入多个家庭；使用时「切换当前家庭」。
- **家庭内两种角色**：管理员（ADMIN）与普通成员（MEMBER），不设厨师/采购员等独立权限角色。
- **家庭菜品库**：名称、简介、图片、分类、制作耗时、食材与用量、单价、制作步骤、创建者。
- **家庭内部点单（含个人独立购物车）**：通过「用户 + 家庭」个人独立购物车选菜，支持就餐时段、备注；后端计价、订单保存快照。
- **备菜协作**：订单整体状态与单道菜备菜状态分离；默认共同协作、可指定负责人（其他成员仍可更新）。
- **食材不足与采购清单**：只记录「实际缺少」食材并进入家庭待购买清单。
- **个人资料与饮食偏好**：头像、昵称、偏好、健康情况标签（疾病，仅饮食参考，不构成医疗建议）；暂不收集用药等敏感信息。

**关键约束**：后端不能只信前端 `familyId`；User/Family/FamilyMember 多对多；金额 `BigDecimal` 后端计算；下单保存需事务；菜品删除不得破坏历史订单；操作需校验家庭归属与权限。

> 说明：需求已确认第一版采用「个人独立购物车」（按「用户 + 家庭」隔离），不再列为待确认项；原 `AGENTS.md` 购物车规划与本需求现已一致。

---

## 2. 当前架构和模块概览

经典分层架构（Controller → Service → Repository → Entity + DTO），44 个 Java 文件，均在 `com.fooddelivery` 包。

| 层 | 文件 |
|----|------|
| 启动 | `FoodDeliveryApplication.java` |
| Controller | `controller/` AuthController、HomeController、RestaurantController、MenuItemController、OrderController、ChatController |
| Service | `service/AuthService.java`、`OrderService.java`；`RestaurantService`+`impl/RestaurantServiceImpl`、`MenuItemService`+`impl/MenuItemServiceImpl`；`service/chat/`（ChatOrchestratorService、ChatTools、GeminiClient） |
| Repository | `repository/` 5 个接口（User/Order/OrderItem/MenuItem/Restaurant） |
| Entity | `entity/User.java`、`Restaurant.java`、`MenuItem.java`、`Order.java`、`OrderItem.java` |
| DTO | `dto/` 11 个类 |
| Security | `security/SecurityConfig.java`、`JwtUtil.java`、`JwtAuthFilter.java`、`CustomUserDetailsService.java` |
| 异常 | `exception/GlobalExceptionHandler.java`、`ErrorResponse.java`、`ResourceNotFoundException.java` |

现有实体（`[源码依据]`）：

- `User`（`users`）：`id/email/password/name/role/active`；`Role` 枚举 `CUSTOMER, RESTAURANT_OWNER, ADMIN`（`User.java:34-36`）。
- `Restaurant`（`restaurants`）：`id/name/address/ownerId/active`；`ownerId` 为普通 `Long` 列、**非外键**（`Restaurant.java:30-32`）。
- `MenuItem`（`menu_items`）：`id/name/description/price/available` + `@ManyToOne restaurant`；**无食材/步骤/分类/创建者**。
- `Order`（`orders`）：`id/customer/restaurant/totalAmount/status/createdAt` + `@OneToMany orderItems`；`OrderStatus` 为外卖状态 `PENDING/CONFIRMED/PREPARING/OUT_FOR_DELIVERY/DELIVERED/CANCELLED`（`Order.java:47-49`）。
- `OrderItem`（`order_items`）：`id/order/menuItem/quantity/priceAtOrder`；只有单价快照，**无名称快照**（`OrderItem.java:33-35`）。

---

## 3. 现有功能与目标需求对照表

| 目标需求 | 现有代码 | 差距 |
|----------|----------|------|
| 注册/登录 | `AuthService.register/login` | 可复用（注册自选 `role` 需修复） |
| 家庭（Family）空间 | 无 | 需新增 |
| 家庭成员（FamilyMember）多对多 | 无 | 需新增 |
| 家庭数据隔离 | 无 | 需新增 |
| 邀请码加入 | 无 | 需新增 |
| 家庭菜品库（含食材/步骤/分类/创建者） | `MenuItem`（仅 name/desc/price） | 需大幅扩展 |
| 菜品权限（创建者/管理员） | 无 | 需新增 |
| 个人独立购物车（用户+家庭） | 无 | 需新增 |
| 家庭内部下单 | `OrderService.createOrder` | 计价/快照思路可复用，归属需改家庭 |
| 订单快照（名称/单价/数量） | `OrderItem.priceAtOrder` | 有单价快照，**缺名称快照** |
| 订单整体状态 + 菜品备菜状态分离 | `Order.status`（单字段） | 需重构 |
| 备菜负责人 | 无 | 需新增 |
| 订单取消权限 | 无取消逻辑 | 需新增 |
| 食材不足记录 | 无 | 需新增 |
| 采购清单 | 无 | 需新增 |
| 饮食偏好 | 无 | 需新增 |
| 个人头像/昵称 | `User.name` | 需扩展 |
| AI 菜谱/建议 | `service/chat/` | 第一阶段暂缓/移除 |

---

## 4. 可复用代码分析

1. **分层架构骨架** + `@RequiredArgsConstructor` 构造器注入，直接沿用。
2. **JWT 认证框架**：`JwtUtil`、`JwtAuthFilter`、`CustomUserDetailsService`、`SecurityConfig` 思路可复用，需补角色授权与归属校验。
3. **BCrypt 密码加密**：`SecurityConfig.passwordEncoder()`（`SecurityConfig.java:25-28`）。
4. **BigDecimal 计价 + 价格快照**：`OrderService.createOrder`（`OrderService.java:30-64`）的 `multiply` 计小计与 `priceAtOrder`，正确示范，可迁移并补名称快照。
5. **事务边界**：`OrderService`/`MenuItemServiceImpl`/`RestaurantServiceImpl` 类级 `@Transactional`，可延续。
6. **DTO 参数校验**：`RegisterRequest`、`OrderRequest`、`MenuItemRequest` 的 `jakarta.validation` 写法。
7. **全局异常处理结构**：`GlobalExceptionHandler` + `ErrorResponse`，需移除 `ex.getMessage()` 泄露（`GlobalExceptionHandler.java:44-53`）。

---

## 5. 必须改造的模块

| 模块 | 文件 | 改造点 |
|------|------|--------|
| 用户角色 | `User.java`、`RegisterRequest.java`、`AuthService.java` | 去掉全局 `CUSTOMER/RESTAURANT_OWNER/ADMIN` 语义；注册强制默认角色（`RegisterRequest.java:24-25`、`AuthService.java:33`） |
| 餐厅 | `Restaurant.java`、`RestaurantServiceImpl.java`、`RestaurantController.java` | 语义重构为「家庭」；`ownerId` 软关联改真实 `FamilyMember` 关系 |
| 菜品 | `MenuItem.java`、`MenuItemServiceImpl.java`、`MenuItemRequest.java` | 归属改 `family`；新增分类/食材/步骤/耗时/图片/创建者；`update` 支持上下架（当前不更新 `available`，`MenuItemServiceImpl.java:55-60`） |
| 订单 | `Order.java`、`OrderService.java`、`OrderController.java` | 重构订单整体状态（与单道菜备菜状态分开管理）；归属改 `family`；增加就餐时段、备注及订单取消逻辑 |
| 订单明细 | `OrderItem.java` | 增加名称快照、单道菜独立备菜状态、负责人 |
| 安全 | `SecurityConfig.java`、`JwtAuthFilter.java`、`CustomUserDetailsService.java` | 引入家庭角色校验；补 JWT 异常处理；补归属校验 |
| 异常 | `GlobalExceptionHandler.java` | 移除 `ex.getMessage()` 泄露，区分 401/403/409 |

> 说明：订单改造的核心是「订单整体状态」与「单道菜备菜状态」**分别管理**——订单整体状态描述整单进度，单道菜备菜状态挂在订单明细上描述每道菜的进度，二者不能混为一个字段。订单状态与菜品备菜状态之间的联动规则（例如某道菜开始备菜后整单状态如何变化）需要在业务设计阶段进一步明确；本阶段不直接确定最终状态枚举或状态机。

---

## 6. 需要新增的模块

以下实体/服务在现有代码**完全不存在**，需从零新建：

| 新模块 | 拟新建内容 | 对应需求 |
|--------|-----------|----------|
| 家庭 | `Family` + `FamilyRepository` + `FamilyService` + `FamilyController` | 家庭空间 |
| 家庭成员 | `FamilyMember`（`user_id`+`family_id`+`role`）+ `FamilyMemberRepository` | 多对多、两种角色 |
| 邀请码 | `Invitation` + 生成/校验/核销服务 | 邀请机制 |
| 购物车 | 购物车相关实体/服务/接口（按「用户 + 家庭」隔离） | 个人独立购物车 |
| 菜品扩展 | `Ingredient`（食材+用量+单位）、`RecipeStep`、`Category` | 家庭菜品库 |
| 备菜状态 | `OrderItem` 增补备菜状态 + 状态机 + 更新接口 | 厨房协作 |
| 采购清单 | `ShoppingItem`（关联 `family`）+ `ShoppingService` | 采购清单 |
| 个人资料/偏好 | `User` 扩展头像/昵称 + `DietaryPreference` | Home |

## 7. 安全与数据隔离风险

### 7.1 现有高危缺陷（须在业务改造前优先处理）

1. **权限提升（最高危）**：注册接口 `RegisterRequest.role` 客户端任意指定，`AuthService.register` 直接落库（`RegisterRequest.java:24-25`、`AuthService.java:33`）。当前因 `SecurityConfig` 无 `hasRole` 未实际生效，但一旦引入角色授权即成为提权入口。`[源码依据]`
2. **越权访问（IDOR）**：订单/餐厅/菜品接口均缺归属校验——`OrderController.getById`（`:31-34`）、`getByRestaurant`（`:42-45`）、`updateStatus`（`:47-51`）；`RestaurantController`、`MenuItemController` 增删改；`ChatTools` 经自然语言查/下任意订单。`[源码依据]`
3. **异常信息泄露**：`GlobalExceptionHandler.handleGeneric` 原样返回 `ex.getMessage()`（`GlobalExceptionHandler.java:49`）。`[源码依据]`
4. **JWT 过滤器缺异常处理**：`JwtAuthFilter.doFilterInternal` 中 `extractEmail` 无 try-catch，非法/过期 token → 500 而非 401（`JwtAuthFilter.java:38`）。`[源码依据]`
5. **登录失败返回码不当**：`BadCredentialsException` 被兜底转 500（`AuthService.java:48-51`）。`[源码依据]`

### 7.2 多家庭隔离对架构的直接影响

- **JWT 只含 `email`（subject）**，不含家庭信息（`JwtUtil.java:26-33`）；家庭角色须每次请求实时查 `FamilyMember` 表。
- **「当前家庭」上下文缺失**：现有 Controller 仅 `Authentication.getName()` 取 email，无法感知当前家庭；需新增机制（如请求头带 `familyId`）并在服务端校验成员资格。具体实现方式**待设计确认**。
- **查询需全量加家庭过滤**：现有 `findByRestaurantId`、`findByCustomerId` 无家庭维度，改造后所有家庭数据访问点必须强制 `familyId` 过滤 + 成员校验，否则隔离失效。

### 7.3 图片上传安全

需求要求菜品/步骤图片上传的类型、大小、文件名、路径安全；现有代码**无任何文件上传能力**，属全新功能，需在菜品扩展阶段一并设计。

---

## 8. 事务与并发风险

1. **下单事务一致**：`OrderService` 类级 `@Transactional`，多表写入同事务，合理可延续（`OrderService.java:22`）。
2. **订单状态并发更新无乐观锁**：`updateStatus` 为「读-改-写」无 `@Version`，并发覆盖（`OrderService.java:87-91`）。备菜协作是多人并发场景，建议引入乐观锁/状态机校验。
3. **注册邮箱唯一性竞态**：`existsByEmail` + `save` 存在竞态窗口，DB 唯一约束兜底但转 500（`AuthService.java:24-37`）。
4. **菜品删除破坏历史订单（需求已确认约束）**：`MenuItemServiceImpl.delete` 为物理删除（`MenuItemServiceImpl.java:63-67`）；`OrderItem` 仅存外键 + `priceAtOrder`、无名称快照，`toResponse` 经 `item.getMenuItem().getName()` 实时取名（`OrderService.java:102`），菜品删除后 `menu_item_id` 悬空 → 历史订单 NPE/丢名。需求已确认「被历史订单引用的菜品只能下架、不得真正删除」，改造时必须引入逻辑删除（下架）+ 名称快照；**未被历史订单引用的菜品是否允许真正删除仍待确认（`[待确认]`），确认前不应实现物理删除路径**。
5. **邀请码并发核销**：管理员可生成、撤销邀请码（已确认），但「使用次数」尚未确认（`[待确认]`）。若后续确认「只能使用一次 / 限定次数」，核销需原子性控制（条件更新/乐观锁），现无此能力。
6. **采购清单一致性**：食材不足记录与订单、菜品关联及聚合方式需设计阶段确定，避免重复记账。`[待确认]`
7. **购物车提交订单的原子性**：购物车持久化后，提交订单需在同一事务内完成「重新计价 + 创建订单/明细 + 清空已提交购物车内容」，提交失败不得清空购物车，避免重复提交或部分清空。
8. **购物车并发修改**：同一用户对同一购物车条目的并发增减数量需保证一致性（具体方案留待系统设计阶段，不在此确定）。

---

## 9. 推荐实施顺序

> 阶段 0 是其余所有阶段的安全前提，须最先做。

| 阶段 | 内容 | 依赖 | 验收要点 |
|------|------|------|----------|
| 阶段 0 | 安全与认证基线修复 | 无 | 注册禁止自选角色；异常脱敏；非法 token 401；登录失败 401 |
| 阶段 1 | 家庭与成员数据模型 | 阶段 0 | `Family`+`FamilyMember`（多对多、ADMIN/MEMBER）；「当前家庭」上下文；归属校验 |
| 阶段 2 | 邀请码加入家庭 | 阶段 1 | `Invitation` 生成/校验/核销 |
| 阶段 3 | 家庭菜品库（含食材/步骤/分类） | 阶段 1 | `MenuItem` 归属 family + `Ingredient`/`RecipeStep`/`Category` |
| 阶段 4 | 个人购物车 + 家庭下单 | 阶段 3 | 购物车（用户+家庭隔离）+ 归属 family、名称快照、就餐时段、备注 |
| 阶段 5 | 备菜/开饭状态 + 采购清单 | 阶段 4 | 两级状态、负责人、`ShoppingItem` |
| 阶段 6 | 个人资料 + 饮食偏好 | 阶段 1 | 头像/昵称、偏好（可并行，建议后置） |
| 后续 | AI 菜谱（可选） | 核心稳定后 | 暂缓 |

---

## 10. 待确认问题

### 10.1 需求阶段的待确认项（不得擅自实现）

与 `REQUIREMENT_CONTEXT.md` 第 7 节保持一致：

| 编号 | 问题 |
|------|------|
| U-001 | 管理员删除其他成员菜品的具体限制 |
| U-002 | 订单中部分菜品开始备菜后能否取消 |
| U-003 | 管理员取消已开始备菜订单的具体规则 |
| U-004 | 订单取消后采购清单如何处理 |
| U-005 | 邀请码有效期 |
| U-006 | 邀请码使用次数（只能一次/可重复） |
| U-007 | 未被历史订单引用的菜品是否允许真正删除 |
| U-008 | 邀请码撤销后的具体影响（已生成码处理、是否影响已加入成员） |

### 10.2 后续技术设计问题（进入设计阶段再定，不阻塞需求澄清）

以下问题属接口/数据模型的技术选型，应在系统设计阶段决定，**不列为需求阶段的阻塞项**：

- **Q-1 当前家庭传递机制**：切换家庭采用请求头 `familyId`、路径参数还是独立上下文（需在服务端校验成员资格）——接口设计问题。
- **Q-2 订单名称快照字段范围**：快照具体包含哪些字段（名称/单价/数量/分类，图片是否需引用）——数据模型设计问题。
- **Q-3 全局 `User.Role` 与家庭 `FamilyMember` 角色关系**：现有 `User.Role` 枚举保留（系统级）还是移除、家庭角色仅存于 `FamilyMember`——数据模型设计问题。
- **Q-4 食材/步骤复用粒度**：食材是否作为可复用字典，还是每条菜品各自维护文本——数据模型设计问题。

---

## 11. 分析依据与文件路径

**需求依据**：

- `docs/requirements/REQUIREMENT_CONTEXT.md`（需求上下文，V1.2）

**源码依据（已逐一核对）**：

- 实体：`src/main/java/com/fooddelivery/entity/{User,Restaurant,MenuItem,Order,OrderItem}.java`
- 安全：`src/main/java/com/fooddelivery/security/{SecurityConfig,JwtUtil,JwtAuthFilter,CustomUserDetailsService}.java`
- 服务：`src/main/java/com/fooddelivery/service/{AuthService,OrderService}.java`、`service/impl/{MenuItemServiceImpl,RestaurantServiceImpl}.java`
- 控制器：`src/main/java/com/fooddelivery/controller/{AuthController,OrderController,MenuItemController,RestaurantController,HomeController}.java`
- 仓库：`src/main/java/com/fooddelivery/repository/*.java`
- DTO：`src/main/java/com/fooddelivery/dto/{RegisterRequest,OrderRequest,OrderItemRequest,MenuItemRequest}.java`
- 异常：`src/main/java/com/fooddelivery/exception/GlobalExceptionHandler.java`

**已有分析文档（交叉参考）**：

- `docs/project-analysis/PROJECT_ANALYSIS.md`（静态分析）
- `docs/project-analysis/RUN_VERIFICATION.md`（构建/启动验证）
- `docs/testing/BASELINE_API_TEST_CASES.md`（接口用例，预期为推导待实测）
- `docs/report/DECISION_LOG.md`（决策历史）

---

> 本文档为差距分析结论，不构成最终 SRS；后续应在需求澄清后形成 Epic/User Story/验收标准及测试用例。所有「待确认」项须人工确认后方可进入设计实现。
