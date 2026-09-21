# 11: codegen 同步

**What to build:** Node/TS 代码生成器（`npm run gen:rbac`）的产物从旧结构改为完整模块骨架：entity/repository/service/ModuleProvider/SQL（schema+data）/pom（jimmer-apt 覆盖写法照抄 modules/system），路径与接口遵循 `/api/v1` 前缀约定与 AGENTS.md"模块化机制"清单。产物仍仅作参考不直接使用。README 同步更新。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 生成产物与现有 modules/system、modules/job 结构一致，复制进工程即可被 scanBasePackages 装配
- [x] 生成的 pom 含 jimmer-apt 执行级覆盖，与红线写法一致
- [x] 生成路径/权限点符合 `/api/v1/{code}` 与 `<code>:实体:动作` 约定
- [x] codegen/README.md 与实际产物一致；一个 commit

## Comments

2026-09-21 完成。

- 重构：删除旧"core 四子模块"生成器(gen/{entity,repository,runtime,service,sql,pom}.ts)，新增自包含 `gen/module.ts` 按模块骨架产出 13 个文件(pom/model/repository/service(ModuleProvider+DtoGeneration)/dto/schema.sql+data.sql)；schema.ts 的 ProjectDef 改模块化字段(moduleCode/moduleName)，EntityDef 增 icon/label(中文菜单名与 @Log 动作名)；cli.ts 换新输出布局；schemas/rbac.ts 改为课程管理示例(教师/课程，含多对一/业务键/QBE)；README 重写为模块骨架说明+模块化红线清单。
- 验证：`npm run typecheck` 零错误；`npm run gen:rbac` 生成 13 文件，抽查 pom(jimmer-apt 执行级覆盖 4 处)、CourseService(/api/v1/course/course+course:course:* 权限点+@Log 中文动作)、ModuleProvider(apiPrefixes /api/v1/course/**+菜单树)、schema.sql(FK 指向目标表 id) 均符合约定。
