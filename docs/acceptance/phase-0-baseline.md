# 阶段 0：仓库与文档基线验收

## 目标

建立统一 Git 根仓库、项目文档入口和阶段追溯规则。

## 验收项

- [x] 根仓库为当前项目仓库；
- [x] `deploy/.git` 不存在；
- [x] `drug-rag/.git` 不存在；
- [x] 根仓库已创建初始提交 `9438fbf`；
- [x] 根目录 `.gitignore` 已排除依赖、构建产物、密钥和 RAG 生成数据；
- [x] 当前工作区无未提交变更；
- [x] 未配置 Git remote；
- [x] 已建立阶段计划、架构说明、ADR 和验收记录目录。

## 验证命令

```powershell
git rev-parse --show-toplevel
git status --porcelain
Test-Path -LiteralPath 'deploy/.git'
Test-Path -LiteralPath 'drug-rag/.git'
git log -1 --oneline
git remote -v
```

## 结果

阶段 0 已完成。后续功能开发从根仓库当前提交开始，并在每个阶段结束时补充提交和标签。
