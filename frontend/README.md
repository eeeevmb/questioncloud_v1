# 题云前端

题云是基于 Vite + Vue3 + TypeScript 实现的老师端题库管理前端，覆盖登录、头像、题集管理、题目 CRUD 等闭环，并与仓库现有 `/api/v1` 接口完成联调。

## 快速开始

```bash
cd frontend
cp .env.example .env   # 根据环境修改 VITE_API_BASE_URL / VITE_DEV_PROXY_TARGET
npm install
npm run dev
```

- `npm run dev`：启动 Vite 开发服务器（默认 <http://localhost:5173>）。
- `npm run build`：产出静态文件供部署。
- `npm run preview`：本地预览 build 结果。

## 后端联调配置

- `VITE_API_BASE_URL`：默认留空，表示使用相对路径（推荐做法，可借助 Vite 代理透传到后端，这样浏览器与后端位于同域，可自动附带 `satoken`）。
- `VITE_DEV_PROXY_TARGET`：仅本地开发会读取，用于告知 Vite 将 `/api` 请求代理到后端，例如 `http://localhost:8080`。
- 若希望直接跨域访问后端，可把 `VITE_API_BASE_URL` 改为 `http://host:port` 并把 `VITE_DEV_PROXY_TARGET` 留空；此时需要后端 CORS 放行。
- Axios 统一封装在 `src/api/client.ts`，默认 `withCredentials: true`，已处理业务错误码和 Cookie 会话。
- Sa-Token Cookie 要求后端 CORS 允许 `credentials` 且 `Access-Control-Allow-Origin` 为具体域名；否则浏览器不会携带 `satoken`。
- 401000 会自动提示 message 并跳转登录。500xxx、4xx、自定义 `Qxxxx` 错误码均弹出 alert。

## 页面导航

| 路由 | 功能 |
| --- | --- |
| `/auth` | 注册 / 登录，登录成功后拉取 `GET /api/v1/user/basicInfo` |
| `/home` | 展示当前用户信息，支持退出登录 |
| `/avatar` | 上传头像（`POST /user/upload-avatar`）并刷新 `<img src="/user/avatar/{id}">` |
| `/collections` | 创建/更新/删除题集，记录常用 ID，并跳转题目分页 |
| `/collections/:id/questions` | 题集题目分页列表，含筛选、分页、进入题目详情 |
| `/collections/:collectionId/questions/new` | 在指定题集中创建题目（支持按题型展示字段） |
| `/questions/:id` | 题目详情，展示统计、附件，可删除 |
| `/questions/:id/edit` | 题目版本化更新，更新后跳回详情 |

题目详情页中的附件引用 `/api/v1/common/file/{fileId}/view`，浏览器直接展示图片；若为其他文件类型则以下载链接显示。

## 常见问题

1. **Network Error / 请求被浏览器拦截**：若后端未开启跨域，保持 `VITE_API_BASE_URL=` 空并设置 `VITE_DEV_PROXY_TARGET=http://后端`，然后重启 `npm run dev`，即可由 Vite 代理到后端避免 CORS。
2. **跨域 Cookie 丢失**：若必须跨域（`VITE_API_BASE_URL` 为绝对地址），请确保后端 `Access-Control-Allow-Origin` 指向具体来源且允许 `credentials`。
3. **401000 提示后仍停在当前页**：大多数场景会自动跳转登录；若访问公开页出现 401，刷新后重新登录即可。
4. **题集 ID 无法得知**：后端创建/更新接口暂未返回 ID，可在数据库或日志中查询，或使用 `/collections` 页内的“常用题集 ID”手动记录。
5. **附件缓存未刷新**：头像等资源加入 `?t=timestamp` 强制刷新；若需替换题目附件，请更新 `ordinal` 或使用新 `fileId`。

更多 API 调试示例见 `docs/api-samples.md`。
