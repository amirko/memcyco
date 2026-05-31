import type { Analytics, ShortLink, Strategy } from '../../src/types/domain';

export const strategies: Strategy[] = [
  {
    name: 'random_base62',
    label: 'Random Base62',
    description: 'Generates a random seven-character code.',
    readOnly: false,
    parameters: []
  },
  {
    name: 'hash_based',
    label: 'Hash based',
    description: 'Uses a deterministic hash from the destination URL.',
    readOnly: true,
    parameters: []
  }
];

export function shortLink(overrides: Partial<ShortLink> = {}): ShortLink {
  return {
    id: 1,
    shortCode: 'abc123',
    shortUrl: 'http://localhost:8080/abc123',
    originalUrl: 'https://example.com/campaign',
    strategy: 'random_base62',
    expiresAt: null,
    maxClicks: null,
    totalClicks: 5,
    status: 'active',
    tags: ['paid', 'q2'],
    createdAt: '2026-05-30T12:00:00Z',
    updatedAt: '2026-05-30T12:00:00Z',
    ...overrides
  };
}

export function analytics(overrides: Partial<Analytics> = {}): Analytics {
  return {
    totalClicks: 7,
    timeSeries: [{ label: '2026-05-30', count: 4 }],
    referers: [{ label: 'https://news.example', count: 3 }],
    userAgents: [{ label: 'Firefox', count: 2 }],
    ...overrides
  };
}
