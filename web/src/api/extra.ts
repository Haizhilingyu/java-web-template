import { getToken } from '@/api/auth';

/**
 * 工单05-09 新增端点的直连封装(不走 jimmer 生成的客户端)：
 * 重置密码/分配用户/头像/首页。与 api/auth.ts 同款鉴权与代理约定，
 * 免去为这些端点重新 gen:api
 */
const BASE = import.meta.env.VITE_API_BASE ?? '';

async function jsonRequest<T>(uri: string, init?: RequestInit): Promise<T> {
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
      // 非 JSON 错误体，用默认提示
    }
    throw new Error(message);
  }
  const text = await response.text();
  return (text.length === 0 ? null : JSON.parse(text)) as T;
}

function authHeaders(): Record<string, string> {
  const token = getToken();
  return token ? { authorization: `Bearer ${token}` } : {};
}

// ---------------------------------------------------------------- 工单05 重置密码

export async function resetUserPassword(id: number, newPassword: string): Promise<void> {
  await jsonRequest<void>(`/api/v1/user/${id}/password`, {
    method: 'PUT',
    body: JSON.stringify({ newPassword }),
  });
}

// ---------------------------------------------------------------- 工单06 分配用户

export interface RoleUserRow {
  id: number;
  username: string;
  nickname?: string;
  enabled: boolean;
  dept?: { name: string };
  posts?: Array<{ name: string }>;
  createdTime?: string;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
}

export async function fetchRoleUsers(
  roleId: number,
  pageIndex: number,
  pageSize: number,
  keyword?: string,
): Promise<PageResult<RoleUserRow>> {
  const params = new URLSearchParams({
    pageIndex: String(pageIndex),
    pageSize: String(pageSize),
    sortCode: 'username asc',
  });
  if (keyword) {
    params.set('keyword', keyword);
  }
  return jsonRequest<PageResult<RoleUserRow>>(`/api/v1/role/${roleId}/users?${params.toString()}`);
}

export async function assignRoleUsers(roleId: number, userIds: Array<number>): Promise<void> {
  await jsonRequest<void>(`/api/v1/role/${roleId}/users`, {
    method: 'POST',
    body: JSON.stringify({ userIds }),
  });
}

export async function unassignRoleUsers(roleId: number, userIds: Array<number>): Promise<void> {
  await jsonRequest<void>(`/api/v1/role/${roleId}/users`, {
    method: 'DELETE',
    body: JSON.stringify({ userIds }),
  });
}

// ---------------------------------------------------------------- 工单07 头像

export interface AvatarInfo {
  hasAvatar: boolean;
  url?: string;
}

/** 拉取当前用户头像 blob 转 objectURL；无头像返回 hasAvatar=false */
export async function fetchAvatar(): Promise<AvatarInfo> {
  const token = getToken();
  const response = await fetch(`${BASE}/api/v1/auth/avatar`, {
    headers: token ? { authorization: `Bearer ${token}` } : {},
  });
  if (response.status === 404) {
    return { hasAvatar: false };
  }
  if (!response.ok) {
    throw new Error(`头像获取失败 (HTTP ${response.status})`);
  }
  const blob = await response.blob();
  return { hasAvatar: true, url: URL.createObjectURL(blob) };
}

export async function uploadAvatar(file: File): Promise<void> {
  const body = new FormData();
  body.append('file', file);
  const response = await fetch(`${BASE}/api/v1/auth/avatar`, {
    method: 'POST',
    headers: authHeaders(),
    body,
  });
  if (!response.ok) {
    let message = `上传失败 (HTTP ${response.status})`;
    try {
      const error = await response.json();
      if (error?.message) {
        message = error.message;
      }
    } catch {
      // 非 JSON 错误体
    }
    throw new Error(message);
  }
}

// ---------------------------------------------------------------- 工单09 首页

export interface HomeSummary {
  userCount: number;
  roleCount: number;
  todayLoginSuccess: number;
  operLogTotal: number;
}

export interface HomeNoticeBrief {
  id: number;
  noticeTitle: string;
  noticeType: string;
  createdTime?: string;
}

export interface HomeNoticeDetail extends HomeNoticeBrief {
  content?: string;
}

export async function fetchHomeSummary(): Promise<HomeSummary> {
  return jsonRequest<HomeSummary>('/api/v1/home/summary');
}

export async function fetchHomeNotices(): Promise<Array<HomeNoticeBrief>> {
  return jsonRequest<Array<HomeNoticeBrief>>('/api/v1/home/notices');
}

export async function fetchHomeNotice(id: number): Promise<HomeNoticeDetail> {
  return jsonRequest<HomeNoticeDetail>(`/api/v1/home/notices/${id}`);
}
