# 08: 公告富文本

**What to build:** notice 页公告内容 textarea 替换为 wangEditor（`@wangeditor/editor` + `@wangeditor/editor-for-vue`）；封装"富文本查看"组件：DOMPurify 清洗后 v-html，本工单先在公告查看处使用（工单 09 首页公告弹窗复用）。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 公告可编辑富文本（加粗/列表/链接等），保存后管理端与查看端回显一致
- [x] 含 script/onerror 的内容在查看端被清洗、不执行
- [x] vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-22 完成。

- 落地内容：引入 @wangeditor/editor + @wangeditor/editor-for-vue@next(5.1.12，Vue3 版)+dompurify。封装 components/rich-text/rich-editor.vue（v-model HTML；两个坑：prop 不可直接 v-model 需计算属性中转；wrapper 的创建事件名是 onCreated 且 config.onChange 在部分场景不触发，改在 handleCreated 里 editor.on(change) 直订补齐同步）与 rich-viewer.vue（DOMPurify 清洗 FORBID script/style/iframe 与 onerror 等后 v-html）。notice 页 textarea 换 RichEditor，查看弹窗用 RichViewer。
- 浏览器冒烟：编辑器经真实 API 写入→v-model 同步→保存 PUT 带 <p>ABC</p>→查看端回显一致；含 script/onerror 的公告在首页弹窗渲染时脚本被清洗不执行 ✓。
- 备注：@wangeditor/editor-for-vue 的 exports 未正确指向类型文件，补 src/types/wangeditor.d.ts 模块声明保 vue-tsc 通过。
