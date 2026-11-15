# 用户认证、JWT、家庭管理与成员管理

**文档范围：** 只记录**已经实际完成**的 M-01（注册/登录/JWT）与 M-02（家庭与家庭成员管理）。
**内容依据：** 当前实际源码 + 本机实际执行过的构建与测试结果。未实际执行的内容不写成"已通过"。菜品、购物车、订单模块均未完成，不在本文档范围内。

---

## 1. 已完成的写入/修改文件清单

### 1.1 本次新增

| 文件 | 职责 |
|---|---|
| `src/main/java/com/fooddelivery/entity/Family.java` | 家庭实体 |
| `src/main/java/com/fooddelivery/entity/FamilyMember.java` | 家庭成员关系实体（含家庭角色枚举） |
| `src/main/java/com/fooddelivery/repository/FamilyRepository.java` | 家庭数据访问 |
| `src/main/java/com/fooddelivery/repository/FamilyMemberRepository.java` | 成员关系查询 |
| `src/main/java/com/fooddelivery/service/CurrentUserService.java` | 获取当前登录用户 + 成员/管理员校验 |
| `src/main/java/com/fooddelivery/service/FamilyService.java` | 家庭服务接口 |
| `src/main/java/com/fooddelivery/service/impl/FamilyServiceImpl.java` | 家庭服务实现 |
| `src/main/java/com/fooddelivery/controller/FamilyController.java` | 家庭 REST 接口 |
| `src/main/java/com/fooddelivery/dto/CreateFamilyRequest.java` | 创建家庭请求 |
| `src/main/java/com/fooddelivery/dto/FamilyResponse.java` | 家庭响应 |
| `src/main/java/com/fooddelivery/dto/FamilyMemberResponse.java` | 成员响应 |
| `src/main/java/com/fooddelivery/exception/ForbiddenException.java` | 403 业务异常 |
| `src/main/java/com/fooddelivery/exception/BadRequestException.java` | 400 业务异常 |
| `src/test/java/com/fooddelivery/service/FamilyServiceImplTest.java` | 家庭服务测试（5 个用例） |
| `src/test/java/com/fooddelivery/service/AuthServiceRegisterTest.java` | 注册角色固定测试（1 个用例） |

### 1.2 本次修改

| 文件 | 修改内容 | 原因 |
|---|---|---|
| `src/main/java/com/fooddelivery/dto/RegisterRequest.java` | 删除 `role` 字段；`name` 增加 `@Size(max = 50)` | 原实现要求前端传 `role`，存在普通用户自行注册为 `ADMIN` 的越权风险 |
| `src/main/java/com/fooddelivery/service/AuthService.java` | `role(request.getRole())` → `role(User.Role.CUSTOMER)` | 同上，注册角色由服务端固定 |
| `src/main/java/com/fooddelivery/exception/GlobalExceptionHandler.java` | 新增 `ForbiddenException`（403）、`BadRequestException`（400）处理 | 统一错误响应结构 |

### 1.3 未改动（复用原有代码）

`User.java`、`LoginRequest.java`、`AuthResponse.java`、`AuthController.java`、`UserRepository.java`、`JwtUtil.java`、`JwtAuthFilter.java`、`CustomUserDetailsService.java`、`SecurityConfig.java`。

注册与登录逻辑未重写，只在注册处收紧角色赋值。

---

## 2. 数据表

当前数据库为 H2 内存库，`ddl-auto=update` 自动建表，**无 SQL 迁移脚本**。

### 2.1 新增表 `families`

| 列 | 类型 | 约束 |
|---|---|---|
| `id` | bigint | 主键，自增 |
| `name` | varchar(50) | not null |
| `created_by` | bigint | not null（创建者用户 ID） |
| `created_at` | timestamp(6) | not null，不可更新 |

### 2.2 新增表 `family_members`

| 列 | 类型 | 约束 |
|---|---|---|
| `id` | bigint | 主键，自增 |
| `family_id` | bigint | not null |
| `user_id` | bigint | not null |
| `role` | enum / 字符串 | not null，取值 `ADMIN`、`MEMBER` |
| `joined_at` | timestamp(6) | not null，不可更新 |

唯一约束：`uk_family_member_user_family (family_id, user_id)`，同一用户在同一家庭只允许一条成员记录。

说明：`family_id` 与 `user_id` 以普通列 + 唯一约束建模，未使用 JPA 关联映射；这也是"用户可加入多个家庭"的多对多表达方式。

