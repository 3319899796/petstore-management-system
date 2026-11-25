# 宠物店宠物信息管理系统 · 前端

> 本科毕业设计演示系统的前端部分，使用 Vue 3 + Element Plus 实现中文管理后台，对接 `src/backend` 的 Spring Boot 接口。

## 1. 技术栈

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| Vue | 3.4.38 | 页面框架（全部 `<script setup>` + JavaScript，不使用 TypeScript） |
| Vite | 5.4.11 | 开发服务器与打包 |
| Vue Router | 4.4.5 | 路由与登录/权限守卫 |
| Pinia | 2.3.0 | 保存登录状态与权限列表 |
| Element Plus | 2.9.1 | 表格、表单、弹窗等 UI 组件 |
| @element-plus/icons-vue | 2.3.1 | 图标 |
| Axios | 1.7.9 | 请求封装与拦截器 |
| ECharts / vue-echarts | 5.5.1 / 7.0.3 | 看板的柱状图与饼图 |

没有引入 TypeScript、Tailwind、UnoCSS 或任何 UI 模板。

## 2. 目录结构

```
src/frontend/
├─ package.json                 # 依赖与 dev/build/preview 脚本
├─ vite.config.js               # 端口 5173、@ 别名、/api 代理到 8080
├─ index.html                   # 页面入口
├─ .env.development             # VITE_API_BASE=/api
└─ src/
   ├─ main.js                   # 挂载 Vue / Pinia / Router / Element Plus(中文)
   ├─ App.vue                   # 根组件 + 少量全局样式
   ├─ api/
   │  ├─ request.js             # axios 实例、token、响应统一拆包、401 处理
   │  └─ index.js               # 按模块导出全部接口函数
   ├─ stores/
   │  └─ user.js                # 登录态 + permissions + hasPerm()
   ├─ router/
   │  └─ index.js               # 路由表 + 全局前置守卫
   ├─ layout/
   │  └─ MainLayout.vue         # 左侧菜单 + 顶栏 + router-view
   └─ views/
      ├─ Login.vue
      ├─ Dashboard.vue
      ├─ pet/PetList.vue
      ├─ owner/OwnerList.vue
      ├─ vaccine/VaccineList.vue
      ├─ medical/MedicalList.vue
      ├─ foster/FosterList.vue
      ├─ adoption/AdoptionList.vue
      ├─ notice/NoticeList.vue
      ├─ user/UserList.vue
      └─ error/NotFound.vue
```

## 3. 环境要求

- Node.js **18 或更高**（Vite 5 的最低要求），npm 9+
- 后端已启动在 `http://127.0.0.1:8080`，接口前缀为 `/api`
- MySQL 已创建数据库并执行 `src/db/schema.sql`、演示数据脚本
- 开发环境不需要额外配置 CORS：Vite 已把 `/api` 代理到 8080

## 4. 安装与启动

在 `D:\论文毕设\src\frontend` 目录执行：

```bash
npm install          # 安装依赖（需要联网，第一次较慢）
npm run dev          # 启动开发服务器，默认 http://localhost:5173
```

其他命令：

```bash
npm run build        # 打包到 dist/
npm run preview      # 本地预览打包结果
```

> 注意：依赖没有在开发 agent 的沙箱里安装过（沙箱无外网），作者本机第一次运行必须先执行 `npm install`。

## 5. 与后端联调顺序

1. 启动 MySQL，导入建表脚本与演示数据。
2. 启动 Spring Boot 后端（8080 端口）。可先用浏览器访问 `http://127.0.0.1:8080/api/auth/login` 确认服务已起来。
3. 进入 `src/frontend`，执行 `npm install`、`npm run dev`。
4. 打开 `http://localhost:5173`，用下方演示账号登录。
5. 建议按顺序点一遍：数据看板 → 宠物管理 → 主人管理 → 免疫记录 → 就诊记录 → 寄养管理 → 领养管理 → 公告管理 → 用户管理。

前端页面上的所有数据都来自后端接口，接口失败会直接提示错误，不会用假数据兜底。

## 6. 演示账号

| 账号 | 密码 | 角色 | 登录后落地页 |
| --- | --- | --- | --- |
| admin | 123456 | 宠物店管理员（全部权限） | `/dashboard` |
| staff | 123456 | 工作人员 | `/dashboard` |
| owner | 123456 | 宠物主人 | `/pet` |

