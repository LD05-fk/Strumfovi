import { cleanup, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch } from '../../../api/client'
import { hr } from '../../../i18n/hr'
import { renderWithProviders } from '../../../test/renderWithProviders'
import type { CurrentUser } from '../types'
import { HomePage } from './HomePage'

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>()

  return {
    ...actual,
    apiFetch: vi.fn(),
  }
})

afterEach(() => {
  cleanup()
  vi.resetAllMocks()
})

function makeUser(role: CurrentUser['role']): CurrentUser {
  return {
    id: 'testnikorisnik',
    displayName: 'Test korisnik',
    email: 'test@example.com',
    avatarUrl: null,
    role,
    status: 'ACTIVE',
    organizerApproval: null,
  }
}

function mockUnauthenticatedUser(): void {
  vi.mocked(apiFetch).mockRejectedValue(
    new ApiError(401, {
      code: 'UNAUTHENTICATED',
      message: 'Korisnik nije prijavljen',
    }),
  )
}

describe('HomePage', () => {
  it('prikazuje se poruka greške na početnoj stranici nakon neuspješne prijave', async () => {
    mockUnauthenticatedUser()

    renderWithProviders(<HomePage />, { route: '/?login=error' })

    expect(await screen.findByRole('alert')).toHaveTextContent(
      hr.auth.home.loginError,
    )
  })

  it('umjetniku su prikazane poveznice „Portfolio” i „Prilike”', async () => {
    vi.mocked(apiFetch).mockResolvedValue(makeUser('ARTIST'))

    renderWithProviders(<HomePage />)

    expect(
      await screen.findByRole('link', { name: hr.auth.home.portfolioAction }),
    ).toHaveAttribute('href', '/portfolio')
    expect(
      screen.getByRole('link', { name: hr.auth.home.opportunitiesAction }),
    ).toHaveAttribute('href', '/opportunities')
  })

  it('organizatoru suprikazane poveznice „Moje prilike” i „Prilike”', async () => {
    vi.mocked(apiFetch).mockResolvedValue(makeUser('ORGANIZER'))

    renderWithProviders(<HomePage />)

    expect(
      await screen.findByRole('link', { name: hr.auth.home.myOpportunities }),
    ).toHaveAttribute('href', '/organizer/opportunities')
    expect(
      screen.getByRole('link', { name: hr.auth.home.opportunitiesAction }),
    ).toHaveAttribute('href', '/opportunities')
  })
})
