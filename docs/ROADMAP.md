# ROADMAP — 下一步开发计划（AI 会话交接文档）

> 本文档写给拿到代码继续开发的 AI/开发者：若依式改造**第一轮五个批次已全部完成并合入**（模块化+认证+动态菜单 → 部门岗位 → 字典参数公告 → 日志/在线用户 → 数据范围 → 个人中心/死链清理/codegen 同步），当前工作是下文**批次 6-9**：对比若依的第二轮功能补齐。
> 开始前必读：`AGENTS.md`（架构红线/Jimmer 0.12.0 要点/已知坑），严格沿用其中"模块化机制"清单。
> 每批次完成后：跑 `./mvnw -pl modules/system -am test` 全绿 + `npx vue-tsc --noEmit` 零错误 + 浏览器冒烟，再进下一批；建议每批次一个 commit。
> 2026-09-18 第一轮规划拷问完成（批次 1-5，工单存档 `.scratch/roadmap-batches/`）；2026-09-21 第二轮规划拷问完成（批次 6-9）：范围口径=**挑选性补齐**（"管理后台开箱可用"优先，不逐项复刻若依）。术语表见 `CONTEXT.md`，会话注册表决策见 `docs/adr/0001-jti-session-registry.md`。

## 已完成基线（勿重做）

- 模块化：core 收敛为 model+runtime（security JWT / ModuleProvider SPI），业务在 modules/system、modules/job；app/pom.xml 依赖即选装开关
- 认证：JWT 无状态，`/api/v1/auth/{login,getInfo,getRouters,logout,profile,password}`；`@PreAuthorize("@perm.has('...')")`，ADMIN `*:*:*` 直通；改密作废该用户全部会话
- 菜单：ModuleProvider.menus() 声明 → MenuSyncService 启动幂等 upsert sys_menu（业务键 parent+name）+ roleCodes 绑定
- REST 统一 `/api/v1` 前缀（见 AGENTS.md"接口前缀约定"红线）；system 模块 CRUD 经 Jimmer 远程服务暴露（service 类内 `@GetMapping` 等），仅 AuthController 是普通 `@RestController`
- 前端：getRouters 动态路由、v-permission 按钮级权限、授权树（t-tree 用 v-model + 保存补全祖先 id）、useDict 字典缓存
- 内置账号：admin/123456（超管）、demo/123456（USER 只读）、frozen/123456（禁用）
- 第一轮成果：部门/岗位（含禁删约束）、字典/参数/公告（textarea）、会话注册表（ADR-0001）+ @Log 操作日志 + 登录日志 + 在线用户/强退、数据范围（五档，service 层 DataScope 构建器，生效点=用户管理分页一处）、个人中心（`/user/profile` 固定路由）、`/` 统一重定向 /system/user、codegen 产物已同步模块化结构

## 批次 6：登录安全（图形验证码 + 密码错误锁定）

后端（全部在 modules/system，core/runtime 不动）：

- 依赖：`com.github.whvcse:easy-captcha` 加在 modules/system/pom.xml（AWT 生成图片、无 servlet 依赖）；验证码答案存 system 内新建的小 Caffeine 实例（key=uuid，TTL 2 分钟，校验即删=一次性）
- `GET /api/v1/auth/captcha`：返回 `{ enabled, key?, image(base64)? }`——开关关闭时只回 `{enabled:false}`，前端据此决定是否渲染验证码框（省一个配置查询接口）
- LoginRequest（.dto）加可选 captchaKey/captchaCode；login 流程：① 开关开则先校验验证码（错→记登录日志失败并拒绝）② 查用户名锁定计数，锁定期内直接拒绝并提示剩余分钟 ③ 认证（只有"用户存在但密码错误"才计数）④ 成功清零计数
- 锁定语义（已敲定）：进程内 Caffeine 计数（key=username，expireAfterWrite=10min），**连续错 5 次锁 10 分钟**；成功登录清零；单机语义、重启即清
- `sys_config` 种 `captchaEnabled=false`（**默认关**：保住 AGENTS.md 的 curl 冒烟登录；参数页可开，文档注明上生产建议打开）——这是 sys_config 第一个真实消费点
- **完成后回写 AGENTS.md 安全要点**：验证码开关位置、锁定语义、TestLogin 为何不受影响（合成会话不走 login 流程）

