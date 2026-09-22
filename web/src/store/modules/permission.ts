import cloneDeep from 'lodash/cloneDeep';
import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';

import { fetchRouters } from '@/api/auth';
import type { RouteItem } from '@/api/auth';
import router, { fixedRouterList, homepageRouterList } from '@/router';
import { PAGE_NOT_FOUND_ROUTE } from '@/utils/route/constant';
import { store } from '@/store';
import { transformObjectToRoute } from '@/utils/route';

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
      // 登录后从后端 /auth/getRouters 拉取按角色过滤的菜单树，
      // component 是字符串(LAYOUT / pages 下组件路径)，转换成真实路由组件。
      // '/' 固定指向首页(router/index.ts)，不再按动态菜单重定向
      const remoteRoutes = await fetchRouters();
      this.asyncRoutes = (transformObjectToRoute(remoteRoutes) as unknown as Array<RouteRecordRaw>)
        // 404 已在 router/index.ts 静态注册为组件路由，滤掉模板 transform 返回的 redirect 版
        .filter((route) => route.name !== PAGE_NOT_FOUND_ROUTE.name);
      await this.initRoutes();
      // 注意: 路由守卫以 routesInited 为放行条件(见 src/permission.ts)，
      // 不能改用 asyncRoutes 是否为空判断，否则守卫会无限递归
      this.routesInited = true;
      return this.asyncRoutes;
    },
    async restoreRoutes() {
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
