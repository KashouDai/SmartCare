# CLAUDE.md — admin-web（hims-front-end-code-master）

## 项目定位

医院预约挂号系统的**管理端**，Vue 2 + Element UI + Vuex，基于 RuoYi-Vue 3.8.6 模板二次开发。API 代理指向 `http://localhost:9999`（后端端口）。

## 构建与运行

```powershell
cd admin-web\hims-front-end-code-master
npm install                        # 首次安装依赖
npm run dev                        # 开发服务器，端口 80，自动打开浏览器
npm run lint                       # ESLint 检查（单引号、无分号、无行尾逗号）
npm run build:prod                 # 生产构建至 dist/
```

## 目录结构

```
src/
├── api/                # 接口封装（按业务模块分目录）
│   ├── info/           # 医院业务接口
│   │   ├── appointments.js
│   │   ├── city.js
│   │   ├── dept.js
│   │   ├── doctor.js
│   │   ├── drugInfo.js
│   │   ├── duty.js
│   │   ├── history.js
│   │   ├── hospital.js
│   │   └── patientsInfo.js
│   ├── order/          # 订单支付接口
│   │   └── orderInfo.js
│   ├── system/         # 系统管理接口
│   │   ├── config.js / dept.js / dict/ / menu.js / notice.js / post.js / role.js / user.js
│   │   └── monitor/    # cache / job / jobLog / logininfor / online / operlog / server
│   └── login.js / menu.js / tool/
├── views/              # 页面（与 api/ 目录对应）
│   ├── info/           # appointments / dept / doctor / drugInfo / duty / history / hospital / patientsInfo
│   ├── order/          # orderInfo
│   ├── system/         # config / dept / dict / menu / notice / post / role / user
│   ├── dashboard/      # 首页面板
│   └── monitor/        # 在线用户/定时任务/Swagger
├── router/             # 路由（index.js，配合 permission.js 做动态路由）
├── store/              # Vuex
│   └── modules/        # app / dict / permission / settings / tagsView / user
├── layout/             # 主布局组件（侧边栏/导航/tagsview）
├── components/         # 公共组件
├── utils/              # 工具函数（request.js / auth.js / validate.js / dict/ 等）
├── plugins/            # Element UI 按需注入等
├── directive/          # 自定义指令（v-permission 等）
├── App.vue / main.js / permission.js / settings.js
└── assets/             # 静态资源（icons/ 走 svg-sprite-loader）
```

## 关键配置

| 文件 | 说明 |
|------|------|
| `vue.config.js` | 开发端口 80；代理 `/dev-api` → `http://localhost:9999`；生产 gzip 压缩 |
| `.env.development` | `VUE_APP_BASE_API = /dev-api`，`VUE_APP_TITLE = 医院预约挂号系统` |
| `.env.production` | `VUE_APP_BASE_API = /api`，`VUE_APP_PUBLIC_PATH = /admin/` |
| `src/settings.js` | 侧边栏主题 `theme-dark`、tagsView 开启、sidebarLogo 开启 |
| `src/utils/request.js` | Axios 封装，统一处理 Token 注入（`Authorization` 头）和 401 跳转 |
| `src/permission.js` | 路由守卫：校验 Token、拉取用户信息/权限、动态注册路由 |
| `src/store/modules/permission.js` | 基于后端返回的菜单树生成前端路由 |

## 前后端接口约定

- 基础路径环境变量：`VUE_APP_BASE_API`，开发环境 `/dev-api`，生产环境 `/prod-api`（见 `.env.*`）
- 所有接口通过 `src/utils/request.js` 发出，自动附加 JWT Token
- 统一响应格式由后端 `AjaxResult` 决定：`{ code, msg, data }`，前端 `request.js` 将 `code !== 200` 的响应 reject
- 分页参数：`page` / `limit`（或 `rows` / `pageNum`，视接口而定），由后端 PageHelper 处理

## 编码规范（ESLint 强制）

- **单引号**，**无分号**，**无行尾逗号**
- 2 空格缩进
- Vue 组件名 PascalCase
- `no-console` 在生产环境报错（开发环境关闭）
- 修改代码后建议先跑 `npm run lint` 再提交

## 与后端联调要点

1. 后端需先启动（端口 9999），否则代理失效
2. 首次登录使用database初始化的管理员账号（见 `database/himsss.sql`）
3. 后端新增接口后，在 `src/api/` 对应模块补充封装，并在 `src/views/` 新增或修改页面
4. 后端新增菜单/权限后，需在系统管理 → 菜单管理中配置，前端通过动态路由自动渲染
