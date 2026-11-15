# 运行验证准备记录（RUN_VERIFICATION）

> 本轮只检查和验证，不修改任何 Java 源码、pom.xml、数据库配置、Dockerfile 或其他业务文件。
> 状态标记规则：已实际读取/确认 → 写"已确认"；已实际执行命令 → 写"已执行"；尚未执行 → 写"待执行"；结论尚未验证 → 写"待验证"。

---

## 1. 检查项目与检查日期

| 项 | 内容 |
|----|------|
| 检查项目 | food-delivery-backend（`d:/code/Java/food-delivery-backend`） |
| 检查日期 | 2026-11-11 |
| 检查方式 | 静态读取配置文件（已确认）；并已执行 `java -version`、`mvn -version`、`mvn clean test`、`mvn spring-boot:run`（见第 5/6/7 节） |

---

## 2. 项目配置中的 Java 版本

- `pom.xml:21`：`<java.version>17</java.version>` → **Java 17**。
- `pom.xml:10`：Spring Boot 父版本 **3.3.2**。
- `Dockerfile:2`：构建镜像 `maven:3.9-eclipse-temurin-17`。
- `Dockerfile:9`：运行镜像 `eclipse-temurin:17-jre`。
- 结论：pom.xml、Dockerfile 构建/运行阶段均为 **Java 17，版本一致**。

---

## 3. 数据库配置

来源：`src/main/resources/application.properties`。

| 配置项 | 值 | 说明 |
|--------|----|----|
| URL | `jdbc:h2:mem:food_delivery` | **H2 内存数据库**，进程结束/重启后数据丢失 |
| Driver | `org.h2.Driver` | H2 驱动 |
| 用户名 | `sa` | 默认 |
| 密码 | 空 | 默认 |
| H2 Console | `${H2_CONSOLE:false}` | 默认**关闭**，设置环境变量 `H2_CONSOLE=true` 可开启 |
| ddl-auto | `update` | 由 Hibernate 按实体自动建表/更新，无需手动建表 |
| show-sql | `true` | 打印 SQL |
| format_sql | `true` | 格式化 SQL |
| open-in-view | `false` | 关闭 Open Session In View |

- 是否存在 `.yml`：**否**（全项目无 `application.yml` / `application-*.yml`）。
- 是否存在数据库初始化脚本：**否**（无 `schema.sql`、`data.sql`、`import.sql`）。
- 是否需要安装独立数据库：**否**，H2 为内嵌运行时依赖。

---

## 4. Maven 依赖

来源：`pom.xml:24-74`。

| 依赖 | 版本 | scope | 用途 |
|------|------|-------|------|
| spring-boot-starter-web | 随父版本 | 默认 | Web/REST |
| spring-boot-starter-data-jpa | 随父版本 | 默认 | JPA/Hibernate |
| spring-boot-starter-validation | 随父版本 | 默认 | 参数校验 |
| spring-boot-starter-security | 随父版本 | 默认 | 安全认证 |
| h2 | 随父版本 | runtime | 内存数据库 |
| lombok | 1.18.32 | provided | 样板代码 |
| jjwt-api | 0.12.5 | 默认 | JWT |
| jjwt-impl | 0.12.5 | runtime | JWT |
| jjwt-jackson | 0.12.5 | runtime | JWT JSON 序列化 |
| spring-boot-starter-test | 随父版本 | test | 测试（已引入） |

---

## 5. Maven 构建情况

- **状态：已执行**（2026-11-11，Windows 11 + PowerShell）。

### 5.1 `java -version`（已执行）

```
openjdk version "17.0.20.1" 2026-08-18
OpenJDK Runtime Environment Temurin-17.0.20.1+1 (build 17.0.20.1+1)
OpenJDK 64-Bit Server VM Temurin-17.0.20.1+1 (build 17.0.20.1+1, mixed mode, sharing)
```

- 结论：本机 JDK 为 **Temurin 17.0.20.1**，满足项目 Java 17 要求。

### 5.2 `mvn -version`（已执行）

