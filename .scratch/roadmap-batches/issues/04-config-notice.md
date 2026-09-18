# 04: 参数配置与公告

**What to build:** 两个管理页面：参数配置（全局键值，configKey 唯一，提供按 key 查询接口；无模板内置消费点，不种 captchaEnabled 等无人读取的键）；公告管理（标题/类型/纯文本内容 textarea/状态，类型标签用 sys_notice_type 字典渲染——依赖工单 03 的字典与 useDict）。详见 `docs/ROADMAP.md` 批次2。

**Blocked by:** 03（字典管理——公告类型标签消费字典）

**Status:** ready-for-agent

- [ ] 参数配置 CRUD 正常，configKey 唯一性校验，权限点 `system:config:*`
- [ ] 按 key 查询接口返回参数值
- [ ] 公告 CRUD 正常，类型标签经字典渲染，权限点 `system:notice:*`
- [ ] ConfigServiceTest、NoticeServiceTest 全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
