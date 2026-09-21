import type { Component } from 'vue';
import type { RouteMeta } from 'vue-router';

/** 动态路由条目(原 permissionModel，随模板遗留清理内联至此) */
export interface RouteItem {
  path: string;
  name: string;
  component?: Component | string;
  components?: Component;
  redirect?: string;
  meta: RouteMeta;
  children?: Array<RouteItem>;
}

/**
 * 认证接口直连封装(不走 jimmer 生成的客户端)：
 * login/getInfo/getRouters/logout + Bearer 令牌存取。
 * 开发模式走 vite 代理，发行模式同源直连，与 api/jimmer.ts 一致
 */
const BASE = import.meta.env.VITE_API_BASE ?? '';

const TOKEN_KEY = 'token';

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? '';
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(uri: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const response = await fetch(`${BASE}${uri}`, {
    ...init,
    headers: {
      'content-type': 'application/json;charset=UTF-8',
      ...(token ? { authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  });
  if (!response.ok) {
    let message = `请求失败 (HTTP ${response.status})`;
    try {
      const error = await response.json();
      if (error?.message) {
        message = error.message;
      }
    } catch {
      // 非 JSON 错误体，使用默认提示
    }
    throw new Error(message);
  }
  const text = await response.text();
  return (text.length === 0 ? null : JSON.parse(text)) as T;
}

export interface UserProfile {
  user: {
    id: number;
    username: string;
    nickname: string;
  };
  roles: Array<string>;
  perms: Array<string>;
}

export async function login(username: string, password: string): Promise<void> {
  const result = await request<{ token: string }>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  setToken(result.token);
}

export async function fetchUserInfo(): Promise<UserProfile> {
  return request<UserProfile>('/api/v1/auth/getInfo');
}

/** 后端按角色下发的动态菜单树，component 为字符串(LAYOUT 或 pages 下的组件路径) */
export async function fetchRouters(): Promise<Array<RouteItem>> {
  return request<Array<RouteItem>>('/api/v1/auth/getRouters');
}

export async function logout(): Promise<void> {
  try {
    await request<void>('/api/v1/auth/logout', { method: 'POST' });
  } finally {
    clearToken();
  }
}
