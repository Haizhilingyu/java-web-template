# 04: 日志导出

**What to build:** 操作日志/登录日志导出端点（按当前查询条件全量，`system:log:export`），复用工单 03 引入的 FastExcel 依赖与导出写法；前端 operlog/logininfor 两页加导出按钮。

**Blocked by:** 03（用户 Excel 导入导出）

**Status:** resolved

- [x] 两页均可按筛选条件导出 Excel，表头中文
- [x] demo（无 system:log:export）调用 403
- [x] 测试覆盖导出；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：新建 `excel/LogExcelController`——`GET /api/v1/operlog/export`（模块/动作/操作人/URI/状态/耗时/异常/创建时间，超长 params/result 不导出）、`GET /api/v1/logininfor/export`（账号/IP/结果/时间），均按 keyword 全量、id 倒序，权限 `system:log:export`；复用工单03 的 FastExcel + inMemory 流式写法。两仓库各增 `listAll(keyword, fetcher)` 全量查询（PageOrders 显式排序）。
- 菜单：监控目录两日志页各加"导出"按钮（共用 system:log:export）；MenuSyncServiceTest 总数 53→55。
- 前端：operlog/logininfor 两页工具栏加导出按钮（携带当前 keyword，走 download.ts）。
- 测试：LogExportTest 3 用例（真实业务写操作+登录尝试造数据后导出，回读断言行数/字段/keyword 空结果；demo 403）。system 109 全绿；vue-tsc 零错误；浏览器冒烟与本批合并执行。
- 踩坑记录：FastExcel 写 xlsx 默认走 POI SXSSF 临时文件，Windows 下偶发 `Can not close IO`（临时文件删除失败）——统一 `.inMemory(true)` 规避；导出行模型保留无参构造+存取器供测试回读。