主人角色没有 `stat:view` 权限，所以看不到"数据看板"菜单；即使手动输入 `/dashboard` 也会被路由守卫拦下并提示"该页面仅工作人员与管理员可用"。

## 7. 权限实现

- 登录成功后把 `token`、用户信息、`permissions` 存入 Pinia，同时持久化到 `localStorage`（key 为 `pet_token`、`pet_userInfo`、`pet_permissions`），刷新页面不会掉登录。
- `stores/user.js` 的 `hasPerm(code)` 判断权限，管理员权限列表是 `*`，代码里做了通配处理；另有 `hasAnyPerm([...])` 用于"满足任一权限即可"的按钮。
- 菜单在 `MainLayout.vue` 里按权限过滤，没有权限的菜单不渲染。
- 路由在 `router/index.js` 用 `meta.perms` 声明所需权限，全局前置守卫统一拦截：
  - 未登录访问受保护页面 → 跳 `/login?redirect=原地址`；
  - 已登录访问 `/login` → 跳回按权限计算的首页；
  - 权限不足 → `ElMessage.warning` 提示并跳回首页（主人访问看板时使用专门文案）。
- 表格里的新增/编辑/删除按钮同样用 `hasPerm` 控制，形成"菜单隐藏 + 路由拦截 + 按钮隐藏 + 后端 403"四层防护。

## 8. 页面与接口对照（自检用）

| 页面 | 调用的后端接口 |
| --- | --- |
| 登录 | `POST /auth/login`、`POST /auth/logout` |
| 数据看板 | `GET /stat/overview`、`GET /vaccine/upcoming?days=7` |
| 宠物管理 | `GET /pet/page`、`GET /pet/options`、`POST /pet/upload`、`POST /pet`、`PUT /pet`、`DELETE /pet/{petId}` |
| 主人管理 | `GET /owner/page`、`POST /owner`、`PUT /owner`、`DELETE /owner/{ownerId}`；宠物页新增时用 `GET /owner/options`（工作人员/管理员）和 `GET /owner/me`（主人取自己的 ownerId） |
| 免疫记录 | `GET /vaccine/page`、`GET /pet/options`、`POST /vaccine`、`PUT /vaccine`、`DELETE /vaccine/{vaccineId}`、`POST /notice/remind` |
| 就诊记录 | `GET /medical/page`、`GET /pet/options`、`POST /medical`、`PUT /medical`、`DELETE /medical/{medicalId}` |
| 寄养管理 | `GET /foster/page`、`GET /pet/options`、`POST /foster`、`PUT /foster`、`POST /foster/audit`、`POST /foster/{fosterId}/finish`、`DELETE /foster/{fosterId}` |
| 领养管理 | `GET /adoption/page`、`POST /adoption`、`POST /adoption/audit`、`POST /adoption/followup`、`DELETE /adoption/{adoptionId}`；主人申请时手填宠物 ID（该角色下 `/pet/options` 只返回自己的宠物） |
| 公告管理 | `GET /notice/page`、`GET /notice/{noticeId}`、`POST /notice`、`PUT /notice`、`DELETE /notice/{noticeId}` |
| 用户管理 | `GET /user/page`、`GET /user/roles`、`POST /user`、`PUT /user`、`POST /user/{userId}/reset-password`、`DELETE /user/{userId}` |

`api/index.js` 里还封装了 `GET /auth/me`、`GET /pet/{petId}`、`GET /vaccine/pet/{petId}`、`GET /medical/pet/{petId}`，本期页面未直接使用，留着方便扩展。

## 9. 关键实现说明

### 9.1 请求层 `api/request.js`

- `baseURL` 取 `import.meta.env.VITE_API_BASE`（开发环境是 `/api`，由 Vite 代理到后端）。
- 请求拦截器从 `localStorage` 读取 token，自动加 `Authorization: Bearer <token>`。
- 响应拦截器把后端的 `{code, message, data}` 拆开：`code === 200` 时直接返回 `data`，页面里就不用每次写 `res.data.data`。
- `code === 401` 或 HTTP 401：清空登录态并跳转登录页（用 `redirecting` 标志防止多个并发请求重复弹窗）。
- 其他业务错误：`ElMessage.error(message)` 后 `reject`，页面通常只需 `try/catch` 兜住即可，不需要重复提示。
- 网络不通时提示"无法连接后端服务，请确认后端已在 8080 端口启动"。

### 9.2 图片上传

