# 05: 重置密码

**What to build:** user 页行操作"重置密码"：管理员输入新密码提交 `PUT /api/v1/user/{id}/password`（`system:user:resetPwd`）。把改密里"作废该用户全部会话"的逻辑抽成可复用方法，重置后同样作废目标用户全部会话。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 重置后目标用户其它会话立即 401，新密码可登录、旧密码不可
- [ ] demo（无 resetPwd 权限点）调用 403
- [ ] 测试覆盖作废会话断言；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
