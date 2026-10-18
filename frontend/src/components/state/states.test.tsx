import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { EmptyState } from './EmptyState'
import { ErrorState } from './ErrorState'
import { LoadingState } from './LoadingState'
import { NotifyProvider } from './NotifyProvider'
import {
  StatusChip,
  type ApplicationStatus,
  type OpportunityStatus,
} from './StatusChip'
import { useNotify } from './useNotify'
import { hr } from '../../i18n/hr'
import { MemoryRouter } from 'react-router'
import { ForbiddenPage } from './ForbiddenPage'
import { NotFoundPage } from './NotFoundPage'

afterEach(cleanup)

describe('shared state components', () => {
  it('marks loading content as busy and labels it in Croatian', () => {
    render(<LoadingState />)

    expect(screen.getByRole('status').getAttribute('aria-busy')).toBe('true')
    expect(screen.getByLabelText(hr.common.loading)).toBeTruthy()
  })

  it('renders an empty message, description and caller-provided action', () => {
    render(
      <EmptyState
        title="Još nema prijava"
        description="Pronađite priliku."
        action={<button>Pregledaj prilike</button>}
      />,
    )

    expect(screen.getByText('Još nema prijava')).toBeTruthy()
    expect(screen.getByText('Pronađite priliku.')).toBeTruthy()
    expect(
      screen.getByRole('button', { name: 'Pregledaj prilike' }),
    ).toBeTruthy()
  })

  it('shows ApiError.message and invokes the retry callback', () => {
    const onRetry = vi.fn()
    const error = new ApiError(503, {
      code: 'UNAVAILABLE',
      message: 'Poslužitelj nije dostupan.',
    })
    render(<ErrorState error={error} onRetry={onRetry} />)

    expect(screen.getByRole('alert').textContent).toBe(error.message)
    fireEvent.click(screen.getByRole('button', { name: hr.common.retry }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})

describe('error pages', () => {
  it('renders the forbidden page and sets its title', async () => {
    render(
      <MemoryRouter>
        <ForbiddenPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('403')).toBeTruthy()
    expect(
      screen.getByRole('heading', { name: hr.errors.forbidden }),
    ).toBeTruthy()
    await waitFor(() => expect(document.title).toBe('403 | ArtConnect'))
  })

  it('renders the not-found page and sets its title', async () => {
    render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('404')).toBeTruthy()
    expect(
      screen.getByRole('heading', { name: hr.errors.notFound }),
    ).toBeTruthy()
    await waitFor(() => expect(document.title).toBe('404 | ArtConnect'))
  })
})

describe('StatusChip', () => {
  it.each(Object.keys(hr.opportunityStatus) as OpportunityStatus[])(
    'shows Croatian opportunity status text for %s',
    (status) => {
      render(<StatusChip type="opportunity" status={status} />)
      expect(screen.getByText(hr.opportunityStatus[status])).toBeTruthy()
    },
  )

  it.each(Object.keys(hr.applicationStatus) as ApplicationStatus[])(
    'shows Croatian application status text for %s',
    (status) => {
      render(<StatusChip type="application" status={status} />)
      expect(screen.getByText(hr.applicationStatus[status])).toBeTruthy()
    },
  )
})

function NotifyButton() {
  const notify = useNotify()
  return (
    <button onClick={() => notify('Promjene su spremljene.', 'success')}>
      Spremi promjene
    </button>
  )
}

describe('NotifyProvider', () => {
  it('shows a global snackbar with the selected severity', () => {
    render(
      <NotifyProvider>
        <NotifyButton />
      </NotifyProvider>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Spremi promjene' }))

    expect(screen.getByRole('status').textContent).toBe(
      'Promjene su spremljene.',
    )
  })
})
