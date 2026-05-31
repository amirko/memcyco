import type { Analytics, ShortLink, ShortLinkPayload, Strategy } from '../types/domain';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers
    },
    ...init
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const body = await response.json();
      message = body.message ?? message;
    } catch {
      // Keep the status message when the response has no JSON body.
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export const api = {
  listLinks: () => request<ShortLink[]>('/api/links'),
  createLink: (payload: ShortLinkPayload) =>
    request<ShortLink>('/api/links', { method: 'POST', body: JSON.stringify(payload) }),
  updateLink: (id: number, payload: ShortLinkPayload) =>
    request<ShortLink>(`/api/links/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteLink: (id: number) => request<void>(`/api/links/${id}`, { method: 'DELETE' }),
  analytics: (id: number) => request<Analytics>(`/api/links/${id}/analytics`),
  strategies: () => request<Strategy[]>('/api/strategies')
};
