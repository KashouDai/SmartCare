# 医院多知识域助手接入 PRD

## 1. 文档信息

| 项目 | 内容 |
|---|---|
| 文档名称 | 医院多知识域助手接入 PRD |
| 版本 | V1.2 |
| 日期 | 2026-10-18 |
| 适用系统 | SmartCare 医院信息管理系统 |
| 关联服务 | `drug-rag` RAG 服务及其后续知识域扩展 |
| 当前状态 | 方案设计，尚未完成生产运行验证 |

## 2. 项目背景

医院管理端工作人员在药品管理、日常业务和医疗服务处理中，需要快速查询药品说明书、医院制度、检查注意事项和健康宣教等资料。

现有 `drug-rag` 已具备药品说明书解析、向量检索、元数据过滤、重排、安全评估和受控问答能力。本项目计划以药品说明书为第一个知识域，逐步扩展为面向医院内部资料的多知识域助手。

本项目的定位是“医院内部多知识域助手”，不是面向患者的诊疗系统，也不建设生产级通用企业知识库。

## 3. 项目目标

### 3.1 总体目标

1. 在 `admin-web` 中提供全局知识助手入口。
2. 支持药品说明书、医院制度与业务流程、检查检验与就诊注意事项、健康宣教四类知识内容。
3. 回答同时展示来源信息和安全提示。
4. 通过 Spring Boot 统一处理接口代理、日志、异常和返回结构。
5. 通过 Docker Compose 部署独立的知识服务。
6. 确保浏览器不直接访问 FastAPI，RAG 服务不直接访问医院业务数据。

### 3.2 MVP 范围

MVP 优先完成药品领域闭环：独立服务、Spring Boot 代理、管理端知识助手、文档管理、索引状态、来源展示和药品领域回归测试。医院制度、检查注意事项和健康宣教在共享 RAG 核心稳定后接入。

### 3.3 非目标

当前阶段不实现以下内容：

- 患者端公开问答入口；
- 基于患者病历、处方或个人信息的个性化诊疗建议；
- 自动修改预约、排班、患者、支付或处方数据；
- 医保、系统手册和设备知识域；
- 临床诊断、检验结果个体化解读和治疗方案推荐；
- 长期记忆、用户画像和后端持久化会话；
- 患者端或公开入口上传文档；
- 模型自动执行业务操作。

## 4. 用户与使用场景

### 4.1 用户角色

| 角色 | 使用范围 |
|---|---|
| 管理端用户 | 使用知识助手，查看问答来源和安全提示；本项目暂不设计细粒度权限 |
| 患者 | 一期不可使用 |

### 4.2 典型场景

- 查询“某药品怎么服用”；
- 查询“某药品有哪些禁忌”；
- 查询“某药品有哪些不良反应”；
- 查询“孕妇、儿童或肝肾功能异常人群是否需要特别注意”；
- 查询“药品应如何贮藏”。

当问题缺少明确药品名称时，助手应要求用户补充药品信息，而不是直接猜测。

## 5. 总体架构

```text
管理员浏览器
      |
      v
admin-web（Vue 2 + Element UI）
      |
      | /api/knowledge/*
      v
Nginx Gateway
      |
      v
Spring Boot Backend
      |  接口代理、日志、错误映射
      |  x-internal-token
      v
drug-rag FastAPI 服务
      |
      +--> 药品目录与处理后文档
      +--> Chroma 向量索引
      +--> DashScope Embedding / Rerank / LLM

Spring Boot 仍负责：MySQL、Redis、预约、患者、排班和支付等业务。
drug-rag 不读写上述业务数据。
```

### 5.1 服务职责

#### admin-web

- 展示知识助手入口和右侧抽屉；
- 输入问题并提交请求；
- 展示回答、来源和安全提示；
- 展示服务不可用、超时和请求失败等状态；
- 仅传递白名单页面上下文，不传递患者和病历数据。

#### Spring Boot

- 接收管理端请求并完成基础参数校验；
- 对外提供 `/api/knowledge/*` 接口；
- 调用内部 `drug-rag` 服务；
- 注入内部服务令牌；
- 统一处理超时、异常、日志和返回结构；
- 不把 RAG 建议直接转换为业务写操作。

#### Knowledge Assistant / drug-rag

