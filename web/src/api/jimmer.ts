import { Api } from './__generated';

/**
 * jimmer 生成的 API 客户端实例。
 *
 * 开发模式走 vite 代理(VITE_API_BASE=/api -> localhost:8080，见 vite.config.ts)；
 * 发行模式由后端托管前端页面，同源直连(VITE_API_BASE 为空)。
 */
const BASE = import.meta.env.VITE_API_BASE ?? '';

export const api = new Api(async ({ uri, method, headers, body }) => {
  const response = await fetch(`${BASE}${uri}`, {
    method,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    headers: {
      'content-type': 'application/json;charset=UTF-8',
      ...headers,
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
  return text.length === 0 ? null : JSON.parse(text);
});
