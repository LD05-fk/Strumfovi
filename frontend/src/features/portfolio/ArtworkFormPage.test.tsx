import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, type ApiFetchOptions } from '../../api/client'
import { NotifyProvider } from '../../components/state/NotifyProvider'
import { theme } from '../../app/theme'
import type { CurrentUser } from '../auth/types'
import { ArtworkFormPage } from './ArtworkFormPage'
import type { Artwork, ArtworkInput } from './types'
import { artworkQueryKey } from './portfolioQueries'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, apiFetch: vi.fn() }
})

const artistId = '00000000-0000-4000-8000-000000000001'
const artworkId = '00000000-0000-4000-8000-000000000011'

const currentUser: CurrentUser = {
  id: artistId,
  displayName: 'Ana Umjetnica',
  email: 'ana@example.test',
  avatarUrl: null,
  role: 'ARTIST',
  status: 'ACTIVE',
  organizerApproval: null,
}

const savedArtwork: Artwork = {
  id: artworkId,
  artistId,
  title: 'Plavi horizont',
  year: 2025,
  technique: 'Ulje na platnu',
  dimensions: null,
  description: null,
  visibility: 'PRIVATE',
  hiddenByAdmin: false,
  images: [],
  createdAt: '2026-01-01T10:00:00Z',
}

const uploadedImage: Artwork['images'][number] = {
  id: '00000000-0000-4000-8000-000000000021',
  file: {
    id: '00000000-0000-4000-8000-000000000031',
    originalName: 'artwork.jpg',
    contentType: 'image/jpeg',
    sizeBytes: 5,
    url: '/api/v1/files/00000000-0000-4000-8000-000000000031',
  },
  altText: 'Plavi horizont',
}

let saveError: ApiError | undefined
let savedInput: ArtworkInput | undefined

async function mockApiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  if (path === '/me') return currentUser as T
  if (path === '/settings/public') {
    return {
      artworkMaxImageBytes: 5 * 1024 * 1024,
      artworkMaxImages: 3,
      applicationMaxImageBytes: 5 * 1024 * 1024,
      applicationMaxPdfBytes: 10 * 1024 * 1024,
      applicationMaxFiles: 10,
    } as T
  }
  if (path === '/artworks' && options.method === 'POST') {
    savedInput = options.json as ArtworkInput
    if (saveError) throw saveError
    return { ...savedArtwork, ...savedInput } as T
  }
  if (path === `/artworks/${artworkId}` && options.method === 'GET') {
    return savedArtwork as T
  }
  if (path === `/artworks/${artworkId}` && options.method === 'PUT') {
    savedInput = options.json as ArtworkInput
    return { ...savedArtwork, ...savedInput } as T
  }
  throw new Error(`Unexpected API request: ${path}`)
}

