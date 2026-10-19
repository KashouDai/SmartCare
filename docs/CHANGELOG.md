# 变更记录

## 2026-10-18

### 仓库与文档基线

- 将 `deploy` 和 `drug-rag` 纳入 SmartCare 根仓库管理；
- 创建根仓库初始提交 `9438fbf`；
- 增加根目录 `.gitignore`，排除构建产物、依赖、密钥和 RAG 生成数据；
- 增加项目文档入口、开发计划、架构说明、ADR 和阶段验收记录。

### 阶段 1：知识模型与领域注册

- 新增通用知识文档、知识片段和来源模型；
- 增加药品模型到通用模型的显式转换；
- 默认只注册 `drug` 知识域，未知领域不会静默回退；
- 增加 9 个不依赖外部模型的单元测试；
- 增加 FastAPI 接口级契约测试，覆盖令牌、健康检查、就绪检查和问答响应；
- 完成 Spring Boot 代理协议审计，确定使用 `AjaxResult.data` 输出公共响应；

### 阶段 2：通用文本解析子阶段

- 创建 `codex/phase-2-ingestion` 开发分支；
- 新增 TXT/Markdown 通用文档解析器；
- 新增 DOCX 通用文档解析器；
- 新增通用 PDF 文档解析入口，按页保留来源；
- 新增通用 KnowledgeChunk JSONL 读写和混合文档批处理；
- 新增通用 KnowledgeChunk 到独立 Chroma collection 的索引适配；
- Catalog 升级为兼容 schema 2，支持版本注册、published 激活和旧版本保留；
- 新增 Catalog 文件读写和索引版本发布流程；
- 新增批处理、通用 Chroma 和 Catalog 发布的端到端工作流；
- 新增通用摘要 collection；
- 新增按 Catalog active 版本加载 chunk/summary collection 的运行时入口；
- 支持知识域、文档版本、标题、章节和领域元数据；
- 增加 20 个通用解析、批处理、索引、Catalog 和工作流测试，当前总测试数为 29；
- 暂未接入通用 JSONL、Chroma 和 Catalog 运行时切换。
- 统一问答响应增加 `domain`、`status` 和 `requestId`，保留原有字段；
- 澄清和无证据结果分别返回明确状态；
- 暂未修改药品检索行为。

### 阶段 3：API 代理源码里程碑

- FastAPI 问答协议支持通用知识域、过滤条件和轻量上下文字段，当前仅启用 `drug`；
- 新增 Spring Boot `/knowledge/ask` 代理、内部令牌、超时设置和错误映射；
- Docker Compose 增加 `drug-rag` 服务，backend 通过服务名访问；
- 管理端新增“知识助手”单轮问答页与请求封装；
- 管理端 Nginx 增加 SPA 深层路由回退，修复直接访问或刷新 `/admin/knowledge/assistant` 返回 404；
- 完成首批 3 份药品说明书的 DashScope embedding 索引构建、Catalog 发布和 Compose 镜像接入；
- 补充药品 PDF 元数据提取、章节识别和通用 JSONL 药品目录兼容；
- 对外响应只保留回答、安全提示、来源和请求编号，不暴露 RAG Trace；
- `drug-rag` 31 个测试通过；Docker 内部问答已命中坎地沙坦酯片说明书来源；LLM 模型已切换为 `qwen3.8-flash`，生成式回答验证成功。

## 记录模板

```markdown
## YYYY-MM-DD

### 阶段名称

- 变更：
- 验证：
- 提交：
- 标签：
- 遗留问题：
```