### 2.3 原有表 `users`

未修改表结构。`role` 仍保存系统级账号角色（`CUSTOMER` / `RESTAURANT_OWNER` / `ADMIN`）。实际启动日志确认建表语句为 `role enum ('ADMIN''CUSTOMER''RESTAURANT_OWNER') not null`。

---

## 3. 角色设计

系统级角色与家庭级角色**分开设计**，互不替代：

| 层级 | 存放位置 | 取值 |
|---|---|---|
| 系统账号角色 | `users.role`（`User.Role`） | `CUSTOMER`、`RESTAURANT_OWNER`、`ADMIN` |
| 家庭角色 | `family_members.role`（`FamilyMember.FamilyRole`） | `ADMIN`、`MEMBER` |

要点：

- 注册流程只会产生 `User.Role.CUSTOMER`，普通注册无法获得系统级管理员权限。
- 家庭管理员身份只由"创建家庭"这一服务端流程写入，不接受前端指定。
- 家庭角色不写入 JWT。每次请求都要按 `familyId + 当前用户` 实时查询 `family_members`，因此成员关系变化会立即生效。

---

## 4. 接口

除注册、登录外，全部要求携带 `Authorization: Bearer <token>`。

### 4.1 原有认证接口（复用）

| 方法 | 路径 | 用途 | 备注 |
|---|---|---|---|
| POST | `/api/auth/register` | 注册 | 角色固定为 `CUSTOMER`；成功返回 201 |
| POST | `/api/auth/login` | 登录 | 成功返回 200 |

### 4.2 本次新增家庭接口

| 方法 | 路径 | 用途 | 权限校验 | 成功状态码 |
|---|---|---|---|---|
| POST | `/api/families` | 创建家庭，创建者自动成为 `ADMIN` | 需登录 | 201 |
| GET | `/api/families` | 当前用户所属的家庭列表 | 需登录 | 200 |
| GET | `/api/families/{familyId}` | 查看指定家庭详情 | 必须是该家庭成员 | 200 |
| GET | `/api/families/{familyId}/members` | 查看该家庭成员列表 | 必须是该家庭成员 | 200 |
| GET | `/api/families/current/members` | 按请求头 `X-Family-Id` 查看"当前家庭"成员 | 必须携带该请求头；必须是该家庭成员 | 200 |

### 4.3 响应结构

`FamilyResponse`：`id`、`name`、`createdBy`、`createdAt`、`currentUserRole`、`memberCount`。

`FamilyMemberResponse`：`userId`、`name`、`role`。**不返回**邮箱、密码哈希、token 等敏感字段。

错误响应沿用项目原有 `ErrorResponse`：`timestamp`、`status`、`message`、`path`，校验失败时额外有 `fieldErrors`。

### 4.4 "当前家庭"的传递方式

使用请求头 `X-Family-Id` 表示当前家庭。后端在每次请求时用该值与当前登录用户查询 `family_members` 校验，属于"服务端实时校验成员关系"，不依赖前端传值可信。

切换家庭在服务端表现为"下一次请求携带不同的 `X-Family-Id`"。**当前未做服务端持久化**（见第 8 节遗留问题 G-02）。

---

## 5. 权限与数据隔离设计

关键类 `CurrentUserService` 提供以下方法，后续模块应复用：

| 方法 | 行为 |
|---|---|
| `getCurrentUser()` / `getCurrentUserId()` | 从 `SecurityContext` 取邮箱并查库得到用户；未认证抛出认证异常 |
| `requireMembership(familyId)` | 查 `family_members(familyId, userId)`，不存在抛 `ForbiddenException`（403） |
| `requireAdmin(familyId)` | 先校验成员资格，再校验家庭角色为 `ADMIN`，否则 403 |
| `requireExistingFamilyMembership(familyId)` | 成员不存在时抛 `ResourceNotFoundException`（404），供不泄露家庭存在性的场景使用 |

隔离要点：

1. 家庭业务接口全部以"服务端查出的当前用户 ID + 目标 familyId"作为唯一可信输入。
2. 前端传来的 `familyId` 只作为"被校验的目标"，不会被直接信任。
3. 非成员访问返回 403，不返回该家庭的任何数据。
4. 同一用户在不同家庭的数据通过 `family_id` 维度区分，成员查询均带 `family_id` 条件。

---

## 6. 实际测试与验证结果

### 6.1 自动化测试（实际执行）

命令：

