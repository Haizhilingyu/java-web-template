# 06: 角色分配用户

**What to build:** role 页行操作"分配用户"打开抽屉：上半部分该角色已绑用户的分页列表，支持批量授权（勾选用户加入角色）/批量取消授权。后端 `GET /api/v1/role/{id}/users`、`POST /api/v1/role/{id}/users`（body 传 userIds）、`DELETE /api/v1/role/{id}/users`（body 传 userIds），权限沿用 `system:role:edit`。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 批量授权后用户出现在已绑列表，该用户角色即时生效（getRouters/权限点反映）
- [x] 批量取消授权后解除绑定，角色权限即时失效
- [x] demo（无 system:role:edit）调用 403
- [x] 测试覆盖批量授权/取消；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-22 完成。

- 落地内容：UserRepository.findByRoleId（按角色分页查已绑用户，keyword 对用户名/昵称模糊）；RoleService 三端点——GET /{id}/users（分页）、POST /{id}/users（批量授权，幂等跳过已绑）、DELETE /{id}/users（批量取消，幂等），权限沿用 system:role:edit，角色不存在 400。授权实现手工组 draft 读改 roleIds（Input.toEntity 会清空未提交集合）。请求体 RoleUserIdsRequest 必须为顶层类——嵌在 @RestController 内的 public record 会被 jimmer-apt 当无映射 API 崩掉编译。
- 前端：role 页行操作"分配用户"抽屉（已绑用户分页+勾选批量取消授权+批量授权弹窗多选启用用户）。
- 测试：RoleAssignTest 4 用例（授权后入列表+getRouters 即时出现 /system；取消后消失+路由回 []；demo 三端点 403；重复授权幂等+空列表 400）。
- 浏览器冒烟：UI 批量授权 smokeuser→已绑列表可见+curl getRouters 即时出现；批量取消→消失+路由回空 ✓。
