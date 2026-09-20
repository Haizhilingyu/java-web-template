# 01: 部门与岗位管理

**What to build:** 管理员在"系统管理"下新增"部门管理"与"岗位管理"两个页面维护组织：部门是 name/parent 的树（排序/状态/负责人/电话/邮箱，负责人为自由文本不引用用户），岗位是独立 CRUD 列表。生命周期约束：部门有子部门或在职用户禁删、岗位被用户绑定禁删、禁用仅在选择器中过滤不追溯影响已有用户。种子：默认部门树 + 4 个岗位。权限点 `system:dept:*` / `system:post:*`，菜单声明进 SystemModuleProvider（节点及全部祖先显式标注角色，防路由断链）。详见 `docs/ROADMAP.md` 批次1。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 部门管理树表（t-enhanced-table）可新增/编辑/删除，父选择用树选择器
- [x] 删除约束生效：有子部门或有用户时拒绝并提示；岗位被绑定时拒绝
- [x] 岗位管理分页 CRUD 正常，按钮级权限（v-permission）生效
- [x] 种子幂等：重启后默认部门树与 4 岗位存在
- [x] DeptServiceTest、PostServiceTest 全绿；`npx vue-tsc --noEmit` 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-20 完成（与工单 02 的边界：User 实体的 dept/posts 关联及 IdView 在本工单落地，用户页面接入留给 02）。

- 落地内容：`sys_dept`/`sys_post`/`sys_user_post` + `sys_user.dept_id`（main/test 两份 SQL 同步）；`Dept`/`Post` 实体、`.dto`、仓库、`/api/v1/dept`+`/api/v1/post` 服务；SystemModuleProvider 追加两菜单（sortOrder 4/5，不授 USER 仅超管可见，与角色/菜单管理一致）与 apiPrefixes；`web/src/pages/system/{dept,post}/index.vue`；gen:api 重新生成 TS 客户端；core/runtime 新增 `BusinessException`+`BusinessExceptionHandler`（400+{message}，删除约束拒绝的统一通道）。
- 验证：system 43 + job 11 测试全绿；vue-tsc 零错误；浏览器冒烟（admin 登录→菜单出现→部门树增删改→删除研发部被拒且返回"部门下存在用户"→岗位增删）。
- code-review 双轴结论：Standards 无硬违反；Spec 符合。"在职用户禁删 vs 无用户禁删"两处文档冲突，按权威决策文档 spec.md（删部门须无子且无用户）取严实现——仅剩冻结用户的部门同样禁删。
- 遗留（低优先，后续工单顺带）：① `GET /dept/list` 无 `@PreAuthorize`（与 MenuService /list 同款设计，选择器全量可见），如需收紧再说；② exists 系查询三处同构可提取；③ main/test 四份 SQL 重复维护沿袭既有布局；④ 编辑子部门时客户端省略 parentId 会静默变根部门（与 MenuInput 同语义，前端表单总是提交 parentId，不受影响）。
