# 01: 部门与岗位管理

**What to build:** 管理员在"系统管理"下新增"部门管理"与"岗位管理"两个页面维护组织：部门是 name/parent 的树（排序/状态/负责人/电话/邮箱，负责人为自由文本不引用用户），岗位是独立 CRUD 列表。生命周期约束：部门有子部门或在职用户禁删、岗位被用户绑定禁删、禁用仅在选择器中过滤不追溯影响已有用户。种子：默认部门树 + 4 个岗位。权限点 `system:dept:*` / `system:post:*`，菜单声明进 SystemModuleProvider（节点及全部祖先显式标注角色，防路由断链）。详见 `docs/ROADMAP.md` 批次1。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 部门管理树表（t-enhanced-table）可新增/编辑/删除，父选择用树选择器
- [ ] 删除约束生效：有子部门或有用户时拒绝并提示；岗位被绑定时拒绝
- [ ] 岗位管理分页 CRUD 正常，按钮级权限（v-permission）生效
- [ ] 种子幂等：重启后默认部门树与 4 岗位存在
- [ ] DeptServiceTest、PostServiceTest 全绿；`npx vue-tsc --noEmit` 零错误；浏览器冒烟；一个 commit
