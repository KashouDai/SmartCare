# `drug_rag` 包开发指南

本文件适用于 `src/drug_rag/` 及其子目录，并继承仓库根目录和 `src/AGENTS.md` 的规则。

## 调用链与依赖方向

保持以下主调用链清晰：

```text
loaders → parsers → pipelines/indexing
                         ↓
intent_router → query_rewriter → retrieval_policy → advanced_retriever → reranker
                                                            ↓
                                                    chains → API/CLI
```

- `schemas`、`config.py`、`io_utils.py` 是基础模块，不应反向依赖 API、CLI、问答链或检索编排。
- `embeddings.py` 和 `llms.py` 封装外部模型访问；调用方通过构建函数和配置使用它们，不要分散实现 HTTP 客户端。
- `api/main.py` 与 `cli.py` 可以编排其他模块，但领域模块不得导入入口层。
- 避免新增循环依赖。若两个模块需要共享结构，将结构放入 `schemas` 或最接近其领域归属的基础模块。

## 公共接口

- 子包对外接口集中在各自的 `__init__.py` 中。只有确实需要供其他模块调用的符号才加入 `__all__`。
- 修改 `DrugChunk`、`DrugDocument`、`SourceSpan`、`QueryRoute`、`QueryRewrite`、`RetrievalDecision`、检索 trace 或 `AnswerResult` 时，检查所有构造位置及 `to_dict()` 输出。
- API 和 CLI 应调用同一套解析、索引、检索和回答函数；不要为某个入口复制一份行为不同的实现。
- 保持配置默认值与 `configs/*.yaml` 一致。新增配置项时，明确缺省行为，并确保缺少可选配置不会破坏规则兜底。

## RAG 行为约束

- 文档解析结果必须保留来源文件、页码、章节、药品名、通用名、分类和稳定标识符。
- metadata 过滤用于药品和章节隔离。触发无过滤兜底时必须在 trace 中标明，并在回答层重新核对药品身份；证据仍不匹配时应追问或说明证据不足。
- 重排只能调整候选证据顺序，不能制造、修改或补充文档事实。
- LLM 意图分类、模型重排和答案生成均属于可选增强；失败时必须保留规则路由、关键词重排或抽取式答案等现有兜底路径。
- 最终回答必须经过安全评估，并保留来源信息。涉及多种药品时应分组呈现，避免把一种药品的说明应用到另一种药品。

## 状态与持久化

- 写入 JSONL、药品目录或 Chroma 前，先确认目标路径属于本次任务；测试应使用独立临时目录。
- 更新已入库文档时，保持“识别旧版本 → 删除旧记录/向量 → 写入新记录/向量 → 重建药品目录 → 清理缓存”的一致性。
- 修改 `get_settings()`、`get_runtime()` 或 `get_drug_directory()` 相关行为时，检查 `lru_cache` 生命周期，避免上传后继续使用旧索引或旧目录。
- 不把真实文档内容、模型响应、密钥或完整用户问题写入新增日志，除非任务明确要求且数据可安全保存。

## 最小验证矩阵

- 基础模块或 schema：执行 `python -m compileall -q src`，并检查相关序列化输出。
- 解析器：验证章节、页码、chunk 边界和稳定标识符。
- 路由/检索：验证意图、改写、metadata filter、策略、阶段命中数和最终来源。
- 回答链：验证正常证据、证据不足、高风险问题和多药品问题。
- API/CLI：验证两个入口的关键字段和核心行为一致。

网络模型不可用时，应明确记录未执行的在线检查，并完成不依赖网络的规则与结构验证。
