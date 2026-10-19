# SmartCare 知识助手总体架构

## 1. 目标架构

```text
admin-web
    |
    v
Spring Boot /api/knowledge/*
    |
    v
drug-rag FastAPI
    |
    +-- Domain Router
    +-- Domain Registry
    +-- Retrieval Pipeline
    +-- Catalog / Chroma
    +-- Embedding / Rerank / LLM
```

浏览器不直接访问 FastAPI。Spring Boot 负责对外接口、参数校验、服务代理、错误转换和 requestId，并使用 `AjaxResult.data` 输出公共响应；`drug-rag` 负责知识解析、索引、检索、证据筛选和受控问答。完整检索 Trace 通过后续管理接口按 requestId 查询，不作为普通助手响应的一部分。知识服务不读取或修改预约、患者、排班和支付业务数据。

## 2. 知识域边界

首期规划：`drug` 药品说明书、`hospital_policy` 医院制度与业务流程、`care_instruction` 检查检验与就诊注意事项、`health_education` 健康宣教。

暂不纳入：医保、系统手册、设备，以及基于患者个人数据的诊疗建议。

## 3. RAG 原则

- 保留现有药品 RAG 能力，采用增量改造；
- 共享通用模型、检索流水线和来源协议；
- 各知识域独立配置解析器、元数据、Prompt、安全规则和索引集合；
- 使用 Catalog 管理活动索引版本，先构建验证，再切换 active；
- 无充分证据时拒答或要求澄清；
- 当前会话只保存在前端内存，不建设长期记忆和复杂会话管理。

## 4. 部署边界

`knowledge-assistant` 作为 Compose 内部服务运行，不直接暴露公网端口。服务间通过 `KNOWLEDGE_INTERNAL_TOKEN` 进行调用隔离；模型密钥只存在服务端环境变量中。

详细需求以[医院多知识域助手接入 PRD](../deploy/docs/医院药品知识库助手接入PRD.md)为准。
