# 10: 死链清理

**What to build:** 5 处指向未注册路由 `/dashboard/base` 的引用（Header logo、SideNav goHome、tabs-router homeRoute、result/success、result/fail）统一改指 `/`（`/` 已重定向到用户管理）。删除无路由引用的模板遗留：pages/{dashboard,detail,form,list,user}（user 是 starter mock 演示页）、components/common-table、api/{detail,list,permission}.ts 与对应 model、locales 无引用 key。pages/result 实有 8 个子页（403/404/500/fail/success/maintenance/network-error/browser-incompatible），404/500 保留，其余**逐个确认无引用后删**。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] logo 点击/回首页/结果页跳转不再 404
- [ ] 遗留文件与无引用 locale key 清除；`npx vue-tsc --noEmit` 零错误、build 成功
- [ ] 浏览器冒烟无白屏/控制台错误；一个 commit
