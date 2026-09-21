# 07: 操作日志

**What to build:** 新增 `@Log(module, action)` 注解（**放 core/runtime**，零依赖纯标记，可选模块也可使用）；AOP 切面与 `sys_oper_log` 实体/落库**放 system**（system 必选，切面对 job 等可选模块同样生效）。切面环绕异步落库：操作人/URI/入参（截断）/结果/耗时/异常；覆盖 system 模块全部写操作。"系统监控"目录如已由工单 06 创建则复用，否则本票创建（声明保持一致）。操作日志分页查询 + 清空页面。测试 profile 用同步执行器保证断言确定。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 任一 system 写操作（如编辑用户）在操作日志页产生记录，字段齐全
- [x] 抛异常的操作也落库并记录异常信息
- [x] 分页查询与清空受 `system:log:*` 控制
- [x] OperLogTest 全绿（测试同步执行器）；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：core/runtime 新增零依赖 `@Log(module, action)` 纯标记注解；system 新增 `sys_oper_log` + `OperLogAspect`（@Around 环绕，采集操作人/URI/入参/结果/耗时/异常，实体在调用线程组装、DB 写入交 `operLogExecutor` 异步；默认单线程后台，test profile 用 `Runnable::run` 同步执行器）+ `OperLogWriter`（INSERT_ONLY）+ `/api/v1/operlog`（list+clear）+ 菜单"操作日志"（系统监控下 sortOrder 1，登录日志/在线用户顺延）。system 全部 19 个写操作（9 实体 save/delete + 强退）已打 @Log。Boot 4 无 aop starter，改引 spring-aspects + aspectjweaver（BOM 管版本）。
- **入参采集的安全陷阱（与工单02 同根源二次踩坑）**：切面用 Jackson 序列化入参会调用 jimmer Input 的懒初始化集合 getter，把"未提交"污染成"已提交空列表"，导致 `isProvided` 误判清空用户岗位/角色关联。改用 `Arrays.toString`（生成 Input 的 toString 直读字段无副作用）+ `password=***` 正则脱敏 + 2000 字符截断。
- 验证：system 75 全绿（OperLogTest 4：字段齐全落库、异常操作落库含异常信息、keyword 过滤、无权限清空被拒；MenuSync 节点数同步 51）；vue-tsc 零错误；冒烟（编辑岗位→操作日志页出现"岗位管理/保存岗位/admin/URI/耗时/成功"记录+详情弹窗）。
