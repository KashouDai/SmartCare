# 源码开发指南

本文件适用于 `src/` 下的全部 Python 源码，并继承仓库根目录 `AGENTS.md` 的通用规则。

## 模块边界

- `drug_rag/schemas/` 定义跨模块传递的数据结构。修改字段时，必须同步检查 JSONL 序列化、Chroma metadata、API 响应和 CLI 输出。
- `drug_rag/loaders/`、`parsers/` 和 `pipelines/` 负责把原始文件转换为 `DrugDocument`、`DrugChunk` 及批处理报告；不要在这些模块中加入检索或回答逻辑。
- `drug_rag/indexing/` 负责向量索引、摘要索引和药品目录。索引标识符与 metadata 字段必须保持稳定，避免已有持久化数据失效。
- `drug_rag/retrieval/` 负责意图分类、查询改写、策略选择、召回和重排。新增策略时，应保持 trace 可观测，并在规则或模型不可用时保留明确的兜底行为。
- `drug_rag/chains/` 负责证据筛选、安全评估和最终回答。回答只能依据检索证据，不得绕过澄清、多药品隔离、安全提示或模板兜底。
- `drug_rag/api/main.py` 和 `drug_rag/cli.py` 是入口层。入口层负责参数校验与编排，不应复制底层业务规则。

## 接口与数据约定

- 使用 `drug_rag...` 形式的绝对导入，并为新增或修改的公共函数保留完整类型标注。
- 跨层结果优先沿用现有 dataclass 或 Pydantic 模型；不要用结构不明确的字典替代已有模型。
- 保持 `to_dict()`、`model_dump()`、trace 字段、错误字段和来源信息的兼容性，除非任务明确要求调整接口契约。
- JSON 和 JSONL 文件统一使用 UTF-8，并设置 `ensure_ascii=False`，确保中文内容可读。
- 路径使用 `pathlib.Path` 和现有项目路径解析逻辑，不要依赖进程启动目录拼接路径。
- 外部模型调用必须从配置读取 provider、model、endpoint 和密钥环境变量；禁止在源码中写入密钥。

## 修改要求

- 修改解析逻辑时，验证 `doc_id`、`chunk_id`、章节名、页码范围和药品 metadata 没有意外变化。
- 修改检索逻辑时，至少覆盖有药品约束、无药品约束、安全类问题和需要澄清的问题，并检查最终文档来源及 trace。
- 修改回答逻辑时，确认答案中的结论可以在所选证据中定位；证据不足时返回澄清或兜底结果，不补充外部医学知识。
- 修改上传或索引更新流程时，保持去重、同名替换、旧向量删除、目录重建和运行时缓存清理的完整顺序。
- 捕获外部服务异常时，应保留可诊断信息并走现有兜底路径；不要使用空的 `except` 隐藏新增代码中的错误。

## 验证要求

先执行根目录 `AGENTS.md` 中的基础验证，再根据改动范围补充检查：

```powershell
$env:PYTHONPATH = "src"

# 路由和 metadata 过滤检查
python -m drug_rag.cli route --question "坎地沙坦酯片怎么吃？" --drug-name "坎地沙坦酯片" --json

# API 启动检查
python -m uvicorn drug_rag.api.main:app --host 127.0.0.1 --port 8000
```

涉及 DashScope、Chroma 持久化数据或真实药品文件的检查，仅在凭证和测试数据明确可用时执行。不要为了验证而覆盖 `data/` 中的现有运行数据；使用独立的临时输入、输出和索引目录。