```
Apache Maven 3.9.15
Java version: 17.0.20.1, vendor: Eclipse Adoptium
OS name: "windows 11", version: "10.0", arch: "amd64"
```

- 结论：Maven **3.9.15**，Java 17，满足要求。

### 5.3 `mvn clean test`（已执行，成功）

- 结果：**BUILD SUCCESS**（Total time 3.670 s）。
- 关键输出：`Compiling 44 source files with javac [debug parameters release 17]`、`No tests to run.`。
- 结论：44 个源文件全部编译通过；因无测试类，Surefire 报告"无测试可运行"。

### 5.4 `mvn spring-boot:run`（已执行，失败）

- 结果：**BUILD FAILURE**（Total time 18.866 s）。
- 关键报错：

```
Exception in thread "main" java.lang.NoClassDefFoundError: org/springframework/boot/SpringApplication
    at com.fooddelivery.FoodDeliveryApplication.main(FoodDeliveryApplication.java:9)
Caused by: java.lang.ClassNotFoundException: org.springframework.boot.SpringApplication
```

- 结论：应用未能启动。`spring-boot:run` fork 出的 JVM 在运行 `FoodDeliveryApplication.main` 时，classpath 中缺少 Spring Boot 核心 jar（`SpringApplication` 类找不到）。
- 分析：从日志看，本次为首次运行 `spring-boot:run`，Maven 在启动前仍在下载插件及其传递依赖（如 `spring-boot-buildpack-platform`、`spring-boot-loader-tools` 等），怀疑依赖解析/下载不完整导致 fork JVM 的 classpath 缺失核心依赖。
- 注意：报错发生在 `main` 第一行（`SpringApplication.run` 之前），**尚未进入配置解析阶段**，因此本次无法验证 `JWT_SECRET`/`GEMINI_API_KEY` 对启动的影响。

---

## 6. 测试情况

- **测试代码检查（已确认）**：全项目无 `src/test` 目录，无任何 `*Test.java` / `*Tests.java` 文件（`search_file` 返回 0 个结果）。
- **测试依赖（已确认）**：`pom.xml:69-73` 引入了 `spring-boot-starter-test`（scope=test），但**尚未被任何测试用例使用**。
- **测试执行（已执行）**：`mvn clean test` 结果为 **BUILD SUCCESS**，Surefire 输出 `No tests to run.`（无任何测试类被运行）。**测试覆盖率为 0**（因无测试代码，非"测试通过"）。

---

## 7. 启动验证情况

- **状态：已执行，启动成功**。
- 经历两次尝试：
  1. `mvn spring-boot:run`：**失败**（`NoClassDefFoundError: SpringApplication`，详见第 5.4 节）。
  2. `mvn clean package -DskipTests` + `java -jar target\food-delivery-backend-0.0.1-SNAPSHOT.jar`：**成功**（改用打包运行方式绕开 fork classpath 问题）。
- 浏览器访问 `http://localhost:8080/` 返回（已确认，来自用户截图）：

```json
{"service":"Food Delivery API","github":"https://github.com/manishxydev/food-delivery-backend","status":"running"}
```

- 该响应正是 `HomeController.home()`（`controller/HomeController.java:10-15`）的返回值，确认应用已正常启动、`GET /` 接口可响应。

### 7.1 启动前必须设置的环境变量

| 变量 | 是否必须 | 说明 |
|------|----------|------|
| `JWT_SECRET` | 必需 | `application.properties:1` 的 `${JWT_SECRET}` 无默认值，未设置会导致启动时无法解析占位符而失败（待验证）。且 JWT 使用 HMAC-SHA，`JwtUtil` 调用 `Keys.hmacShaKeyFor(secret.getBytes())`，**密钥需 ≥ 32 字节**。 |
| `GEMINI_API_KEY` | 视情况 | `application.properties:18` 的 `${GEMINI_API_KEY}` 无默认值；`GeminiClient` 通过 `@Value` 注入。若其 Bean 在启动时被装配，缺该变量可能导致启动失败（待验证）。若仅做非 AI 接口验证，仍需确认是否阻断启动。 |
| `PORT` | 可选 | `${PORT:8080}` 有默认值 8080。 |
| `H2_CONSOLE` | 可选 | `${H2_CONSOLE:false}` 默认关闭。 |

