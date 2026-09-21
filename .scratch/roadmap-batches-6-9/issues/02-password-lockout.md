# 02: 密码错误锁定

**What to build:** 进程内 Caffeine 计数（key=username，expireAfterWrite=10min）：连续错 5 次锁 10 分钟；login 流程先查锁定，锁定期内直接拒绝并提示剩余分钟；只有"用户存在但密码错误"才计数（用户名不存在不计、验证码错误不计）；成功登录清零。阈值与开关做成可注入，测试不受 sys_config 种子影响。**完成后回写 AGENTS.md 安全要点**：验证码开关位置、锁定语义、TestLogin 为何不受影响。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 连错 5 次后，即使密码正确也拒绝登录并提示剩余锁定时间
- [x] 锁定到期自动恢复；中途成功登录清零计数
- [x] 用户名不存在不计数；验证码失败不计入密码错误计数
- [x] AGENTS.md 安全要点已补验证码+锁定说明
- [x] 测试覆盖计数/到期/清零；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：`LoginProtectionProperties`（`core.security.login-protection.enabled/max-attempts/lock-minutes`，默认 true/5/10，app yml 显式声明）；`LoginLockService` 进程内 Caffeine 计数（key=username，expireAfterWrite=lockMinutes，滑动窗口；达阈值记 lockedAt，锁定期内不重写条目防延长锁定，到期自动解锁）；`LoginLockedException`（AuthenticationException 子类）由 AuthExceptionHandler 转 401+提示剩余分钟，锁定拒绝同样记登录日志。
- 登录流接线：验码（不计入）→ 查锁定（锁定期直接拒绝）→ 认证（BadCredentials 时查库确认"用户存在"才计数，用户名不存在不计）→ 成功 reset 清零。配置经 SecurityBeans `@EnableConfigurationProperties` 注入，测试不受 sys_config 种子影响。
- 测试策略：测试 yml 默认 `enabled: false`（Caffeine 不随 @Transactional 回滚，跨用例错误登录会累积误锁 admin）；LoginLockoutTest 以 `properties=...enabled=true` 单独开上下文做端到端（连错 5 次正确密码也拒+剩余分钟文案、用户名不存在 6 次仍报凭据错误、中途成功清零）；LoginLockServiceTest 纯单元（短 TTL 验证到期解锁、锁定期重复失败不延长）。system 101 全绿；vue-tsc 零错误。
- AGENTS.md 安全要点已回写：验证码开关位置、锁定语义、TestLogin 为何不受影响。
- 浏览器冒烟与本批其余工单合并执行（见各自 Comments；锁定文案经 UI 登录失败提示验证）。
