# 07: 操作日志

**What to build:** 新增 `@Log(module, action)` 注解（**放 core/runtime**，零依赖纯标记，可选模块也可使用）；AOP 切面与 `sys_oper_log` 实体/落库**放 system**（system 必选，切面对 job 等可选模块同样生效）。切面环绕异步落库：操作人/URI/入参（截断）/结果/耗时/异常；覆盖 system 模块全部写操作。"系统监控"目录如已由工单 06 创建则复用，否则本票创建（声明保持一致）。操作日志分页查询 + 清空页面。测试 profile 用同步执行器保证断言确定。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 任一 system 写操作（如编辑用户）在操作日志页产生记录，字段齐全
- [ ] 抛异常的操作也落库并记录异常信息
- [ ] 分页查询与清空受 `system:log:*` 控制
- [ ] OperLogTest 全绿（测试同步执行器）；vue-tsc 零错误；浏览器冒烟；一个 commit
