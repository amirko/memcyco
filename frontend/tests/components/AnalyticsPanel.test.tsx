import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import AnalyticsPanel from '../../src/components/AnalyticsPanel';
import { analytics, shortLink } from '../test/fixtures';

describe('AnalyticsPanel', () => {
  it('asks the user to select a link when nothing is selected', () => {
    render(<AnalyticsPanel link={null} analytics={null} />);

    expect(screen.getByRole('heading', { name: /select a link/i })).toBeInTheDocument();
    expect(screen.getByText(/choose a short code/i)).toBeInTheDocument();
  });

  it('renders metrics and chart rows for a selected link', () => {
    const link = shortLink({ maxClicks: 20 });

    render(<AnalyticsPanel link={link} analytics={analytics()} />);

    expect(screen.getByRole('heading', { name: link.shortCode })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /open/i })).toHaveAttribute('href', link.shortUrl);
    expect(screen.getByText('Total clicks')).toBeInTheDocument();
    expect(screen.getByText('Max clicks')).toBeInTheDocument();
    expect(screen.getByText('2026-05-30')).toBeInTheDocument();
    expect(screen.getByText('https://news.example')).toBeInTheDocument();
    expect(screen.getByText('Firefox')).toBeInTheDocument();
  });

  it('falls back to link click count and empty chart states before analytics load', () => {
    render(<AnalyticsPanel link={shortLink({ totalClicks: 2, maxClicks: null })} analytics={null} />);

    expect(screen.getByText('No limit')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getAllByText(/no data yet/i)).toHaveLength(3);
  });
});
