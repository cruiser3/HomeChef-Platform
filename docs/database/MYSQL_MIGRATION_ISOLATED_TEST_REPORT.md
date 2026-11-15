# MySQL 9.6 隔离兼容性测试报告

**测试日期：** 2026-11-14

**报告状态：** 迁移前历史报告。隔离探针部分通过；当时 Spring Boot 与 Flyway 13.12.2 集成失败、Hibernate validate 未执行。后续已改用 Spring Boot BOM 管理的 Flyway 10.10.0 完成正式迁移，见 [数据库设计与迁移记录](./MYSQL_DATABASE_DESIGN.md)。

**测试边界：** 本报告记录的测试仅检查本机 MySQL 9.6.0 和隔离 schema `homechef_flyway_check`。当时未创建或修改正式目标 schema `homechef_platform`，也未迁移 H2 临时数据；这不是后续正式迁移状态的描述。

## 1. 测试环境

| 组件 | 实际版本/状态 |
|---|---|
| Java | 17.0.20.1 |
| Spring Boot | 3.3.2 |
| Hibernate ORM | 6.5.2.Final（由 Spring Boot 3.3.2 管理） |
| MySQL Server | 9.6.0，`127.0.0.1:3306` |
| MySQL Connector/J | 9.7.0 |
| Flyway Core / MySQL module | 13.12.2（从 Spring Boot BOM 管理的 10.10.0 显式调整） |
| 隔离 schema | `homechef_flyway_check` |

本报告不包含数据库密码、JWT 密钥或本机凭据文件内容。

## 2. 测试结果摘要

| 验证项 | 结果 | 说明 |
|---|---|---|
| Connector/J JDBC 连接 | 通过 | 只读查询确认 MySQL 9.6.0、端口 3306 和隔离测试账号身份 |
| Flyway 10.10.0 简单迁移 | 迁移成功，但有警告 | Flyway 提示 MySQL 9.6 高于该版本已测试范围，日志称最新支持版本为 8.1 |
| Flyway 13.12.2 独立迁移及校验 | 迁移成功，但有警告 | 识别并验证由 10.10.0 建立的 V1，应用 V2，再验证两条迁移；仍提示 MySQL 9.6 高于已验证版本，最新验证版本为 9.4 |
| Spring Boot 3.3.2 + Flyway 13.12.2 自动配置 | 失败 | 自动配置调用 Flyway 13.12.2 中不存在的 API，触发 `NoSuchMethodError` |
| Hibernate `ddl-auto=validate` | 未执行 | Spring ApplicationContext 在 Flyway 自动配置阶段失败，EntityManagerFactory 未建立 |
| 11 张业务表临时迁移 | 未执行 | Workbench 表清单只显示两张探针表和 `flyway_schema_history` |
| 正式目标库 | 未触及 | `homechef_platform` 未创建或修改 |

## 3. Flyway 独立隔离探针

### 3.1 Flyway 10.10.0

在隔离 schema 中成功执行了简单 V1 探针，创建 `flyway_schema_history` 和 `flyway_compat_probe`。Flyway 输出警告：

> MySQL 9.6 is newer than this version of Flyway and support has not been tested. The latest supported version of MySQL is 8.1.

这只证明探针 DDL 在该本地组合下可执行，不代表 Flyway 10.10.0 对 MySQL 9.6 已获验证。

### 3.2 Flyway 13.12.2

Flyway 13.12.2 成功读取并验证 V1 history，随后执行简单 V2 探针并再次验证。只读查询确认：

- `flyway_compat_probe` 与 `flyway_13_compat_probe` 两张探针表存在。
- `flyway_schema_history` 中有两条成功的迁移记录。

首次探针尝试把 `validate()` 放在 `migrate()` 前，Flyway 按预期报告 V2 尚未应用；该次未执行迁移。改为先 `migrate()` 后 `validate()` 后，探针通过。

Flyway 13.12.2 运行时仍输出警告：

> Using MySQL 9.6 which is newer than the version Flyway has been verified with. The latest verified version of MySQL is 9.4.

因此 Flyway 13.12.2 的独立简单迁移虽成功，MySQL 9.6 仍超出其已验证范围，不能据此认定官方支持或生产兼容。

## 4. Spring Boot 集成失败

使用可执行应用 JAR、隔离 schema、独立 Flyway history 表及临时提取的 11 表设计 DDL 进行启动验证。Spring Boot 3.3.2 启动日志显示应用上下文进入初始化，但在 Flyway 自动配置阶段失败：

```text
java.lang.NoSuchMethodError:
  org.flywaydb.core.api.configuration.FluentConfiguration.cleanOnValidationError(boolean)
```

调用方为 Spring Boot 3.3.2 的 `FlywayAutoConfiguration`；Flyway Core 13.12.2 中不存在该方法。这是 Spring Boot 自动配置与 Flyway 13.12.2 之间的 API 二进制兼容问题。随后 JPA Repository 因 EntityManagerFactory 未创建而无法初始化，故 Hibernate validate 没有执行。

Workbench 只读查看到的 `homechef_flyway_check` 表名为：

1. `flyway_compat_probe`
2. `flyway_13_compat_probe`
3. `flyway_schema_history`

未发现临时 history 表或 11 张业务表，说明 Spring Boot 集成启动失败前没有应用临时业务 DDL。首次调试启动曾因 classpath 缺少 Spring 依赖而未进入应用；改用重新打包的可执行 JAR 后取得上述真实兼容性错误。

## 5. 构建和依赖检查

实际执行并通过：

```text
mvn -B -DskipTests compile
mvn -B dependency:tree "-Dincludes=org.flywaydb:flyway-core,org.flywaydb:flyway-mysql,com.mysql:mysql-connector-j"
mvn -B -DskipTests package
```

依赖树确认使用 Connector/J 9.7.0、Flyway Core/MySQL 13.12.2。`package` 成功不代表数据库集成或 Hibernate validate 通过。本次未执行 `mvn clean test`；当前工作区没有测试源码。

## 6. 未执行事项与数据边界

- 未创建或修改 `homechef_platform`，未执行正式迁移或目标 DDL。
- 未修改 Java 源码、`application.properties`、MySQL 服务配置；未添加正式迁移脚本。
- 未导出或迁移 H2 临时测试数据。
- 隔离 schema 保留两张探针表及其 Flyway history，未删除。
- Spring Boot 失败测试使用的临时 SQL 文件已清理；未继续重试。
- Hibernate validate、正式 11 表 DDL、业务接口、家庭隔离、事务和并发测试均未完成。

## 7. 后续方案（待用户决定）

1. **保持 Spring Boot 3.3.2，使用其 BOM 管理的 Flyway 10.10.0**：可先在隔离 schema 验证自动配置和 Hibernate validate；但 MySQL 9.6 的未验证警告仍存在，不能宣称官方兼容。
2. **保留 Flyway 13.12.2，评估 Spring Boot 升级**：需要单独评估升级影响并取得用户批准；本次没有升级 Spring Boot 或 Hibernate。
3. 不建议通过自定义 Flyway Bean、排除自动配置或忽略错误来规避 API 不兼容，因为这会绕过本次要求的真实集成验证。

**历史阶段结论：** 当时尚不能进入正式迁移阶段，需决定 Flyway/Spring Boot 兼容路线。该阻塞后来通过采用 Flyway 10.10.0 解决，正式迁移和 Hibernate validate 已完成；Flyway 对 MySQL 9.6 的已验证范围警告仍须单独评估。
