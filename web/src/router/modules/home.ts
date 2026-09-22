import type { RouteRecordRaw } from 'vue-router';

import LAYOUT from '@/layouts/index.vue';

// 首页为固定路由，不进 sys_menu(工单09)：'/' 直接渲染首页
const homeRoutes: Array<RouteRecordRaw> = [
  {
    path: '/home',
    name: 'home',
    component: LAYOUT,
    redirect: '/home/index',
    meta: { title: { zh_CN: '首页', en_US: 'Home' } },
    children: [
      {
        path: 'index',
        name: 'homeIndex',
        component: () => import('@/pages/home/index.vue'),
        meta: { title: { zh_CN: '首页', en_US: 'Home' } },
      },
    ],
  },
];

export default homeRoutes;
