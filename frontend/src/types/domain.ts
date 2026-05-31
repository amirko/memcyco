export type ShortLinkStatus = 'active' | 'expired' | 'click_exhausted';

export interface ShortLink {
  id: number;
  shortCode: string;
  shortUrl: string;
  originalUrl: string;
  strategy: string;
  expiresAt: string | null;
  maxClicks: number | null;
  totalClicks: number;
  status: ShortLinkStatus;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ShortLinkPayload {
  originalUrl: string;
  customAlias?: string;
  strategy: string;
  expiresAt?: string | null;
  maxClicks?: number | null;
  tags: string[];
  parameters?: Record<string, unknown>;
}

export interface StrategyParameter {
  name: string;
  type: string;
  required: boolean;
  description: string;
}

export interface Strategy {
  name: string;
  label: string;
  description: string;
  readOnly: boolean;
  parameters: StrategyParameter[];
}

export interface MetricPoint {
  label: string;
  count: number;
}

export interface Analytics {
  totalClicks: number;
  timeSeries: MetricPoint[];
  referers: MetricPoint[];
  userAgents: MetricPoint[];
}
