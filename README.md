# 家庭私厨点单与饮食协作系统

面向家庭、情侣和合租室友等熟人小群体的点单与饮食协作系统。成员可以共同维护家庭菜品、使用个人购物车下单，并协作备菜、记录实际缺少的食材。

本项目由原 Spring Boot 外卖后端改造而来；面向家庭场景，不提供餐厅配送、支付或平台抽佣等功能，原外卖遗留的餐厅、旧菜单、旧订单和 Gemini 聊天模块已删除。

## 功能概览

- 认证与用户：注册、登录（JWT）、个人资料与饮食偏好（过敏原、医疗状况、饮食限制）。
- 家庭协作：创建家庭、邀请码加入、成员与角色管理，家庭数据严格隔离。
- 家庭菜品：菜品、分类、食材、制作步骤的增删改查与上下架，支持按菜系、卡路里区间、关键字筛选。
- 美食广场：内置 HowToCook 开源菜谱数据源，浏览/搜索/筛选系统菜谱，一键复制到自家；支持菜品评论、回复与星级评价。
- 点单与备菜：按「用户 + 家庭」隔离的个人购物车、家庭订单、逐菜品备菜状态与开饭管理。
- 采购清单：记录备菜时实际缺少的食材并标记已购。
- 菜品评论：家庭菜品评价与回复；广场菜品评论、回复与星级聚合。
- AI 辅助（可选）：菜谱文本解析、购物车分析、卡路里估算、菜系自动分类，以及「小铲」多会话流式对话助手（悬浮球 + 全屏对话 + 会话历史 + 设置面板），支持本地 Ollama 或用户自配 API（DeepSeek / Kimi / OpenAI / 自定义，OpenAI 兼容）。

## 技术栈

### 后端

- Java 17、Spring Boot 3.3.2、Maven
- Spring Web、Spring Data JPA、Spring Validation、Spring Security + JWT（jjwt 0.12.5）
- MySQL（Connector/J 9.7.0）+ Flyway 数据库迁移
- Lombok
- AI：本地 Ollama（默认模型 `qwen2.5:7b`，可配置关闭）；「小铲」对话助手支持多服务商（Ollama / DeepSeek / Kimi / OpenAI / 自定义，OpenAI 兼容）

### 前端

- Vue 3、Vite、Vant 4（移动端组件库）、Pinia、Vue Router、Axios
- 响应式布局，兼顾手机「点单风」与桌面端

## 项目结构

```text
.
├── src/main/java/com/fooddelivery/
│   ├── config/           # 安全、Web 等配置
│   ├── controller/       # REST API 控制器
│   ├── dto/              # 请求/响应对象
│   ├── entity/           # JPA 实体
│   ├── exception/        # 业务异常与统一错误响应
│   ├── repository/       # Spring Data JPA 数据访问
│   ├── security/         # Spring Security / JWT
│   ├── service/          # 业务接口与实现
│   │   └── importing/    # HowToCook 菜谱导入与解析
│   └── util/             # 工具类
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/     # Flyway 迁移脚本（V1～V21）
├── src/test/java/        # 单元测试
├── frontend/             # Vue 3 + Vite 前端
│   ├── src/api/          # 后端接口封装
│   ├── src/views/        # 页面组件
│   ├── src/router/       # 路由与鉴权守卫
│   ├── src/layouts/      # 布局
│   ├── src/store/        # Pinia 状态
│   └── src/styles/       # 样式
├── HowToCook/            # 美食广场菜谱数据源（Markdown + 图片 + LICENSE）
├── Dockerfile            # 后端容器化（两阶段构建）
├── pom.xml
└── README.md
```

## 已实现的后端接口

| 模块 | 路径 | 主要能力 |
|---|---|---|
| 认证 | `/api/auth` | 注册、登录并获取 JWT |
| 家庭 | `/api/families` | 创建/查询家庭、成员列表、邀请码获取与重置、邀请码加入、移除成员、角色分配 |
| 菜品 | `/api/dishes` | 菜品与分类 CRUD、上下架、按菜系/卡路里/关键字筛选；菜品评论与回复 |
| 菜系 | `/api/cuisines` | 菜系列表与新增 |
| 购物车 | `/api/cart` | 查询、添加、修改数量、删除、清空 |
| 家庭订单 | `/api/family-orders` | 提交、列表、详情、取消、开始备菜、开饭、逐菜品备菜状态 |
| 采购清单 | `/api/shopping-list` | 记录缺少食材、更新、标记已购、删除 |
| 系统菜谱 | `/api/system-dishes` | 分类、列表（分类/菜系/卡路里/关键字筛选）、详情、设置菜系、复制到家庭、AI 菜系分类、评论与回复、星级聚合 |
| AI | `/api/ai` | 解析菜谱、分析购物车、估算卡路里；「小铲」流式对话（SSE）与多会话管理 |
| AI 配置 | `/api/ai/config` | 获取/保存/重置「小铲」模型来源、API Key（AES 加密）与角色设定 |
| 上传 | `/api/upload` | 图片上传 |
| 用户 | `/api/users` | 获取/更新个人资料与饮食偏好 |

