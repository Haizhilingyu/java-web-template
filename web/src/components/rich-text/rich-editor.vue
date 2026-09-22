<template>
  <div class="rich-editor">
    <Toolbar class="rich-editor-toolbar" :editor="editorRef" :default-config="toolbarConfig" />
    <Editor
      class="rich-editor-content"
      v-model="innerValue"
      :default-config="editorConfig"
      :style="{ height: props.height }"
      @onCreated="handleCreated"
    />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import { computed, onBeforeUnmount, ref, watch } from 'vue';

// 富文本编辑器(工单08)：封装 wangEditor，v-model 双向绑定 HTML 内容。
// 查看侧必须经 RichViewer 的 DOMPurify 清洗后渲染
const props = withDefaults(
  defineProps<{
    modelValue: string;
    height?: string;
  }>(),
  { height: '300px' },
);

const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>();

// prop 不可直接 v-model，用计算属性中转
const innerValue = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
});

const editorRef = ref();
// 工具栏按钮默认即可覆盖 加粗/列表/链接 等基础排版
const toolbarConfig = {};
const editorConfig = { placeholder: '请输入内容...' };

watch(
  () => props.modelValue,
  (value) => {
    // 外部清空时同步到编辑器
    if (value === '' && editorRef.value) {
      editorRef.value.clear();
    }
  },
);

function handleCreated(editor: any) {
  editorRef.value = editor;
  // 除 v-model 外直订 changed 事件：某些环境下 config.onChange 不触发
  editor.on('change', (ed: any) => {
    emit('update:modelValue', ed.getHtml());
  });
}

onBeforeUnmount(() => {
  editorRef.value?.destroy();
});
</script>

<style lang="less" scoped>
.rich-editor {
  border: 1px solid var(--td-component-border);
  border-radius: 3px;
  z-index: 10;

  .rich-editor-toolbar {
    border-bottom: 1px solid var(--td-component-border);
  }

  .rich-editor-content {
    overflow-y: hidden;
  }
}
</style>
