import { getToken } from '@/api/auth';

/**
 * 头像直连封装(工单07)：二进制流式读写不走 jimmer 生成的 JSON 客户端，
 * 与 api/auth.ts 同款鉴权与代理约定。其余工单端点(重置密码/分配用户/首页)
 * 已由 gen:api 生成，消费方直接用 api.userService/roleService/homeController
 */
const BASE = import.meta.env.VITE_API_BASE ?? '';

function authHeaders(): Record<string, string> {
  const token = getToken();
  return token ? { authorization: `Bearer ${token}` } : {};
}

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
