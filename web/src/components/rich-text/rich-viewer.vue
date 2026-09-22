<template>
  <!-- DOMPurify 清洗后才 v-html：script/onerror 等在查看端不执行(工单08) -->
  <div class="rich-viewer" v-html="sanitized"></div>
</template>

<script setup lang="ts">
import DOMPurify from 'dompurify';
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    content: string;
  }>(),
  { content: '' },
);

const sanitized = computed(() =>
  DOMPurify.sanitize(props.content ?? '', {
    // 默认已剥离全部事件属性与 javascript: URL，这里再显式禁危险标签
    FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed'],
  }),
);
</script>

<style lang="less" scoped>
.rich-viewer {
  line-height: 1.7;
  word-break: break-word;

  :deep(img) {
    max-width: 100%;
  }

  :deep(a) {
    color: var(--td-brand-color);
  }
}
</style>
