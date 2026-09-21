# 09: 个人中心

**What to build:** `GET /api/v1/auth/profile` 返回当前用户详情（含部门/角色）；`PUT /api/v1/auth/password` 校验旧密码后修改，**成功即作废该用户全部会话**（基于工单 05 的注册表）。前端新增固定路由 `/user/profile`（不进 sys_menu）：查看资料、改昵称、改密码；Header 头像下拉"个人中心"从死链 `/user/index` 改指此页。

**Blocked by:** 05（会话注册表）

**Status:** resolved

- [x] 改密成功后该用户其它会话立即 401，需重新登录
- [x] 旧密码错误被拒绝且不改密
- [x] 个人中心页资料/改昵称/改密码可用；Header 不再 404
- [x] 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：`GET /api/v1/auth/profile`（部门/角色/岗位名称）+ `PUT /api/v1/auth/nickname` + `PUT /api/v1/auth/password`（BCrypt 校验旧密码→更新→`sessionRegistry.removeByUser` 作废全部会话含当前）；AuthModels 增 ProfileResponse/NicknameRequest/ChangePasswordRequest。前端：固定路由 `/user/profile`（router/modules/profile.ts，LAYOUT 包裹+LocalizedTitle meta），页面放 `pages/profile`（避开工单 10 要删的 pages/user 目录），Header 下拉"个人中心"改指此页；改密成功前端清 token 跳登录页。
- 验证：system 86 全绿（ProfileTest 4：资料字段、旧密码错误 400 且密文不变、改密后双会话全 401+新密码可登录旧密码不可、改昵称生效）；vue-tsc 零错误；冒烟（资料渲染部门"总公司"/角色"管理员"；UI 改密 123456→654321 后自动跳登录页，随后 API 改回）。
