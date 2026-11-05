import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { ApiError, apiFetch, type ApiFetchOptions } from '../../api/client'
import { NotifyProvider } from '../../components/state/NotifyProvider'
import type { CurrentUser } from '../auth/types'
import type {
  ArtistPublicProfile,
  Participation,
} from '../artist-profile/types'
import { theme } from '../../app/theme'
import { PublicPortfolioPage } from './PublicPortfolioPage'
import type { Artwork, ArtworkPage } from './types'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, apiFetch: vi.fn() }
})

const artistId = '00000000-0000-4000-8000-000000000001'
const baseProfile: ArtistPublicProfile = {
  userId: artistId,
  displayName: 'Ana Umjetnica',
  avatarUrl: null,
  biography: 'Slikarica.',
  artistStatement: 'Istražujem boju.',
  artFields: [{ id: 'field-1', code: 'painting', name: 'Slikarstvo' }],
  links: [{ label: null, url: 'https://ana.example.test/profile' }],
  pastExhibitions: [],
  participations: [],
}

function artwork(
  id: string,
  visibility: Artwork['visibility'] = 'PUBLIC',
  hiddenByAdmin = false,
): Artwork {
  return {
    id,
    artistId,
    title: `Djelo ${id}`,
    year: 2025,
    technique: 'Ulje na platnu',
    dimensions: '80 × 60 cm',
    description: 'Opis djela.',
    visibility,
    hiddenByAdmin,
    images: [
      {
        id: `image-${id}`,
        file: {
          id: `file-${id}`,
          originalName: `${id}.jpg`,
          contentType: 'image/jpeg',
          sizeBytes: 1200,
          url: `/api/v1/files/${id}`,
        },
        altText: `Alternativni tekst ${id}`,
      },
    ],
    createdAt: '2025-01-01T00:00:00Z',
  }
}

let profileResponse: ArtistPublicProfile = baseProfile
let artworksResponse: ArtworkPage = {
  data: [artwork('public-1')],
  meta: { page: 1, pageSize: 24, total: 1 },
}
let currentUser: CurrentUser | null = null
let profileError: unknown = null

async function mockApiFetch<T>(path: string, _options?: ApiFetchOptions) {
  if (path === `/artists/${artistId}`) {
    if (profileError) throw profileError
    return profileResponse as T
  }
  if (path.startsWith(`/artists/${artistId}/artworks?`)) {
    return artworksResponse as T
  }
  if (path === '/me') {
    if (currentUser) return currentUser as T
    throw new ApiError(401, { code: 'UNAUTHENTICATED', message: 'Not signed in' })
  }
  throw new Error(`Unexpected API request: ${path}`)
}

function renderPage(path = `/artists/${artistId}`) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const router = createMemoryRouter(
    [{ path: '/artists/:artistId', element: <PublicPortfolioPage /> }],
    { initialEntries: [path] },
  )
  return {
    queryClient,
    router,
    ...render(
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <NotifyProvider>
            <RouterProvider router={router} />
          </NotifyProvider>
        </ThemeProvider>
      </QueryClientProvider>,
    ),
  }
}

afterEach(() => {
  vi.clearAllMocks()
  profileResponse = baseProfile
  artworksResponse = {
    data: [artwork('public-1')],
    meta: { page: 1, pageSize: 24, total: 1 },
  }
  currentUser = null
  profileError = null
})

