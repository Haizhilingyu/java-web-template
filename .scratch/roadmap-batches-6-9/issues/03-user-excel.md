# 03: 用户 Excel 导入导出

**What to build:** 引入 FastExcel（`cn.idev.excel:fastexcel`，EasyExcel 停维护后的社区延续版）到 system 模块。端点走普通 `@RestController`（multipart/流式下载与 Jimmer 远程服务参数序列化不搭，照 AuthController 写法）：用户导出（按当前查询条件全量不分页，`system:user:export`）、导入模板下载、导入（multipart，`system:user:import`，**成功行入库、失败行回显** `[{rowNum, reason}]`，与若依行为一致）。前端 user 页工具栏加导入/导出按钮；导入弹窗：下载模板+上传+结果回显表格。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 按部门/关键词筛选后导出，内容与筛选一致；表头中文
- [ ] 模板下载可用；导入合法行入库，重复/非法行回显行号+原因且不入库
- [ ] demo（无 export/import 权限点）调用导出/导入 403
- [ ] 测试覆盖导入成功行/失败行、导出行数；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
