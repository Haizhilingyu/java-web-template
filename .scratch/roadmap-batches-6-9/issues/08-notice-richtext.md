# 08: 公告富文本

**What to build:** notice 页公告内容 textarea 替换为 wangEditor（`@wangeditor/editor` + `@wangeditor/editor-for-vue`）；封装"富文本查看"组件：DOMPurify 清洗后 v-html，本工单先在公告查看处使用（工单 09 首页公告弹窗复用）。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 公告可编辑富文本（加粗/列表/链接等），保存后管理端与查看端回显一致
- [ ] 含 script/onerror 的内容在查看端被清洗、不执行
- [ ] vue-tsc 零错误；浏览器冒烟；一个 commit
