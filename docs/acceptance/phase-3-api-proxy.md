# 阶段 3：API 代理

## 状态

源码实现完成，环境联调待执行。

## 本次完成内容

- `/api/ask` 支持通用请求字段：`domain`、`filters`、`history`、`context`。
- 保留 `drug_name`、`generic_name`、`category` 等原有药品请求字段，兼容旧调用方。
- 当前仅注册 `drug` 知识领域；未注册领域返回 HTTP 422。
- 通用 `filters` 中的药品过滤条件会映射到现有药品检索约束。
- `history` 与 `context` 本阶段只完成协议接收，尚未参与回答生成或长期记忆。
- Spring Boot 新增 `/api/knowledge/ask`，转发 JSON 请求到内部 FastAPI。
- 代理支持内部令牌、连接超时和读取超时配置，并将响应包装为 `AjaxResult`。
- HTTP 调用已抽离到 `KnowledgeRagClient`，控制器只负责协议映射和错误包装。
- FastAPI 非 2xx 响应会作为业务错误返回；连接、超时或解析异常统一返回服务不可用。

## 验证结果

在 `drug-rag` 目录执行：

```text
python -m compileall -q src                 通过
python -m unittest discover -s tests -v     31 个测试通过
git diff --check                             通过
```

覆盖了旧请求兼容、通用 filters 映射和未知领域拒绝等契约行为。

Spring Boot 编译验证未执行：当前环境未安装 `mvn`，项目也未提供 Maven Wrapper。

已通过 `docker compose build backend` 使用 Dockerfile 中的 Maven 3.8.8 / Java 8 构建环境执行 `mvn -DskipTests clean package`，9 个 Maven 模块构建成功并生成后端镜像；Java 测试仍未执行。

已使用本地 Compose 环境成功启动 MySQL、Redis、drug-rag 和 backend，四个服务均达到 healthy。匿名访问 backend `/knowledge/ask` 返回既有 JWT 安全过滤器的 401，未进入知识助手控制器；认证后的代理转发仍需使用管理端登录令牌联调，不能通过放开接口绕过验证。

管理端 ESLint 验证未执行：当前环境未安装 `npm`。

已通过 `docker compose build admin-web` 使用 Node 16 构建管理端生产镜像，`npm run build:prod` 成功；构建仅报告既有 Browserslist 过期提示和资源体积警告。

## 范围边界

Docker Compose 已加入 `drug-rag` 服务，并通过服务名 `drug-rag:8000` 配置 Spring Boot 代理；实际镜像构建和容器启动验证仍需 Docker 环境。

`drug-rag` 的 Compose 构建上下文为 `../../drug-rag`，以匹配其 Dockerfile 中相对 `COPY` 路径。已使用 Docker Compose 和示例环境变量成功构建并启动该服务，`/api/health` 返回 200；`/api/ask` 无内部令牌返回 401，携带正确令牌但空请求体返回 422。

管理端和网关对外调用路径为 `/api/knowledge/ask`。网关会去除 `/api` 前缀后转发，因此 Spring Boot 内部映射为 `/knowledge/ask`。

管理端请求封装位于 `src/api/knowledge.js`，通过既有 Axios 基地址 `/api` 调用，不直接访问 FastAPI。

管理端已加入静态“知识助手”菜单和单轮问答页，展示回答、安全提示、来源与请求编号；不实现会话历史、长期记忆或知识库管理。

管理端 Nginx 已配置 `try_files` SPA 回退，直接访问或刷新 `/admin/knowledge/assistant` 时返回前端入口，不再按静态文件路径返回 404；已通过浏览器刷新验证。

已使用 3 份本地药品说明书构建 `text-embedding-v3` 向量和摘要索引，发布 Catalog 版本 `drug-20261018-v5`，并将 `drug_chunks_v5`、`drug_summaries_v5` 和药品目录随 drug-rag 镜像加载。指定“坎地沙坦酯片”的问答已命中对应说明书来源。

Spring Boot 只向管理端输出 `answer`、`domain`、`status`、`sources`、`safety` 和 `requestId`，不透传 FastAPI 内部 `trace`。

代理会将 FastAPI 422 映射为 400 参数错误，401/403 映射为内部令牌校验失败，503 映射为服务不可用；连接失败、超时和响应解析失败分别返回明确错误提示。

本次已使用系统 Python 并显式设置 `PYTHONPATH=src` 执行 `python -m unittest discover -s tests -v`，31 个 drug-rag 测试通过。

## 已完成环境验证

- 使用 Docker 内 Maven 8 编译 Spring Boot，9 个模块构建成功；
- 使用 Docker 内 Node 16 完成管理端生产构建；
- 使用 Docker Compose 启动全栈，验证健康检查、内部令牌和登录后端到端问答链路；
- 通过浏览器验证管理端深层路由刷新和问答结果展示。

## 遗留问题

- 当前 `drug-rag` 尚未加载正式药品知识库，测试回答的来源列表仍可能为空；
- Java 单元测试和管理端 ESLint 未执行，当前验证以 Docker 构建和运行时联调为主。
- 当前 DashScope LLM 已配置为 `qwen3.8-flash`，生成式回答、embedding、检索和来源返回均已验证正常。
