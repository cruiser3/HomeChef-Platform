# 家庭私厨点单与饮食协作系统

面向家庭、情侣和合租室友等熟人小群体的点单与饮食协作系统。成员可以共同维护家庭菜品、使用个人购物车下单，并协作备菜、记录实际缺少的食材。

本项目由原 Spring Boot 外卖后端改造而来；当前实现面向家庭场景，不是面向公众的外卖平台，不提供餐厅配送、支付或平台抽佣等功能。原外卖遗留的餐厅、旧菜单、旧订单和 Gemini 聊天模块已删除。

## 当前进度

**截至 2026-11-15：**

- 后端已实现注册/登录与 JWT、家庭创建和成员信息查询、家庭菜品及分类/食材/步骤、按用户和家庭隔离的购物车、家庭订单和备菜协作、家庭待采购清单。
- 数据库已从 H2 迁移到 MySQL 9.6.0（库 `homechef_platform`），由 Flyway V1 管理表结构，Hibernate `ddl-auto=validate` 校验。
- 前端工程 `frontend/` 已建立（Vue 3 + Vite），阶段 0（工程初始化）已验收、阶段 1（路由/五导航/响应式布局/通用组件）已实现，后续阶段按路线图逐阶段推进，详见 `docs/frontend/FRONTEND_DEVELOPMENT_ROADMAP.md`。
- 邀请码加入家庭、用户资料与饮食偏好等功能尚未实现。
- `src/test/` 现有 2 个测试类、共 3 项单元测试（邮箱规范化、采购清单校验），`mvn -B clean test` 通过；业务接口另以 HTTP 冒烟测试覆盖。测试范围有限，详见 `docs/backend/BACKEND_PROGRESS.md` 第 4 节。

## 技术栈

- Java 17、Spring Boot 3.3.2、Maven
- Spring Web、Spring Validation、Spring Security、JWT（jjwt 0.12.5）
- Spring Data JPA / Hibernate
- MySQL 9.6.0（Connector/J 9.7.0）+ Flyway、Lombok
- 前端：Vue 3、Vite

## 项目结构

```text
.
├── src/main/java/com/fooddelivery/
│   ├── controller/       # REST API 控制器（Auth/Cart/Dish/Family/FamilyOrder/Home/ShoppingList）
│   ├── dto/              # API 请求与响应对象
│   ├── entity/           # 用户、家庭、菜品、购物车、家庭订单、采购清单等实体
│   ├── exception/        # 业务异常和统一错误响应
│   ├── repository/       # Spring Data JPA 数据访问接口
│   ├── security/         # Spring Security、JWT 认证
│   └── service/          # 业务接口及实现
├── src/main/resources/
│   ├── application.properties   # 后端及 MySQL 数据库配置
│   └── db/migration/           # Flyway 数据库迁移脚本（V1 建表）
├── src/test/java/        # 单元测试（邮箱规范化、采购清单校验）
├── frontend/             # Vue 3 + Vite 前端工程（阶段 0/1 已实现）
├── docs/
│   ├── backend/          # 后端模块说明、进度与 API 契约
│   ├── database/         # MySQL 数据库设计
│   ├── frontend/         # 前端设计、开发路线图与实施指南
│   ├── project-analysis/ # 项目分析与运行验证记录
│   ├── requirements/     # 需求及需求分析资料
│   ├── report/           # 课程报告和过程记录
│   └── testing/          # 测试资料
└── screenshots/          # 界面设计参考截图
```

## 已实现的后端接口范围

| 模块 | 路径 | 主要能力 |
|---|---|---|
| 认证 | `/api/auth` | 注册、登录并获取 JWT |
| 家庭 | `/api/families` | 创建家庭、查询家庭及成员信息 |
| 菜品 | `/api/dishes` | 菜品、分类、食材、步骤的查询与创建/修改、上下架 |
| 购物车 | `/api/cart` | 查询、添加、修改数量、删除项目、清空 |
| 家庭订单 | `/api/family-orders` | 提交订单、查询、取消、开始备菜、开饭、更新菜品备菜状态及负责人 |
| 采购清单 | `/api/shopping-list` | 查看清单、新增实际缺少的食材 |

