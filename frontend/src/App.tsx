import { useEffect, useMemo, useState } from 'react';
import { api } from './api/client';
import AnalyticsPanel from './components/AnalyticsPanel';
import LinkForm from './components/LinkForm';
import LinksTable from './components/LinksTable';
import type { Analytics, ShortLink, ShortLinkPayload, Strategy } from './types/domain';

const POLL_INTERVAL_MS = 5000;

export default function App() {
  const [links, setLinks] = useState<ShortLink[]>([]);
  const [strategies, setStrategies] = useState<Strategy[]>([]);
  const [selected, setSelected] = useState<ShortLink | null>(null);
  const [editing, setEditing] = useState<ShortLink | null>(null);
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void bootstrap();
  }, []);

  useEffect(() => {
    if (!selected) {
      setAnalytics(null);
      return;
    }
    void loadAnalytics(selected);
  }, [selected?.id]);

  useEffect(() => {
    if (loading) {
      return;
    }

    const intervalId = window.setInterval(() => {
      void pollLinks();
    }, POLL_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [loading]);

  useEffect(() => {
    if (!selected?.id) {
      return;
    }

    const intervalId = window.setInterval(() => {
      void loadAnalytics(selected);
    }, POLL_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [selected?.id]);

  const selectedFromFreshList = useMemo(
    () => links.find(link => link.id === selected?.id) ?? selected,
    [links, selected]
  );

  async function bootstrap() {
    setLoading(true);
    setError(null);
    try {
      const [nextLinks, nextStrategies] = await Promise.all([api.listLinks(), api.strategies()]);
      setLinks(nextLinks);
      setStrategies(nextStrategies);
      setSelected(nextLinks[0] ?? null);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function refreshLinks(options: { preserveFallback?: boolean } = {}) {
    const nextLinks = await api.listLinks();
    setLinks(nextLinks);
    setSelected(current => {
      if (current) {
        return nextLinks.find(link => link.id === current.id) ?? null;
      }
      return options.preserveFallback ? null : nextLinks[0] ?? null;
    });
  }

  async function pollLinks() {
    try {
      await refreshLinks({ preserveFallback: true });
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function saveLink(payload: ShortLinkPayload, id?: number) {
    setError(null);
    try {
      const saved = id ? await api.updateLink(id, payload) : await api.createLink(payload);
      await refreshLinks();
      setSelected(saved);
      setEditing(null);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function deleteLink(link: ShortLink) {
    setError(null);
    try {
      await api.deleteLink(link.id);
      await refreshLinks();
      if (editing?.id === link.id) {
        setEditing(null);
      }
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function loadAnalytics(link: ShortLink) {
    try {
      setAnalytics(await api.analytics(link.id));
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <main>
      <header className="topbar">
        <div>
          <p className="eyebrow">URL shortener</p>
          <h1>Links and analytics</h1>
        </div>
        <button className="secondary-button" onClick={bootstrap}>
          Refresh
        </button>
      </header>

      {error && (
        <div className="alert" role="alert">
          {error}
        </div>
      )}

      <div className="layout">
        <LinkForm strategies={strategies} editing={editing} onCancelEdit={() => setEditing(null)} onSubmit={saveLink} />
        <div className="work-area">
          {loading ? (
            <section className="panel empty-state">Loading links...</section>
          ) : (
            <LinksTable
              links={links}
              selectedId={selected?.id ?? null}
              onSelect={setSelected}
              onEdit={setEditing}
              onDelete={deleteLink}
            />
          )}
          <AnalyticsPanel link={selectedFromFreshList} analytics={analytics} />
        </div>
      </div>
    </main>
  );
}
