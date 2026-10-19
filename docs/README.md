# SmartCare 知识助手项目文档

本目录记录 SmartCare 多知识域 RAG 助手的需求、架构、开发阶段、设计决策和验收结果。

## 文档入口

- [产品需求文档](../deploy/docs/医院药品知识库助手接入PRD.md)
- [开发计划](DEVELOPMENT_PLAN.md)
- [总体架构](ARCHITECTURE.md)
- [变更记录](CHANGELOG.md)
- [设计决策](decisions/)
- [阶段验收记录](acceptance/)

## 维护规则

1. 需求变化先更新 PRD，再修改代码。
2. 影响系统边界、数据模型、索引版本或接口协议的决定，新增或更新 ADR。
3. 每个开发阶段必须有对应验收记录，并关联 Git 提交或标签。
4. 文档中的指标是开发目标，未经过实际测试不得表述为已达成结果。