function renderPage(route = '/portfolio/artworks/new') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const page = <ArtworkFormPage />
  const router = createMemoryRouter(
    [
      { path: '/portfolio/artworks/new', element: page },
      { path: '/portfolio/artworks/:artworkId', element: page },
      { path: '/portfolio', element: <div>Moj portfolio</div> },
      { path: '*', element: <div>Not found</div> },
    ],
    { initialEntries: [route] },
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

async function fillRequiredFields(user: ReturnType<typeof userEvent.setup>) {
  await user.type(
    await screen.findByRole('textbox', { name: 'Naslov' }),
    'Plavi horizont',
  )
  await user.type(screen.getByRole('spinbutton', { name: 'Godina' }), '2025')
  await user.type(
    screen.getByRole('textbox', { name: 'Tehnika' }),
    'Ulje na platnu',
  )
}

afterEach(() => {
  vi.clearAllMocks()
  saveError = undefined
  savedInput = undefined
})

describe('ArtworkFormPage', () => {
  it('validates required fields before making an API request', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Spremi' }))

    expect(await screen.findAllByText('Ovo polje je obavezno.')).toHaveLength(2)
    expect(screen.getByText('Godina je obavezna.')).toBeInTheDocument()
    expect(apiFetch).not.toHaveBeenCalledWith(
      '/artworks',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it(
    'saves the selected visibility and opens the saved artwork for image uploads',
    { timeout: 15000 },
    async () => {
      vi.mocked(apiFetch).mockImplementation(mockApiFetch)
      const user = userEvent.setup()
      const { queryClient, router } = renderPage()

      await fillRequiredFields(user)
      await user.click(screen.getByRole('button', { name: 'Javno' }))
      await user.click(screen.getByRole('button', { name: 'Spremi' }))

      await waitFor(() => {
        expect(router.state.location.pathname).toBe(
          `/portfolio/artworks/${artworkId}`,
        )
      })
      expect(savedInput).toEqual({
        title: 'Plavi horizont',
        year: 2025,
        technique: 'Ulje na platnu',
        dimensions: null,
        description: null,
        visibility: 'PUBLIC',
      })
      expect(
        queryClient.getQueryData(artworkQueryKey(artworkId)),
      ).toMatchObject({
        id: artworkId,
        visibility: 'PUBLIC',
      })
      expect(await screen.findByText('Djelo spremljeno')).toBeInTheDocument()
    },
  )

  it(
    'maps server validation details to the corresponding form field',
    { timeout: 15000 },
    async () => {
      vi.mocked(apiFetch).mockImplementation(mockApiFetch)
      saveError = new ApiError(400, {
        code: 'VALIDATION_ERROR',
        message: 'Neispravni podaci.',
        details: [{ field: 'title', issue: 'Naslov nije dopušten.' }],
      })
      const user = userEvent.setup()
      renderPage()

      await fillRequiredFields(user)
      await user.click(screen.getByRole('button', { name: 'Spremi' }))

      const title = await screen.findByRole('textbox', { name: 'Naslov' })
      expect(
        await screen.findByText('Naslov nije dopušten.'),
      ).toBeInTheDocument()
      expect(title).toHaveAttribute('aria-invalid', 'true')
      expect(title.getAttribute('aria-describedby')).toContain(
        'artwork-title-helper-text',
      )
    },
  )

  it(
    'loads hidden artworks and can submit an updated visibility',
    { timeout: 15000 },
    async () => {
      vi.mocked(apiFetch).mockImplementation(
        async <T,>(path: string, options: ApiFetchOptions = {}): Promise<T> => {
          if (path === '/me') return currentUser as T
          if (path === '/settings/public') {
            return {
              artworkMaxImageBytes: 5 * 1024 * 1024,
              artworkMaxImages: 3,
              applicationMaxImageBytes: 5 * 1024 * 1024,
              applicationMaxPdfBytes: 10 * 1024 * 1024,
              applicationMaxFiles: 10,
            } as T
          }
          if (path === `/artworks/${artworkId}` && !options.method) {
            return { ...savedArtwork, hiddenByAdmin: true } as T
          }
          if (path === `/artworks/${artworkId}` && options.method === 'PUT') {
            savedInput = options.json as ArtworkInput
            return { ...savedArtwork, ...savedInput } as T
          }
          throw new Error(`Unexpected API request: ${path}`)
        },
      )
      const user = userEvent.setup()
      const { router } = renderPage(`/portfolio/artworks/${artworkId}`)

      expect(
        await screen.findByText(
          'Skriveno od administratora — djelo nije javno vidljivo.',
        ),
      ).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Javno' }))
      await user.click(screen.getByRole('button', { name: 'Spremi' }))

      await waitFor(() => {
        expect(savedInput?.visibility).toBe('PUBLIC')
        expect(router.state.location.pathname).toBe(
          `/portfolio/artworks/${artworkId}`,
        )
      })
      expect(apiFetch).toHaveBeenCalledWith(
        `/artworks/${artworkId}`,
        expect.objectContaining({ method: 'PUT' }),
      )
    },
  )

  it('preserves unsaved form edits when artwork images are refreshed', async () => {
    let images: Artwork['images'] = []
    vi.mocked(apiFetch).mockImplementation(
      async <T,>(path: string, options: ApiFetchOptions = {}): Promise<T> => {
        if (path === '/me') return currentUser as T
        if (path === '/settings/public') {
          return {
            artworkMaxImageBytes: 5 * 1024 * 1024,
            artworkMaxImages: 3,
            applicationMaxImageBytes: 5 * 1024 * 1024,
            applicationMaxPdfBytes: 10 * 1024 * 1024,
            applicationMaxFiles: 10,
          } as T
        }
        if (
          path === `/artworks/${artworkId}/images` &&
          options.method === 'POST'
        ) {
          images = [uploadedImage]
          return uploadedImage as T
        }
        if (path === `/artworks/${artworkId}` && !options.method) {
          return { ...savedArtwork, images } as T
        }
        if (path === `/artworks/${artworkId}` && options.method === 'PUT') {
          savedInput = options.json as ArtworkInput
          return { ...savedArtwork, ...savedInput, images } as T
        }
        throw new Error(`Unexpected API request: ${path}`)
      },
    )
    const user = userEvent.setup()
    renderPage(`/portfolio/artworks/${artworkId}`)

    const title = await screen.findByRole('textbox', { name: 'Naslov' })
    await user.clear(title)
    await user.type(title, 'Novi nespremljeni naslov')
    await user.upload(
      await screen.findByLabelText('Odaberi sliku'),
      new File(['image'], 'artwork.jpg', { type: 'image/jpeg' }),
    )

    await waitFor(() => {
      expect(
        screen.getByRole('img', { name: 'Plavi horizont' }),
      ).toBeInTheDocument()
      expect(title).toHaveValue('Novi nespremljeni naslov')
    })
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    await waitFor(() => {
      expect(savedInput?.title).toBe('Novi nespremljeni naslov')
    })
  })

  it('shows not found when the artwork belongs to another artist', async () => {
    vi.mocked(apiFetch).mockImplementation(
      async <T,>(path: string): Promise<T> => {
        if (path === '/me') return currentUser as T
        if (path === `/artworks/${artworkId}`) {
          return {
            ...savedArtwork,
            artistId: '00000000-0000-4000-8000-000000000002',
          } as T
        }
        throw new Error(`Unexpected API request: ${path}`)
      },
    )
    renderPage(`/portfolio/artworks/${artworkId}`)

    expect(
      await screen.findByRole('heading', { name: 'Djelo nije pronađeno' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Natrag na portfolio' }),
    ).toHaveAttribute('href', '/portfolio')
    expect(
      screen.queryByRole('textbox', { name: 'Naslov' }),
    ).not.toBeInTheDocument()
  })
})
