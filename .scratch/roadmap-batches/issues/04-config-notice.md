# 04: 参数配置与公告

**What to build:** 两个管理页面：参数配置（全局键值，configKey 唯一，提供按 key 查询接口；无模板内置消费点，不种 captchaEnabled 等无人读取的键）；公告管理（标题/类型/纯文本内容 textarea/状态，类型标签用 sys_notice_type 字典渲染——依赖工单 03 的字典与 useDict）。详见 `docs/ROADMAP.md` 批次2。

**Blocked by:** 03（字典管理——公告类型标签消费字典）

**Status:** resolved

- [x] 参数配置 CRUD 正常，configKey 唯一性校验，权限点 `system:config:*`
- [x] 按 key 查询接口返回参数值
- [x] 公告 CRUD 正常，类型标签经字典渲染，权限点 `system:notice:*`
- [x] ConfigServiceTest、NoticeServiceTest 全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-20 完成。

- 落地内容：`sys_config`（业务键 configKey，幂等 upsert）+ `sys_notice`（content 用 CLOB 纯文本；无自然业务键，saveNotice 用 `NON_IDEMPOTENT_UPSERT` 模式——有 id 更新、无 id 插入）；`/api/v1/config`（含 configKey/{key} 开放接口）+ `/api/v1/notice`；菜单"参数配置"(sortOrder 7)/"公告管理"(8) 仅超管；前端 pages/system/{config,notice}，公告类型下拉与标签列经 useDict('sys_notice_type') 渲染（消费工单03 字典种子）。配置键不预置（决策），公告无种子。
- 验证：system 64 测试全绿（Config 4 + Notice 4：键幂等更新/按键查询/类型过滤/关键字/更新内容/删除）；vue-tsc 零错误；冒烟（公告新增全流程，类型下拉渲染字典项"通知/公告"，表格类型列显示"通知"；参数页加载+空态）。
