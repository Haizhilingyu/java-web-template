import { defineStore } from 'pinia';

import { clearToken, fetchUserInfo, getToken, login as loginApi, logout as logoutApi } from '@/api/auth';
import type { UserInfo } from '@/types/interface';

const InitUserInfo: UserInfo = {
  name: '', // 用户名，用于展示在页面右上角头像处
  roles: [],
  perms: [], // 按钮级权限标识，超级管理员为 ['*:*:*']
};

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: { ...InitUserInfo },
  }),
  getters: {
    roles: (state) => {
      return state.userInfo?.roles;
    },
    perms: (state) => {
      return state.userInfo?.perms;
    },
  },
  actions: {
    /** 账号密码登录，成功后令牌由 api/auth 写入 localStorage */
    async login(form: { username?: string; password?: string }) {
      await loginApi(form.username ?? '', form.password ?? '');
      this.token = getToken();
    },
    /** 拉取用户信息并缓存，已缓存时直接返回(路由守卫每次导航都会调用) */
    async getUserInfo() {
      if (this.userInfo.name) {
        return;
      }
      const profile = await fetchUserInfo();
      this.userInfo = {
        name: profile.user.nickname || profile.user.username,
        roles: profile.roles,
        perms: profile.perms,
      };
    },
    async logout() {
      try {
        await logoutApi();
      } finally {
        this.token = '';
        this.userInfo = { ...InitUserInfo };
        clearToken();
      }
    },
  },
});