- 宠物照片用 `el-upload` 的 `:http-request` 自定义上传，不再走 Element Plus 默认的 action。
- 上传时用 `FormData`，字段名必须是 `file`（与后端一致），请求 `POST /pet/upload`。
- 后端返回形如 `/files/2026/11/xxx.jpg` 的相对路径，表单里保存这个路径；预览时拼上 `VITE_API_BASE` 得到 `/api/files/...`。
- 前端先校验类型（jpg/jpeg/png/gif/webp）和大小（≤5MB），后端还会再校验一次。

### 9.3 主人角色的特殊处理

- 主人新增宠物时，前端先调 `GET /owner/me` 拿到自己的 `ownerId` 并自动填入，不允许手选别人；如果该账号还没有主人档案，接口会报错，页面提示联系店员绑定。
- 主人访问看板会被守卫拦截（见第 7 节）。
- 主人只能看到自己宠物的免疫/就诊/寄养记录，这是后端自动按角色过滤的，前端只负责传分页参数。

### 9.4 状态机按钮

- 寄养：`PENDING 待审核 → APPROVED 寄养中 → FINISHED 已结束`，`PENDING → REJECTED 已驳回`。只有待审核能编辑/审核，只有寄养中能"结束寄养"，只有待审核/已驳回能删除。
- 领养：`PENDING 待审核 → APPROVED 已通过 / REJECTED 已驳回`。只有待审核能审核，只有已通过能登记回访（有 `adoption:followup` 权限才显示），只有已驳回能删除。
- 审核驳回时"原因"为必填，前端有校验，后端也会再校验一次。

## 10. 已知限制与需要作者注意的点

1. **主人申请领养时只能手填宠物 ID**：`GET /pet/options` 对主人角色只返回自己名下的宠物，而系统又不允许申请自己名下的宠物，所以主人无法通过下拉框浏览"别人的宠物"。当前实现是只给主人角色显示"申请领养"按钮，并在弹窗里让主人输入目标宠物 ID（可向店员询问）；工作人员没有 `adoption:add`，管理员虽然权限是 `*` 但账号没有主人档案（后端会返回"当前账号还没有主人档案"），所以这两类角色都不显示申请按钮，避免提交必然失败的表单。若要彻底解决，需要后端新增一个"可领养宠物列表"接口，本期不改后端。
2. **手机号脱敏不能回填**：
   - 主人管理：列表里的手机号是 `138****8000` 形式。编辑弹窗中手机号留空提交会清空原号码（后端收到空串就更新为空），因此编辑时若要保留手机号，必须重新输入完整 11 位号码。
   - 用户管理：编辑时手机号留空不会发送 `phone` 字段，从而保留原号码；要修改则输入完整号码。
3. **系统疫苗提醒只读**：标题以 `【疫苗到期提醒】` 开头的公告由定时任务生成，页面不显示编辑/删除按钮，后端也会拦截。
4. **上传限制**：仅支持 jpg/jpeg/png/gif/webp，单文件不超过 5MB；图片存在后端本机磁盘，不是对象存储。
5. **编辑用户密码**：留空表示不修改，前端不会把 `password` 字段发给后端（后端对空串会触发长度校验）。
6. **接口失败不伪造数据**：看板请求失败时卡片显示 `—` 并给出错误提示，表格为空时显示 Element Plus 的空状态文案。
7. **日期格式**：所有日期选择器统一提交 `yyyy-MM-dd`，时间字段由后端返回 `yyyy-MM-dd HH:mm:ss`。
8. **生产部署**：`npm run build` 产物在 `dist/`。可以交给 Nginx 托管，并把 `/api`、`/files` 反向代理到后端 8080；也可以按学校要求把 dist 放到后端静态资源目录（需调整后端配置）。
9. 界面按桌面端管理后台设计，未做移动端适配；未做国际化切换（固定中文）。

## 11. 常见问题

| 现象 | 排查方向 |
| --- | --- |
| 登录提示"无法连接后端服务" | 后端没启动，或端口不是 8080；检查 `vite.config.js` 的 proxy |
| 登录后立刻跳回登录页 | token 过期或后端重启导致 JWT 失效，重新登录即可 |
| 图片显示 404 | 检查后端静态资源映射是否为 `/api/files/**`，以及数据库里的 `photo_path` 是否存在 |
| 提交表单提示 403 | 当前账号没有对应权限码，用 admin 或对应角色账号操作 |
| `npm install` 很慢/失败 | 切换 npm 镜像源，或确认本机网络能访问 npm registry |
