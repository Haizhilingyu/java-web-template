import { getToken } from '@/api/auth';

/**
 * 文件下载/上传直连封装(工单03)：Excel 导入导出这类二进制端点
 * 不走 jimmer 生成的客户端，与 api/auth.ts 一致的鉴权方式。
 * 开发模式走 vite 代理，发行模式同源直连
 */
const BASE = import.meta.env.VITE_API_BASE ?? '';

function authHeaders(extra?: Record<string, string>): Record<string, string> {
  const token = getToken();
  return {
    ...(token ? { authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

/** 触发浏览器保存响应体为文件 */
export async function downloadFile(
  uri: string,
  params?: Record<string, string | number | boolean | undefined>,
): Promise<void> {
  const query = params
    ? Object.entries(params)
        .filter(([, value]) => value !== undefined && value !== '')
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
        .join('&')
    : '';
  const response = await fetch(`${BASE}${uri}${query ? `?${query}` : ''}`, {
    headers: authHeaders(),
  });
  if (!response.ok) {
    let message = `下载失败 (HTTP ${response.status})`;
    try {
      const error = await response.json();
      if (error?.message) {
        message = error.message;
      }
    } catch {
      // 二进制错误体无 JSON，用默认提示
    }
    throw new Error(message);
  }
  const blob = await response.blob();
  const disposition = response.headers.get('content-disposition') ?? '';
  const star = /filename\*=UTF-8''([^;]+)/.exec(disposition);
  const plain = /filename="?([^";]+)"?/.exec(disposition);
  const filename = star
    ? decodeURIComponent(star[1])
    : (plain ? plain[1] : 'download.xlsx');
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export interface ImportRowFailure {
  rowNum: number;
  reason: string;
}

export interface ImportResult {
  total: number;
  successCount: number;
  failures: ImportRowFailure[];
}

/** multipart 上传，后端返回 JSON 结果(导入回显用) */
export async function uploadForJson<T>(uri: string, file: File, fieldName = 'file'): Promise<T> {
  const body = new FormData();
  body.append(fieldName, file);
  const response = await fetch(`${BASE}${uri}`, {
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
  return (await response.json()) as T;
}
