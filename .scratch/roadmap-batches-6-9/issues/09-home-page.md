# 09: 首页轻量版

**What to build:** system 新增 HomeController（普通 `@RestController`，仅 authenticated、无权限点要求）：`GET /api/v1/home/summary`（用户数/角色数/今日登录成功次数/操作日志总数）、`GET /api/v1/home/notices`（status=启用的最新 5 条标题级字段）、`GET /api/v1/home/notices/{id}`（启用才可见，含 content）。前端新增首页页：欢迎语（当前用户昵称）+ 4 统计卡 + 公告列表卡（点击弹窗，复用工单 08 的富文本查看组件）；`/` 路由从 redirect /system/user 改指本页。

**Blocked by:** 08（公告富文本）

**Status:** ready-for-agent

- [ ] 登录后 `/` 渲染欢迎页，统计数字与库中一致
- [ ] demo/USER（无 system:notice 权限）可见公告列表与详情；停用公告不可见
- [ ] Header logo / SideNav home（已统一指向 `/`）正常落首页
- [ ] 测试覆盖 summary 计数+notices 启用过滤；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
