import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../../../api/client'
import type { ReactNode } from 'react'
import type { CurrentUser } from '../types'
import { OnboardingOnly } from './OnboardingOnly'
import { RequireAuth } from './RequireAuth'
import { RequireRole } from './RequireRole'

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>()
  return {
    ...actual,
    apiFetch: vi.fn(),
  }
})

const artist: CurrentUser = {
  id: 'artist-id',
  displayName: 'Ana Umjetnica',
  email: 'ana@example.com',
  avatarUrl: null,
  role: 'ARTIST',
  status: 'ACTIVE',
  organizerApproval: null,
}

function setCurrentUser(user: CurrentUser | null, error?: ApiError) {
  if (error) {
    vi.mocked(apiFetch).mockRejectedValue(error)
  } else if (user) {
    vi.mocked(apiFetch).mockResolvedValue(user)
  } else {
    vi.mocked(apiFetch).mockRejectedValue(
      new ApiError(401, {
        code: 'UNAUTHENTICATED',
        message: 'Potrebno je prijaviti se.',
      }),
    )
  }
}

function renderRoutes(element: ReactNode, initialEntry = '/private') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/" element={<h1 data-testid="home">Početna</h1>} />
          <Route path="/onboarding" element={<h1>Odabir uloge</h1>} />
          <Route path="*" element={element} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('RequireAuth', () => {
  it('prikazuje učitavanje dok dohvaća sesiju', () => {
    vi.mocked(apiFetch).mockReturnValue(new Promise<never>(() => {}))

    renderRoutes(
      <RequireAuth>
        <h1>Zaštićeni sadržaj</h1>
      </RequireAuth>,
    )

    expect(screen.getByRole('status')).toBeInTheDocument()
  })

  it('prikazuje panel za neprijavljenog korisnika', async () => {
    setCurrentUser(null)

    renderRoutes(
      <RequireAuth>
        <h1>Zaštićeni sadržaj</h1>
      </RequireAuth>,
    )

    expect(await screen.findByTestId('login-required')).toBeInTheDocument()
    expect(await screen.findByTestId('google-sign-in')).toHaveAttribute(
      'href',
      '/oauth2/authorization/google',
    )
  })

  it('preusmjerava korisnika u onboarding', async () => {
    setCurrentUser({ ...artist, status: 'ONBOARDING', role: null })

    renderRoutes(
      <RequireAuth>
        <h1>Zaštićeni sadržaj</h1>
      </RequireAuth>,
    )

    expect(
      await screen.findByRole('heading', { name: 'Odabir uloge' }),
    ).toBeInTheDocument()
  })

  it('prikazuje pogrešku mreže umjesto da tretira korisnika kao gosta', async () => {
    setCurrentUser(
      null,
      new ApiError(0, {
        code: 'NETWORK_ERROR',
        message: 'Network error',
      }),
    )

    renderRoutes(
      <RequireAuth>
        <h1>Zaštićeni sadržaj</h1>
      </RequireAuth>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('Network error')
    expect(
      await screen.findByRole('button', { name: 'Pokušaj ponovno' }),
    ).toBeInTheDocument()
  })
})

describe('RequireRole', () => {
  it.each([
    ['ARTIST', 'ARTIST', true],
    ['ARTIST', 'ORGANIZER', false],
    ['ORGANIZER', 'ORGANIZER', true],
    ['ORGANIZER', 'ADMIN', false],
    ['ADMIN', 'ADMIN', true],
    ['ADMIN', 'ARTIST', false],
  ] as const)(
    'zahtijeva ulogu %s za korisnika s ulogom %s',
    async (requiredRole, userRole, allowed) => {
      setCurrentUser({ ...artist, role: userRole })

      renderRoutes(
        <RequireRole roles={[requiredRole]}>
          <h1>Zaštićeni sadržaj</h1>
        </RequireRole>,
      )

      if (allowed) {
        expect(
          await screen.findByRole('heading', { name: 'Zaštićeni sadržaj' }),
        ).toBeInTheDocument()
      } else {
        expect(
          await screen.findByText('Nemate pristup ovoj stranici.'),
        ).toBeInTheDocument()
      }
    },
  )

  it('prikazuje panel korisniku bez sesije', async () => {
    setCurrentUser(null)

    renderRoutes(<RequireRole roles={['ARTIST']} />)

    expect(await screen.findByTestId('login-required')).toBeInTheDocument()
  })

  it('može preusmjeriti neprijavljenog korisnika na zadanu rutu', async () => {
    setCurrentUser(null)

    renderRoutes(
      <RequireRole roles={['ARTIST']} unauthenticatedRedirect="/" />,
    )

    expect(await screen.findByTestId('home')).toBeInTheDocument()
  })

  it('preusmjerava korisnika u onboardingu na onboarding', async () => {
    setCurrentUser({ ...artist, status: 'ONBOARDING', role: null })

    renderRoutes(<RequireRole roles={['ARTIST']} />)

    expect(
      await screen.findByRole('heading', { name: 'Odabir uloge' }),
    ).toBeInTheDocument()
  })
})

describe('OnboardingOnly', () => {
  it('dopušta pristup korisniku u statusu ONBOARDING', async () => {
    setCurrentUser({ ...artist, status: 'ONBOARDING', role: null })

    renderRoutes(
      <OnboardingOnly>
        <h1>Onboarding sadržaj</h1>
      </OnboardingOnly>,
    )

    expect(
      await screen.findByRole('heading', { name: 'Onboarding sadržaj' }),
    ).toBeInTheDocument()
  })

  it('preusmjerava korisnika koji je završio onboarding na početnu', async () => {
    setCurrentUser(artist)

    renderRoutes(<OnboardingOnly />)

    expect(await screen.findByTestId('home')).toBeInTheDocument()
  })

  it('preusmjerava neprijavljenog korisnika na početnu', async () => {
    setCurrentUser(null)

    renderRoutes(<OnboardingOnly />)

    expect(await screen.findByTestId('home')).toBeInTheDocument()
  })

  it.each(['ARTIST', 'ORGANIZER', 'ADMIN'] as const)(
    'preusmjerava korisnika s ulogom %s na početnu',
    async (role) => {
      setCurrentUser({ ...artist, role })

      renderRoutes(<OnboardingOnly />)

      expect(await screen.findByTestId('home')).toBeInTheDocument()
    },
  )
})