- 按知识域选择对应的解析器、索引和检索策略；
- 药品域解析药品名、通用名和药品分类；
- 进行意图路由、问题改写、元数据过滤和混合检索；
- 对召回内容重排并筛选证据；
- 执行安全风险评估；
- 基于说明书证据生成受控回答；
- 返回回答、来源、检索 Trace 和安全评估。

#### Chroma 与模型服务

- Chroma 按知识域分别保存原文 chunk 和摘要索引；
- Embedding 用于向量检索；
- Rerank 用于召回结果重排；
- LLM 仅基于检索证据生成回答；
- `DASHSCOPE_API_KEY` 只允许在服务端配置。

## 6. 核心业务流程

```text
1. 管理员在管理端打开知识助手
2. 输入问题
3. admin-web 调用 Spring Boot /api/knowledge/ask
4. Spring Boot 处理请求校验和接口代理
5. Spring Boot 调用 drug-rag /api/ask
6. 知识服务识别知识域和问题意图
7. 知识服务查询对应 Chroma 集合并筛选证据
8. 知识服务执行领域安全规则并生成回答
9. Spring Boot 转换响应并记录调用结果
10. admin-web 展示回答、来源和安全提示
```

## 7. 功能需求

### 7.1 知识助手入口

- 入口位于管理端顶部导航栏；
- 点击后打开右侧抽屉；
- 抽屉打开后不影响当前业务页面；
- 桌面端建议宽度约 480px；
- 小屏设备使用全宽布局；
- 患者端不展示该入口。

### 7.2 知识域范围

一期及近期规划包含以下四个知识域：

| 知识域 | 标识 | 主要内容 |
|---|---|---|
| 药品说明书 | `drug` | 适应症、用法用量、禁忌、不良反应、特殊人群用药、贮藏 |
| 医院制度与业务流程 | `hospital_policy` | 入院、出院、转诊、会诊、退费、值班、药品管理等制度和流程 |
| 检查检验与就诊注意事项 | `care_instruction` | 抽血、超声、CT、MRI、胃肠镜及检查前后的准备事项 |
| 健康宣教 | `health_education` | 慢病教育、出院指导、饮食指导、康复和居家护理知识 |

以下领域暂不纳入本项目：医保、系统手册、设备、临床诊断、检验结果个体化解读和治疗方案推荐。

护理规范、感染防控、医疗文书书写规范可作为后续候选知识域，但不纳入当前实现和验收。

不同知识域应使用独立索引和领域策略，避免将所有内容直接混入药品集合。

### 7.3 RAG 架构改造

#### 7.3.1 改造原则

现有药品 RAG 不推倒重写。药品问答作为 `drug` 领域实现保留，在其外部增加通用模型、领域路由、领域注册、独立索引和统一响应结构，逐步形成“共享 RAG 核心 + 领域插件”的架构。

每一步改造都必须先保证现有药品问答回归通过，再接入新的知识域。

#### 7.3.2 保留能力

以下现有能力继续复用：

- FastAPI 接口框架；
- Chroma 向量存储；
- Embedding、Rerank 和 LLM 调用；
- 原文与摘要双索引；
- 证据长度控制；
- 来源引用和检索 Trace；
- Catalog 版本模型；
- 内部服务令牌；
- 健康和就绪检查。

#### 7.3.3 需要改造的能力

- 将药品专用数据模型扩展为通用知识文档和 Chunk 模型；
- 在药品意图路由之前增加一级领域路由；
- 按知识域加载独立的 Chroma 集合；
- 增加通用 PDF、DOCX、TXT 和 Markdown 解析器；
- 将 metadata filter、Prompt、安全规则和检索参数改为领域配置；
- 使用统一请求和响应协议，避免前端依赖药品专属字段；
- 使 Catalog 的活动版本真正控制运行时集合选择。

#### 7.3.4 通用数据模型

新增通用模型，但暂不删除现有 `DrugDocument` 和 `DrugChunk`：

```text
KnowledgeDocument
├─ doc_id
├─ domain
├─ title
├─ version
├─ source_type
└─ metadata

KnowledgeChunk
├─ chunk_id
├─ doc_id
├─ domain
├─ text
├─ section
├─ source
├─ chunk_index
└─ metadata
```