前端：登录页 mounted 拉取 `/auth/captcha`，enabled 时渲染验证码框+点击换图；登录失败后刷新验证码。

测试：验证码校验（开关开/关各断言）、锁定计数/到期/清零（阈值与开关做成可注入，不受测试库 sys_config 种子影响）

## 批次 7：Excel 导入导出

- 依赖：FastExcel `cn.idev.excel:fastexcel`（EasyExcel 停维护后的社区延续版，API 同 EasyExcel）加在 modules/system/pom.xml
- 端点走**普通 `@RestController`**（multipart 上传/流式下载与 Jimmer 远程服务的参数序列化不搭，照抄 AuthController 的写法）：用户导出（按当前查询条件全量不分页）、导入模板下载、导入（**成功行入库、失败行回显** `[{rowNum, reason}]`，与若依行为一致）；操作日志/登录日志导出
- 权限点：`system:user:export` / `system:user:import` / `system:log:export`
- 前端：user 页工具栏加导入/导出（导入弹窗：下载模板+上传+结果回显表格）；operlog/logininfor 页加导出按钮

测试：导入成功行/失败行断言、导出行数断言

## 批次 8：既有页面补全包（重置密码/分配用户/头像/富文本）

- **重置密码**：user 页行操作，管理员设新密码；`PUT /api/v1/user/{id}/password`，权限 `system:user:resetPwd`；把 AuthController 改密里的 `sessionRegistry.removeByUser` 抽成可复用方法，重置后同样作废该用户全部会话
- **角色分配用户**：role 页行操作打开抽屉——该角色已绑用户分页列表 + 批量授权/批量取消授权；`GET /api/v1/role/{id}/users`、`POST|DELETE /api/v1/role/{id}/users`（body 传 userIds），权限沿用 `system:role:edit`
- **头像上传**：User 实体加 `@Nullable avatar`（byte[]，system-schema.sql 建表语句同步加列）；`POST /api/v1/auth/avatar`（multipart，≤2MB，图片扩展名白名单）+ `GET /api/v1/auth/avatar`（authenticated 流式返回；前端 fetch blob → objectURL 展示——JWT 在 header，`<img src>` 直链带不上凭证）；**不抽通用文件服务**，头像内联实现避免过度设计
- **公告富文本**：wangEditor（`@wangeditor/editor` + `@wangeditor/editor-for-vue`）替换 notice 页 textarea；封装"富文本查看"组件——DOMPurify 清洗后 v-html，批次 9 首页复用

测试：重置密码作废会话断言、avatar 写读、角色批量授权/取消

## 批次 9：首页轻量版（真首页 + 公告用户侧入口）

- system 新增 HomeController（普通 `@RestController`，仅 authenticated、无权限点要求）：`GET /api/v1/home/summary`（用户数/角色数/今日登录成功次数/操作日志总数）、`GET /api/v1/home/notices`（status=启用的最新 5 条标题级字段）、`GET /api/v1/home/notices/{id}`（启用才可见，含 content）
- 前端 `pages/home/index.vue`：欢迎语（当前用户昵称）+ 4 统计卡 + 公告列表卡（点击弹窗，复用批次 8 富文本查看组件）；`router/index.ts` 的 `/` 从 redirect /system/user 改为本页（批次 5 死链清理已把 Header logo/SideNav home 统一指向 `/`，直接受益）

测试：summary 计数断言、notices 仅启用可见（demo/USER 无 system:notice 权限也应可读）

## 明确不做（2026-09-21 第二轮拷问裁定）

- 服务监控/缓存监控/数据监控：H2+Caffeine 单机无对应物（Druid/Redis 监控面不成立，actuator 自建页收益不抵成本）
- 代码生成在线化：与"离线脚本产物仅参考"定位冲突，codegen 产物结构已同步模块化
- 表单构建（拖拽设计器）：演示性功能，真实业务表单不靠它生成
- 同端互踢/单点登录：与 ADR-0001"多会话并存"模型冲突；在线用户+强退已覆盖治理需求（再提此需求须先修订术语表与 ADR-0001）
- 租户管理界面：TenantAware/TenantFilter 继续作为架构预留；H2 单机上"租户套餐+开户+切换"的体量不值
- 注册功能：管理后台模板无注册场景（若依的注册开关默认也是关闭的）
- 全屏按钮：忽略级装饰
