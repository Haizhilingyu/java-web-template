import cloneDeep from 'lodash/cloneDeep';
import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';

import router, { fixedRouterList, homepageRouterList } from '@/router';
import { store } from '@/store';

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    whiteListRouters: ['/login'],
    routers: [] as Array<RouteRecordRaw>,
    removeRoutes: [] as Array<RouteRecordRaw>,
    asyncRoutes: [] as Array<RouteRecordRaw>,
    routesInited: false,
  }),
  actions: {
    async initRoutes() {
      const accessedRouters = this.asyncRoutes;

      // 在菜单展示全部路由
      this.routers = cloneDeep([...homepageRouterList, ...accessedRouters, ...fixedRouterList]);
    },
    async buildAsyncRoutes() {
      // 模板原先通过 mock 接口(/get-menu-list)动态下发路由；
      // 当前路由全部静态定义(见 router/modules)，接入真实登录后再恢复动态路由。
      // 注意: 路由守卫以本方法是否已执行过为放行条件(见 src/permission.ts 的 routesInited)，
      // 不能再依赖 asyncRoutes 是否为空，否则守卫会无限递归
      await this.initRoutes();
      this.routesInited = true;
      return this.asyncRoutes;
    },
    async restoreRoutes() {
      // 不需要在此额外调用initRoutes更新侧边导肮内容，在登录后asyncRoutes为空会调用
      this.asyncRoutes.forEach((item: RouteRecordRaw) => {
        if (item.name) {
          router.removeRoute(item.name);
        }
      });
      this.asyncRoutes = [];
      this.routesInited = false;
    },
  },
});

export function getPermissionStore() {
  return usePermissionStore(store);
}
