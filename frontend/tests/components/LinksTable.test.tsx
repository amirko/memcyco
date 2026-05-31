import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import LinksTable from '../../src/components/LinksTable';
import { shortLink } from '../test/fixtures';

describe('LinksTable', () => {
  it('renders links and exposes row actions', async () => {
    const user = userEvent.setup();
    const link = shortLink({ status: 'click_exhausted', totalClicks: 11 });
    const onSelect = vi.fn();
    const onEdit = vi.fn();
    const onDelete = vi.fn();

    render(<LinksTable links={[link]} selectedId={link.id} onSelect={onSelect} onEdit={onEdit} onDelete={onDelete} />);

    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: link.originalUrl })).toHaveAttribute('href', link.originalUrl);
    expect(screen.getByText('click limited')).toBeInTheDocument();
    expect(screen.getByText('paid')).toBeInTheDocument();

    await user.click(screen.getByRole('row', { name: new RegExp(link.shortCode) }));
    await user.click(screen.getByTitle(/edit link/i));
    await user.click(screen.getByTitle(/delete link/i));

    expect(onSelect).toHaveBeenCalledWith(link);
    expect(onEdit).toHaveBeenCalledWith(link);
    expect(onDelete).toHaveBeenCalledWith(link);
    expect(onSelect).toHaveBeenCalledTimes(1);
  });

  it('supports keyboard row selection and ignores destination link clicks', async () => {
    const user = userEvent.setup();
    const link = shortLink();
    const onSelect = vi.fn();

    render(<LinksTable links={[link]} selectedId={null} onSelect={onSelect} onEdit={vi.fn()} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('link', { name: link.originalUrl }));
    expect(onSelect).not.toHaveBeenCalled();

    screen.getByRole('row', { name: new RegExp(link.shortCode) }).focus();
    await user.keyboard('{Enter}');
    await user.keyboard(' ');

    expect(onSelect).toHaveBeenCalledTimes(2);
    expect(onSelect).toHaveBeenCalledWith(link);
  });

  it('renders an empty state', () => {
    render(<LinksTable links={[]} selectedId={null} onSelect={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} />);

    expect(screen.getByText(/no links yet/i)).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
  });
});
