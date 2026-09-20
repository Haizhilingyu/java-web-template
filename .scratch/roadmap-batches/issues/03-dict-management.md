# 03: 字典管理

**What to build:** 字典的"类型 + 数据"两级管理页（主表点击进入数据明细）；开放接口按类型取**启用**的字典数据；前端 `useDict` hook 按 type 拉取并缓存（同 type 不重复请求）、渲染字典标签。种子：sys_yes_no、sys_notice_type 等基础字典。后端不加缓存（决策已敲定）。详见 `docs/ROADMAP.md` 批次2。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 字典类型 CRUD 与字典数据 CRUD（含启用/停用）正常，权限点 `system:dict:*`
- [x] `GET /api/v1/dict/data/type/{type}` 只返回启用项
- [x] useDict 缓存生效；下拉/标签渲染正确
- [x] DictServiceTest 全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-20 完成。

- 落地内容：`sys_dict_type`（业务键 type 全局唯一）+ `sys_dict_data`（业务键 dictType+value；`value` 列名 `data_value` 避开 H2 保留字；`@ManyToOne` 因目标端有租户过滤器按 jimmer 要求 `@Nullable + inputNotNull=true`）；`/api/v1/dict/type` 与 `/api/v1/dict/data` 服务，`GET /dict/data/type/{type}` 仅返回启用条目（类型与条目都启用）、按 sortOrder 排序、登录即可访问；有条目的类型禁删；菜单"字典管理"（sortOrder 6，仅超管）。前端：字典页主表-明细二级布局（点行/点条目进入），`web/src/hooks/useDict.ts`（模块级 Map 缓存同 type 去重、`label()` 渲染、`clearDictCache` 供保存/删除后失效），hooks/index.ts 导出。种子：sys_yes_no（是/否）、sys_notice_type（通知/公告，工单04 消费）。
- 验证：system 56 测试全绿（DictServiceTest 9：类型/条目 CRUD、幂等更新、type 接口过滤禁用、按 dictTypeId+keyword 过滤、有条目禁删）；vue-tsc 零错误；冒烟（主表两字典→条目明细→新增"未知/U"→删除还原）。
- 已知问题（记录给工单11）：**bySuperQBE 的 sortCode 参数目前是装饰性的**——jimmer `fetchPage(page,size,factory)` 不读取 PageRequest 的 Sort，所有分页接口实际无 ORDER BY（小数据量下按插入序恰好表现正常）。属模板既有缺陷，将在工单11（codegen 同步）统一定模式修复。
