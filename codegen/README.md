# jimmer-codegen

根据实体模型 Schema（TypeScript 定义）一键生成整套 jimmer 后端模块：

- **model**：jimmer 实体接口（`@Entity`/`@Table`/`@Key`/关联注解/`@IdView`）+ `BaseEntity`/`TenantAware` 公共基类
- **repository**：`AbstractJavaRepository` 超级 QBE 分页查询 + 单业务键查询 + 树形根节点查询
- **runtime**：租户提供者、时间戳/租户 DraftInterceptor、全局 TenantFilter
- **service**：REST 服务（web 注解直挂 service）、`.dto` 文件（Input + Bean Validation 校验 + Specification）、`@EnableDtoGeneration` 开关
- **sql**：`h2-schema.sql` 建表脚本（identity 主键、business_key 唯一约束、外键、中间表）+ 可选 `h2-data.sql` 种子数据
- **Maven pom**：core 聚合模块 + 四个子模块的 pom（含 jimmer-apt 处理器在执行级覆盖 lombok 的配置）

生成代码的约定与本项目 `core/` 目录手写实现完全一致，可直接对照。

## 使用

```bash
cd codegen
npm install          # 首次
npm run gen:rbac     # 用 schemas/rbac.ts 生成到 out/
# 或自定义
npm run gen -- -s schemas/your-schema.ts -o ../core-generated
```

Schema 以 TypeScript 编写并默认导出（享受编辑器类型提示），格式见 `src/schema.ts` 的类型定义与 `schemas/rbac.ts` 示例。要点：

| 配置 | 说明 |
|---|---|
| `project` | java 包名、Maven 坐标、jimmer/Spring Boot/Jackson 版本、根工程坐标、默认租户配置项 |
| `entities[].props` | `id` / `scalar`(key、nullable、length、validation) / `manyToOne`(onDissociate) / `oneToMany`(mappedBy, orderedBy) / `manyToMany`(joinTable 拥有方 / mappedBy 反向方) |
| `entities[].search` | 超级 QBE 条件：keyword(多列 OR 模糊)、eq、flatLike(关联属性模糊) |
| `entities[].tenantAware` | 多租户隔离（生成 TenantFilter 等基础设施与 tenant 列） |
| `seeds` | 原样拼入 h2-data.sql 的 INSERT 语句行 |

## 注意事项

- DTO 校验注解须用全限定名（jimmer dto 语法要求），生成时会原样输出
- 表名需自行规避 SQL 保留字（如 `sys_user`）
- 生成的 `service` 模块依赖根 pom 提供 lombok 之外的编译环境与 `jimmer.version` 属性
