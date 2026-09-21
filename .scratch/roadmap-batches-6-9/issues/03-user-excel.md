# 03: 用户 Excel 导入导出

**What to build:** 引入 FastExcel（`cn.idev.excel:fastexcel`，EasyExcel 停维护后的社区延续版）到 system 模块。端点走普通 `@RestController`（multipart/流式下载与 Jimmer 远程服务参数序列化不搭，照 AuthController 写法）：用户导出（按当前查询条件全量不分页，`system:user:export`）、导入模板下载、导入（multipart，`system:user:import`，**成功行入库、失败行回显** `[{rowNum, reason}]`，与若依行为一致）。前端 user 页工具栏加导入/导出按钮；导入弹窗：下载模板+上传+结果回显表格。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 按部门/关键词筛选后导出，内容与筛选一致；表头中文
- [x] 模板下载可用；导入合法行入库，重复/非法行回显行号+原因且不入库
- [x] demo（无 export/import 权限点）调用导出/导入 403
- [x] 测试覆盖导入成功行/失败行、导出行数；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：system 引入 `cn.idev.excel:fastexcel:1.3.0`。新建 `excel/UserExcelController`（普通 @RestController，照 AuthController 约定——签名不出现 servlet/multipart 类型，经 RequestContextHolder/WebUtils 取当前请求与响应，规避 jimmer-apt 编译期 NPE）：`GET /export`（列表同款 Specification+部门树过滤全量，@PreAuthorize `system:user:export`）、`GET /import-template`（仅表头空表）、`POST /import`（multipart 取 file，成功行入库 BCrypt 加密默认密码 123456，失败行回显 `[{rowNum, reason}]`，行号=Excel 数据行号；校验用户名格式/文件内重复/库内重复/部门编号存在/状态枚举；上限 1000 行）。UserRepository 增 `listAll` 全量查询（带工单12 的显式排序）；DeptRepository 增 `existsById`。
- 菜单：用户管理下增按钮 导出用户/导入用户（system:user:export/import）；MenuSyncServiceTest 断言同步更新（用户管理 4→6 按钮，总数 51→53）。app yml 增加 multipart 10MB 上限。
- 前端：`api/download.ts` 通用下载/上传直连（Bearer 鉴权、blob 触发保存、解析 filename*）；user 页工具栏加 导出（携带当前筛选）/导入按钮，导入弹窗含 下载模板+选择文件+结果回显表格。
- 测试：UserExcelTest 5 用例（全量导出 3 行+筛选禁用 1 行+中文表头/模板非空/导入成功行入库（密文+部门）+重复与非法行回显行号原因/demo 三端点 403/缺文件 400）。system 106 全绿；vue-tsc 零错误；浏览器冒烟与本批合并执行。
- 备注：导出未接数据范围（权限点仅授管理员，范围收紧时再接入，控制器注释已说明）。
