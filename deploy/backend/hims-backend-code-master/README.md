# CLAUDE.md — backend后端模块（hims-backend-code-master）

## 模块定位

本目录是医院预约挂号系统的 Spring Boot 单体后端，基于 RuoYi-Vue 3.8.6 二次开发。
入口：`hims-admin/src/main/java/com/hims/HimsApplication.java`，默认端口 **9999**。

## Maven 多模块结构

```
hims (root pom)
├── hims-admin           # 启动入口 + web controller 层（聚合所有模块）
├── hims-common          # 公共工具：RedisCache / UUID / XSS / Excel / 异常体系
├── hims-framework       # 框架层：安全(JWT)、数据源、缓存、AOP 切面、拦截器
├── hims-system          # 系统管理：用户/角色/菜单/部门/字典/日志（RuoYi 标准）
├── hims-info            # 医院业务：医生/科室/排班/挂号/病历/药品
├── hims-order           # 订单支付：支付宝集成、订单、支付记录（依赖 hims-info）
├── hims-quartz          # 定时任务：Quartz 调度
└── hims-generator       # 代码生成器：Velocity 模板生成 CRUD
```

**依赖方向**：`hims-admin → hims-framework → hims-common`；业务模块 `hims-info`、`hims-order` 依赖 `hims-common` 和 `hims-system`，`hims-order` 额外依赖 `hims-info`。修改任一模块后需 `mvn clean package -Dmaven.test.skip=true` 重新构建。

## 常用路径速查

| 内容 | 路径 |
|------|------|
| 主配置 | `hims-admin/src/main/resources/application.yml` |
| 数据源（Druid） | `hims-admin/src/main/resources/application-druid.yml` |
| MyBatis 配置 | `hims-admin/src/main/resources/mybatis/mybatis-config.xml` |
| 所有 Mapper XML | `hims-*-src/main/resources/mapper/<模块>/` |
| 自定义注解 | `hims-common/src/main/java/com/hims/common/annotation/` |
| AOP 切面 | `hims-framework/src/main/java/com/hims/framework/aspectj/` |
| JWT 过滤器 | `hims-framework/src/main/java/com/hims/framework/security/filter/JwtAuthenticationTokenFilter.java` |
| Redis 工具 | `hims-common/src/main/java/com/hims/common/core/redis/` |
| 工具类 | `hims-common/src/main/java/com/hims/common/utils/` |
| 业务域对象基类 | `hims-common/src/main/java/com/hims/common/core/domain/BaseEntity.java` |
| 接口响应封装 | `hims-common/src/main/java/com/hims/common/core/domain/AjaxResult.java`、`R.java` |
| database脚本 | 上层 `database/himsss.sql`（主库初始化）+ `sql/`（补充表/Quartz） |

## 关键框架约定

- **包路径**：`com.hims.*`，各模块控制器放在对应模块的 `controller/` 下，通过 `hims-admin` 的 `web/controller/` 统一暴露（或直接放 `hims-admin/web/` 下）
- **分页**：PageHelper，`helperDialect: mysql`，`params: count=countSql`
- **动态数据源**：`@DataSource` 注解切换主/从库，`DynamicDataSourceContextHolder` 管理线程上下文（当前从库关闭）
- **安全**：JWT（`token.secret` 和 `token.expireTime` 在 `application.yml`）
- **缓存**：`RedisCache` 封装 String/List/Set/Hash，key 前缀定义在 `CacheConstants`
- **自定义注解**：`@RateLimiter`（限流）、`@RepeatSubmit`（防重复提交）、`@Log`（操作日志）、`@DataScope`（数据权限）、`@Anonymous`（匿名访问）
- **雪花 ID**：`SnowflakeIdUtils`（`hims-common/utils/uuid/`）已实现但**未接入**业务表，当前业务表用 MySQL AUTO_INCREMENT

## 构建与调试

```powershell
cd backend\hims-backend-code-master
mvn clean package -Dmaven.test.skip=true    # 构建，产物: hims-admin/target/hims-admin.jar
mvn spring-boot:run -pl hims-admin -am       # 启动，端口 9999
```

- Druid 监控：`http://localhost:9999/druid/index.html`（账号和密码通过环境变量配置）
- Swagger：`http://localhost:9999/swagger-ui/index.html`（`swagger.enabled=true`）
- 日志级别：`com.hims` 为 `debug`，`org.springframework` 为 `warn`（见 `logback.xml`）

## database要点

- database名：`himsss`，字符集 `utf8`（**非** utf8mb4），COLLATE `utf8_general_ci`
- 连接：`localhost:3306`，用户和密码通过环境变量配置（见 `application-druid.yml`）
- 重要业务表：`appointments_info`（预约）、`duty_info`（排班）、`doctor_info`（医生）、`patients_info`（患者）、`order`（订单）、`pay_record`（支付记录）
- 建库脚本：上层 `database/himsss.sql`；Quartz 初始表：`sql/quartz.sql`
- **预约相关迁移脚本固定放在本模块的 `sql/` 目录**，命名 `<业务域>_phase<N>[_用途].sql`（如 `appointment_phase2.sql`、`appointment_phase2_audit.sql`、`appointment_phase2_rollback.sql`）
- **所有预约写入必须经过统一事务 Service** `OrderServiceImpl.createRegisteredOrder`，不得新增绕过该事务的占号/建单入口（占号、建预约、建订单、幂等落库必须在同一事务内）
- **新增预约相关迁移前必须先执行只读审计 SQL**（如 `sql/appointment_phase2_audit.sql`），并确认「重复有效预约」为 0 行后方可执行迁移；迁移脚本须注明备份、执行顺序、前置条件、失败处理与回滚方式

## 安全注意

JWT 密钥、支付宝私钥/公钥、数据源口令与 Druid 控制台口令都已改为环境变量占位符
（见 `application.yml`、`application-druid.yml`，变量清单在上层 `deploy/deploy/.env.example`），
真实值不落库。新增密钥类配置时沿用同一方式，不要把明文写回配置文件。
`database/himsss.sql` 等种子脚本中的姓名、手机号、邮箱、身份证号已脱敏。

## 许可证与第三方声明

许可证文件固定放在后端模块目录：

- [LICENSE](deploy/backend/hims-backend-code-master/LICENSE)
- [NOTICE](deploy/backend/hims-backend-code-master/NOTICE)

项目部分后端、管理端和患者端代码基于 RuoYi-Vue 二次开发。RuoYi-Vue 的来源、MIT 许可和二次开发说明见 `NOTICE`。其他依赖遵循其自身许可证，依赖清单见各模块的 `pom.xml`、`package-lock.json` 和 `requirements.txt`。
