# AGENTS.md

全栈模板：Spring Boot 4 + Jimmer ORM + Spring Security(JWT) 多模块后端（Maven，参照若依的模块化思路），Vue3 + TDesign 前端（动态路由 + 按钮级权限），Node 代码生成器。

> **新会话从这里开始**：下一批开发任务（登录安全/Excel 导入导出/页面补全包/首页轻量版）见 `docs/ROADMAP.md`，按批次实施。

## 目录结构

- `app/` — 装配点（主类 `MainApplication`、`WebMvcConfig` SPA 回退、`StartupUrlLogger`、assembly 发行包）。**app/pom.xml 是模块选装开关**：注释 `modules:job` 依赖重启，其表/接口/菜单全部消失
- `core/` — 框架层两个模块：`model`(仅 `BaseEntity`/`TenantAware` 基类) + `runtime`(租户过滤/拦截器、`security/` SecurityFilterChain+JWT 过滤器+TokenService、`module/` ModuleProvider SPI+MenuNode)。依赖链 runtime → model
- `modules/system` — 必选业务模块（用户/角色/菜单实体+仓库+服务、AuthController `/api/v1/auth/*`、菜单同步引擎 `MenuSyncService`、`SystemModuleProvider` 菜单声明、自带 `sql/system-*.sql` 与测试）
- `modules/job` — 可选示例模块（定时任务：`JobScheduler` 动态调度 + `JobHandler` SPI、`JobModuleProvider`、自带 SQL 与测试）
- `web/` — Vue3 + TDesign 前端。登录后 `/api/v1/auth/getRouters` 拉动态菜单 → `transformObjectToRoute` 转真实路由；静态路由目录 `router/modules/` 已清空仅留固定路由。构建产物内嵌 app jar
- `codegen/` — Node/TS 代码生成器（`npm run gen:rbac`，产物仅作参考不直接使用）

## 常用命令

```bash
# 后端（需先 export JAVA_HOME=$(dirname $(dirname $(which java)))）
./mvnw clean package                # 全量（含前端构建，约 4 分钟）
./mvnw -pl app -am clean package -DskipTests   # 只打 app（前端仍会构建）
./mvnw -pl modules/system -am test  # system 模块 30 测试（Auth/菜单同步/用户/角色/菜单）
./mvnw -pl modules/job -am test     # job 模块 11 测试（CRUD + 调度）
# 单模块跑必须带 -am（兄弟模块从未 install 到本地仓库）

# 前端（cd web）
npm run dev:linux                   # vite dev @3002，接口代理 /api → 8080 透传不重写（后端 jar 须在跑）
npx vue-tsc --noEmit                # 类型检查（build 第一步也会跑）
npm run gen:api                     # 从运行中的后端下载 /ts.zip 重新生成 TS 客户端到 src/api/__generated

# 冒烟：内置账号 admin/123456(超管 *:*:*)、demo/123456(USER 只读)、frozen/123456(禁用)
curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}'
```

前端改动一律用 dev server 热重载，不要为改 `.vue` 重新打包整个项目。

## 接口前缀约定（架构红线）

