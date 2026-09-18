# 03: 字典管理

**What to build:** 字典的"类型 + 数据"两级管理页（主表点击进入数据明细）；开放接口按类型取**启用**的字典数据；前端 `useDict` hook 按 type 拉取并缓存（同 type 不重复请求）、渲染字典标签。种子：sys_yes_no、sys_notice_type 等基础字典。后端不加缓存（决策已敲定）。详见 `docs/ROADMAP.md` 批次2。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 字典类型 CRUD 与字典数据 CRUD（含启用/停用）正常，权限点 `system:dict:*`
- [ ] `GET /api/v1/dict/data/type/{type}` 只返回启用项
- [ ] useDict 缓存生效；下拉/标签渲染正确
- [ ] DictServiceTest 全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
