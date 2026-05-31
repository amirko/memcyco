import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../src/api/client';

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init
  });
}

describe('api client', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('loads links from the backend', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse([{ id: 1 }]));

    await expect(api.listLinks()).resolves.toEqual([{ id: 1 }]);
    expect(fetchMock).toHaveBeenCalledWith('/api/links', {
      headers: { 'Content-Type': 'application/json' }
    });
  });

  it('posts create payloads as JSON', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ id: 2 }));
    const payload = {
      originalUrl: 'https://example.com',
      strategy: 'random_base62',
      tags: ['launch'],
      parameters: {}
    };

    await expect(api.createLink(payload)).resolves.toEqual({ id: 2 });
    expect(fetchMock).toHaveBeenCalledWith('/api/links', {
      method: 'POST',
      body: JSON.stringify(payload),
      headers: { 'Content-Type': 'application/json' }
    });
  });

  it('returns undefined for delete responses with no content', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }));

    await expect(api.deleteLink(4)).resolves.toBeUndefined();
  });

  it('uses backend error messages when available', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ message: 'Alias already exists' }, { status: 409 }));

    await expect(api.createLink({ originalUrl: 'bad', strategy: 'random_base62', tags: [] })).rejects.toThrow(
      'Alias already exists'
    );
  });

  it('falls back to status text when an error body is not JSON', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('nope', { status: 500 }));

    await expect(api.analytics(99)).rejects.toThrow('Request failed with status 500');
  });
});