### 7.2 启动成功后应观察的日志特征（待验证）

- Spring Boot 启动横幅 + 版本号（`Tomcat started on port 8080`）。
- Hibernate 建表 SQL（因 `show-sql=true`、`ddl-auto=update`），可能出现 `create table users/restaurants/menu_items/orders/order_items`。
- `Started FoodDeliveryApplication in ... seconds`（启动完成标志）。

### 7.3 适合验证的现有接口

无需认证（`SecurityConfig` 中 `permitAll`）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 健康/信息接口，返回 `{service, status, github}` |

需要认证的接口（其余均需 JWT）：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册（无需认证） |
| POST | `/api/auth/login` | 登录（无需认证），返回 JWT |
| GET | `/api/restaurants` | 餐厅列表 |
| POST | `/api/restaurants` | 新建餐厅 |
| GET | `/api/restaurants/{id}` | 餐厅详情 |
| PUT | `/api/restaurants/{id}` | 更新餐厅 |
| DELETE | `/api/restaurants/{id}` | 删除餐厅 |
| POST | `/api/menu-items` | 新建菜品 |
| GET | `/api/menu-items/{id}` | 菜品详情 |
| GET | `/api/menu-items/restaurant/{restaurantId}` | 按餐厅查菜品 |
| PUT | `/api/menu-items/{id}` | 更新菜品 |
| DELETE | `/api/menu-items/{id}` | 删除菜品 |
| POST | `/api/orders` | 下单 |
| GET | `/api/orders/{id}` | 订单详情 |
| GET | `/api/orders/my-orders` | 当前用户订单 |
| GET | `/api/orders/restaurant/{restaurantId}` | 按餐厅查订单 |
| PUT | `/api/orders/{id}/status?status=...` | 更新订单状态 |
| POST | `/api/chat` | AI 聊天（需 Gemini Key） |
| DELETE | `/api/chat` | 重置会话 |

> 验证建议顺序：先 `GET /` 确认服务启动 → `POST /api/auth/register` 注册 → `POST /api/auth/login` 拿 token → 携带 `Authorization: Bearer <token>` 调受保护接口。

---

## 8. 尚未解决的问题

1. **`mvn spring-boot:run` 启动失败**（已确认现象，已有替代方案）：`NoClassDefFoundError: SpringApplication`，fork JVM 缺少 Spring Boot 核心依赖。已通过 `java -jar` 方式成功启动，可继续用 jar 方式开发验证；`spring-boot:run` 的 fork classpath 根因**待确认**（可重试一次，插件依赖缓存后可能恢复）。
2. **`GEMINI_API_KEY` 缺失是否阻断启动**：本次启动时设置了占位值（非真实 key），应用成功启动。说明只要该环境变量被设置即可通过占位符解析；但**完全不设置该变量是否会阻断启动**仍待验证（可单独测试）。
3. **`JWT_SECRET` 缺失是否阻断启动**：本次已设置，未验证缺失场景（待验证）。
4. **登录失败/非法 token 的 HTTP 状态码**：静态分析怀疑返回 500 而非 401/403，待运行验证。
5. **受保护接口的完整功能验证**：仅 `GET /` 已验证；注册、登录、下单等接口**待验证**。

### 已解决/已确认

- JDK 版本：**17.0.20.1**，满足要求（已确认）。
- Maven 版本：**3.9.15**，满足要求（已确认）。
- `mvn clean test`：**BUILD SUCCESS**，44 源文件编译通过，无测试可运行（已确认）。
- 应用启动：**已成功**（`java -jar` 方式），`GET /` 返回预期 JSON（已确认）。

---

> 说明：本文件仅记录本轮"检查 + 验证准备"结果；所有"待执行"项需在后续实际运行命令后更新为"已执行"并补充真实输出，不得在未执行前写成成功。
