# 01: 登录验证码

**What to build:** `sys_config` 种 `captchaEnabled`（**默认 false**）；`GET /api/v1/auth/captcha` 返回 `{ enabled, key?, image(base64)? }`——开关关时只回 `{enabled:false}`，前端据此决定是否渲染验证码框。easy-captcha 生成图形，答案存一次性 Caffeine（key=uuid，TTL 2 分钟，校验即删）。LoginRequest 加可选 captchaKey/captchaCode；login 流程开关开时先验码（错→记登录日志失败并拒绝）。前端登录页 mounted 拉取验证码，enabled 时渲染验证码框+点击换图，登录失败后刷新。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 开关开启：登录页显示验证码框；错码/过期码拒绝登录且记登录日志；验证码一次性（同 key 重放失败）
- [x] 开关关闭：登录页无验证码框，AGENTS.md 的 curl 冒烟登录不受影响
- [x] 参数页修改 captchaEnabled 即时生效（不发版）
- [x] 测试覆盖开关开/关两种断言；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-21 完成。

- 落地内容：system 引入 `com.github.whvcse:easy-captcha:1.6.2`（Central 实际坐标带连字符）；`sys_config` 种 `captchaEnabled`(默认 false，主/测两份 data.sql 同步)。`CaptchaService`：开关每次直读库（参数页改后即时生效）、easy-captcha 纯字母图形（130x48x5，TYPE_ONLY_CHAR 规避 0O/1I）、答案存进程内一次性 Caffeine（key=uuid，TTL 2 分钟，校验即删防重放）。`GET /api/v1/auth/captcha` 免认证（SecurityConfig permitAll 扩到 login+captcha），开关关只回 `{enabled:false}`。LoginRequest 加可选 captchaKey/captchaCode；login 在认证前验码，错码记登录日志"登录失败：验证码错误"并 400 拒绝。
- 前端：auth.ts 加 `fetchCaptcha()`；Login.vue mounted 拉取，enabled 时渲染验证码框+点击换图，登录失败自动刷新（答案已消费）。
- 测试：CaptchaTest 4 用例（关：接口只回 enabled+传统登录不受影响；开：图形下发+错码拒绝且记日志+正确码登录成功；一次性重放失败；开启时缺字段拒绝）。system 94 全绿；vue-tsc 零错误；浏览器冒烟与工单02 合并执行（见 02 Comments）。
- 备注：开关读库在登录链路无租户头场景下不带租户过滤（TenantFilter 既有行为），configKey 全局唯一，行为确定。
