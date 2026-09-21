# 10: 死链清理

**What to build:** 5 处指向未注册路由 `/dashboard/base` 的引用（Header logo、SideNav goHome、tabs-router homeRoute、result/success、result/fail）统一改指 `/`（`/` 已重定向到用户管理）。删除无路由引用的模板遗留：pages/{dashboard,detail,form,list,user}（user 是 starter mock 演示页）、components/common-table、api/{detail,list,permission}.ts 与对应 model、locales 无引用 key。pages/result 实有 8 个子页（403/404/500/fail/success/maintenance/network-error/browser-incompatible），404/500 保留，其余**逐个确认无引用后删**。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] logo 点击/回首页/结果页跳转不再 404
- [x] 遗留文件与无引用 locale key 清除；`npx vue-tsc --noEmit` 零错误、build 成功
- [x] 浏览器冒烟无白屏/控制台错误；一个 commit

## Comments

2026-09-21 完成。

- 处理明细：
  - 3 处死链改 `/`（Header logo、SideNav goHome、tabs-router homeRoute）；result/fail、result/success 两页经确认无外部引用后整页删除（其内部死链随之消失）。
  - 删除 pages/{dashboard,detail,form,list,user}、result/{403,fail,success,maintenance,network-error,browser-incompatible}（404/500 保留：router 兜底与 EXCEPTION_COMPONENT 引用）、components/common-table、api/{detail,list,permission}.ts、api/model/{detailModel,listModel}.ts。
  - `permissionModel.ts` 不能直接删：auth.ts 与 permission store 依赖其 RouteItem——将 RouteItem/Component 类型内联进 api/auth.ts，store 改从 '@/api/auth' 导入；utils/route 的 component 判断改为 typeof 收窄（原 `as string` 与真实 Component 联合类型冲突）。
  - locales：扫描全部源码 t('...') 引用，剪除无引用叶子（zh_CN/en_US 各保留 74 个叶子，删除 dashboard/detail/form/list/user 相关）。
- 验证：vue-tsc 零错误 + `npm run build` 成功；冒烟（登录→用户列表 2 行→点 logo 落 /system/user 无 404→系统监控/操作日志菜单在→无 console 错误/无白屏）。