- **所有业务 REST 统一以 `/api/v1` 开头**（`@RequestMapping("/api/v1/user")` 等）。将来出 v2 时新建 `/api/v2` 路由并行过渡
- 原因：前端 history 路由（/system/user、/job/task）与 REST 同源同前缀会互相误伤——SPA 回退把不存在的 GET 接口兜成 index.html、Security 宽前缀(/job/**)把页面路由拦成 401 JSON。`/api` 开头的路径在 `WebMvcConfig` 被排除回退(不存在即 404)、vite 代理透传不重写
- `ModuleProvider.apiPrefixes()` 按新前缀声明认证范围（默认约定 `/api/v1/{code}/**`）；SecurityConfig permitAll 仅 `/api/v1/auth/login` + 静态资源/文档
- MockMvc 测试、curl 冒烟都必须带完整 `/api/v1` 前缀；gen:api 生成的客户端 uri 已含前缀，前端不再额外拼 BASE

## 模块化机制（新增业务模块必读）

- 模块实现 `ModuleProvider`（`code/name/order/apiPrefixes/menus()`）注册为 Bean，包名 `com.jezetek.modules.<code>`；app 的 `scanBasePackages="com.jezetek"` 自动发现
- **菜单是代码的唯一来源**：`MenuSyncService` 启动时把 `menus()` 声明幂等 upsert 进 `sys_menu`（业务键 parent+name，`Menu` 实体 `@Key`），再按节点 `roleCodes` MERGE 绑定角色。**要让角色看到某节点，该节点及其所有祖先都必须显式 `.roles("角色编码")`**——祖先漏标会导致路由树断链（子菜单查不出来），测试库有预置绑定会掩盖此问题
- 模块自带 `sql/<code>-schema.sql`/`-data.sql`，app 的 yml 用 `classpath*:sql/*-schema.sql` 通配加载；**模块间禁外键**（规避跨 jar 脚本顺序）
- pom 需要照抄 `modules/system/pom.xml` 的 jimmer-apt 覆盖写法（见红线）；实体放在 `model/`，DTO 用 `.dto` + 本模块 `DtoGeneration` 标记类
- 前端页面放 `web/src/pages/<code>/index.vue`，菜单声明 component 填 `/code/index`；权限点 `<code>:实体:动作`，页面按钮用 `v-permission`

## 架构红线

- **spring-boot-maven-plugin 只在 app/pom.xml 声明**。根 pom 声明会被所有模块继承，repackage 在无主类模块上直接失败
- **根 pom 在 `default-compile`/`default-testCompile` 执行级配置了 lombok-only 的 annotationProcessorPaths，会遮蔽子模块插件级配置**。需要 jimmer-apt 的模块（core/model、modules/system、modules/job）必须在相同执行 id 上覆盖——新注解处理器模块照抄这些 pom 的写法
- **不要配置 `jimmer.client.openapi.ui-path`**：jimmer 的 scalarUiConfig 会抢注 `/**` 静态资源映射（自动配置最后注册、必然覆盖），导致 SPA 页面 404。swagger 页面用自建的 `app/src/main/resources/static/swagger-ui.html`（webjars swagger-ui）
- **core 不放业务**：`core/model` 只有 BaseEntity/TenantAware。业务实体进 modules/*/model——旧实体残留会与模块实体撞表（jimmer 扫 classpath 全部实体，报 `table "SYS_XXX" is shared by both`）
- 前端 permission store 守卫依赖 `routesInited` 标记，**不要**恢复 `asyncRoutes.length === 0` 判断（会守卫无限递归、页面冻结）；登录态下 `userStore.getUserInfo()` 靠 userInfo.name 缓存短路，不要每次导航都打 /auth/getInfo

## Jimmer 0.12.0 要点

- 实体是接口；`USER`/`ROLE` 是 SQL 保留字，表名用 `sys_*`（实体 `@Table` 与模块 `sql/*-schema.sql` 必须同步，`database-validation-mode: ERROR` 启动即校验）
- **修改已加载的不可变对象用 `XxxDraft.$.produce(entity, draft -> ...)`**（producer 实例在生成类 `XxxDraft.$` 上），不存在 `ImmutableObjects.produce`
- **`.dto` 生成 Input 的部分更新陷阱**：`toEntity()`/`saveCommand(input)` 对未提交标量无条件写 null（"密码留空不改原值"会失效）；且**集合 id 视图（roleIds/postIds 等）的 getter 懒初始化空列表**，`getXxx() != null` 永远为真，未提交也会被当成"提交了空列表"而清空关联。两类都要手工组 draft 才能部分更新，集合的"是否提交"只能读 Input 私有字段判断（见 `UserService.saveUser` + `isProvided`）
- `.dto` 文件：校验注解必须**全限定名**且写在属性行**之前**；多参数函数必须 `as 别名`；改 `.dto` 后需重新编译
- `@Key` upsert 要求键属性**全部加载**：可空外键做键时，根节点必须显式 `setParent(null)`（未加载 ≠ null，未加载会让 jimmer 判定业务键不完整拒绝保存）
- 分页返回的是 **Spring 的 `org.springframework.data.domain.Page`**（`fetchPage + SpringPageFactory`），不是 jimmer 的 Page；总数用 `getTotalElements()`
- Fetcher：`children(true)` 只抓 id；递归树用 `recursiveChildren()`。动态对象上访问未抓取字段抛 `UnloadedException`（调度器这类"约定入参全形状"的组件，调用方必须用 `allScalarFields` 加载）
- Spring Boot 4 用 **Jackson 3**：bean 类型是 `tools.jackson.databind.ObjectMapper`（`JsonMapper`），测试里别再注入 `com.fasterxml.jackson.databind.*`；注解包仍是 `com.fasterxml.jackson.annotation`

## 安全要点

- SecurityConfig permitAll 仅 `/auth/login`、`/auth/captcha`（工单01 验证码接口，登录页免认证拉取）、静态资源、swagger、h2-console；其余 authenticated + `@EnableMethodSecurity`；401/403 统一 JSON
- 权限判断走 `@PreAuthorize("@perm.has('模块:实体:动作')")`；ADMIN 角色 perms `*:*:*` 在 PermissionChecker 直通；测试里用 `TestLogin`（合成 LoginUser，不走 UserDetails）注入权限
- 登录请求不带 tenant 头：TenantFilter 在无请求上下文/空租户时**不加过滤条件**（启动同步依赖此行为）；查询当前用户务必经 `SecurityUtils`
- **登录验证码（工单01）**：开关在 `sys_config` 的 `captcha.enabled`（种子默认 false），登录链路每次直读库——参数页改后即时生效不发版；答案存进程内一次性 Caffeine（key=uuid，TTL 2 分钟，校验即删防重放），重启即失效；开关关时 login 完全不校验验证码（curl 冒烟不受影响）
- **密码连错锁定（工单02）**：进程内 Caffeine 计数（key=username），连错 5 次锁 10 分钟（`core.security.login-protection.*` 可配）；只有"用户存在但密码错误"才计数，用户名不存在/验证码错误/账号禁用均不计；成功登录清零；锁定自最后一次失败起算到期自动解锁。**TestLogin 不受影响**：它直接构造 SecurityContext 注入权限，不经过 login 端点与计数器；测试上下文默认 `enabled: false`（Caffeine 不随 @Transactional 回滚，跨用例累积会误锁 admin），锁定行为由 LoginLockoutTest 以 properties 覆盖单独开上下文验证

## 已知坑

- **改 `web/pom.xml` 的 node.version 前必须删 `web/node/`**：frontend-maven-plugin 覆盖安装会残留旧版 npm 的嵌套依赖，npm 启动即崩（`Class extends value undefined`）
- Windows/Git Bash 下：杀后台 java 用 `taskkill //F //PID <pid>`；app jar 被运行中进程锁住会导致 `mvn clean` 失败（先杀进程）；curl 命令行传中文会编码错乱（URL 编码或用测试覆盖）
- vue-i18n 文案中的字面量 `@` 必须写 `{'@'}`，否则运行时消息编译抛 SyntaxError → 整站白屏
- TDesign 树形表格必须用 `t-enhanced-table`（`t-table` 会静默忽略 `tree` 配置且类型定义上完全相同）；授权树用 `t-tree`（联动勾选），保存时把勾选节点的**祖先 id 一并补全**再提交，否则父目录未绑定导致动态路由丢子树
- 动态菜单 title 是纯字符串，`MenuContent`/`Breadcrumb` 已做兼容；新增消费 `meta.title` 的组件要注意
- IDEA 开着时其 Maven 自动导入可能清空/重建构建目录，别与其并发构建冲突

- **jimmer-apt 对解析错误零容错且静默失败**：实体/控制器里任何一个符号解析错误（如不存在的 Fetcher 方法、未导入的类）或 @FetchBy 注解位置使用全限定类名（如 Page<@FetchBy('X') com.jezetek.model.User>），会让 jimmer-apt 整体不出码，全模块报"找不到符号 Fetchers/xxxTable/service.dto"的连锁假错。排错先看自己刚改的文件有没有 resolve 错误；@FetchBy 处必须用 import 后的简单类名

## Agent skills

### Issue tracker

Issues live as local markdown files under `.scratch/<feature>/` in this repo. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-label vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
