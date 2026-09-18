# 11: codegen 同步

**What to build:** Node/TS 代码生成器（`npm run gen:rbac`）的产物从旧结构改为完整模块骨架：entity/repository/service/ModuleProvider/SQL（schema+data）/pom（jimmer-apt 覆盖写法照抄 modules/system），路径与接口遵循 `/api/v1` 前缀约定与 AGENTS.md"模块化机制"清单。产物仍仅作参考不直接使用。README 同步更新。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 生成产物与现有 modules/system、modules/job 结构一致，复制进工程即可被 scanBasePackages 装配
- [ ] 生成的 pom 含 jimmer-apt 执行级覆盖，与红线写法一致
- [ ] 生成路径/权限点符合 `/api/v1/{code}` 与 `<code>:实体:动作` 约定
- [ ] codegen/README.md 与实际产物一致；一个 commit