药品名、通用名、制度类型、检查类型等领域专属字段存放在 `metadata` 中。药品解析阶段仍可使用强类型药品模型，进入共享索引层时再转换为通用模型。

#### 7.3.5 两级路由

```text
用户问题
    |
    v
Domain Router
    ├─ drug
    ├─ hospital_policy
    ├─ care_instruction
    └─ health_education
    |
    v
领域内部 Intent Router
    |
    v
对应领域检索策略
```

领域选择优先级为：前端显式选择、页面白名单上下文、问题自动分类。自动分类仍不明确时，应要求用户选择知识域，而不是同时检索全部集合。

#### 7.3.6 独立索引与版本

各领域分别维护原文和摘要集合，例如：

```text
drug_chunks_v1
drug_summaries_v1
hospital_policy_chunks_v1
hospital_policy_summaries_v1
care_instruction_chunks_v1
care_instruction_summaries_v1
health_education_chunks_v1
health_education_summaries_v1
```

Catalog 负责维护 `domain → active version → collection`。新版本先构建并验证，再切换活动版本；旧版本保留用于回退，不在现有活动集合上直接覆盖更新。

#### 7.3.7 解析器与领域策略

```text
Parser Registry
├─ drug → DrugInstructionParser
└─ 其他领域 → GenericDocumentParser
```

通用解析器负责保留文档名、标题层级、章节和页码，并对过长章节继续切分。医院制度、检查注意事项和健康宣教初期共用通用解析器，不为每个领域重复实现解析流程。

每个领域独立维护元数据字段、路由规则、Prompt、安全提示和评测问题。共享核心只负责召回、去重、重排、证据筛选、引用和 Trace。

#### 7.3.8 推荐代码结构

```text
src/drug_rag/
├─ api/
├─ core/
│  ├─ models.py
│  ├─ pipeline.py
│  ├─ retrieval.py
│  ├─ reranker.py
│  └─ citations.py
├─ domains/
│  ├─ base.py
│  ├─ registry.py
│  ├─ drug/
│  ├─ hospital_policy/
│  ├─ care_instruction/
│  └─ health_education/
├─ ingestion/
│  ├─ generic_parser.py
│  ├─ chunker.py
│  └─ indexer.py
└─ catalog/
```

当前阶段不强制将根包 `drug_rag` 重命名为 `knowledge_assistant`，避免产生与功能无关的大范围修改；功能稳定后再单独评估命名迁移。

### 7.4 问答功能

输入要求：

- 问题不能为空；
- 问题长度由后端限制，建议不超过 1000 字符；
- 支持药品名称、制度事项、检查事项、健康宣教主题和自然语言问题；
- 不支持上传患者隐私信息作为问答上下文。

输出内容：

- 回答正文；
- 命中的知识域和主题；
- 来源文档、章节和页码；
- 安全等级；
- 风险提示和就医/咨询建议；
- 无充分证据时的明确说明。

### 7.5 追问与不确定回答

当问题无法确定知识域或必要实体时，系统应返回澄清提示，例如“请选择知识类型”或“请补充具体药品名称”。

当检索不到足够证据时，系统不得使用模型常识补充答案，应返回证据不足提示。

### 7.6 业务跳转

如果回答需要引导用户办理业务，只返回白名单跳转信息，例如：

```json
{
  "type": "business_redirect",
  "route": "/info/drug",
  "label": "前往药品管理"
}
```

跳转由用户点击触发，知识助手不直接执行任何预约、排班、支付或患者数据写操作。

### 7.7 文档上传

文档上传作为“文档管理”页面的基础能力，支持管理端用户上传 PDF、DOCX、TXT 和 Markdown 文档。`drug-rag` 已有 `/api/upload` 能力，接入时需要补充文件校验、索引任务状态和失败重试；不提供患者端或独立公开上传入口。

### 7.8 前端管理菜单

管理端左侧新增“知识库管理”菜单，当前项目不设计细粒度菜单权限，管理端用户直接可见。

```text
知识库管理
├─ 文档管理       /knowledge/documents
├─ 索引任务       /knowledge/tasks
├─ 问答记录       /knowledge/traces
└─ 效果评测       /knowledge/evaluations
```

顶部导航保留“知识助手”入口。它用于即时问答，不替代知识库管理菜单。

