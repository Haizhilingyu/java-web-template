# 06: 登录日志与在线用户

**What to build:** 登录成功/失败/登出各写一条登录日志（账号/IP/消息/时间）；"系统监控"目录下新增在线用户页：实时列出注册表中的会话（账号/昵称/IP/登录时间），管理员可强退任一会话（立即 401）。菜单目录"系统监控"由本票创建，子菜单"操作日志/登录日志/在线用户"声明与工单 07 保持一致（谁先落地谁建目录，声明相同不冲突——MenuSync 幂等）。权限点 `system:log:*` / `system:online:*`。

**Blocked by:** 05（会话注册表）

**Status:** resolved

- [x] 登录成功/失败/登出均产生日志记录并可分页查询
- [x] 在线用户列表实时反映注册表；强退后目标会话立即 401
- [x] 日志清空受权限控制；按钮级权限生效
- [x] 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：`sys_logininfor` + `LoginLogWriter`（独立 @Component，AuthController 登录成功/失败/登出时写入；登录日志无业务键用 INSERT_ONLY 模式）；`/api/v1/logininfor`（list/bySuperQBE + clear，keyword 由仓库手拼 `Predicate.or(like...)`）；`/api/v1/online`（list 读 `SessionRegistry.snapshot()`、forceLogout 删 jti）；菜单新建"系统监控"目录(sortOrder 3, icon chart) + 登录日志/在线用户两个子页（仅超管）。前端 pages/monitor/{logininfor,online}。
- **jimmer-apt 三连坑（本次排障核心，已验证）**：@EnableImplicitApi 包扫描下，(1) `@RestController` 里无映射注解的 public 方法（如内部写入口）会令 apt 生成 API 元数据 NPE——写入口抽到非 Controller 组件；(2) 端点返回原始类型 `int` 同样 NPE——用 void/包装类型；(3) **实体/DTO 类型引用若在源码中不可解析（如 import 被误删的 `LocalDateTime` ERROR 类型）也是同一 NPE**——报错不指明文件，只能二分。另外 jimmer 无 `createDeleteQuery`，删除用 `sql.createDelete(table).execute()`。
- 测试注意：MockMvc 请求后测试框架清空 SecurityContext，直连 service 调用前需重新 `TestLogin.loginAs`。
- 验证：system 71 全绿（LoginLogOnlineTest 3：三态日志、在线列表实时+强退立即 401、清空权限 403/200）；vue-tsc 零错误；冒烟（在线列表 2 会话→强退→本会话 401 跳登录页；登录日志三条成功记录→清空→空态）。