除注册和登录外，接口需要 JWT。家庭相关请求按接口契约携带当前家庭上下文（`X-Family-Id`）；具体请求和响应格式请参见 [API 契约](docs/backend/API_FRONTEND_CONTRACT.md)。

## 本地运行

### 环境要求

- JDK 17
- Maven
- 本地可访问的 MySQL 9.x 实例，并已创建数据库 `homechef_platform` 与应用账号

### 启动后端（Windows PowerShell）

在项目根目录打开 PowerShell。`JWT_SECRET` 和 `DB_PASSWORD` 是必需配置；以下命令会生成临时随机 JWT 密钥，并只对当前终端会话生效。

当前 Windows 用户目录包含中文字符，而 `spring-boot:run` 启动的子进程未能从该路径加载 Maven 依赖。使用临时 ASCII 盘符映射运行；以下命令假设 `M:` 尚未被占用：

```powershell
subst M: "$env:USERPROFILE"
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:DB_PASSWORD = "你的 MySQL 应用账号密码"
mvn "-Dmaven.repo.local=M:\.m2\repository" spring-boot:run
```

如果 `M:` 已被占用，请换成其他空闲盘符，并同步替换 Maven 命令中的盘符。等待终端出现 Spring Boot 启动完成信息后，保持此终端运行。默认后端地址为 `http://localhost:8080`；可通过 `PORT` 环境变量更改端口。

停止服务时在运行后端的终端按 `Ctrl+C`。服务停止后，可以移除本次临时盘符映射：

```powershell
subst M: /D
```

### 验证后端

在另一个 PowerShell 终端运行：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/
```

成功时会收到 HTTP 响应，JSON 中包含 `"status": "running"`。也可以检查端口：

```powershell
Test-NetConnection 127.0.0.1 -Port 8080
```

`TcpTestSucceeded` 为 `True` 表示该端口有程序接受连接；建议同时请求根路径，以确认响应来自本应用。

## 构建与测试

在项目根目录验证后端编译与单元测试：

```powershell
mvn clean test
```

当前 `src/test/` 有 2 个测试类、共 3 项单元测试；该命令成功表示主代码编译通过且这 3 项测试通过。业务接口的完整验证见 `docs/backend/BACKEND_PROGRESS.md` 第 4 节的 HTTP 冒烟测试记录。

## 数据库与配置

- 当前数据库：MySQL 9.6.0，连接地址 `jdbc:mysql://127.0.0.1:3306/homechef_platform`，由 Flyway V1 自动建表；Hibernate `ddl-auto=validate` 校验实体与表结构一致。
- `JWT_SECRET`：必需的 JWT 签名密钥；本地可按启动步骤临时生成。不要把真实密钥提交到版本库。
- `DB_PASSWORD`：必需的 MySQL 应用账号密码。
- `DB_URL`、`DB_USERNAME`：可选，默认分别为 `jdbc:mysql://127.0.0.1:3306/homechef_platform` 和 `homechef_app`。
- `PORT`：后端端口，可选，默认 `8080`。

## 相关文档

`docs/` 中部分分析和过程文档是早期阶段快照，可能保留旧模型、旧测试结果或旧前端状态；与当前实现有冲突时，以源码和本 README 的当前进度说明为准。

- [后端进度总览](docs/backend/BACKEND_PROGRESS.md)
- [认证与家庭模块](docs/backend/AUTH_FAMILY.md)
- [菜品模块](docs/backend/DISH_MODULE.md)
- [购物车模块](docs/backend/CART_MODULE.md)
- [订单模块](docs/backend/ORDER_MODULE.md)
- [后端 API 契约](docs/backend/API_FRONTEND_CONTRACT.md)
- [MySQL 数据库设计](docs/database/MYSQL_DATABASE_DESIGN.md)
- [前端开发路线图](docs/frontend/FRONTEND_DEVELOPMENT_ROADMAP.md)
- [前端开发实施指南](docs/frontend/FRONTEND_DEVELOPMENT_GUIDE.md)
- [文档索引](docs/README.md)
