import type { ShortLink } from '../types/domain';

interface Props {
  links: ShortLink[];
  selectedId: number | null;
  onSelect: (link: ShortLink) => void;
  onEdit: (link: ShortLink) => void;
  onDelete: (link: ShortLink) => void;
}

export default function LinksTable({ links, selectedId, onSelect, onEdit, onDelete }: Props) {
  return (
    <section className="panel table-panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Inventory</p>
          <h2>Short links</h2>
        </div>
        <span className="counter">{links.length}</span>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Destination</th>
              <th>Clicks</th>
              <th>Status</th>
              <th>Created</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {links.map(link => (
              <tr key={link.id} className={selectedId === link.id ? 'selected' : ''}>
                <td>
                  <button className="code-link" onClick={() => onSelect(link)}>
                    {link.shortCode}
                  </button>
                </td>
                <td>
                  <a href={link.originalUrl} target="_blank" rel="noreferrer">
                    {link.originalUrl}
                  </a>
                  <div className="tags">
                    {link.tags.map(tag => (
                      <span key={tag}>{tag}</span>
                    ))}
                  </div>
                </td>
                <td>{link.totalClicks}</td>
                <td>
                  <span className={`status ${link.status}`}>{formatStatus(link.status)}</span>
                </td>
                <td>{new Date(link.createdAt).toLocaleDateString()}</td>
                <td>
                  <div className="row-actions">
                    <button type="button" onClick={() => onEdit(link)} title="Edit link">
                      Edit
                    </button>
                    <button type="button" onClick={() => onDelete(link)} title="Delete link">
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {links.length === 0 && (
              <tr>
                <td colSpan={6} className="empty">
                  No links yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function formatStatus(status: ShortLink['status']) {
  if (status === 'click_exhausted') {
    return 'click limited';
  }
  return status;
}