```
$env:JWT_SECRET='...'; $env:GEMINI_API_KEY='test-dummy-key'; mvn clean test
```

Surefire 报告实际内容：

| 测试类 | 结果 |
|---|---|
| `com.fooddelivery.service.AuthServiceRegisterTest` | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` |
| `com.fooddelivery.service.FamilyServiceImplTest` | `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` |
| 合计 | `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS` |

已覆盖的断言（来自实际测试代码）：

1. 创建家庭后创建者自动成为 `ADMIN`，且成员表只有 1 条记录。
2. 家庭成员列表能返回多名成员。
3. 非成员查询家庭成员列表抛 `ForbiddenException`。
4. 非成员查询家庭详情抛 `ForbiddenException`（不泄露家庭数据）。
5. `listMyFamilies` 只返回本人所属家庭，非成员返回空列表。
6. 注册接口产出的角色恒为 `CUSTOMER`。

测试使用 `@SpringBootTest` + `@Transactional`，通过手工写入 `SecurityContext` 模拟登录用户。

未覆盖的部分（如实说明）：

- 端到端 HTTP 层的状态码断言（401/403/400）未写成自动化测试。
- 家庭管理员专属操作的权限校验函数 `requireAdmin` 已实现，但尚未有业务调用方，因此没有对应测试。

### 6.2 手工端到端验证（实际执行）

打包后以 `java -jar` 启动应用，用真实 HTTP 请求验证，实际观察结果：

| 验证动作 | 实际结果 |
|---|---|
| 注册时传 `"role":"ADMIN"` | 返回 `role = CUSTOMER` |
| 创建家庭 A、B | 均 201，`currentUserRole = ADMIN` |
| U1 查询 `/api/families` | 返回 A、B 两个家庭 |
| U1 查询 `/api/families/{B}` | 200，返回 B 的详情 |
| U2（非成员）查询 B 的成员 | 403 |
| U1 查询 A 的成员 | 200，返回 `[{userId, name, role=ADMIN}]` |
| U1 携带 `X-Family-Id=B` 调 `/api/families/current/members` | 200 |
| U2 携带 `X-Family-Id=B` 调同一接口（伪造） | 403 |
| 无 token 创建家庭 | 403（非预期的 401，见 G-01） |
| 空名称创建家庭 | 400 |

以上均为实际执行结果，未执行的项目不在此表内。

---

## 7. 涉及的实现要点

- 创建家庭在同一个 `@Transactional` 事务内完成"插入家庭 + 插入 ADMIN 成员"，避免出现无管理员的家庭。
- `FamilyMemberRepository` 均基于 `familyId` 与 `userId` 两列查询，不涉及懒加载关联，避免 `open-in-view=false` 下的会话问题。
- 成员列表中的昵称通过 `userRepository.findAllById(...)` 一次性批量取回，避免逐个查询。
- 响应 DTO 与实体分离，成员响应只暴露昵称与家庭角色。

---

## 8. 遗留问题

| 编号 | 问题 | 具体表现 | 建议 |
|---|---|---|---|
| B-01 | 未认证请求返回 403 而非 401 | 无 token 调用 `/api/families` 实测 403；原项目 `/api/restaurants` 同样 403；`SecurityConfig` 未配置 `authenticationEntryPoint` | 需人工批准后修改安全配置，补 401 入口点与 403 处理 |
| B-02 | "当前家庭"未持久化 | 只通过请求头 `X-Family-Id` 表达，客户端无法跨请求记住选择 | 需确认是否增加 `current_family` 持久化字段或独立表 |
| B-03 | 注册重复邮箱抛 `RuntimeException` | `AuthService.register` 中抛通用异常，会被兜底处理器转成 500，而非 409/400 | 后续改为明确的业务异常 |
| B-04 | `requireAdmin` 尚无业务调用方 | 已实现家庭管理员校验能力，但当前没有管理员专属接口使用它 | 待邀请码模块（M-03）实现时接入 |
| B-05 | 家庭成员加入方式尚未实现 | 目前只能通过"创建家庭"成为成员，邀请码加入（M-03）未开发 | 待阶段 2 |
| B-06 | 测试依赖环境变量 | 缺少 `JWT_SECRET` 或 `GEMINI_API_KEY` 时 Spring 上下文无法加载，测试全部报错 | 本地与 CI 需注入变量（属既有问题） |

---

> 维护记录：本文档只覆盖已完成的认证与家庭模块；M-03 及之后的模块完成后再单独建文档，不在此文件继续堆叠。