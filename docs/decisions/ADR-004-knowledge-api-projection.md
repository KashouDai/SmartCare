# ADR-004：通过 Spring Boot 输出知识助手公共响应

## 状态

已接受

## 背景

SmartCare 后端现有接口统一使用 `AjaxResult`，而 `drug-rag` 的响应包含面向检索排障的完整 Trace。前端助手不应依赖 FastAPI 内部字段，也不需要在每次问答时展示全部检索过程。

## 决策

Spring Boot 提供 `/api/knowledge/ask`，使用现有 `AjaxResult` 包装知识助手公共响应：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "answer": "...",
    "domain": "drug",
    "status": "succeeded",
    "sources": [],
    "safety": {},
    "requestId": "..."
  }
}
```

Spring Boot 负责：

- 接收和校验管理端请求；
- 调用内部 `drug-rag` 服务并注入 `x-internal-token`；
- 映射超时、连接失败、401、503 和业务错误；
- 只向助手返回公共响应字段；
- 使用 `requestId` 关联日志和后续 Trace 查询。

完整 `trace` 不进入普通助手响应，由后续问答记录详情接口按 `requestId` 提供。

## 取舍

这样可以隔离 Python 内部实现，降低前端耦合；代价是 Spring Boot 需要维护一次字段映射，并确保错误码和 requestId 在代理层不丢失。

## 实施边界

当前只记录协议决策，不在本阶段引入新的 HTTP 客户端依赖或实现 Spring Boot 代理。代理实现放到阶段 3，并沿用现有 `spring-web` 能力。
