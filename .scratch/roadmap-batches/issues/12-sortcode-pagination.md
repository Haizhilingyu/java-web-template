# 12: 分页接口 sortCode 排序生效

**What to build:** 全部 `/list/bySuperQBE` 分页接口的 `sortCode` 参数目前是装饰性的——jimmer `fetchPage(pageNo, size, pageFactory)` 不读取 Spring `PageRequest` 的 Sort，SQL 实际无 ORDER BY（小数据量下按插入序恰好表现正常）。为每个 Repository 的 find 显式翻译 sortCode 为 jimmer `orderBy`（属性名白名单 switch 或等价机制），并补排序断言测试（乱序种子下断言顺序而非集合）。

**Why:** 排序失效属模板既有缺陷（先于工单 02 存在，工单 03 冒烟时暴露并记录在 03 Comments），工单 11 未如约承接。前端排序控件与"默认排序"语义目前均不成立。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 各 Repository find 的 sortCode 翻译为显式 orderBy（属性白名单，非法属性回退 id）
- [ ] 单测：故意乱序的种子下断言分页内容顺序（至少覆盖 User/Post/OperLog）
- [ ] codegen 的 gen/module.ts 同步该模式（生成带排序的 repository）
- [ ] system+job 测试全绿；vue-tsc 零错误；一个 commit

## Comments

2026-09-21 由工单 02–11 批量 Spec 审查开票。范围提示：涉及 11 个 Repository（user/role/menu/dept/post/dictType/dictData/config/notice/logininfor/operlog），改动机械但面广；gen/module.ts 生成的仓库骨架必须与手写保持一致，避免再次分叉。
