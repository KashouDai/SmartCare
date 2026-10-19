# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

预约挂号用户端，基于 Nuxt 2 SSR 构建的单页应用。Element UI 组件库，接口直连后端 `http://localhost:9999`。

## Build & Run Commands

```powershell
cd patient-web\hims-site
npm install && npm run dev      # 开发服务器，端口 3000
npm run lint                    # ESLint 检查（规则宽松，仅 plugin:vue/essential）
npm run build && npm start      # 生产构建 + 启动
npm run generate                # 静态站点生成
```

## Architecture

Nuxt 2 约定式路由，目录即页面：

- `pages/` — 路由页面，自动映射为 URL 路径
  - `hospital/` — 医院列表、详情页、排班预约、公告
  - `order/` — 订单列表、支付展示
  - `patient/` — 患者信息管理
  - `user/` — 用户中心
- `api/` — Axios 接口封装，每个模块对应一个文件
- `layouts/` — `default.vue`（含 header/footer）为主布局，`empty.vue` 用于无框架页面
- `plugins/myPlugin.js` — 全局注册 Element UI（全量引入）和 vue-qriously
- `utils/request.js` — 统一 axios 实例，token 注入、重复提交防抖、错误提示
- `utils/auth.js` — Cookie 存取，key 为 `Admin-Token`

## Key Code Patterns

- **API 调用**：所有业务接口在 `api/` 目录下按模块组织（`appointments.js`、`orderInfo.js`、`hosp.js` 等），使用 `~/utils/request.js` 中导出的 axios 实例
- **Token**：存在 Cookie 中（`js-cookie`），请求头格式 `Authorization: Bearer <token>`
- **重复提交防护**：`request.js` 拦截器记录上次请求，1 秒内相同 URL+Data 的 POST/PUT 直接 reject
- **Element UI**：全量引入（`element-ui` + `theme-chalk`），未使用 tree-shaking

## Coding Conventions

- UTF-8 编码，LF 换行
- Vue 组件使用 PascalCase 文件名
- ESLint 规则宽松（仅 `plugin:vue/essential`），无强制缩进/引号/分号约束
- 不要进行与改动无关的格式化
- 样式通过 `layouts/default.vue` 全局引入 CSS 文件，局部样式用 scoped
