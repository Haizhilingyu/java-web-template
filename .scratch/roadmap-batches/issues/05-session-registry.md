# 05: 会话注册表

**What to build:** 登录签发的 token 携带 jti，core/runtime 的 TokenService 把会话（用户/登录时间/IP）登记进 Caffeine（TTL = 令牌过期时长，版本用 Boot 管理）；认证过滤器校验 jti 必须在册；登出删除对应会话。同一用户多会话并存、互不互踢；服务重启全部会话失效（已由 ADR-0001 记录接受）。在线用户/强退/改密作废都以此为基座。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 登出后原 token 立即 401；同用户其他并存会话不受影响
- [ ] 双浏览器登录并存，各自独立失效
- [ ] 令牌过期后注册表条目随 TTL 消失
- [ ] system 与 job 既有测试全绿（回归）；一个 commit
