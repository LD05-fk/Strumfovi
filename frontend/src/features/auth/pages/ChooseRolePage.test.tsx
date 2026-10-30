import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch } from '../../../api/client'
import { theme } from '../../../app/theme'
import { NotifyProvider } from '../../../components/state/NotifyProvider'
import { OnboardingOnly } from '../routing/OnboardingOnly'
import { RequireRole } from '../routing/RequireRole'
import type { CurrentUser } from '../types'
import { ChooseRolePage } from './ChooseRolePage'

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>()
  return {
    ...actual,
    apiFetch: vi.fn(),
  }
})

const artistUser: CurrentUser = {
  id: 'user-1',
  displayName: 'Ana Horvat',
  email: 'ana@example.com',
  avatarUrl: null,
  role: 'ARTIST',
  status: 'ACTIVE',
  organizerApproval: null,
}

const onboardingUser: CurrentUser = {
  ...artistUser,
  role: null,
  status: 'ONBOARDING',
}

function LocationDisplay() {
  const location = useLocation()
  return <div data-testid="location">{location.pathname}</div>
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  queryClient.setQueryData(['me'], onboardingUser)
  vi.mocked(apiFetch).mockResolvedValue(onboardingUser)

  const result = render(
    <NotifyProvider>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/onboarding']}>
          <ThemeProvider theme={theme}>
            <LocationDisplay />
            <Routes>
              <Route
                path="/onboarding"
                element={
                  <OnboardingOnly>
                    <ChooseRolePage />
                  </OnboardingOnly>
                }
              />
              <Route
                path="/profile"
                element={
                  <RequireRole roles={['ARTIST', 'ORGANIZER']}>
                    <h1>Profil</h1>
                  </RequireRole>
                }
              />
              <Route path="/" element={<h1>Početna</h1>} />
            </Routes>
          </ThemeProvider>
        </MemoryRouter>
      </QueryClientProvider>
    </NotifyProvider>,
  )

  return { ...result, queryClient }
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('ChooseRolePage', () => {
  it('onemogućuje ponovni odabir tijekom slanja', async () => {
    const user = userEvent.setup()
    const { queryClient } = renderPage()
    let resolveResponse!: (user: CurrentUser) => void
    const response = new Promise<CurrentUser>((resolve) => {
      resolveResponse = resolve
    })
    vi.mocked(apiFetch).mockReturnValueOnce(response)

    await user.click(screen.getByRole('radio', { name: /Organizator/ }))
    await user.click(screen.getByRole('button', { name: 'Potvrdi' }))

    expect(screen.getByRole('button', { name: 'Spremanje…' })).toBeDisabled()
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Organizator/ })).toBeDisabled()

    resolveResponse({
      ...artistUser,
      role: 'ORGANIZER',
      organizerApproval: 'APPROVED',
    })
    await waitFor(() => {
      expect(screen.getByTestId('location')).toHaveTextContent('/profile')
      expect(queryClient.getQueryData(['me'])).toEqual({
        ...artistUser,
        role: 'ORGANIZER',
        organizerApproval: 'APPROVED',
      })
    })
  })

  it('prikazuje poruku API-ja za grešku 400', async () => {
    const user = userEvent.setup()
    renderPage()
    vi.mocked(apiFetch).mockRejectedValueOnce(
      new ApiError(400, {
        code: 'VALIDATION_ERROR',
        message: 'Odaberite jednu od ponuđenih uloga.',
      }),
    )

    await user.click(screen.getByRole('radio', { name: /Umjetnik/ }))
    await user.click(screen.getByRole('button', { name: 'Potvrdi' }))

    expect(
      await screen.findByText('Odaberite jednu od ponuđenih uloga.'),
    ).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/onboarding')
  })

  it('za mrežnu grešku prikazuje poruku i omogućuje ponovni pokušaj', async () => {
    const user = userEvent.setup()
    renderPage()
    vi.mocked(apiFetch).mockRejectedValueOnce(
      new ApiError(0, {
        code: 'NETWORK_ERROR',
        message: 'Poslužitelj nije dostupan.',
      }),
    )

    await user.click(screen.getByRole('radio', { name: /Organizator/ }))
    await user.click(screen.getByRole('button', { name: 'Potvrdi' }))

    expect(
      await screen.findByText('Poslužitelj nije dostupan.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Potvrdi' })).toBeEnabled()
  })

  it('na 409 invalidira korisnika, prikazuje obavijest i vodi na početnu', async () => {
    const user = userEvent.setup()
    const { queryClient } = renderPage()
    queryClient.setQueryData(['me'], { ...artistUser, role: null })
    const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries')
    vi.mocked(apiFetch).mockRejectedValueOnce(
      new ApiError(409, {
        code: 'ROLE_ALREADY_CHOSEN',
        message: 'Uloga je već odabrana.',
      }),
    )

    await user.click(screen.getByRole('radio', { name: /Umjetnik/ }))
    await user.click(screen.getByRole('button', { name: 'Potvrdi' }))

    expect(await screen.findByText('Uloga je već odabrana')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByTestId('location')).toHaveTextContent('/')
      expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['me'] })
    })
  })
})
