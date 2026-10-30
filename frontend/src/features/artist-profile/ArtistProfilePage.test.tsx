import { act, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, type ApiFetchOptions } from '../../api/client'
import { NotifyProvider } from '../../components/state/NotifyProvider'
import { renderWithProviders } from '../../test/renderWithProviders'
import type { CurrentUser } from '../auth/types'
import { ArtistProfilePage } from './ArtistProfilePage'
import type { ArtFieldPage, ArtistProfile, ArtistProfileUpdate } from './types'
import { artistProfileQueryKey } from './artistProfileQueries'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, apiFetch: vi.fn() }
})

const profile: ArtistProfile = {
  userId: '00000000-0000-4000-8000-000000000001',
  displayName: 'Ana Umjetnica',
  avatarUrl: null,
  biography: 'Slikarica.',
  artistStatement: 'Istražujem boju.',
  artFields: [
    {
      id: '00000000-0000-4000-8000-000000000010',
      code: 'slikarstvo',
      name: 'Slikarstvo',
    },
  ],
  links: [
    { label: 'Prva stranica', url: 'https://first.example.test' },
    { label: 'Druga stranica', url: 'https://second.example.test' },
  ],
}

const artFields: ArtFieldPage = {
  data: [
    {
      ...profile.artFields[0],
      active: true,
      position: 1,
    },
  ],
  meta: { page: 1, pageSize: 100, total: 1 },
}

const currentUser: CurrentUser = {
  id: profile.userId,
  displayName: profile.displayName,
  email: 'ana@example.test',
  avatarUrl: null,
  role: 'ARTIST',
  status: 'ACTIVE',
  organizerApproval: null,
}

let savedUpdate: ArtistProfileUpdate | undefined
let saveError: ApiError | undefined
let profileResponse = profile

async function mockApiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  if (path === '/me/artist-profile' && options.method === 'PUT') {
    savedUpdate = options.json as ArtistProfileUpdate
    if (saveError) throw saveError
    return profileResponse as T
  }
  if (path === '/me/artist-profile') return profileResponse as T
  if (path === '/art-fields?pageSize=100') return artFields as T
  if (path === '/me') return currentUser as T
  throw new Error(`Unexpected API request: ${path}`)
}

function renderPage() {
  return renderWithProviders(
    <NotifyProvider>
      <ArtistProfilePage />
    </NotifyProvider>,
    { route: '/profile' },
  )
}

afterEach(() => {
  vi.clearAllMocks()
  savedUpdate = undefined
  saveError = undefined
  profileResponse = profile
})

