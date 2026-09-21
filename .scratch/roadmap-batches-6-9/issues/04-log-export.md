# 04: 日志导出

**What to build:** 操作日志/登录日志导出端点（按当前查询条件全量，`system:log:export`），复用工单 03 引入的 FastExcel 依赖与导出写法；前端 operlog/logininfor 两页加导出按钮。

**Blocked by:** 03（用户 Excel 导入导出）

**Status:** ready-for-agent

- [ ] 两页均可按筛选条件导出 Excel，表头中文
- [ ] demo（无 system:log:export）调用 403
- [ ] 测试覆盖导出；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
