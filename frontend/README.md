# 家庭私厨前端

基于 Vue 3、Vite、Vue Router、Pinia、Vant 4 的单页应用，已实现完整的家庭私厨点单与饮食协作功能：

- 认证与个人资料：登录（JWT）、健康档案与饮食偏好（过敏原、忌口、饮食限制、健康状况）
- 家庭空间：创建 / 邀请码加入、成员与角色管理
- 家庭菜品：菜品 / 分类 / 食材 / 制作步骤、上下架、按菜系 / 卡路里 / 关键字筛选、评论与回复
- 美食广场：系统菜谱浏览 / 搜索 / 筛选、复制到我家
- 购物车、家庭订单（逐菜品备菜状态、开饭）、采购清单
- AI 助手「小铲」：多会话流式对话（SSE），以及菜谱解析、购物车分析、卡路里估算

## 开发

```powershell
npm install
npm run dev
```

开发服务器默认监听 `0.0.0.0:5173`，供本机及同一局域网设备访问。

## API 代理

`.env.development` 将 API 基础路径设为 `/api`，Vite 将 `/api/*` 转发到 `API_PROXY_TARGET`（默认 `http://127.0.0.1:8080`）。本机如需覆盖，可在被 Git 忽略的 `.env.development.local` 中设置；不要把数据库密码、JWT 密钥或其他凭据放入 `VITE_*` 变量或前端文件。

## 构建与测试

```powershell
npm test
npm run build
npm run preview
```
