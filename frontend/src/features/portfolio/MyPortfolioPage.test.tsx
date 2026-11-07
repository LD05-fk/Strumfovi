import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, type ApiFetchOptions } from '../../api/client'
import { NotifyProvider } from '../../components/state/NotifyProvider'
import { renderWithProviders } from '../../test/renderWithProviders'
import type { CurrentUser } from '../auth/types'
import { MyPortfolioPage } from './MyPortfolioPage'
import type { Artwork, ArtworkPage } from './types'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, apiFetch: vi.fn() }
})

const artistId = '00000000-0000-4000-8000-000000000001'
const currentUser: CurrentUser = {
  id: artistId,
  displayName: 'Ana Umjetnica',
  email: 'ana@example.test',
  avatarUrl: null,
  role: 'ARTIST',
  status: 'ACTIVE',
  organizerApproval: null,
}

const publicArtwork: Artwork = {
  id: '00000000-0000-4000-8000-000000000011',
  artistId,
  title: 'Plavi horizont',
  year: 2025,
  technique: 'Ulje na platnu',
  dimensions: null,
  description: null,
  visibility: 'PUBLIC',
  hiddenByAdmin: false,
  images: [
    {
      id: '00000000-0000-4000-8000-000000000021',
      file: {
        id: '00000000-0000-4000-8000-000000000031',
        originalName: 'horizont.jpg',
        contentType: 'image/jpeg',
        sizeBytes: 1200,
        url: '/api/v1/files/00000000-0000-4000-8000-000000000031',
      },
      altText: null,
    },
  ],
  createdAt: '2026-01-01T10:00:00Z',
}

const privateArtwork: Artwork = {
  ...publicArtwork,
  id: '00000000-0000-4000-8000-000000000012',
  title: 'Tihi dvor',
  visibility: 'PRIVATE',
  hiddenByAdmin: true,
  images: [],
}

let pageResponse: ArtworkPage = {
  data: [publicArtwork, privateArtwork],
  meta: { page: 1, pageSize: 4, total: 2 },
}

async function mockApiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  if (path === '/me') return currentUser as T
  const pageRequest = path.match(/\/artworks\?page=(\d+)&pageSize=(\d+)/)
  if (pageRequest?.[1] === '1') {
    return {
      ...pageResponse,
      meta: { ...pageResponse.meta, pageSize: Number(pageRequest[2]) },
    } as T
  }
  if (pageRequest?.[1] === '2') {
    return {
      data: [privateArtwork],
      meta: { page: 2, pageSize: Number(pageRequest[2]), total: 13 },
    } as T
  }
  if (path.startsWith('/artworks/') && options.method === 'DELETE') {
    return undefined as T
  }
  throw new Error(`Unexpected API request: ${path}`)
}

function renderPage(route = '/portfolio?page=1') {
  return renderWithProviders(
    <NotifyProvider>
      <MyPortfolioPage />
    </NotifyProvider>,
    { route },
  )
}

afterEach(() => {
  vi.clearAllMocks()
  pageResponse = {
    data: [publicArtwork, privateArtwork],
    meta: { page: 1, pageSize: 4, total: 2 },
  }
})

describe('MyPortfolioPage', () => {
  it('shows visibility and hidden labels and uses image alt text', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderPage()

    expect(await screen.findByText('Plavi horizont')).toBeInTheDocument()
    expect(screen.getAllByText('Javno')).toHaveLength(1)
    expect(screen.getAllByText('Privatno')).toHaveLength(1)
    expect(screen.getByText('Skriveno od administratora')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Plavi horizont' })).toHaveAttribute(
      'loading',
      'lazy',
    )
    expect(
      screen.getByRole('img', {
        name: 'Tihi dvor — nema fotografije',
      }),
    ).toBeInTheDocument()
  })

  it('shows the empty state and a link to create the first artwork', async () => {
    pageResponse = { data: [], meta: { page: 1, pageSize: 4, total: 0 } }
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderPage()

    expect(await screen.findByText('Još nemate djela')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Dodaj prvo djelo' }),
    ).toHaveAttribute('href', '/portfolio/artworks/new')
  })

  it('replaces an out-of-range URL page with the last page when artworks exist', async () => {
    let expectedLastPage = 0
    vi.mocked(apiFetch).mockImplementation(
      async <T,>(path: string): Promise<T> => {
        if (path === '/me') return currentUser as T
        const pageRequest = path.match(/\/artworks\?page=(\d+)&pageSize=(\d+)/)
        if (!pageRequest) throw new Error(`Unexpected API request: ${path}`)

        const requestedPage = Number(pageRequest[1])
        const pageSize = Number(pageRequest[2])
        expectedLastPage = Math.ceil(20 / pageSize)
        return {
          data: requestedPage === 99 ? [] : [publicArtwork],
          meta: {
            page: requestedPage,
            pageSize,
            total: 20,
          },
        } as T
      },
    )
    const { router } = renderPage('/portfolio?page=99')

    expect(await screen.findByText('Plavi horizont')).toBeInTheDocument()
    await waitFor(() => {
      expect(router.state.location.search).toBe(`?page=${expectedLastPage}`)
    })
  })

  it('limits each page to two artwork rows and shows pagination beneath them', async () => {
    const artworks = Array.from({ length: 4 }, (_, index) => ({
      ...publicArtwork,
      id: `00000000-0000-4000-8000-${String(index + 11).padStart(12, '0')}`,
      title: `Djelo ${index + 1}`,
    }))
    pageResponse = {
      data: artworks,
      meta: { page: 1, pageSize: 4, total: 5 },
    }
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderPage()

    expect(await screen.findByText('Djelo 1')).toBeInTheDocument()
    expect(screen.getAllByRole('article')).toHaveLength(4)
    expect(
      screen.getByRole('navigation', { name: 'Straničenje portfolija' }),
    ).toBeInTheDocument()
    expect(apiFetch).toHaveBeenCalledWith(
      `/artists/${artistId}/artworks?page=1&pageSize=4`,
    )
  })

  it('deletes only after confirmation and notifies on success', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Plavi horizont')
    await user.click(
      within(screen.getAllByRole('article')[0]).getByRole('button', {
        name: 'Obriši: Plavi horizont',
      }),
    )
    const dialog = screen.getByRole('dialog')
    expect(dialog).toHaveTextContent('Plavi horizont')
    expect(
      vi
        .mocked(apiFetch)
        .mock.calls.some(
          ([path, options]) =>
            path === `/artworks/${publicArtwork.id}` &&
            options?.method === 'DELETE',
        ),
    ).toBe(false)

    await user.click(within(dialog).getByRole('button', { name: 'Obriši' }))
    expect(await screen.findByText('Djelo obrisano')).toBeInTheDocument()
    expect(apiFetch).toHaveBeenCalledWith(
      `/artworks/${publicArtwork.id}`,
      expect.objectContaining({ method: 'DELETE' }),
    )
  })

  it('uses the URL page and returns to the previous page after deleting its last artwork', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    const { router } = renderPage('/portfolio?page=2')

    await screen.findByText('Tihi dvor')
    const card = screen.getByRole('article')
    await user.click(
      within(card).getByRole('button', { name: 'Obriši: Tihi dvor' }),
    )
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', {
        name: 'Obriši',
      }),
    )

    await waitFor(() => {
      expect(router.state.location.search).toBe('?page=1')
    })
  })
})
