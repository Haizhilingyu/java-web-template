# 09: 个人中心

**What to build:** `GET /api/v1/auth/profile` 返回当前用户详情（含部门/角色）；`PUT /api/v1/auth/password` 校验旧密码后修改，**成功即作废该用户全部会话**（基于工单 05 的注册表）。前端新增固定路由 `/user/profile`（不进 sys_menu）：查看资料、改昵称、改密码；Header 头像下拉"个人中心"从死链 `/user/index` 改指此页。

**Blocked by:** 05（会话注册表）

**Status:** ready-for-agent

- [ ] 改密成功后该用户其它会话立即 401，需重新登录
- [ ] 旧密码错误被拒绝且不改密
- [ ] 个人中心页资料/改昵称/改密码可用；Header 不再 404
- [ ] 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