describe('PublicPortfolioPage', () => {
  it('renders a public artist profile and artwork for an unauthenticated visitor', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Ana Umjetnica' }))
      .toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Djela' })).toBeInTheDocument()
    expect(screen.getByText('Djelo public-1')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'ana.example.test ↗' })).toHaveAttribute(
      'rel',
      'noopener noreferrer',
    )
  })

  it('hides an empty participation section and renders its badge for mock data', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const firstRender = renderPage()
    await screen.findByRole('heading', { name: 'Ana Umjetnica' })
    expect(
      screen.queryByRole('heading', {
        name: 'Sudjelovanja preko ArtConnecta',
      }),
    ).not.toBeInTheDocument()
    firstRender.unmount()

    const participation: Participation = {
      id: 'participation-1',
      title: 'Natječaj za mural „Grad”',
      organizerName: 'Gradska galerija Zagreb',
      year: 2026,
    }
    profileResponse = { ...baseProfile, participations: [participation] }
    renderPage()

    expect(
      await screen.findByText('Ostvareno preko ArtConnecta'),
    ).toBeInTheDocument()
    expect(screen.getByText(participation.title)).toBeInTheDocument()
  })

  it('requests only public artworks in the owner view', async () => {
    currentUser = {
      id: artistId,
      displayName: 'Ana Umjetnica',
      email: 'ana@example.test',
      avatarUrl: null,
      role: 'ARTIST',
      status: 'ACTIVE',
      organizerApproval: null,
    }
    artworksResponse = {
      data: [
        artwork('public'),
      ],
      meta: { page: 1, pageSize: 24, total: 1 },
    }
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderPage()

    expect(await screen.findByText('Djelo public')).toBeInTheDocument()
    expect(screen.getByText('Ovo je vaš javni profil')).toBeInTheDocument()
    expect(apiFetch).toHaveBeenCalledWith(
      `/artists/${artistId}/artworks?page=1&pageSize=24`,
      { credentials: 'omit' },
    )
  })

  it('renders the artist-specific 404 state', async () => {
    profileError = new ApiError(404, {
      code: 'NOT_FOUND',
      message: 'Not found',
    })
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderPage()

    expect(
      await screen.findByRole('heading', { name: 'Umjetnik nije pronađen' }),
    ).toBeInTheDocument()
  })

  it('shows a retryable alert for server errors', async () => {
    profileError = new ApiError(500, {
      code: 'INTERNAL_ERROR',
      message: 'Greška poslužitelja.',
    })
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Greška poslužitelja.',
    )
    const callsBeforeRetry = vi.mocked(apiFetch).mock.calls.length
    await user.click(screen.getByRole('button', { name: 'Pokušaj ponovno' }))
    await waitFor(() => {
      expect(vi.mocked(apiFetch).mock.calls.length).toBeGreaterThan(
        callsBeforeRetry,
      )
    })
  })

  it('keeps an out-of-range page URL and offers navigation to an available page', async () => {
    artworksResponse = {
      data: [],
      meta: { page: 5, pageSize: 24, total: 30 },
    }
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    const { router } = renderPage(`/artists/${artistId}?page=5`)

    expect(await screen.findByText('Nema djela na ovoj stranici.'))
      .toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Prva stranica' }))
    await waitFor(() => {
      expect(router.state.location.search).toBe('')
    })
    expect(apiFetch).toHaveBeenCalledWith(
      `/artists/${artistId}/artworks?page=1&pageSize=24`,
      { credentials: 'omit' },
    )
  })

  it('uses page one for an invalid page query without redirecting the URL', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const { router } = renderPage(`/artists/${artistId}?page=not-a-number`)

    expect(await screen.findByText('Djelo public-1')).toBeInTheDocument()
    expect(apiFetch).toHaveBeenCalledWith(
      `/artists/${artistId}/artworks?page=1&pageSize=24`,
      { credentials: 'omit' },
    )
    expect(router.state.location.search).toBe('?page=not-a-number')
  })

  it('opens artworks in a keyboard-accessible lightbox', async () => {
    const first = artwork('public-1')
    const secondImage = {
      ...first.images[0],
      id: 'image-public-2',
      file: {
        ...first.images[0].file,
        id: 'file-public-2',
        url: '/api/v1/files/public-2',
      },
      altText: 'Druga alternativna slika',
    }
    artworksResponse = {
      ...artworksResponse,
      data: [{ ...first, images: [...first.images, secondImage] }],
    }
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderPage()
    const opener = await screen.findByRole('button', {
      name: 'Djelo public-1, 2025',
    })
    await user.click(opener)

    expect(
      screen.getByRole('dialog', { name: 'Djelo public-1' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('img', { name: 'Alternativni tekst public-1' }),
    ).toBeInTheDocument()
    fireEvent.keyDown(document, { key: 'ArrowRight' })
    expect(
      screen.getByRole('img', { name: 'Druga alternativna slika' }),
    ).toBeInTheDocument()
    fireEvent.keyDown(document, { key: 'Escape' })
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(opener).toHaveFocus()
  })
})
