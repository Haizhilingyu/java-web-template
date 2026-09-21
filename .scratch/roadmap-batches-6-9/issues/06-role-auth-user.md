# 06: 角色分配用户

**What to build:** role 页行操作"分配用户"打开抽屉：上半部分该角色已绑用户的分页列表，支持批量授权（勾选用户加入角色）/批量取消授权。后端 `GET /api/v1/role/{id}/users`、`POST /api/v1/role/{id}/users`（body 传 userIds）、`DELETE /api/v1/role/{id}/users`（body 传 userIds），权限沿用 `system:role:edit`。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 批量授权后用户出现在已绑列表，该用户角色即时生效（getRouters/权限点反映）
- [ ] 批量取消授权后解除绑定，角色权限即时失效
- [ ] demo（无 system:role:edit）调用 403
- [ ] 测试覆盖批量授权/取消；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