#### 7.8.1 文档管理

文档管理页展示文档总数、已发布数量、处理中数量和失败数量，并提供知识域、状态、格式和关键词筛选。

表格字段包括文档名称、知识域、版本、文件类型、Chunk 数量、状态、更新时间和操作。支持查看详情、重新索引和删除。

上传弹窗包含文档标题、知识域、版本和文件。根据知识域显示少量固定元数据：

- `drug`：药品名称、通用名称、药品分类；
- `hospital_policy`：制度类型、适用部门、生效日期；
- `care_instruction`：检查名称、检查类型、适用人群；
- `health_education`：宣教主题、适用人群、内容分类。

一期不提供在线编辑文档内容，内容变更通过重新上传完成。

#### 7.8.2 索引任务

索引任务页展示文档解析、文本分块、Embedding、写入 Chroma、更新 Catalog 等处理阶段，支持查看任务详情和重试失败任务。

```text
等待处理 → 文档解析 → 文本分块 → Embedding → 写入 Chroma → 更新 Catalog → 完成
```

一期不设计任务优先级、复杂调度和分布式队列。

#### 7.8.3 问答记录

问答记录页展示问题、知识域、检索策略、来源数量、处理状态、响应耗时和 requestId。详情页展示领域路由、问题改写、Metadata Filter、召回文档、Rerank 顺序、最终来源、回答和安全提示。

问答记录用于 RAG Trace 排查，不等同于长期会话历史。

#### 7.8.4 效果评测

效果评测页使用固定问题集验证领域路由和来源命中情况，展示来源命中率、无证据问题拒答率、领域路由正确率、平均响应时间和失败问题数量。

一期不把向量相似度直接当作回答正确率。

### 7.9 知识助手前端交互

知识助手使用全局右侧抽屉，挂载在管理端全局布局，入口位于顶部导航栏。桌面端建议宽度约 480px，小屏设备使用全宽布局。

```text
┌─────────────────────────────────────┐
│ 医院知识助手             [新建] [关闭] │
├─────────────────────────────────────┤
│ 知识域：[自动识别] [药品] [制度]      │
│         [检查] [宣教]                 │
├─────────────────────────────────────┤
│ 对话消息区                           │
│ 用户问题                             │
│ 助手回答                             │
│ 来源依据（可折叠）                   │
│ 安全提示                             │
├─────────────────────────────────────┤
│ 请输入问题……                    [发送] │
└─────────────────────────────────────┘
```

支持初始状态、加载状态、知识域不明确、证据不足、服务异常和请求超时等状态。回答必须区分正文、来源和安全提示。

“新建”按钮只清空当前前端消息和临时上下文，重新开始问答，不创建后端会话。

### 7.10 临时会话边界

支持当前抽屉内的多轮上下文，方便用户连续追问，但不做长期记忆和复杂会话管理。

当前会话只保存在前端内存中，必要时可以使用 `sessionId` 标识本次请求，但不在 MySQL 中保存会话表，不做历史会话搜索、归档、重命名、恢复、跨设备同步或用户画像。

会话上下文不得绑定患者、病历、处方或支付数据。点击“新建”或关闭页面后，当前临时上下文可以被清除。

## 8. 接口设计

### 8.1 管理端对外接口

#### `POST /api/knowledge/ask`

请求示例：

```json
{
  "question": "坎地沙坦酯片有哪些禁忌？",
  "domain": "drug",
  "filters": {
    "drug_name": "坎地沙坦酯片"
  },
  "context": {
    "module": "drug",
    "route": "/info/drug"
  }
}
```

响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "answer": "...",
    "domain": "drug",
    "sources": [
      {
        "domain": "drug",
        "docId": "drug-candesartan-001",
        "title": "坎地沙坦酯片说明书",
        "version": "1.0",
        "section": "禁忌",
        "page": 2,
        "content": "..."
      }
    ],
    "safety": {
      "riskLevel": "high",
      "warnings": ["..."],
      "medicalDisclaimer": true
    },
    "requestId": "..."
  }
}
```

前端不需要直接依赖 Python 服务返回的全部字段。`trace` 可在后续管理员排障页面中单独展示。

`domain` 可以由前端显式指定；未指定时由知识服务执行领域路由。`filters` 使用通用对象承载各领域特有条件，统一接口不再增加 `drug_name`、`policy_type` 等领域专属顶层字段。

#### `GET /api/knowledge/health`

用于管理端或运维侧查看代理服务状态。该接口不应泄露 API Key、内部地址或内部令牌。

#### 知识库管理接口

管理端页面通过 Spring Boot 代理调用以下接口：

```text
GET    /api/knowledge/documents
POST   /api/knowledge/documents/upload
GET    /api/knowledge/documents/{docId}
DELETE /api/knowledge/documents/{docId}
POST   /api/knowledge/documents/{docId}/reindex

