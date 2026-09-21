# 08: 数据范围

**What to build:** 角色表单新增数据范围五档单选（1全部/2自定义/3本部门/4本部门及以下/5仅本人，默认 1）；选"自定义"时出现部门树勾选，语义为**精确等于勾选集合**（不含未勾选的子孙；"本部门及以下"则含全部子孙）。core/runtime 提供 `DataScope` 条件构建器（经 SecurityUtils 取会话各角色范围、多角色取最大；ADMIN 直通；"仅本人"按用户 id），**生效点在 service 层显式调用**，本批仅用户管理分页列表一处；不做全局 Jimmer Filter（避免误伤 username 查重等必须全量可见的查询——已敲定，见 ROADMAP 批次4）。

**Blocked by:** 01（部门与岗位管理）

**Status:** resolved

- [x] DataScopeTest 五档各自断言：全部/自定义精确集合/本部门/本部门及以下含子孙/仅本人
- [x] 用户持多角色时取最大范围
- [x] 非生效点查询（如 username 查重）不受数据范围影响
- [x] 角色表单五档单选与自定义部门树勾选可用；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：`sys_role.data_scope`（默认 1）+ `sys_role_dept` 关联 + `Role.customDepts`；core/runtime 新增 `DataScope`（Level 五档 + broadest 合并取编码最小=范围最大 + `current()` 从会话取规则，超管返回 null 不过滤）；`LoginUser` 增第 7 字段 `dataScope`（登录/回库时由 UserDetailsServiceImpl 按角色合并算好放入会话，DEPT_AND_CHILD 的子孙扩展留给生效点）；生效点仅 `UserService.findUsersBySuperQBE`（treeDeptIds/scopedDeptIds/selfUserId 三条独立条件 AND）；saveRole 改手工 draft（Input 未提交 dataScope 的 getter 直接抛异常 + 集合懒初始化同款问题）。角色表单：五档 radio-group + dataScope==2 时显示 checkStrictly 部门树（精确集合，不自动补子孙）。
- 验证：system 82 全绿（DataScopeTest 7：五档各自断言+子孙扩展证明(市场一组用例)+多角色合并+非生效点不受影响）；job 11 回归全绿；vue-tsc 零错误；端到端冒烟（admin 把 USER 角色改为自定义{研发部}→demo 登录→用户列表只见研发部的自己，admin 不可见→admin 登录可见全部）。
