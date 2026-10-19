# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.


## Build & Run Commands

```powershell
# 后端（Maven 多模块，Java 8，Spring Boot 2.5）
cd backend\hims-backend-code-master
mvn clean package -Dmaven.test.skip=true    # 构建，产物: lws-admin/target/lws-admin.jar
mvn spring-boot:run -pl lws-admin -am       # 启动后端，默认端口 9999
# 启动前需准备 MySQL database lwsss 和本地 Redis（6379），并检查 application.yml

# 管理端（Vue 2 + Element UI）
cd admin-web\hims-front-end-code-master
npm install && npm run dev     # 开发服务器，端口 80
npm run lint                   # ESLint 检查（单引号、无分号、无行尾逗号）
npm run build:prod             # 生产构建至 dist/

# 用户端（Nuxt 2 SSR）
cd patient-web\hims-site
npm install && npm run dev     # 开发服务器，端口 3000
npm run lint
npm run build && npm start

# Docker Compose 验收（从仓库根目录执行）
Copy-Item deploy\.env.example deploy\.env
# 必须逐服务构建：compose v5 的 up --build 走 bake 并发路径，当仓库绝对路径含非 ASCII 字符
# 且构建目标 ≥2 个时，buildx 会在建立构建 session 时报
# x-docker-expose-session-sharedkey 错误（单目标不建 session，故可绕开）。
docker compose --env-file deploy\.env -f deploy\docker-compose.yml build mysql
docker compose --env-file deploy\.env -f deploy\docker-compose.yml build backend
docker compose --env-file deploy\.env -f deploy\docker-compose.yml build admin-web
docker compose --env-file deploy\.env -f deploy\docker-compose.yml build site-web
docker compose --env-file deploy\.env -f deploy\docker-compose.yml up -d
docker compose --env-file deploy\.env -f deploy\docker-compose.yml ps
docker compose --env-file deploy\.env -f deploy\docker-compose.yml down

# 网关默认暴露 8080：/api/ 转发后端，/admin/ 转发管理端，/ 转发用户端。
# deploy\.env 仅供本地使用，不要提交；生产环境必须替换database、Redis 和应用密钥。
```

## Backend Module Architecture

```
lws (root pom, dependency management)
├── lws-admin              # 启动入口，聚合所有模块，web controller 层
├── lws-common             # 公共工具：RedisCache、UUID、XSS、Excel、异常体系
├── lws-framework          # 框架层：安全(JWT)、数据源、缓存、AOP切面、拦截器
├── lws-system             # 系统管理：用户/角色/菜单/部门/字典/日志（RuoYi 标准）
├── lws-info               # 医院业务：医生/科室/排班/挂号/病历/药品
├── lws-order              # 订单支付：支付宝集成、订单、支付记录（依赖 lws-info）
├── lws-quartz             # 定时任务：Quartz 调度
└── lws-generator          # 代码生成器：Velocity 模板生成 CRUD
```

**包依赖方向**：`lws-admin → lws-framework → lws-common`；业务模块 `lws-info`、`lws-order` 依赖 `lws-common` 和 `lws-system`，`lws-order` 额外依赖 `lws-info`。

**Mapper XML 分布**：各模块的 `src/main/resources/mapper/<模块名>/` 下，如 `lws-info/src/main/resources/mapper/info/`。MyBatis 扫描路径为 `classpath*:mapper/**/*Mapper.xml`。

## Key Framework Points

- **入口类**：`lws-admin/src/main/java/com/lws/LwsApplication.java`
- **自定义注解**（`lws-common/annotation/`）：`@RateLimiter`（限流）、`@RepeatSubmit`（防重复提交）、`@Log`（操作日志）、`@DataScope`（数据权限）、`@Anonymous`（匿名访问）
- **切面实现**（`lws-framework/aspectj/`）：`RateLimiterAspect`、`LogAspect`、`DataScopeAspect`、`DataSourceAspect`（读写分离）
- **安全**：JWT 令牌（`framework/security/filter/JwtAuthenticationTokenFilter`），密钥和有效期在 `application.yml` 的 `token` 配置段
- **缓存**：`RedisCache`（`lws-common/core/redis/`）封装了 String/List/Set/Hash 四种 Redis 数据结构操作；`CacheConstants` 定义缓存 key 前缀
- **分页**：PageHelper，`helperDialect: mysql`，`params: count=countSql`
- **动态数据源**：`@DataSource` 注解切换主/从库，`DynamicDataSourceContextHolder` 管理线程上下文
- **雪花 ID**：`SnowflakeIdUtils`（`lws-common/utils/uuid/`）已实现但**未接入**业务表，当前仍使用 MySQL AUTO_INCREMENT

## Frontend Architecture

- **管理端**（`admin-web/hims-front-end-code-master/src/`）：Vue 2 + Element UI + Vuex store。按 `api/`（接口封装）、`views/`（页面）、`router/`（路由）、`store/`（状态）分层。API 代理指向 `http://localhost:9999`
- **用户端**（`patient-web/hims-site/`）：Nuxt 2 SSR，路由即页面（`pages/`），接口封装在 `api/`。页面包括：`hospital/`（医院详情/公告）、`order/`（订单）、`patient/`（患者管理）、`user/`（用户中心）

## Database

- **脚本**：`database/hims-public.sql`（公开版主库初始化，保留结构与基础字典数据），后端 `sql/` 子目录有补充表和 Quartz 数据
- **连接池**：Druid，监控路径 `/druid/index.html`
- **重要业务表**：`appointments_info`（预约）、`duty_info`（排班）、`doctor_info`（医生）、`patients_info`（患者）、`order`（订单）、`pay_record`（支付记录）
- **字符集**：utf8（非 utf8mb4），COLLATE utf8_general_ci

## Coding Conventions

- UTF-8 编码，LF 换行
- Java：4 空格缩进，PascalCase 类名，camelCase 方法/字段，`com.lws.*` 包路径
- 前端：2 空格缩进；管理端 ESLint 强制单引号、无分号、无行尾逗号；Vue 组件 PascalCase
- 不要进行与改动无关的格式化

## Security Notes

密钥一律走环境变量，配置文件只保留占位符：

| 配置项 | 环境变量 |
|--------|----------|
| `token.secret` | `TOKEN_SECRET` |
| `alipay.appId` / `appPrivateKey` / `alipayPublicKey` | `ALIPAY_APPID` / `ALIPAY_APP_PRIVATE_KEY` / `ALIPAY_PUBLIC_KEY` |
| `spring.datasource.druid.master.password` | `SPRING_DATASOURCE_DRUID_MASTER_PASSWORD` |
| Druid 控制台 `login-password` | `DRUID_LOGIN_PASSWORD` |

完整变量清单见 `deploy/.env.example`。未设置数据库或 Druid 密码时，应用不会使用内置默认口令；
JWT 密钥与支付宝凭据无默认值，缺失时对应功能直接不可用。新增配置项时不要把真实值写回 yml。

`database/hims-public.sql` 不包含患者、预约、订单或登录日志数据，仅保留结构与基础字典数据。