describe('ArtistProfilePage', () => {
  it('submits a complete payload, normalizes blanks, and preserves link order', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    const { queryClient } = renderPage()
    queryClient.setQueryData(['artists', profile.userId], { ...profile })

    const biography = await screen.findByRole('textbox', { name: 'Biografija' })
    await user.clear(biography)
    await user.clear(screen.getByRole('textbox', { name: 'Naziv poveznice 2' }))
    await user.click(
      screen.getByRole('button', { name: 'Pomakni poveznicu gore 2' }),
    )
    await user.click(screen.getByRole('button', { name: 'Spremi profil' }))

    await screen.findByText('Profil spremljen')

    expect(savedUpdate).toEqual({
      biography: null,
      artistStatement: 'Istražujem boju.',
      artFieldIds: [profile.artFields[0].id],
      links: [
        { label: null, url: 'https://second.example.test' },
        { label: 'Prva stranica', url: 'https://first.example.test' },
      ],
    })
    expect(queryClient.getQueryData(artistProfileQueryKey)).toEqual(profile)
    expect(
      queryClient.getQueryState(['artists', profile.userId])?.isInvalidated,
    ).toBe(true)
  })

  it('renders the blank form for an artist with no saved profile fields', async () => {
    profileResponse = {
      ...profile,
      biography: null,
      artistStatement: null,
      artFields: [],
      links: [],
    }
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderPage()

    expect(
      await screen.findByText(
        'Popunite biografiju i dodajte područja umjetnosti kako bi vas organizatori lakše pronašli.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Biografija' })).toHaveValue('')
    expect(
      screen.getByRole('textbox', { name: 'Opis umjetničkog rada' }),
    ).toHaveValue('')
    expect(screen.getByText('Još nema poveznica.')).toBeInTheDocument()
  })

  it('maps links[N].url validation errors to the matching form field', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    saveError = new ApiError(400, {
      code: 'VALIDATION_ERROR',
      message: 'Neispravni podaci.',
      details: [{ field: 'links[0].url', issue: 'URL nije dopušten.' }],
    })
    const user = userEvent.setup()
    renderPage()

    await user.type(
      await screen.findByRole('textbox', { name: 'Biografija' }),
      ' Dodatak',
    )
    await user.click(screen.getByRole('button', { name: 'Spremi profil' }))

    expect(await screen.findByText('URL nije dopušten.')).toBeInTheDocument()
    expect(
      screen.getByRole('textbox', { name: 'URL poveznice 1' }),
    ).toHaveAttribute('aria-invalid', 'true')
  })

  it('shows an already selected inactive area and keeps it in the request', async () => {
    const inactiveProfile = {
      ...profile,
      artFields: [{ ...profile.artFields[0], name: 'Neaktivno područje' }],
    }
    vi.mocked(apiFetch).mockImplementation(
      async <T,>(path: string, options: ApiFetchOptions = {}): Promise<T> => {
        if (path === '/me/artist-profile' && options.method === 'PUT') {
          savedUpdate = options.json as ArtistProfileUpdate
          return inactiveProfile as T
        }
        if (path === '/me/artist-profile') return inactiveProfile as T
        if (path === '/art-fields?pageSize=100') {
          return { ...artFields, data: [] } as T
        }
        if (path === '/me') return currentUser as T
        throw new Error(`Unexpected API request: ${path}`)
      },
    )
    const user = userEvent.setup()
    renderPage()

    expect(
      await screen.findByText('Neaktivno područje (neaktivno)'),
    ).toBeInTheDocument()
    await user.type(
      screen.getByRole('textbox', { name: 'Biografija' }),
      ' Novo',
    )
    await user.click(screen.getByRole('button', { name: 'Spremi profil' }))

    await waitFor(() => {
      expect(savedUpdate?.artFieldIds).toEqual([profile.artFields[0].id])
    })
  })

  it('shows retry controls when profile loading fails', async () => {
    vi.mocked(apiFetch).mockRejectedValue(
      new ApiError(503, {
        code: 'INTERNAL_ERROR',
        message: 'Privremena pogreška.',
      }),
    )
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Privremena pogreška.',
    )
    expect(
      screen.getByRole('button', { name: 'Pokušaj ponovno' }),
    ).toBeVisible()
  })

  it('blocks internal navigation while the form is dirty', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    const { router } = renderPage()

    await user.type(
      await screen.findByRole('textbox', { name: 'Biografija' }),
      ' Promjena',
    )
    await act(async () => {
      await router.navigate('/next')
    })

    expect(
      await screen.findByRole('dialog', { name: 'Napuštate stranicu?' }),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Nastavi uređivati' }))
    expect(router.state.location.pathname).toBe('/profile')

    await act(async () => {
      await router.navigate('/next')
    })
    await user.click(screen.getByRole('button', { name: 'Odbaci promjene' }))
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/next')
    })
  })

  it('warns before a browser unload while the form is dirty', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderPage()

    await user.type(
      await screen.findByRole('textbox', { name: 'Biografija' }),
      ' Promjena',
    )
    const event = new Event('beforeunload', {
      bubbles: false,
      cancelable: true,
    })
    window.dispatchEvent(event)

    expect(event.defaultPrevented).toBe(true)
  })
})
