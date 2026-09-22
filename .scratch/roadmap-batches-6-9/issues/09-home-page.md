# 09: 首页轻量版

**What to build:** system 新增 HomeController（普通 `@RestController`，仅 authenticated、无权限点要求）：`GET /api/v1/home/summary`（用户数/角色数/今日登录成功次数/操作日志总数）、`GET /api/v1/home/notices`（status=启用的最新 5 条标题级字段）、`GET /api/v1/home/notices/{id}`（启用才可见，含 content）。前端新增首页页：欢迎语（当前用户昵称）+ 4 统计卡 + 公告列表卡（点击弹窗，复用工单 08 的富文本查看组件）；`/` 路由从 redirect /system/user 改指本页。

**Blocked by:** 08（公告富文本）

**Status:** resolved

- [x] 登录后 `/` 渲染欢迎页，统计数字与库中一致
- [x] demo/USER（无 system:notice 权限）可见公告列表与详情；停用公告不可见
- [x] Header logo / SideNav home（已统一指向 `/`）正常落首页
- [x] 测试覆盖 summary 计数+notices 启用过滤；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-22 完成。

- 落地内容：HomeController（普通 @RestController，仅 authenticated 无权限点）：GET /home/summary（用户数/角色数/今日登录成功次数/操作日志总数，各仓库新增 count 系查询）、GET /home/notices（启用最新 5 条标题级字段）、GET /home/notices/{id}（启用才可见含 content，否则 400）。/api/v1/home/** 加入 SystemModuleProvider.apiPrefixes（该文件与重置密码按钮同票提交）。
- 前端：新增 pages/home/index.vue（欢迎语+4 统计卡+公告列表卡点击弹窗复用 RichViewer+快捷入口）；router/modules/home.ts 固定路由，/ 改为 redirect /home/index，permission store 移除按动态菜单重定向逻辑（守卫 routesInited 机制未动），无菜单用户兜底跳 /home/index。
- 测试：HomeTest 3 用例（summary 计数与库一致/公告启用过滤+详情含内容+停用 400/demo 可访问）。
- 浏览器冒烟：登录落首页，欢迎语+统计数字与库一致+公告列表/详情弹窗（含富文本清洗）、停用公告不可见、demo 可访问 ✓。
