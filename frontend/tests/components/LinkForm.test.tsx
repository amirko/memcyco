import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import LinkForm from '../../src/components/LinkForm';
import { shortLink, strategies } from '../test/fixtures';

describe('LinkForm', () => {
  it('builds a trimmed create payload from form input', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);

    render(<LinkForm strategies={strategies} editing={null} onCancelEdit={vi.fn()} onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText(/destination url/i), ' https://example.com/product ');
    await user.type(screen.getByLabelText(/custom alias/i), ' summer-sale ');
    await user.selectOptions(screen.getByLabelText(/strategy/i), 'hash_based');
    await user.type(screen.getByLabelText(/max clicks/i), '42');
    await user.type(screen.getByLabelText(/tags/i), ' paid, newsletter, , launch ');
    await user.click(screen.getByRole('button', { name: /create short link/i }));

    expect(onSubmit).toHaveBeenCalledWith(
      {
        originalUrl: 'https://example.com/product',
        customAlias: 'summer-sale',
        strategy: 'hash_based',
        expiresAt: null,
        maxClicks: 42,
        tags: ['paid', 'newsletter', 'launch'],
        parameters: {}
      },
      undefined
    );
  });

  it('loads edit values and submits with the link id', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const link = shortLink({
      id: 12,
      shortCode: 'edit-me',
      originalUrl: 'https://example.com/old',
      maxClicks: 3,
      expiresAt: '2030-01-02T03:04:00Z'
    });

    render(<LinkForm strategies={strategies} editing={link} onCancelEdit={vi.fn()} onSubmit={onSubmit} />);

    expect(screen.getByRole('heading', { name: /edit link/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/destination url/i)).toHaveValue('https://example.com/old');
    await user.clear(screen.getByLabelText(/max clicks/i));
    await user.type(screen.getByLabelText(/max clicks/i), '9');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ maxClicks: 9, customAlias: 'edit-me' }), 12);
  });

  it('cancels editing', async () => {
    const user = userEvent.setup();
    const onCancelEdit = vi.fn();

    render(<LinkForm strategies={strategies} editing={shortLink()} onCancelEdit={onCancelEdit} onSubmit={vi.fn()} />);

    await user.click(screen.getByTitle(/cancel edit/i));
    expect(onCancelEdit).toHaveBeenCalledTimes(1);
  });
});
