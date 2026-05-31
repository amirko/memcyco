import { FormEvent, useEffect, useMemo, useState } from 'react';
import type { ShortLink, ShortLinkPayload, Strategy } from '../types/domain';

interface Props {
  strategies: Strategy[];
  editing: ShortLink | null;
  onCancelEdit: () => void;
  onSubmit: (payload: ShortLinkPayload, id?: number) => Promise<void>;
}

const emptyForm = {
  originalUrl: '',
  customAlias: '',
  strategy: 'random_base62',
  expiresAt: '',
  maxClicks: '',
  tags: ''
};

export default function LinkForm({ strategies, editing, onCancelEdit, onSubmit }: Props) {
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!editing) {
      setForm(emptyForm);
      return;
    }

    setForm({
      originalUrl: editing.originalUrl,
      customAlias: editing.shortCode,
      strategy: editing.strategy,
      expiresAt: editing.expiresAt ? editing.expiresAt.slice(0, 16) : '',
      maxClicks: editing.maxClicks?.toString() ?? '',
      tags: editing.tags.join(', ')
    });
  }, [editing]);

  const selectedStrategy = useMemo(
    () => strategies.find(strategy => strategy.name === form.strategy),
    [form.strategy, strategies]
  );

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    try {
      const payload: ShortLinkPayload = {
        originalUrl: form.originalUrl.trim(),
        customAlias: form.customAlias.trim() || undefined,
        strategy: form.strategy,
        expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
        maxClicks: form.maxClicks ? Number(form.maxClicks) : null,
        tags: form.tags
          .split(',')
          .map(tag => tag.trim())
          .filter(Boolean),
        parameters: {}
      };
      await onSubmit(payload, editing?.id);
      if (!editing) {
        setForm(emptyForm);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="panel form-panel" onSubmit={submit}>
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Short link</p>
          <h2>{editing ? 'Edit link' : 'Create link'}</h2>
        </div>
        {editing && (
          <button className="icon-button" type="button" onClick={onCancelEdit} title="Cancel edit">
            X
          </button>
        )}
      </div>

      <label>
        Destination URL
        <input
          required
          type="url"
          placeholder="https://example.com/campaign"
          value={form.originalUrl}
          onChange={event => setForm({ ...form, originalUrl: event.target.value })}
        />
      </label>

      <div className="form-grid">
        <label>
          Custom alias
          <input
            placeholder="spring-sale"
            value={form.customAlias}
            onChange={event => setForm({ ...form, customAlias: event.target.value })}
          />
        </label>
        <label>
          Strategy
          <select value={form.strategy} onChange={event => setForm({ ...form, strategy: event.target.value })}>
            {strategies.map(strategy => (
              <option key={strategy.name} value={strategy.name}>
                {strategy.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {selectedStrategy && (
        <div className="strategy-note">
          <strong>{selectedStrategy.label}</strong>
          <span>{selectedStrategy.description}</span>
        </div>
      )}

      <div className="form-grid">
        <label>
          Expiration
          <input
            type="datetime-local"
            value={form.expiresAt}
            onChange={event => setForm({ ...form, expiresAt: event.target.value })}
          />
        </label>
        <label>
          Max clicks
          <input
            min="1"
            type="number"
            placeholder="No limit"
            value={form.maxClicks}
            onChange={event => setForm({ ...form, maxClicks: event.target.value })}
          />
        </label>
      </div>

      <label>
        Tags
        <input
          placeholder="paid, newsletter, q2"
          value={form.tags}
          onChange={event => setForm({ ...form, tags: event.target.value })}
        />
      </label>

      <button className="primary-button" type="submit" disabled={submitting}>
        {submitting ? 'Saving...' : editing ? 'Save changes' : 'Create short link'}
      </button>
    </form>
  );
}
