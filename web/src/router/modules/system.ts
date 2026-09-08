import { SettingIcon } from 'tdesign-icons-vue-next';
import { shallowRef } from 'vue';
import type { RouteRecordRaw } from 'vue-router';

import { LAYOUT } from '@/utils/route/constant';

export default [
  {
    path: '/system',
    component: LAYOUT,
    redirect: '/system/user',
    name: 'system',
    meta: {
      title: {
        zh_CN: '系统管理',
        en_US: 'System',
      },
      icon: shallowRef(SettingIcon),
      orderNo: 0,
    },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/pages/system/user/index.vue'),
        meta: {
          title: {
            zh_CN: '用户管理',
            en_US: 'User',
          },
        },
      },
      {
        path: 'role',
        name: 'SystemRole',
        component: () => import('@/pages/system/role/index.vue'),
        meta: {
          title: {
            zh_CN: '角色管理',
            en_US: 'Role',
          },
        },
      },
      {
        path: 'menu',
        name: 'SystemMenu',
        component: () => import('@/pages/system/menu/index.vue'),
        meta: {
          title: {
            zh_CN: '菜单管理',
            en_US: 'Menu',
          },
        },
      },
    ],
  },
] satisfies RouteRecordRaw[];
