# jimmer-codegen

根据实体模型 Schema（TypeScript 定义）一键生成**业务模块骨架**——与
`modules/system`、`modules/job` 的实际结构完全一致，复制进工程即可被
app 的 `scanBasePackages("com.jezetek")` 装配。产物仅供对照参考，业务逻辑需自行调整。

## 产物结构

```
out/
├── pom.xml                                   # 模块 pom：依赖 core/runtime + jimmer starter + validation；
│                                             #   jimmer-apt 在 default-compile/default-testCompile 执行级
│                                             #   覆盖 lombok-only 配置(照抄 modules/system/pom.xml 红线写法)
├── src/main/java/com/jezetek/modules/{code}/
│   ├── model/{Entity}.java                   # @Entity/@Table/@Key/@KeyUniqueConstraint/关联注解/@IdView
│   ├── repository/{Entity}Repository.java    # AbstractJavaRepository 超级 QBE + 单业务键查询 + 树形根查询
│   ├── service/{Entity}Service.java          # web 注解直挂 service；/api/v1/{code}/{entity} 路径；
│   │                                         #   {code}:{entity}:list/add/edit/delete 权限点；@Log 操作日志
│   ├── service/DtoGeneration.java            # @EnableDtoGeneration + @EnableImplicitApi
│   └── module/{Code}ModuleProvider.java      # code/name/apiPrefixes(/api/v1/{code}/**)+菜单树(每实体 CRUD 四按钮)
├── src/main/dto/{Entity}.dto                 # Input(全限定名校验注解) + Specification(超级 QBE)
└── src/main/resources/sql/
    ├── {code}-schema.sql                     # identity(100,1) 主键、business_key 唯一约束、外键、中间表
    └── {code}-data.sql                       # 可选种子数据
```

## 使用

```bash
cd codegen
npm install          # 首次
npm run gen:rbac     # 用 schemas/rbac.ts(课程管理示例) 生成到 out/
# 或自定义
npm run gen -- -s schemas/your-schema.ts -o ../modules/your-module
```

Schema 以 TypeScript 编写并默认导出（享受编辑器类型提示），格式见 `src/schema.ts`。要点：

| 配置 | 说明 |
|---|---|
| `project.moduleCode` / `moduleName` | 模块编码(决定包名 `com.jezetek.modules.{code}`、前缀 `/api/v1/{code}`、权限点 `{code}:...`)与中文显示名 |
| `project.groupId/version/rootProject/jimmerVersion/javaVersion` | Maven 坐标与版本(与根 pom 对齐) |
| `entities[].props` | `id` / `scalar`(key、nullable、length、validation) / `manyToOne`(onDissociate) / `oneToMany`(mappedBy, orderedBy) / `manyToMany`(joinTable 拥有方 / mappedBy 反向方) |
| `entities[].icon` / `label` | 菜单图标(tdesign icons)与中文显示名(菜单名/动作名) |
| `entities[].search` | 超级 QBE 条件：keyword(多列 OR 模糊)、eq、flatLike(关联属性模糊) |
| `seeds` | 可选种子数据：实体名 -> 原样拼进 {code}-data.sql 的 INSERT 行 |

## 模块化红线(生成物已遵守，手改时勿破坏)

- pom 的 jimmer-apt 必须在 `default-compile`/`default-testCompile` 执行级覆盖(根 pom 的 lombok-only 会遮蔽插件级配置)
- 模块间禁外键(规避跨 jar 脚本顺序)；表名避开 SQL 保留字(USER/ROLE 等，用 sys_ 前缀)
- 所有 REST 以 `/api/v1` 开头；权限点 `{code}:{实体}:{动作}`；菜单节点及祖先显式 `.roles(...)` 防路由断链
- SQL 文件命名 `{code}-schema.sql`/`{code}-data.sql`，由 app 的 yml 通配加载
