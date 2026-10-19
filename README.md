# SmartCare

SmartCare 是一个面向本地学习、工程实践和功能演示的医疗信息系统与知识助手项目，包含预约挂号系统、管理端、患者端、药品知识 RAG 服务、数据库脚本和 Docker Compose 部署配置。

> 本项目仅用于学习和本地演示，不是可直接用于真实医疗业务的生产系统。医疗问答不能替代医生诊断、处方或专业意见。

## 项目定位

- **预约挂号系统**：医院、科室、医生、排班、患者、预约、订单和管理后台。
- **药品知识助手**：说明书解析、索引、检索、安全评估和受控问答。

请求链路：

```text
浏览器 → Nginx Gateway :8080
       ├─ /          → Nuxt 患者端
       ├─ /admin/    → Vue 管理端
       └─ /api/      → Spring Boot :9999 → drug-rag :8000
```

## 主要功能

### 预约挂号

- 医院、城市、科室、医生和排班信息管理
- 患者注册、患者信息维护和预约入口
- 预约创建、订单状态和支付生命周期相关代码
- 用户、角色、菜单、部门、字典和操作日志管理
- 基于事务、条件更新和数据库唯一约束的预约一致性控制
- Redis 缓存和 MySQL 业务状态持久化

### 药品知识助手

- 支持 PDF、DOCX、TXT、Markdown 文档解析
- 说明书章节识别和结构化文本切分
- 元数据过滤、向量检索、摘要检索和混合检索
- 意图路由和可选模型重排
- 基于检索证据的受控答案生成
- 禁忌、不良反应、特殊人群等问题的安全评估
- 缺少药品名称时返回澄清提示
- 文档上传、去重、解析和索引更新

## 目录结构

```text
SmartCare/
├─ deploy/
│  ├─ backend/hims-backend-code-master/       # Spring Boot 多模块后端
│  ├─ admin-web/hims-front-end-code-master/   # Vue 2 管理端
│  ├─ patient-web/hims-site/                  # Nuxt 2 患者端
│  ├─ database/hims-public.sql                # 公开版数据库脚本
│  └─ deploy/                                 # Compose、镜像和 Nginx
├─ drug-rag/                                  # FastAPI 药品知识服务
├─ docs/                                      # 架构、ADR、计划和验收
└─ README.md
```

## 技术栈

| 模块 | 技术 |
|---|---|
| 后端 | Java 8、Spring Boot、Spring Security、MyBatis、MySQL |
| 管理端 | Vue 2、Vue Router、Vuex、Element UI |
| 患者端 | Nuxt 2、Vue 2 |
| 知识服务 | Python 3.11+、FastAPI、Uvicorn |
| 检索 | ChromaDB、Embedding、混合检索、重排 |
| 基础设施 | Redis 7、Nginx、Docker Compose |

## Docker 快速启动

前置条件：Docker Desktop、Docker Compose，以及可选的 DashScope API Key。

```powershell
cd deploy\deploy
Copy-Item .env.example .env
```

至少配置以下本地演示变量：

```dotenv
MYSQL_ROOT_PASSWORD=change-me-root
MYSQL_APP_PASSWORD=change-me-app
REDIS_PASSWORD=change-me-redis
TOKEN_SECRET=change-me-token-secret
KNOWLEDGE_INTERNAL_TOKEN=change-me-knowledge-token
```

启用模型问答时设置：

```powershell
$env:DASHSCOPE_API_KEY = "your-api-key"
```

