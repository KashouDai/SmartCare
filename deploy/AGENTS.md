# Repository Guidelines

仓库由后端、管理端、用户端和database脚本四部分组成。根目录当前没有 `.git` 元数据，因此提交规范只能作为后续建议基线。

## Project Structure & Module Organization

- `backend/hims-backend-code-master/`：Java 8、Spring Boot 2.5、Maven 多模块后端。入口为 `lws-admin/src/main/java/com/lws/LwsApplication.java`；公共能力位于 `lws-common`、`lws-framework`，业务模块为 `lws-info`、`lws-order`，其余模块提供系统、定时任务和代码生成能力。Mapper XML 放在各模块的 `src/main/resources/mapper/`。
- `admin-web/hims-front-end-code-master/src/`：Vue 2、Element UI 管理端。按 `api/`、`views/`、`router/`、`store/` 分层。
- `patient-web/hims-site/`：Nuxt 2 预约挂号用户端。路由页面放在 `pages/`，接口封装放在 `api/`。
- `database/hims-public.sql`：公开版 MySQL 初始化结构与基础字典数据；后端 `sql/` 内的脚本补充业务表或 Quartz 数据。

## Build, Test, and Development Commands

```powershell
cd backend\hims-backend-code-master
mvn clean package -Dmaven.test.skip=true  # 构建，产物为 lws-admin/target/lws-admin.jar
mvn spring-boot:run -pl lws-admin -am      # 启动后端，默认端口 9999
```

启动前需准备 MySQL database `lwsss` 和本地 Redis（默认 6379），并检查 `application*.yml`。

```powershell
cd admin-web\hims-front-end-code-master
npm install
npm run dev          # 管理端开发服务器
npm run lint         # ESLint 检查
npm run build:prod   # 生产构建
```

```powershell
cd patient-web\hims-site
npm install
npm run dev          # Nuxt 开发服务器，默认 3000
npm run lint
npm run build
npm start
```

## Coding Style & Naming Conventions

统一使用 UTF-8 和 LF 换行。Java 使用 4 空格缩进、`PascalCase` 类名、`camelCase` 方法和字段，并保留 `com.lws.*` 包结构。前端使用 2 空格缩进；管理端 ESLint 要求单引号、无分号、无行尾逗号。Vue 组件使用 `PascalCase`，页面目录沿用现有小写或 camelCase 命名。不要进行与改动无关的格式化。

## Testing Guidelines

当前仓库没有测试目录、测试依赖、覆盖率和 `test` 脚本。`mvn test` 与 `npm run lint` 可执行现有检查，但不能替代验证。修改接口时检查参数、权限、database和异常路径；修改界面时运行对应 lint/build 并完成手工回归。若新增自动化测试，后端放入 `src/test/java`，前端应同步引入实际使用的测试框架和命令。

## Commit & Pull Request Guidelines

由于没有 Git 历史，建议提交使用简短的 `<scope>: <summary>`，例如 `lws-admin: fix appointment validation`。每次提交聚焦一个改动。Pull Request 应说明影响模块、关联 Issue、验证命令、database或配置变更；界面改动附截图。不要提交 `node_modules/`、`target/`、`dist/` 或 IDE 临时文件。

## Security & Configuration Tips

database 密码、JWT 密钥、支付宝私钥及环境地址不得使用生产值入库。将本地差异放在未跟踪的配置或环境变量中；若密钥已经公开，应在部署前轮换。导入 `database/hims-public.sql` 前确认其中数据是否允许用于当前环境。