除注册、登录和根路径外，接口需要 JWT；家庭相关请求需携带 `X-Family-Id` 请求头标识当前家庭上下文，后端会校验成员资格。

## 美食广场（系统菜谱库）

广场菜谱数据来自开源项目 HowToCook（已保留其 `LICENSE`），存放在仓库 `HowToCook/` 目录。后端启动时按 `application.properties` 中的配置幂等导入：

```properties
app.system-dish.import.enabled=true
app.system-dish.import.howtocook-path=./HowToCook
```

首次启动会把 `HowToCook/dishes` 下的 Markdown 菜谱解析入库，封面图复制到 `uploads/`；重复启动跳过已存在项，不会重复导入。

广场系统菜谱支持评论、回复与星级评价：登录用户可发表/回复评论并匿名，星级按顶级评论评分实时聚合均值展示（与小家菜品一致）。

## AI 功能（可选）

AI 能力分两部分，不启用也不影响主体点单 / 备菜 / 采购功能。

### 基础 AI（菜谱解析、购物车分析、卡路里估算、菜系分类）

通过本地 [Ollama](https://ollama.com) 提供：

```properties
app.ai.enabled=true
app.ai.ollama.base-url=http://localhost:11434
app.ai.ollama.model=qwen2.5:7b
```

启用前需本地运行 Ollama 并拉取对应模型（如 `ollama pull qwen2.5:7b`）；若未运行 Ollama，请将 `app.ai.enabled` 设为 `false` 以避免 AI 接口报错。

### 「小铲」多会话对话助手

前端悬浮球 + 全屏对话，支持多会话历史与独立角色设定。模型来源可在前端设置面板自配，支持预设 **Ollama / DeepSeek / Kimi / OpenAI** 或「自定义」（统一映射为 OpenAI 兼容协议）；用户配置的 API Key 由后端用 AES-256-GCM 加密后落库，接口返回时脱敏。

```properties
# 用户自配 API Key 的加密密钥（经 SHA-256 派生 256-bit 密钥；生产环境务必覆盖为强随机值）
app.ai.config.encryption-key=${AI_CONFIG_ENCRYPTION_KEY:local-dev-key-change-me}
```

## 本地运行

### 环境要求

- JDK 17、Maven
- MySQL，已创建数据库 `homechef_platform` 与应用账号
- Node.js（运行前端）
- 可选：Ollama（启用 AI 功能）

### 启动后端（Windows PowerShell）

`JWT_SECRET` 和 `DB_PASSWORD` 为必需环境变量。若当前 Windows 用户名含中文，`spring-boot:run` 子进程可能无法从用户目录加载 Maven 依赖，可用临时 ASCII 盘符映射规避（以下假设 `M:` 未被占用）：

```powershell
subst M: "$env:USERPROFILE"
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:DB_PASSWORD = "你的 MySQL 应用账号密码"
mvn "-Dmaven.repo.local=M:\.m2\repository" spring-boot:run
```

如果 `M:` 已被占用，请换成其他空闲盘符，并同步替换 Maven 命令中的盘符。启动完成后默认后端地址为 `http://localhost:8080`，可通过 `PORT` 环境变量更改端口。停止服务后可用 `subst M: /D` 移除映射。

### 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端开发服务器默认监听 `0.0.0.0:5173`，并将 `/api`、`/uploads` 请求代理到后端 `http://127.0.0.1:8080`（可用 `API_PROXY_TARGET` 环境变量覆盖）。

### 验证后端

```powershell
Invoke-RestMethod -Uri http://localhost:8080/
```

成功时返回 JSON 含 `"status": "running"`。

## 构建与测试

后端编译与单元测试：

```powershell
mvn clean test
```

前端生产构建：

```powershell
cd frontend
npm run build
```

`src/test/` 现有 4 个测试类（邮箱规范化、采购清单校验、系统菜谱服务、HowToCook 解析器）。

## 数据库与配置

- 数据库：MySQL，Flyway 以 `src/main/resources/db/migration` 下 V1～V21 脚本管理表结构与演进，Hibernate `ddl-auto=validate` 校验。
- `JWT_SECRET`：必需的 JWT 签名密钥；本地可按启动步骤临时生成，不要把真实密钥提交到版本库。
- `DB_PASSWORD`：必需的 MySQL 应用账号密码。
- `DB_URL`、`DB_USERNAME`：可选，默认分别为 `jdbc:mysql://127.0.0.1:3306/homechef_platform` 和 `homechef_app`。
- `PORT`：后端端口，可选，默认 `8080`。
- `API_PROXY_TARGET`：前端开发代理目标，可选，默认 `http://127.0.0.1:8080`。
- `AI_CONFIG_ENCRYPTION_KEY`：可选，「小铲」用户自配 API Key 的加密密钥，默认 `local-dev-key-change-me`，生产环境务必覆盖为强随机值。

## Docker 部署

后端镜像两阶段构建：

```bash
docker build -t homechef-backend .
docker run -e JWT_SECRET=... -e DB_PASSWORD=... -p 8080:8080 homechef-backend
```

> 镜像仅包含后端；如需美食广场菜谱，请将 `HowToCook/` 目录挂载进容器，或关闭 `app.system-dish.import.enabled`。
