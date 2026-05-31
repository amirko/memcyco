import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../src/App';
import { api } from '../src/api/client';
import { analytics, shortLink, strategies } from './test/fixtures';

vi.mock('../src/api/client', () => ({
  api: {
    listLinks: vi.fn(),
    strategies: vi.fn(),
    analytics: vi.fn(),
    createLink: vi.fn(),
    updateLink: vi.fn(),
    deleteLink: vi.fn()
  }
}));

const mockedApi = vi.mocked(api);

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedApi.listLinks.mockResolvedValue([shortLink()]);
    mockedApi.strategies.mockResolvedValue(strategies);
    mockedApi.analytics.mockResolvedValue(analytics());
    mockedApi.createLink.mockResolvedValue(shortLink({ id: 2, shortCode: 'newone' }));
    mockedApi.updateLink.mockResolvedValue(shortLink());
    mockedApi.deleteLink.mockResolvedValue(undefined);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('bootstraps links, strategies, and analytics', async () => {
    render(<App />);

    expect(screen.getByText(/loading links/i)).toBeInTheDocument();
    expect(await screen.findByRole('row', { name: /abc123/i })).toBeInTheDocument();
    expect(await screen.findByText('2026-05-30')).toBeInTheDocument();
    expect(mockedApi.listLinks).toHaveBeenCalledTimes(1);
    expect(mockedApi.strategies).toHaveBeenCalledTimes(1);
    expect(mockedApi.analytics).toHaveBeenCalledWith(1);
  });

  it('creates a link and refreshes the table', async () => {
    const user = userEvent.setup();
    mockedApi.listLinks.mockResolvedValueOnce([shortLink()]).mockResolvedValueOnce([
      shortLink(),
      shortLink({ id: 2, shortCode: 'newone', originalUrl: 'https://example.com/new' })
    ]);

    render(<App />);

    await screen.findByRole('row', { name: /abc123/i });
    await user.type(screen.getByLabelText(/destination url/i), 'https://example.com/new');
    await user.click(screen.getByRole('button', { name: /create short link/i }));

    await waitFor(() => expect(mockedApi.createLink).toHaveBeenCalled());
    expect(mockedApi.createLink).toHaveBeenCalledWith(
      expect.objectContaining({ originalUrl: 'https://example.com/new', strategy: 'random_base62' })
    );
    expect(await screen.findByRole('row', { name: /newone/i })).toBeInTheDocument();
  });

  it('loads analytics for the selected table row', async () => {
    const user = userEvent.setup();
    const first = shortLink({ id: 1, shortCode: 'first' });
    const second = shortLink({ id: 2, shortCode: 'second', originalUrl: 'https://example.com/second' });
    mockedApi.listLinks.mockResolvedValue([first, second]);
    mockedApi.analytics
      .mockResolvedValueOnce(analytics({ totalClicks: 3, timeSeries: [{ label: 'first-day', count: 3 }] }))
      .mockResolvedValueOnce(analytics({ totalClicks: 9, timeSeries: [{ label: 'second-day', count: 9 }] }));

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'first' })).toBeInTheDocument();

    await user.click(screen.getByRole('row', { name: /second/i }));

    expect(await screen.findByRole('heading', { name: 'second' })).toBeInTheDocument();
    expect(await screen.findByText('second-day')).toBeInTheDocument();
    expect(mockedApi.analytics).toHaveBeenLastCalledWith(2);
  });

  it('shows API errors', async () => {
    mockedApi.listLinks.mockRejectedValue(new Error('Backend is unavailable'));

    render(<App />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Backend is unavailable');
  });

  it('polls links and selected-link analytics', async () => {
    vi.useFakeTimers();
    mockedApi.listLinks
      .mockResolvedValueOnce([shortLink()])
      .mockResolvedValueOnce([shortLink({ totalClicks: 6 })]);
    mockedApi.analytics
      .mockResolvedValueOnce(analytics())
      .mockResolvedValueOnce(analytics({ totalClicks: 8, timeSeries: [{ label: '2026-05-31', count: 8 }] }));

    render(<App />);

    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByRole('row', { name: /abc123/i })).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });

    expect(mockedApi.listLinks).toHaveBeenCalledTimes(2);
    expect(mockedApi.analytics).toHaveBeenCalledTimes(2);
    expect(screen.getByText('2026-05-31')).toBeInTheDocument();
  });
});
