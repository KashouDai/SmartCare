# SmartCare

SmartCare 是一个面向本地学习与演示的医疗预约挂号与知识助手项目，包含预约挂号业务、管理端/患者端页面，以及基于 RAG 的药品知识查询服务。

## 项目结构

- `deploy/`：Spring Boot 后端、Vue 管理端、Nuxt 患者端和本地部署配置。
- `drug-rag/`：药品知识检索与问答服务，具体使用方式见其目录下的 README。
- `docs/`：架构、开发计划、设计决策和阶段验收记录。

## 安全边界

本项目仅供学习和本地演示，不是可直接用于真实医疗业务的生产系统。数据库示例应使用虚构数据；密码、JWT 密钥、内部服务令牌和支付凭据必须通过环境变量配置，不能提交到仓库。

## 第三方声明

项目部分后端、管理端和患者端代码基于 RuoYi-Vue 二次开发。许可和来源说明见 [LICENSE](deploy/backend/hims-backend-code-master/LICENSE) 与 [NOTICE](deploy/backend/hims-backend-code-master/NOTICE)。

## 当前状态

源码和文档会持续整理。README 中只记录已在当前仓库验证过的内容；部署成功不等同于浏览器端到端验收，也不代表具备生产合规性。
