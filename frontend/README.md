# 家庭私厨前端

基于 Vue 3、Vite、Vue Router 和 JavaScript 的单页应用。当前已完成阶段 0 工程初始化及阶段 1 基础布局；业务模块会按 `../docs/frontend/FRONTEND_DEVELOPMENT_ROADMAP.md` 分阶段实现。

## 开发

```powershell
npm install
npm run dev
```

开发服务器默认监听 `0.0.0.0:5173`，供本机及同一局域网设备访问。

## API 代理

`.env.development` 将 API 基础路径设为 `/api`，Vite 将 `/api/*` 转发到 `API_PROXY_TARGET`（默认 `http://127.0.0.1:8080`）。本机如需覆盖，可在被 Git 忽略的 `.env.development.local` 中设置；不要把数据库密码、JWT 密钥或其他凭据放入 `VITE_*` 变量或前端文件。

## 构建

```powershell
npm test
npm run build
npm run preview
```

阶段一提供五个主导航、响应式布局、路由守卫骨架和通用展示组件，暂不连接业务接口。登录页面和真实 Token 管理属于阶段二。

阶段二接入真实登录前，受保护导航默认会跳转到 `/login` 占位页。若要在本地只预览阶段一布局，可在浏览器开发者工具 Console 执行：

```js
localStorage.setItem('homechef.token', 'phase1-preview')
location.reload()
```

此值仅用于展示框架，不是真实 JWT，也不会授予后端访问权限。清理预览状态：

```js
localStorage.removeItem('homechef.token')
location.reload()
```
