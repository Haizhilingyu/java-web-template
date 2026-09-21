# 12: 分页接口 sortCode 排序生效

**What to build:** 全部 `/list/bySuperQBE` 分页接口的 `sortCode` 参数目前是装饰性的——jimmer `fetchPage(pageNo, size, pageFactory)` 不读取 Spring `PageRequest` 的 Sort，SQL 实际无 ORDER BY（小数据量下按插入序恰好表现正常）。为每个 Repository 的 find 显式翻译 sortCode 为 jimmer `orderBy`（属性名白名单 switch 或等价机制），并补排序断言测试（乱序种子下断言顺序而非集合）。

**Why:** 排序失效属模板既有缺陷（先于工单 02 存在，工单 03 冒烟时暴露并记录在 03 Comments），工单 11 未如约承接。前端排序控件与"默认排序"语义目前均不成立。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 各 Repository find 的 sortCode 翻译为显式 orderBy（属性白名单，非法属性回退 id）
- [x] 单测：故意乱序的种子下断言分页内容顺序（至少覆盖 User/Post/OperLog）
- [x] codegen 的 gen/module.ts 同步该模式（生成带排序的 repository）
- [x] system+job 测试全绿；vue-tsc 零错误；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：core/runtime 新增 `PageOrders.translate(Sort, resolver)`——Spring Sort → jimmer orderBy 的白名单翻译器，白名单外属性跳过、结果为空回退 id，且始终以 id 收尾保证分页窗口稳定。10 个含分页 find 的 system 仓库（user/role/menu/post/dictType/dictData/config/notice/logininfor/operlog）+ job 的 JobRepository 全部接入（部门为全量树接口无分页 find，不涉及）。白名单只放本表标量属性+createdTime/modifiedTime，CLOB（notice.content/operlog.params 等）与跨表路径（如 roles.code，m2m 排序有重复行风险）不进白名单。
- 测试：新增 PaginationSortTest 4 用例（User username asc/desc+非法回退、Post sortOrder、OperLog operator asc+id desc），乱序种子下断言内容顺序；system 90 + job 11 全绿；vue-tsc 零错误。
- codegen：gen/module.ts 生成的仓库骨架同款翻译（白名单由 schema props + BaseEntity 时间戳推导），`npx tsc --noEmit` 通过，gen:rbac 产物已验证与手写一致。
- 遗留：前端表格列尚未接 TDesign sorter（sortCode 契约已成立，控件接入不在本工单范围）。