GET    /api/knowledge/tasks
GET    /api/knowledge/tasks/{taskId}
POST   /api/knowledge/tasks/{taskId}/retry

GET    /api/knowledge/traces
GET    /api/knowledge/traces/{requestId}

GET    /api/knowledge/evaluations
POST   /api/knowledge/evaluations/run
GET    /api/knowledge/evaluations/{runId}
```

这些接口只用于当前项目的管理端演示和验证，后端实现可以先使用简单的文件、Catalog 或已有运行数据，不要求建设生产级任务平台。

### 8.2 Spring Boot 到 drug-rag 的内部接口

一期调用：

```text
POST http://knowledge-assistant:8000/api/ask
GET  http://knowledge-assistant:8000/api/ready
```

请求必须携带：

```text
x-internal-token: ${KNOWLEDGE_INTERNAL_TOKEN}
```

`drug-rag` 的 `/api/health` 和 `/api/ready` 用于探活时可免内部令牌；其他 `/api/*` 接口必须校验内部令牌。

## 9. 数据边界与基础安全

1. 浏览器不得直接访问 FastAPI 服务。
2. FastAPI 必须校验 `KNOWLEDGE_INTERNAL_TOKEN`，用于服务间隔离。
3. `DASHSCOPE_API_KEY` 只能注入后端或知识服务容器环境变量。
4. 页面上下文采用白名单字段，不传患者、病历、处方和身份证信息。
5. 问答日志不得默认保存完整患者相关文本。
6. 医疗相关回答必须明确“仅供信息参考，具体情况请咨询医生或药师”等安全提示。
7. 助手不得直接修改任何医院业务数据。
8. API 超时、模型失败和索引不可用时，返回可识别的错误状态，不暴露内部堆栈。

本项目不设计生产级用户权限、知识域权限、文档级 ACL、部门权限或合规审计。

## 10. Docker Compose 部署要求

在现有 Compose 中新增 `knowledge-assistant` 服务：

- 构建文件使用 `drug-rag/Dockerfile`；
- 容器监听 `0.0.0.0:8000`；
- 不直接映射到宿主机公网端口；
- 仅通过 Compose 内部网络供 Spring Boot 调用；
- 增加 `/api/ready` 健康检查；
- 注入 `DASHSCOPE_API_KEY` 和 `KNOWLEDGE_INTERNAL_TOKEN`；
- 挂载 Chroma、处理后文档和上传文档数据卷；
- `backend` 在 RAG 服务就绪后再开放知识助手功能。

生成数据不提交 Git：

```text
drug-rag/data/chroma/
drug-rag/data/processed/
drug-rag/data/uploads/
```

索引初始化和重建应作为独立运维步骤，不在每次容器启动时自动执行。

## 11. 配置项

建议配置：

```text
KNOWLEDGE_BASE_URL=http://knowledge-assistant:8000
KNOWLEDGE_INTERNAL_TOKEN=<随机生成的内部令牌>
DASHSCOPE_API_KEY=<服务端配置>
```

`drug-rag` 的模型、Embedding、Rerank 和 Prompt 继续由其自身配置文件管理。配置变更需要重新执行就绪检查和问答验收。

## 12. 异常与降级

| 异常 | 系统行为 |
|---|---|
| 请求参数不合法 | 返回参数错误提示 |
| RAG 服务未启动 | 返回知识服务暂不可用 |
| RAG 服务未就绪 | 不允许进入正常问答，返回服务未就绪 |
| 请求超时 | 返回超时提示，并记录 requestId |
| 未检索到证据 | 返回证据不足或澄清提示 |
| LLM 不可用 | 使用 `drug-rag` 的模板兜底能力 |
| 文档来源缺失 | 不展示无来源的确定性医学结论 |
| 内部令牌错误 | FastAPI 返回 401，Spring Boot 记录配置错误 |

## 13. 验收标准

### 13.1 功能验收

- 管理员登录后可以从顶部导航打开知识助手；
- 未登录用户无法调用 `/api/knowledge/ask`；
- 患者端不存在知识助手入口；
- 输入明确药品问题可以返回回答；
- 回答展示至少一个可追溯来源，或明确提示没有足够证据；
- 禁忌、不良反应、孕妇、儿童等高风险问题展示安全提示；
- 未指明药品时返回澄清提示；
- RAG 服务停止后，前端显示可理解的错误信息；
- 知识助手不会修改预约、患者、排班和支付数据。
- 管理端左侧可以进入文档管理、索引任务、问答记录和效果评测；
- 文档管理可以按知识域筛选并查看索引状态；
- 索引任务可以展示处理阶段，失败任务可以重试；
- 问答记录可以查看来源和检索 Trace；
- 知识助手支持当前抽屉内连续追问和“新建”临时会话；
- 新建会话后不会保留上一轮消息，关闭页面后不产生长期记忆。

### 13.2 接口验收

- Spring Boot 能通过 Compose 服务名访问 `knowledge-assistant:8000`；
- 直接从浏览器访问 FastAPI 不在部署网络中开放；
- 缺少 `KNOWLEDGE_INTERNAL_TOKEN` 时，业务接口不会静默放行；
- `/api/ready` 能识别配置、Catalog 或 Chroma 不可用状态；
- 内部异常不会向前端返回 API Key、内部路径或 Python 堆栈。

### 13.3 部署验收

- `mysql`、`redis`、`backend`、`admin-web`、`site-web`、`gateway` 和 `knowledge-assistant` 均可启动；
- 重启容器后索引数据仍然存在；
- 生成索引和上传文档不进入 Git；
- 没有把真实密钥写入 Compose、源码或文档。

## 14. 实施阶段

### 阶段一：服务接入

- 增加 Compose 服务；
- 配置内部地址和令牌；
- 验证 `/api/ready` 和 `/api/ask`；
- 记录服务超时和错误映射。

### 阶段二：共享知识服务核心

- 为现有药品问答建立回归用例；
- 增加通用文档、Chunk、来源和领域模型；
- 接入 Catalog，使活动版本决定运行时集合；
- 增加 Domain Registry、领域路由和统一响应结构；
- 将现有药品逻辑注册为 `drug` 领域；
- 增加通用文档解析器和通用索引流程；
- 增加操作日志和 requestId；
- 编写接口参数和异常测试。

### 阶段三：管理端入口

- 在全局布局挂载助手抽屉；
- 在顶部导航增加入口；
- 展示回答、来源和安全提示；
- 增加知识库管理菜单及文档管理页面；
- 增加索引任务、问答记录和效果评测页面骨架；
- 完成桌面端和小屏布局适配。

### 阶段四：新增知识域

- 接入 `hospital_policy`，验证通用解析、独立集合和流程型问答；
- 接入 `care_instruction`，验证检查事项识别和注意事项检索；
- 接入 `health_education`，验证健康宣教回答边界；
- 为每个知识域准备独立的测试问题和预期来源；
- 每接入一个领域都重新执行药品领域回归测试。

### 阶段五：端到端验收

- 验证管理端入口和问答链路；
- 验证 Chroma 数据持久化；
- 验证高风险问题和无证据问题；
- 验证 RAG 服务不可用时的降级；
- 验证业务数据库没有被知识助手修改。

### 阶段六：后续扩展评估

- 护理规范、感染防控、医疗文书等候选知识域；
- 更复杂的知识库运营能力。

## 15. 后续多知识域扩展

本项目规划的知识域按领域隔离数据和检索策略：

```text
Knowledge Assistant
├─ drug                药品说明书
├─ hospital_policy     医院制度与业务流程
├─ care_instruction    检查检验与就诊注意事项
└─ health_education    健康宣教
```

每个知识域需要独立评估文档来源、索引、Prompt、召回策略和验收数据集。不能仅把其他文档直接混入现有药品索引后就宣称已经具备多知识域能力。

## 16. 当前限制

本文档描述的是接入设计和一期验收边界。以下事项在正式上线前仍需现场验证：

- Docker Compose 实际构建和启动；
- Chroma 索引数据挂载与重启持久化；
- DashScope 网络连通性和模型响应；
- Spring Boot 到 FastAPI 的真实请求超时行为；
- 管理端实际菜单和路由配置；
- 多管理员并发操作和大规模索引重建。

## 17. 开发范围与里程碑

### 17.1 MVP：药品域闭环

MVP 先完成现有药品问答的可用闭环：

- `drug-rag` 独立容器；
- Spring Boot `/api/knowledge/*` 代理；
- 管理端知识助手抽屉；
- 药品问答、来源和安全提示；
- 文档管理、上传和索引状态；
- 当前抽屉内临时多轮问答；
- 药品领域回归测试。

MVP 的基本验收标准是：现有药品问答不退化，浏览器不直接访问 FastAPI，助手不修改医院业务数据。

### 17.2 V1：多知识域核心

在 MVP 稳定后，补充通用文档模型、领域路由、Catalog V2、独立领域集合和通用解析器，再接入 `hospital_policy`、`care_instruction` 和 `health_education`。

### 17.3 V2：管理与评测

补充索引任务详情、问答 Trace、效果评测、版本切换和版本回退能力。

## 18. 知识数据清单

每个知识域在开发前至少准备 3～5 份可用于本地验证的文档，并建立清单：

```text
文档名称
文件路径
文件格式
知识域
文档版本
预期章节
验收问题
预期来源
```

推荐目录：

```text
drug-rag/data/raw/
├─ drug/
├─ hospital_policy/
├─ care_instruction/
└─ health_education/
```

每批文档配套一个 `manifest.yaml`，用于记录领域、标题、版本和领域元数据。生成的 Chroma、处理后 JSONL 和上传临时文件不进入 Git。

## 19. Catalog V2 数据结构

一个领域版本需要同时记录原文集合和摘要集合：

```json
{
  "schema_version": 2,
  "revision": 3,
  "domains": {
    "drug": {
      "active": "drug-20261018-v1",
      "retained": ["drug-20261001-v1"]
    }
  },
  "versions": {
    "drug-20261018-v1": {
      "domain": "drug",
      "status": "published",
      "collections": {
        "chunks": "drug_chunks_20261018_v1",
        "summaries": "drug_summaries_20261018_v1"
      },
      "embedding_model": "text-embedding-v3",
      "document_count": 10,
      "chunk_count": 326,
      "created_at": "2026-10-18T10:00:00+08:00",
      "published_at": "2026-10-18T10:10:00+08:00"
    }
  }
}
```

版本状态固定为：

```text
indexing → published
         ↘ failed

published → offline
```

新版本必须先构建和验证，再切换领域的 `active` 指针。旧版本保留用于回退，不在活动集合上直接覆盖更新。

## 20. 文档与索引生命周期

上传、删除和重新索引统一采用生成新版本的方式：

```text
上传 / 删除 / 重新索引
        ↓
创建 indexing 版本
        ↓
构建新的 Chroma 集合
        ↓
验证成功
        ↓
切换 Catalog active
```

删除采用逻辑删除：文档从新版本中移除，原文件移动到 `data/archive/`，旧索引版本保留。重新索引使用同一文档生成新版本，不修改当前活动集合。

同一知识域同时只允许一个索引任务。检测到已有任务时返回 `TASK_CONFLICT`。

## 21. 管理数据持久化

本项目不将知识库管理数据写入医院业务 MySQL，也不引入生产级任务队列。FastAPI 使用独立 SQLite 保存管理元数据：

```text
drug-rag/data/management/knowledge.db
```

建议表：

```text
documents
index_tasks
query_traces
evaluation_runs
```

数据职责如下：

| 数据 | 保存位置 |
|---|---|
| 原始文档 | `data/uploads/` |
| 已归档文档 | `data/archive/` |
| 文档和任务元数据 | SQLite |
| 活动索引版本 | Catalog JSON |
| 向量和摘要 | Chroma |
| 模型和领域配置 | YAML |
| 临时会话 | 前端内存，不持久化 |

索引任务使用 FastAPI 后台任务执行。服务重启时，未完成的 `running` 任务标记为 `failed`，由用户手动重试。

## 22. 统一状态与错误码

文档状态：

```text
uploaded
indexing
published
failed
deleted
```

任务阶段：

```text
pending
parsing
chunking
embedding
indexing
publishing
succeeded
failed
```

问答状态：

```text
succeeded
no_evidence
need_clarification
failed
```

统一错误码：

```text
DOMAIN_UNRESOLVED
INVALID_FILTER
NO_EVIDENCE
INDEX_NOT_READY
FILE_UNSUPPORTED
FILE_TOO_LARGE
TASK_CONFLICT
TASK_FAILED
KNOWLEDGE_SERVICE_UNAVAILABLE
KNOWLEDGE_SERVICE_TIMEOUT
```

## 23. 完整接口契约

问答请求统一使用：

```json
{
  "question": "做胃镜前可以喝水吗？",
  "domain": "care_instruction",
  "filters": {},
  "history": [
    {
      "role": "user",
      "content": "我要做胃镜检查"
    }
  ],
  "context": {
    "module": "outpatient",
    "route": "/outpatient/check"
  }
}
```

约束：问题长度为 1～1000 字符；历史最多保留最近 6 条消息，总长度不超过 6000 字符；历史消息只用于理解追问，不能作为回答证据。

管理列表接口沿用 RuoYi 风格：

```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 126,
  "rows": []
}
```

统一分页参数为 `pageNum`、`pageSize`、`orderByColumn` 和 `isAsc`，`pageSize` 最大值为 100。

超时建议为：连接 FastAPI 3 秒、普通管理接口 30 秒、问答接口 90 秒；上传和索引接口立即返回 `taskId`，由后台任务继续处理。

一期使用普通 HTTP 请求，不接入 Token 级流式输出。当前 `/api/ask/stream` 只返回生命周期事件，尚未提供真正的逐 Token 输出。

## 24. 领域元数据 Schema

所有 Chunk 必须包含：

```text
chunk_id
doc_id
domain
title
version
section
source_file
page_start
page_end
chunk_index
content_hash
```

领域字段如下：

| 领域 | 领域字段 |
|---|---|
| `drug` | `drug_name`、`generic_name`、`category`、`approval_number` |
| `hospital_policy` | `policy_type`、`department`、`effective_date` |
| `care_instruction` | `examination_name`、`examination_type`、`stage`、`target_population` |
| `health_education` | `topic`、`content_category`、`target_population` |

`care_instruction.stage` 固定为 `before`、`during`、`after` 或 `general`。领域字段缺失时允许为空，但 `domain`、`title`、`section` 和来源字段必须存在。

## 25. 临时上下文规则

当前抽屉支持连续追问，但不做长期记忆和复杂会话管理。

- `sessionId` 仅作为前端临时标识；
- 当前会话只保存在前端内存；
- 点击“新建”后清空消息和临时上下文；
- 关闭页面后可以清除当前上下文；
- 不建立后端会话表；
- 不做历史搜索、归档、重命名、恢复和跨设备同步；
- 不绑定患者、病历、处方或支付数据。

## 26. 评测数据与验收阈值

每个知识域准备 10 条固定评测问题，记录：

```text
question
expected_domain
expected_document
expected_section
should_answer
```

初始目标为：

```text
领域路由正确率 ≥ 90%
来源 Hit@5 ≥ 80%
无证据问题拒答率 ≥ 80%
有答案的问题必须展示来源
```

上述是开发目标，不代表当前系统已经达到这些结果。首次评测后根据实际数据调整。

## 27. 开发完成标准

满足以下条件后，才认为本阶段开发完成：

- 药品领域原有问答回归通过；
- Spring Boot 可以代理问答和管理接口；
- 浏览器不会直接访问 FastAPI；
- 四个知识域可以独立选择或自动路由；
- 文档上传、索引任务和 Catalog 发布流程可运行；
- 失败任务可以重试；
- 回答展示来源和安全提示；
- 无证据问题不会被模型强行补充；
- 管理端菜单和知识助手抽屉可正常使用；
- 生成索引、上传文件和真实密钥不进入 Git；
- 业务数据库中的预约、患者、排班和支付数据未被修改。
