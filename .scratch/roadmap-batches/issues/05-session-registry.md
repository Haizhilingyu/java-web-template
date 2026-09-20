# 05: 会话注册表

**What to build:** 登录签发的 token 携带 jti，core/runtime 的 TokenService 把会话（用户/登录时间/IP）登记进 Caffeine（TTL = 令牌过期时长，版本用 Boot 管理）；认证过滤器校验 jti 必须在册；登出删除对应会话。同一用户多会话并存、互不互踢；服务重启全部会话失效（已由 ADR-0001 记录接受）。在线用户/强退/改密作废都以此为基座。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 登出后原 token 立即 401；同用户其他并存会话不受影响
- [x] 双浏览器登录并存，各自独立失效
- [x] 令牌过期后注册表条目随 TTL 消失
- [x] system 与 job 既有测试全绿（回归）；一个 commit

## Comments

2026-09-20 完成。

- 落地内容：core/runtime 新增 `SessionInfo`/`SessionRegistry`（Caffeine expireAfterWrite=TTL，Boot BOM 管版本）；`TokenService.create(userId, username, nickname, ip)` 签发带 jti 并登记，`parse` 返回 `TokenPayload(userId, jti)`；`JwtAuthenticationFilter` 校验 jti 在册（不在册=已撤销，与非法令牌同待遇）；`AuthController.login` 登记会话（IP 取 X-Forwarded-For 首值否则 remoteAddr），`logout` 解析并删除 jti。多会话并存不互踢；重启全失效（ADR-0001 接受）。
- 验证：system 68 + job 11 全绿（新增 SessionRegistryTest 3：登记/撤销、短 TTL 过期、按用户作废；AuthControllerTest 新增"登出后令牌立即失效且同用户并存会话不受影响"双令牌用例）。本工单无前端改动（前端登出原本即调 /auth/logout）。
- 基座就绪：工单06（在线用户列表+强退消费 `all()`/`remove`）、工单09（改密作废消费 `removeByUser`）。
