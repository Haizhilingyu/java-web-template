import { ref, type Ref } from 'vue';

import { api } from '@/api/jimmer';

/**
 * 字典 hook：按字典编码拉取启用条目并按编码缓存(模块级，跨组件共享)，
 * 后端不加缓存(单机 H2 无收益)。
 *
 * const { items, label } = useDict('sys_yes_no');
 * label('Y') === '是'
 */
export interface DictItem {
  label: string;
  value: string;
}

const cache = new Map<string, Promise<DictItem[]>>();

function loadDict(type: string): Promise<DictItem[]> {
  let promise = cache.get(type);
  if (!promise) {
    promise = api.dictDataService.findEnabledByType({ type }).then((list) =>
      list.map((item) => ({ label: item.label, value: item.value })),
    );
    cache.set(type, promise);
    promise.catch(() => cache.delete(type));
  }
  return promise;
}

export function useDict(type: string): {
  items: Ref<DictItem[]>;
  label: (value: string | number | undefined | null) => string;
} {
  const items = ref<DictItem[]>([]);
  loadDict(type).then((list) => {
    items.value = list;
  });

  const label = (value: string | number | undefined | null): string => {
    if (value === undefined || value === null || value === '') {
      return '-';
    }
    const hit = items.value.find((item) => item.value === String(value));
    return hit?.label ?? String(value);
  };

  return { items, label };
}

/** 清空字典缓存：字典管理页保存/删除后调用，让各页面重新拉取 */
export function clearDictCache(type?: string): void {
  if (type) {
    cache.delete(type);
  } else {
    cache.clear();
  }
}