启动和停止：

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f gateway backend drug-rag
docker compose down
```

`docker compose down -v` 会删除本地数据库和 Redis 数据卷，执行前请确认数据可丢失。示例值不能用于生产环境，真实密钥不得提交到 Git。

## 访问地址

默认网关端口为 `8080`：

- 患者端：<http://localhost:8080/>
- 管理端：<http://localhost:8080/admin/>
- 后端根路径：<http://localhost:8080/api/>

管理端登录依赖数据库账号和验证码。公开版 SQL 只保留结构和基础字典数据，不承诺提供可直接使用的管理员账号。

## 服务端口

| 服务 | 容器端口 | 访问方式 |
|---|---:|---|
| gateway | 80 | `http://localhost:8080` |
| backend | 9999 | 经 gateway `/api/` 访问 |
| admin-web | 80 | 经 gateway `/admin/` 访问 |
| site-web | 3000 | 经 gateway `/` 访问 |
| drug-rag | 8000 | 默认仅内部访问 |
| MySQL | 3306 | Compose 内部访问 |
| Redis | 6379 | Compose 内部访问 |

## 主要接口

网关会将 `/api/` 前缀去除后转发给 Spring Boot：

```text
GET  /api/captchaImage
POST /api/login
GET  /api/info/hospital/list
GET  /api/info/city/town/{cityCode}
```

drug-rag 子服务主要接口：

```text
GET  /api/health
GET  /api/catalog
POST /api/retrieve
POST /api/ask
POST /api/upload
```

完整 RAG 配置、CLI 和接口说明见 [drug-rag/README.md](drug-rag/README.md)。知识服务通过 `KNOWLEDGE_INTERNAL_TOKEN` 与后端进行内部认证，不建议直接暴露到公网。

## 数据库说明

公开版初始化脚本为 `deploy/database/hims-public.sql`。该脚本保留表结构、城市/地区、字典和药品目录等基础数据，移除了患者、预约、订单、医院联系人、用户和登录日志等业务样例记录。

预约相关增量脚本位于 `deploy/backend/hims-backend-code-master/sql/`，执行前应确认数据库版本、执行顺序和当前数据状态。

## 独立运行 drug-rag

```powershell
cd drug-rag
pip install -r requirements.txt
$env:DASHSCOPE_API_KEY = "your-api-key"
python -m uvicorn drug_rag.api.main:app --host 127.0.0.1 --port 8000
```

全新克隆后，向量库和处理后的索引属于生成数据，需要先准备说明书并构建索引，再运行依赖索引的问答功能。具体命令见 [drug-rag/README.md](drug-rag/README.md)。

## 安全边界

- 数据库密码、JWT 密钥、内部服务令牌和支付宝凭据通过环境变量注入。
- 不要提交 `.env`、私钥、访问令牌、数据库业务导出、日志、`node_modules`、`target`、`dist`、`.venv` 或向量库生成物。
- 患者身份信息、手机号、身份证号和真实预约记录不得写入公开仓库。
- RAG 答案受检索证据和安全评估约束，但仍需要人工医学审核。
- Docker 构建成功只代表镜像和文件路径可用，不等同于完整浏览器端到端验收。

## 验证建议

```powershell
docker compose ps
Invoke-WebRequest http://localhost:8080/ -UseBasicParsing
Invoke-WebRequest http://localhost:8080/admin/ -UseBasicParsing
Invoke-WebRequest http://localhost:8080/api/ -UseBasicParsing
Invoke-WebRequest http://localhost:8080/api/captchaImage -UseBasicParsing
```

浏览器验证应覆盖患者端首页、管理端验证码和登录、后端 API，以及知识助手代理链路。当前已验证过公开版 MySQL 镜像构建和本地 Compose 服务启动；生产部署、真实支付/医保/短信对接、医疗合规和生产性能指标不属于当前验证结论。


## 许可证与第三方声明

许可证文件固定放在后端模块目录：

- [LICENSE](deploy/backend/hims-backend-code-master/LICENSE)
- [NOTICE](deploy/backend/hims-backend-code-master/NOTICE)

项目部分后端、管理端和患者端代码基于 RuoYi-Vue 二次开发。RuoYi-Vue 的来源、MIT 许可和二次开发说明见 `NOTICE`。其他依赖遵循其自身许可证，依赖清单见各模块的 `pom.xml`、`package-lock.json` 和 `requirements.txt`。
