import cloneDeep from 'lodash/cloneDeep';
import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';

import { fetchRouters } from '@/api/auth';
import type { RouteItem } from '@/api/auth';
import router, { fixedRouterList, homepageRouterList } from '@/router';
import { PAGE_NOT_FOUND_ROUTE } from '@/utils/route/constant';
import { store } from '@/store';
import { transformObjectToRoute } from '@/utils/route';

/** 取菜单树首个可达页面路径，作为 '/' 的重定向目标 */
function firstLeafPath(routes: Array<RouteItem>): string {
  for (const route of routes) {
    const firstChild = route.children?.[0];
    if (firstChild?.path) {
      // 子路由是相对路径，需要拼上父级
      return firstChild.path.startsWith('/') ? firstChild.path : `${route.path}/${firstChild.path}`;
    }
    if (route.path) {
      return route.path;
    }
  }
  return '/system/user';
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    whiteListRouters: ['/login'],
    routers: [] as Array<RouteRecordRaw>,
    removeRoutes: [] as Array<RouteRecordRaw>,
    asyncRoutes: [] as Array<RouteRecordRaw>,
    routesInited: false,
    firstRoutePath: '/system/user',
  }),
  actions: {
    async initRoutes() {
      const accessedRouters = this.asyncRoutes;

      // 在菜单展示全部路由
      this.routers = cloneDeep([...homepageRouterList, ...accessedRouters, ...fixedRouterList]);
    },
    async buildAsyncRoutes() {
      // 登录后从后端 /auth/getRouters 拉取按角色过滤的菜单树，
      // component 是字符串(LAYOUT / pages 下组件路径)，转换成真实路由组件
      const remoteRoutes = await fetchRouters();
      this.firstRoutePath = firstLeafPath(remoteRoutes);
      // 同名路由 addRoute 会整体替换，刷新 '/' 的重定向目标
      router.addRoute({ path: '/', name: 'root', redirect: this.firstRoutePath });

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
      router.addRoute({ path: '/', name: 'root', redirect: this.firstRoutePath });
    },
  },
});

export function getPermissionStore() {
  return usePermissionStore(store);
}
