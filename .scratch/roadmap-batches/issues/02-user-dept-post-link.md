# 02: 用户接入部门/岗位

**What to build:** 用户可归属一个部门（可空）、担任多个岗位；用户管理页左侧出现部门树，点选筛选**含全部子孙部门**；编辑表单提供部门选择与岗位多选。部分更新沿用既有惯例：未提供岗位时不清空关联、密码留空不改原值（手工组 draft，见 AGENTS.md Jimmer 要点）。

**Blocked by:** 01（部门与岗位管理）

**Status:** resolved

- [x] 用户实体带 deptId/postIds（IdView），表单可设置并正确保存
- [x] 部门树点选筛选含子孙部门生效；不选部门时列表不过滤
- [x] 部门/岗位未提供时不清空既有关联；密码留空不改原值不回归
- [x] 既有用户测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-20 完成。

- 落地内容：`/api/v1/user/list/bySuperQBE` 新增可选 `deptId` 参数（`DeptRepository.findSelfAndDescendantIds` 逐层下探解析子孙，内存拼接）；用户页左侧部门树（activable 点选，再点取消）+ 表格部门/岗位列 + 表单部门树选择器（过滤禁用节点，保留其子孙）与岗位多选（仅启用岗位）；DEFAULT_FETCHER 带 dept/posts。
- **框架级发现（已回写 AGENTS.md）**：jimmer 生成 Input 的集合 id 视图 getter 懒初始化空列表，`getXxx() != null` 永远为真——未提交的 postIds/roleIds 会被当成"提交了空列表"而清空关联；`saveCommand(input)` 同样对未提交标量写 null。部分更新必须手工组 draft，集合的"是否提交"读 Input 私有字段判断（`UserService.isProvided`）。语义锁定：未提交→保留原值；显式空列表→清空。该修复同时消除了存量 roleIds 的同款隐患。
- 验证：system 47 测试全绿（UserServiceTest 14：新增含子孙筛选/带部门岗位保存/不提交保留/空列表清空）；vue-tsc 零错误；冒烟（部门树点选总公司 2 人/研发部 1 人；表单回填；保存请求体完整）。
