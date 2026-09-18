# ROADMAP — 下一步开发计划（AI 会话交接文档）

> 本文档写给拿到代码继续开发的 AI/开发者：当前代码已完成若依式改造的**第一批**（模块化 + 认证 + 动态菜单 + 定时任务），下一批工作是下文五个批次。
> 开始前必读：`AGENTS.md`（架构红线/Jimmer 0.12.0 要点/已知坑），严格沿用其中"模块化机制"清单。
> 每批次完成后：跑 `./mvnw -pl modules/system -am test` 全绿 + `npx vue-tsc --noEmit` 零错误 + 浏览器冒烟，再进下一批；建议每批次一个 commit。

## 已完成基线（勿重做）

- 模块化：core 收敛为 model+runtime（security JWT / ModuleProvider SPI），业务在 modules/system、modules/job；app/pom.xml 依赖即选装开关
- 认证：JWT 无状态，`/api/v1/auth/{login,getInfo,getRouters,logout}`；`@PreAuthorize("@perm.has('...')")`，ADMIN `*:*:*` 直通
- 菜单：ModuleProvider.menus() 声明 → MenuSyncService 启动幂等 upsert sys_menu（业务键 parent+name）+ roleCodes 绑定
- REST 统一 `/api/v1` 前缀（见 AGENTS.md"接口前缀约定"红线）
- 前端：getRouters 动态路由、v-permission 按钮级权限、授权树（t-tree 用 v-model + 保存补全祖先 id）
- 内置账号：admin/123456（超管）、demo/123456（USER 只读）、frozen/123456（禁用）

## 批次 1：部门/岗位管理（数据权限的前置）

后端（全部在 modules/system 内）：
- `sys_dept` 表 + `Dept` 实体：name/parent 树形（沿用 Menu 的 `@Key(parent,name)` 模式）、orderNum/status/leader/phone/email；`DeptService` 树查询+CRUD，权限点 `system:dept:*`
- `sys_post` 表 + `Post` 实体 + `sys_user_post` 关联；User 实体加 `@Nullable dept` 关联与 `posts` 多对多（IdView：deptId/postIds）；`PostService` 分页 CRUD（`system:post:*`）
- 追加 `modules/system/src/main/resources/sql/system-schema.sql`/`system-data.sql`（默认部门树种子 + 4 个岗位种子）
- `SystemModuleProvider` 追加菜单声明：部门管理、岗位管理
前端：
- `pages/system/dept`（t-enhanced-table 树表，参照 pages/system/menu/index.vue）、`pages/system/post`（普通表格，参照 role 页）
- 用户管理页（pages/system/user）：左侧部门树筛选 + 表单部门选择/岗位多选
测试：DeptServiceTest、PostServiceTest

## 批次 2：字典/参数/公告

- `sys_dict_type` + `sys_dict_data`：DictService 类型 CRUD、数据 CRUD、`GET /api/v1/dict/data/type/{type}` 按类型取启用字典
- `sys_config`：configKey 唯一，ConfigService CRUD
- `sys_notice`：noticeTitle/noticeType/content(textarea，不引富文本)/status
- 前端 `pages/system/{dict,config,notice}`（字典页为"主表+点击进数据明细"二级布局）；`web/src/hooks/useDict.ts`（按 type 拉取+缓存，渲染字典标签）
- 种子：sys_yes_no、sys_notice_type 等基础字典；菜单声明追加
测试：DictServiceTest、ConfigServiceTest、NoticeServiceTest

## 批次 3：操作日志/登录日志/在线用户

- **会话注册表**（在线用户与强退的基础）：core/runtime `TokenService` 签发时登记 jti→(LoginUser,登录时间,ip) 到 Caffeine（TTL=expire-hours）；`JwtAuthenticationFilter` 校验 jti 必须存在（登出/强退即时生效）；logout 删会话；core/runtime pom 加 caffeine 依赖
- `sys_oper_log` + 新 `@Log(module, action)` 注解 + AOP 环绕异步落库（操作人/URI/入参截断/结果/耗时/异常），先覆盖 system 模块各写操作
- `sys_logininfor`：AuthController 登录成功/失败/登出记录
- 查询分页 + 清空（`system:log:*`）；在线用户列表+强退（`system:online:*`）
- 菜单声明新增"系统监控"目录 → 操作日志/登录日志/在线用户
- 前端 `pages/monitor/{operlog,logininfor,online}`
测试：OperLogTest（AOP 落库断言）

## 批次 4：数据权限

- `sys_role` 加 `data_scope` 列（1全部/2自定义/3本部门/4本部门及以下/5仅本人，默认 1）+ `sys_role_dept` 关联表
- 角色表单加数据权限单选；选"自定义"时显示部门树勾选
- core/runtime 新 `DataScopeFilter`（参照 TenantFilter）：对 User 查询按当前登录用户各角色 data_scope 取最大范围追加 dept 条件（ADMIN 直通；"仅本人"按 id）；通过 SecurityUtils 取会话角色范围
- 生效点：用户管理分页列表、角色管理列表按本部门范围过滤
测试：DataScopeTest（五种 scope 断言）

## 批次 5：个人中心 + 死链清理 + codegen 同步

- 后端：`GET /api/v1/auth/profile`（当前用户详情含部门/角色）、`PUT /api/v1/auth/password`（校验旧密码，改后作废其它会话）
- 前端：固定路由 `/user/profile`（router/modules 新增 profile.ts，不进 sys_menu）：资料+改昵称+改密码；Header 头像下拉"个人中心"指向它（当前指向 /user/index 会 404）
- 死链清理：Header logo / SideNav goHome / tabs-router 默认路径指向未注册的 `/dashboard/base`，统一改 `/`；删除无路由引用遗留：pages/{dashboard,detail,form,list,user}、pages/result 仅保留 404/500、api/{detail,list,permission}.ts、api/model/{detail,listModel}.ts、components/common-table；同步删 locales 无引用 key
- codegen 同步新结构：生成器产物改为 `modules/<code>`（entity/repository/service/ModuleProvider/SQL/pom，含 jimmer-apt 覆盖写法与 /api/v1 路径），更新 codegen/README.md

## 明确不做（再往后）

- 服务监控/缓存监控（依赖 actuator/admin 端点暴露策略）
- 公告富文本编辑器、租户管理界面（租户设施为预留能力）
