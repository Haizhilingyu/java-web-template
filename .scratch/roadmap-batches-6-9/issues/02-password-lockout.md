# 02: 密码错误锁定

**What to build:** 进程内 Caffeine 计数（key=username，expireAfterWrite=10min）：连续错 5 次锁 10 分钟；login 流程先查锁定，锁定期内直接拒绝并提示剩余分钟；只有"用户存在但密码错误"才计数（用户名不存在不计、验证码错误不计）；成功登录清零。阈值与开关做成可注入，测试不受 sys_config 种子影响。**完成后回写 AGENTS.md 安全要点**：验证码开关位置、锁定语义、TestLogin 为何不受影响。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 连错 5 次后，即使密码正确也拒绝登录并提示剩余锁定时间
- [ ] 锁定到期自动恢复；中途成功登录清零计数
- [ ] 用户名不存在不计数；验证码失败不计入密码错误计数
- [ ] AGENTS.md 安全要点已补验证码+锁定说明
- [ ] 测试覆盖计数/到期/清零；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
