import type { Directive } from 'vue';

import { useUserStore } from '@/store/modules/user';

/**
 * 按钮级权限指令：无对应权限标识时直接移除元素。
 *
 * 用法：v-permission="'system:user:add'" 或 v-permission="['system:user:add', 'system:user:edit']"
 * (传数组表示任一命中即可)。超级管理员权限为 *:*:* 直通
 */
export const permission: Directive<HTMLElement, string | Array<string>> = {
  mounted(el, binding) {
    const needed = Array.isArray(binding.value) ? binding.value : [binding.value];
    const { perms = [] } = useUserStore().userInfo;
    const allowed = perms.includes('*:*:*') || needed.some((item) => perms.includes(item));
    if (!allowed) {
      el.parentNode?.removeChild(el);
    }
  },
};
