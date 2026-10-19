# 仓库指南

## 项目结构

- `src/drug_rag/` 存放 Python 包。API 处理器位于 `api/`，问答与安全流程位于 `chains/`，数据导入位于 `loaders/`、`parsers/` 和 `pipelines/`，索引持久化位于 `indexing/`，查询路由与检索逻辑位于 `retrieval/`。
- `configs/` 存放运行时 YAML 配置和提示词。除非同步更新所有调用方，否则应保留现有配置键。
- `web/` 是由 FastAPI 提供服务的原生 HTML/CSS/JavaScript 客户端，不依赖前端框架。
- `data/uploads/`、`data/processed/`、`data/chroma/` 和 `logs/` 属于运行时产物。除非任务明确要求使用测试夹具，否则不要编辑或提交生成的索引、上传文件、处理结果或日志。

## 环境与命令

使用 Python 3.11 或更高版本，并从仓库根目录执行命令。在 PowerShell 中：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
$env:PYTHONPATH = "src"
```

常用命令：

```powershell
# 在 http://127.0.0.1:8000 启动 API 和 Web 界面
python -m uvicorn drug_rag.api.main:app --host 127.0.0.1 --port 8000 --reload

# 查看 CLI 用法
python -m drug_rag.cli --help

# 解析一个 PDF
python -m drug_rag.cli parse --pdf <path> --out <output.jsonl>

# 基于已有索引提问
python -m drug_rag.cli ask --question "坎地沙坦酯片怎么吃？"
```

当前配置中的 Embedding、重排模型和 LLM 使用 DashScope。调用这些功能时，需要在本地设置 `DASHSCOPE_API_KEY`；禁止将密钥保存到源代码、YAML 配置、日志或示例中。

## 实现约定

- 遵循现有的类型标注 Python 风格：使用 `from __future__ import annotations`、类型提示、`pathlib.Path`、dataclass/Pydantic 模型以及简洁的模块级辅助函数。
- 医学回答必须基于证据。不得绕过元数据隔离、引用/来源处理、澄清路由、安全评估或模板兜底机制来让回答看起来更完整。
- 修改检索行为时，保留现有的 trace 字段和 API 响应结构；Web 客户端和 CLI 都依赖这些字段。
- 使用现有的配置和路径辅助函数解析仓库相对路径。不要让程序行为依赖调用者当前所在的工作目录。
- 只进行聚焦修改。不要重新格式化或重构无关代码，也不要在非必要情况下重新生成 Chroma 或处理后的数据。

## 验证

当前仓库没有已提交的自动化测试套件。每次修改至少执行：

```powershell
python -m compileall -q src
$env:PYTHONPATH = "src"
python -m drug_rag.cli --help
```

此外，还应执行与改动最相关的冒烟检查：

- 解析/导入修改：使用临时样例进行解析，并检查生成的 JSONL 字段；
- 检索/路由修改：使用具有代表性的中文问题执行 `route` 或 `retrieve --json`；
- API 修改：启动 Uvicorn，检查 `/api/health` 以及被修改的接口；
- 前端修改：打开服务页面，验证受影响的交互。

除非明确具备凭证和测试数据，否则日常检查中不要执行依赖网络的模型或索引操作。需要说明被跳过的检查及原因。

## 变更规范

- 编辑前先检查 `git status`，保留用户已有的无关修改和未跟踪运行数据。
- 不要提交生成文件、凭证、本地 IDE 配置或虚拟环境。
- 提交信息应简短并使用祈使句，例如 `fix retrieval section filtering` 或 `add upload validation`。
- 交付说明中应注明修改的行为、执行过的验证，以及尚未验证的模型或数据相关路径。
