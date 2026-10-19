# 阶段 2：通用文档导入与索引

## 当前子阶段：通用文本解析

### 已完成

- 创建阶段分支 `codex/phase-2-ingestion`；
- 审计现有 PDF 解析、批处理、Chroma 和 Catalog 流程；
- 新增 TXT/Markdown 通用解析器；
- 新增 DOCX 通用解析器，读取段落文本；
- 新增通用 PDF 解析入口，按页保留来源信息；
- 新增通用 `KnowledgeChunk` JSONL 读写；
- 新增 TXT、Markdown、DOCX、PDF 混合批处理入口；
- 空批次也会生成空 JSONL，失败文档记录在批处理报告中。
- 新增通用 KnowledgeChunk 到独立 Chroma collection 的索引适配；
- 写入前按 chunk ID 删除旧记录，并保留 domain、version、section 和页面来源 metadata；
- 使用 mock Chroma 完成写入、删除、持久化和空输入测试。
- Catalog schema 升级为兼容版本 2；
- CollectionEntry 支持摘要集合、创建时间、文档数和 chunk 数；
- 支持注册索引版本并仅激活 `published` 版本；
- 切换 active 时自动保留旧版本，可用于回退；
- 旧 schema 1 Catalog 读取后自动按当前 schema 2 处理。
- 新增 Catalog 文件加载和原子保存；
- 新增“注册 published 版本并切换 active”的发布流程；
- 未发布版本不会写入 Catalog；
- 发布新版本时保留旧 active 版本用于回退。
- 新增端到端构建工作流，串联批处理、JSONL、通用 Chroma 和 Catalog 发布；
- 批处理失败或没有 chunk 时不会发布 Catalog 版本；
- 工作流作为独立模块入口，避免索引基础模块产生循环依赖。
- 新增通用摘要 collection；
- 新增按 Catalog active 版本加载 chunk 和 summary collection 的运行时入口；
- 缺少 active 领域时明确失败，不回退到其他领域。
- 支持知识域、文档版本和领域元数据；
- 按标题分段，并在章节内进行滑窗切片；
- 保留文档标题、章节、来源文件和来源路径；
- 增加不依赖外部模型和向量库的解析测试。

### 暂未验证或不在本阶段接入

- 真实 Chroma 数据目录上的模型和持久化验证；
- 现有药品 API 切换到通用 active collection；
- Spring Boot 和管理端接入。

## 验证结果

执行目录：`drug-rag/`

```text
python -m compileall -q src       通过
python -m unittest discover -s tests -v  29 个测试通过
git diff --check                  通过
```

## 关联提交

```text
3082d4b feat(rag): add generic text document parser
0aa717a feat(rag): support generic docx ingestion
a3d55b6 feat(rag): add generic pdf ingestion
f0659e6 feat(rag): add generic jsonl batch ingestion
123bd2c feat(rag): add generic knowledge vector index
0f9061a feat(rag): add catalog version activation
9bddee5 feat(rag): add catalog publish workflow
2f3ca27 feat(rag): connect ingestion index publish workflow
9447a5f feat(rag): add generic summary index
df656bb feat(rag): load active knowledge collections
```

## 下一步

阶段 2 的代码和离线验证已完成。真实 Chroma/模型服务验证留到部署联调阶段；下一步进入阶段 3，接入 FastAPI 通用问答和 Spring Boot 代理。
