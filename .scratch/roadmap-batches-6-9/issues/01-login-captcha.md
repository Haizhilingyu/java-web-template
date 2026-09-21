# 01: 登录验证码

**What to build:** `sys_config` 种 `captchaEnabled`（**默认 false**）；`GET /api/v1/auth/captcha` 返回 `{ enabled, key?, image(base64)? }`——开关关时只回 `{enabled:false}`，前端据此决定是否渲染验证码框。easy-captcha 生成图形，答案存一次性 Caffeine（key=uuid，TTL 2 分钟，校验即删）。LoginRequest 加可选 captchaKey/captchaCode；login 流程开关开时先验码（错→记登录日志失败并拒绝）。前端登录页 mounted 拉取验证码，enabled 时渲染验证码框+点击换图，登录失败后刷新。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 开关开启：登录页显示验证码框；错码/过期码拒绝登录且记登录日志；验证码一次性（同 key 重放失败）
- [ ] 开关关闭：登录页无验证码框，AGENTS.md 的 curl 冒烟登录不受影响
- [ ] 参数页修改 captchaEnabled 即时生效（不发版）
- [ ] 测试覆盖开关开/关两种断言；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
