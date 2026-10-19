# 阶段 1：知识模型与领域注册实施记录

## 目标

在不改变现有药品问答 API 和检索行为的前提下，为后续多知识域扩展建立最小共享模型和领域注册边界。

## 本次实现

- 新增 `KnowledgeDocument`、`KnowledgeChunk` 和 `KnowledgeSource`；
- 增加药品模型到通用模型的显式转换方法；
- 新增 `DomainRegistry` 和未知领域异常；
- 默认只注册 `drug` 领域；
- 增加模型转换和领域注册单元测试。

## 本次补充

- 在保留原有字段的基础上，统一问答结果增加 `domain`、`status` 和 `requestId`；
- 澄清结果使用 `need_clarification`，无证据结果使用 `no_evidence`。
- 增加 FastAPI 接口级测试，覆盖令牌校验、健康检查、就绪检查和问答响应。
- 完成 Spring Boot 代理层协议审计，确定使用 `AjaxResult.data` 输出公共响应，完整 Trace 延后到管理查询接口。

## 明确未实现

- 未修改 `/api/ask` 请求参数；响应仅向后兼容地增加通用字段；
- 未修改检索、解析、索引和 Catalog 运行逻辑；
- 未接入 `hospital_policy`、`care_instruction`、`health_education`；
- 未接入 Spring Boot、Compose 和前端；
- 未执行依赖 DashScope、Chroma 真实数据的在线检查。

## 验证结果

执行目录：`drug-rag/`

```text
python -m compileall -q src       通过
python -m unittest discover -s tests -v  9 个测试通过
python -m drug_rag.cli --help     通过
```

## 关联提交

```text
6d2dba5 feat(rag): add shared knowledge domain models
a27f1a5 feat(api): add shared answer response fields
64c842e test(api): cover knowledge api contract
```

## 当前状态

阶段 1 仍在进行中。接口契约验证和 Spring Boot 协议审计已完成，下一步进入阶段 2，处理通用文档解析、切片、独立索引和 Catalog 版本管理。
