# 药品说明书智能问答系统

基于 RAG（检索增强生成）的药品说明书智能问答系统，支持多文档上传、意图路由、多路召回与重排，以及受控 LLM 生成安全回答。

## 功能特性

- **多格式文档解析**：支持 PDF、DOCX、TXT、MD，自动识别药品说明书章节结构
- **意图路由**：基于关键词规则的意图分类（用法用量、禁忌、不良反应、特殊人群等），稳定可解释
- **多路检索**：元数据过滤 + 向量检索 + 摘要索引两跳检索，策略自适应选择
- **重排与证据筛选**：支持关键词重排和 LLM 重排，证据长度预算控制
- **安全评估**：10 类风险关键词匹配（孕妇、儿童、肝肾功能、相互作用等），低/中/高三级风险等级
- **受控生成**：LLM 只能基于检索证据回答，不允许补充证据外的医学知识；LLM 不可用时自动退回模板答案
- **上传管理**：支持用户上传新药品说明书，MD5 去重，同名覆盖更新，自动重建索引

## 技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| 语言 | Python 3.11+ | 核心运行时 |
| Web 框架 | FastAPI + Uvicorn | REST API 与静态服务 |
| 向量库 | ChromaDB | 原文 chunk 与摘要向量存储 |
| LLM | 阿里云 DashScope（OpenAI 兼容接口） | 意图分类、重排、答案生成 |
| Embedding | DashScope text-embedding-v3 | 向量检索 |
| 分词 | 自定义中文分词（正则 + 二元模型） | 零依赖兜底方案 |
| 前端 | 原生 HTML / CSS / JavaScript | 无框架依赖的单页应用 |

## 架构

```
PDF/DOCX → parsers/instruction_parser.py → DrugChunk[]（含章节、页码、药品元数据）
                                          ↓
                         索引构建：Chroma 集合 "drug_chunks" + "drug_summaries"
                         + 元数据目录 JSON
                                          ↓
                  API 请求 → intent_router（关键词规则）→ query_rewriter
                                          ↓
                  metadata_filter（Chroma where 条件）
                                          ↓
                  retrieval_policy → 策略分发（metadata_first / safety_hybrid /
                        summary_guided_hybrid / hybrid / clarification / catalog_search）
                                          ↓
                  advanced_retriever → 多阶段检索 + 去重 + 重排
                                          ↓
                  qa_chain → 证据筛选 → safety_chain → LLM/模板答案
```

### 检索策略说明

| 策略 | 触发条件 | 说明 |
|------|----------|------|
| `metadata_first` | 固定章节问题（贮藏、用法用量等） | 仅按章节名过滤，不做向量检索 |
| `safety_hybrid` | 高风险问题（不良反应、禁忌等） | 全文检索 + 安全优先级加权 |
| `summary_guided_hybrid` | 模糊问题 | 先用摘要索引定位相关章节，再回原文取证 |
| `hybrid` | 有药品约束但意图不明 | 元数据过滤 + 向量混合检索 |
| `clarification` | 缺少药品名 | 返回追问提示而非直接回答 |
| `catalog_search` | 购药指导类问题 | 通过元数据目录匹配 |

### 安全设计

1. **不引入外部医学知识**：LLM 系统提示词明确限制答案只能基于检索证据
2. **强制安全评估**：每个回答生成前必须经过 `assess_safety()` 风险评估
3. **多药品隔离**：跨多药品的证据按药品分组回答，防止结论误用
4. **追问优于猜测**：缺少药品身份的问题进入 `clarification` 策略
5. **证据预算控制**：单文档上限 1200 字、单药品上限 1800 字、总计上限 6000 字
6. **模板兜底**：LLM 不可用时退回基于规则的模板答案

## 快速开始

### 安装

```bash
pip install -r requirements.txt
```

### 配置

复制并修改配置文件：

```bash
# 设置环境变量（必需）
export DASHSCOPE_API_KEY="your-api-key"
```

配置文件位于 `configs/app.yaml`，可调整 Embedding、LLM、Rerank 模型和路径。

### 启动服务

```bash
python -m uvicorn drug_rag.api.main:app --host 127.0.0.1 --port 8000
```

访问 `http://127.0.0.1:8000` 使用 Web 界面。

### CLI 命令

```bash
# 解析单个 PDF
python -m drug_rag.cli parse <pdf_path>

# 批量解析目录下所有 PDF
python -m drug_rag.cli batch-parse <dir>

# 批量解析 + 构建向量索引 + 摘要索引
python -m drug_rag.cli batch-build <dir>

# 基于已有 JSONL 构建索引
python -m drug_rag.cli ingest <jsonl_path>

# 完整问答
python -m drug_rag.cli ask "坎地沙坦酯片怎么吃？"
```

### Docker 构建

镜像会把已构建好的索引一起打包：

```dockerfile
COPY data/chroma/ ./data/chroma/
COPY data/processed/drug_directory.json ./data/processed/drug_directory.json
```

这两个路径都是生成产物，不在版本库中（见 `.gitignore`），`data/raw/` 也只有一个占位文件。
因此**全新 clone 后直接 `docker compose build knowledge-assistant` 会失败**。构建前先在本地生成索引：

```bash
export DASHSCOPE_API_KEY="your-api-key"
# 把药品说明书 PDF 放进 data/raw/，然后：
python -m drug_rag.cli batch-build data/raw
```

该命令会产出 `data/processed/all_drug_chunks.jsonl`、`data/processed/drug_directory.json`
和 `data/chroma/` 下的向量库，之后镜像构建才能通过。

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 前端页面 |
| GET | `/api/health` | 系统健康状态 |
| GET | `/api/catalog` | 药品目录列表 |
| GET | `/api/catalog/resolve` | 从问题中识别药品约束 |
| POST | `/api/retrieve` | 执行检索，返回 Trace |
| POST | `/api/ask` | 完整问答（答案 + Trace + 安全评估） |
| POST | `/api/upload` | 上传文档，自动解析并入库 |

## 项目结构

```
configs/          # YAML 配置文件
  app.yaml        # 主配置（路径、模型、rerank、安全设置）
  prompts.yaml    # QA 系统提示词和用户模板
  retriever.yaml  # 检索超参数（top_k、chunk 大小等）
  safety.yaml     # 安全风险等级定义
data/
  raw/            # 原始药品说明书 PDF
  uploads/        # 用户上传的文档
  processed/      # 解析产物（JSONL chunks + 药品目录）
  chroma/         # Chroma 向量库持久化
src/drug_rag/
  api/main.py     # FastAPI 应用主体
  chains/         # QA 管线与安全评估
  indexing/       # 向量索引与元数据目录构建
  loaders/        # PDF/DOCX 加载器
  parsers/        # 说明书解析器（章节识别、分块）
  retrieval/      # 意图路由、检索策略、重排
  schemas/        # 数据模型（DrugChunk、DrugDocument）
web/              # 前端（HTML/CSS/JS）
requirements.txt
```

## 环境变量

| 变量 | 说明 |
|------|------|
| `DASHSCOPE_API_KEY` | 阿里云 DashScope API 密钥（必需） |
| `DRUG_RAG_CONFIG` | 自定义配置文件路径 |
| `DRUG_RAG_CHROMA_DIR` | Chroma 持久化目录 |
| `DRUG_RAG_COLLECTION` | 向量集合名称 |
| `DRUG_RAG_WEB_DIR` | 前端静态文件目录 |
| `DRUG_RAG_UPLOAD_DIR` | 上传文件存储目录 |

## License

MIT
