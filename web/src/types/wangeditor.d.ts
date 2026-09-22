// @wangeditor/editor-for-vue 的 package.json exports 未正确指向类型文件，
// vue-tsc 无法解析声明，这里手动补模块声明(工单08)
declare module '@wangeditor/editor-for-vue' {
  import type { Component } from 'vue';

  export const Editor: Component;
  export const Toolbar: Component;
}
