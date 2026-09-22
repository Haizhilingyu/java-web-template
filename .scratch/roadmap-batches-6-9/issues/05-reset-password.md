# 05: 重置密码

**What to build:** user 页行操作"重置密码"：管理员输入新密码提交 `PUT /api/v1/user/{id}/password`（`system:user:resetPwd`）。把改密里"作废该用户全部会话"的逻辑抽成可复用方法，重置后同样作废目标用户全部会话。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 重置后目标用户其它会话立即 401，新密码可登录、旧密码不可
- [x] demo（无 resetPwd 权限点）调用 403
- [x] 测试覆盖作废会话断言；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-22 完成。

- 落地内容：新增 `PasswordManager.updatePasswordAndRevokeSessions(userId, 密文)`——落库(手工组 draft 防未提交属性写 null)+作废该用户全部会话的共用通道；`AuthController.changePassword` 改为复用该通道。`UserService.resetPassword`：`PUT /api/v1/user/{id}/password`（`@PreAuthorize system:user:resetPwd`，AuthModels 加 ResetPasswordRequest 校验 6~100 位），用户不存在 400。菜单加"重置密码"按钮（用户管理第 7 个，MenuSyncServiceTest 断言同步 55→56）。前端 user 页行操作"重置密码"+弹窗（新密码校验+会话作废提示），走 api/extra.ts 直连。
- 测试：ResetPasswordTest 3 用例（重置后旧会话立即 401+旧密码失效新密码可登录/demo 无权限 403/不存在用户 400）。
- 浏览器冒烟：admin 经 UI 重置 demo 密码→成功提示→curl 旧密码 401"用户名或密码错误"、新密码 200 ✓。
- 备注：重置密码不解除连错锁定（锁定按用户名进程内计数，语义独立）。
