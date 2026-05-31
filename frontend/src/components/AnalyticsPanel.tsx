import type { Analytics, ShortLink } from '../types/domain';

interface Props {
  link: ShortLink | null;
  analytics: Analytics | null;
}

export default function AnalyticsPanel({ link, analytics }: Props) {
  if (!link) {
    return (
      <section className="panel analytics-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Analytics</p>
            <h2>Select a link</h2>
          </div>
        </div>
        <p className="muted">Choose a short code from the table to inspect click activity.</p>
      </section>
    );
  }

  return (
    <section className="panel analytics-panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Analytics</p>
          <h2>{link.shortCode}</h2>
        </div>
        <a className="short-url" href={link.shortUrl} target="_blank" rel="noreferrer">
          Open
        </a>
      </div>

      <div className="metric-row">
        <div>
          <span>Total clicks</span>
          <strong>{analytics?.totalClicks ?? link.totalClicks}</strong>
        </div>
        <div>
          <span>Max clicks</span>
          <strong>{link.maxClicks ?? 'No limit'}</strong>
        </div>
      </div>

      <Chart title="Clicks by day" data={analytics?.timeSeries ?? []} />
      <Chart title="Referers" data={analytics?.referers ?? []} />
      <Chart title="User agents" data={analytics?.userAgents ?? []} />
    </section>
  );
}

function Chart({ title, data }: { title: string; data: { label: string; count: number }[] }) {
  const max = Math.max(1, ...data.map(point => point.count));
  return (
    <div className="chart">
      <h3>{title}</h3>
      {data.length === 0 ? (
        <p className="muted">No data yet.</p>
      ) : (
        data.map(point => (
          <div className="bar-row" key={point.label}>
            <span title={point.label}>{point.label}</span>
            <div className="bar-track">
              <div className="bar-fill" style={{ width: `${Math.max(8, (point.count / max) * 100)}%` }} />
            </div>
            <strong>{point.count}</strong>
          </div>
        ))
      )}
    </div>
  );
}
