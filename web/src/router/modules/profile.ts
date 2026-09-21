import type { RouteRecordRaw } from 'vue-router';

import LAYOUT from '@/layouts/index.vue';

// 个人中心为固定路由，不进 sys_menu
const profileRoutes: Array<RouteRecordRaw> = [
  {
    path: '/user/profile',
    name: 'userProfile',
    component: LAYOUT,
    redirect: '/user/profile/index',
    meta: { title: { zh_CN: '个人中心', en_US: 'Profile' } },
    children: [
      {
        path: 'index',
        name: 'userProfileIndex',
        component: () => import('@/pages/profile/index.vue'),
        meta: { title: { zh_CN: '个人中心', en_US: 'Profile' } },
      },
    ],
  },
];

export default profileRoutes;
