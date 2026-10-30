import { API_BASE_URL } from '../config/env.js';

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

export async function apiRequest(path, { json, ...options } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    ...(json === undefined ? {} : { body: JSON.stringify(json) }),
    headers: {
      Accept: 'application/json',
      ...(json === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...options.headers,
    },
  });
  if (!response.ok) {
    let message = `Request failed (${response.status}). Please try again.`;
    let code;
    if ([400, 404, 409].includes(response.status)) {
      const body = await response.json().catch(() => null);
      if (typeof body?.message === 'string' && body.message.trim())
        message = body.message;
      code = body?.code;
    }
    throw new ApiError(message, response.status, code);
  }
  return response.status === 204 ? null : response.json();
}
