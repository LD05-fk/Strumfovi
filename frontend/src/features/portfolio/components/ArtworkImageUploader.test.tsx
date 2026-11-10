import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, type ApiFetchOptions } from '../../../api/client'
import { theme } from '../../../app/theme'
import { ArtworkImageUploader } from './ArtworkImageUploader'
import type { Artwork } from '../types'

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>()
  return { ...actual, apiFetch: vi.fn() }
})

const artworkId = '00000000-0000-4000-8000-000000000011'
const artistId = '00000000-0000-4000-8000-000000000001'
const imageFile = new File(['image'], 'artwork.jpg', { type: 'image/jpeg' })
const uploadedImage = {
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

const artwork: Artwork = {
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

let uploadError: ApiError | undefined
let deleteError: ApiError | undefined
let publicSettings = {
  artworkMaxImageBytes: 5 * 1024 * 1024,
  artworkMaxImages: 3,
  applicationMaxImageBytes: 5 * 1024 * 1024,
  applicationMaxPdfBytes: 10 * 1024 * 1024,
  applicationMaxFiles: 10,
}

async function mockApiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  if (path === '/settings/public') return publicSettings as T
  if (path.endsWith('/images') && options.method === 'POST') {
    if (uploadError) throw uploadError
    return uploadedImage as T
  }
  if (path.endsWith(`/${uploadedImage.id}`) && options.method === 'DELETE') {
    if (deleteError) throw deleteError
    return undefined as T
  }
  throw new Error(`Unexpected API request: ${path}`)
}

function renderUploader(
  currentArtwork: Artwork | null = artwork,
  title = 'Plavi horizont',
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <ArtworkImageUploader
          artwork={currentArtwork ?? undefined}
          artistId={artistId}
          title={title}
        />
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

function getFileInput() {
  return screen.getByLabelText('Odaberi sliku') as HTMLInputElement
}

afterEach(() => {
  vi.clearAllMocks()
  uploadError = undefined
  deleteError = undefined
  publicSettings = {
    artworkMaxImageBytes: 5 * 1024 * 1024,
    artworkMaxImages: 3,
    applicationMaxImageBytes: 5 * 1024 * 1024,
    applicationMaxPdfBytes: 10 * 1024 * 1024,
    applicationMaxFiles: 10,
  }
})

describe('ArtworkImageUploader', () => {
  it('disables adding images until a new artwork is saved', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderUploader(null, '')

    expect(
      await screen.findByText('Spremite djelo kako biste dodali slike.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Odaberi sliku' })).toBeDisabled()
  })

  it('rejects a PDF before making an upload request', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    renderUploader()

    await screen.findByRole('button', { name: 'Odaberi sliku' })
    fireEvent.change(getFileInput(), {
      target: {
        files: [new File(['pdf'], 'document.pdf', { type: 'application/pdf' })],
      },
    })

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Dopuštene su samo JPG i PNG slike.',
    )
    expect(apiFetch).not.toHaveBeenCalledWith(
      expect.stringContaining('/images'),
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('rejects a file that exceeds the limit from public settings', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderUploader()

    await screen.findByRole('button', { name: 'Odaberi sliku' })
    const oversizedFile = new File(
      [new Uint8Array(5 * 1024 * 1024 + 1)],
      'large.jpg',
      { type: 'image/jpeg' },
    )
    await user.upload(getFileInput(), oversizedFile)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Slika je veća od 5 MB.',
    )
    expect(apiFetch).not.toHaveBeenCalledWith(
      expect.stringContaining('/images'),
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('disables the upload control when the configured image limit is reached', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    publicSettings.artworkMaxImages = 1
    renderUploader({ ...artwork, images: [uploadedImage] })

    expect(
      await screen.findByText(/Dosegnut je najveći broj slika/),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Odaberi sliku' }),
    ).not.toBeInTheDocument()
  })

  it('submits the prepared alt text and file as multipart form data', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderUploader()

    await screen.findByRole('button', { name: 'Odaberi sliku' })
    await user.upload(getFileInput(), imageFile)

    await waitFor(() => {
      expect(apiFetch).toHaveBeenCalledWith(
        `/artworks/${artworkId}/images`,
        expect.objectContaining({ method: 'POST', body: expect.any(FormData) }),
      )
    })
    const request = vi
      .mocked(apiFetch)
      .mock.calls.find(
        ([path, options]) =>
          path === `/artworks/${artworkId}/images` &&
          options?.method === 'POST',
      )
    const formData = request?.[1]?.body as FormData
    expect((formData.get('file') as File).name).toBe('artwork.jpg')
    expect(formData.get('altText')).toBe('Plavi horizont')
  })

  it('uses the artwork title when the alt text input is left empty', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderUploader()

    const altTextInput = await screen.findByRole('textbox', {
      name: 'Opis slike (alternativni tekst)',
    })
    expect(altTextInput).toHaveValue('')
    expect(altTextInput).toHaveAttribute('placeholder', 'Plavi horizont')
    await user.upload(getFileInput(), imageFile)

    await waitFor(() => {
      expect(apiFetch).toHaveBeenCalledWith(
        `/artworks/${artworkId}/images`,
        expect.objectContaining({ method: 'POST', body: expect.any(FormData) }),
      )
    })
    const request = vi
      .mocked(apiFetch)
      .mock.calls.find(
        ([path, options]) =>
          path === `/artworks/${artworkId}/images` &&
          options?.method === 'POST',
      )
    expect(request).toBeDefined()
    if (!request) throw new Error('Expected image upload request')
    expect((request[1].body as FormData).get('altText')).toBe('Plavi horizont')
  })

  it('sends a custom alt text instead of the artwork title', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderUploader()

    await user.type(
      await screen.findByRole('textbox', {
        name: 'Opis slike (alternativni tekst)',
      }),
      'Detalj poteza kistom',
    )
    await user.upload(getFileInput(), imageFile)

    await waitFor(() => {
      const request = vi
        .mocked(apiFetch)
        .mock.calls.find(
          ([path, options]) =>
            path === `/artworks/${artworkId}/images` &&
            options?.method === 'POST',
        )
      expect(request).toBeDefined()
      if (!request) return
      expect((request[1].body as FormData).get('altText')).toBe(
        'Detalj poteza kistom',
      )
    })
  })

  it('uploads selected files one at a time and stops at the image limit', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    publicSettings.artworkMaxImages = 2
    const user = userEvent.setup()
    renderUploader({ ...artwork, images: [uploadedImage] })

    await screen.findByRole('button', { name: 'Odaberi sliku' })
    await user.upload(getFileInput(), [
      new File(['first'], 'first.jpg', { type: 'image/jpeg' }),
      new File(['second'], 'second.jpg', { type: 'image/jpeg' }),
    ])

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Djelo već ima najveći dopušteni broj slika.',
    )
    expect(
      vi
        .mocked(apiFetch)
        .mock.calls.filter(
          ([path, options]) =>
            path === `/artworks/${artworkId}/images` &&
            options?.method === 'POST',
        ),
    ).toHaveLength(1)
  })

  it('asks for confirmation before deleting an existing image', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderUploader({ ...artwork, images: [uploadedImage] })

    await user.click(await screen.findByRole('button', { name: 'Ukloni' }))
    const dialog = await screen.findByRole('dialog', {
      name: 'Ukloniti sliku?',
    })
    expect(apiFetch).not.toHaveBeenCalledWith(
      `/artworks/${artworkId}/images/${uploadedImage.id}`,
      expect.objectContaining({ method: 'DELETE' }),
    )

    await user.click(within(dialog).getByRole('button', { name: 'Ukloni' }))
    await waitFor(() => {
      expect(apiFetch).toHaveBeenCalledWith(
        `/artworks/${artworkId}/images/${uploadedImage.id}`,
        expect.objectContaining({ method: 'DELETE' }),
      )
    })
  })

  it('closes the confirmation and shows an error when deleting fails', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    deleteError = new ApiError(404, {
      code: 'NOT_FOUND',
      message: 'Server error.',
    })
    const user = userEvent.setup()
    renderUploader({ ...artwork, images: [uploadedImage] })

    await user.click(await screen.findByRole('button', { name: 'Ukloni' }))
    const dialog = await screen.findByRole('dialog', {
      name: 'Ukloniti sliku?',
    })
    await user.click(within(dialog).getByRole('button', { name: 'Ukloni' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Djelo nije pronađeno ili više nije dostupno.',
    )
    expect(
      screen.queryByRole('dialog', { name: 'Ukloniti sliku?' }),
    ).not.toBeInTheDocument()
  })

  it.each([
    [415, 'UNSUPPORTED_FILE_TYPE', 'Dopuštene su samo JPG i PNG slike.'],
    [413, 'FILE_TOO_LARGE', 'Slika je veća od 5 MB.'],
    [409, 'IMAGE_LIMIT_REACHED', 'Djelo već ima najveći dopušteni broj slika.'],
    [404, 'NOT_FOUND', 'Djelo nije pronađeno ili više nije dostupno.'],
  ])(
    'shows the server message for HTTP %s image errors',
    async (status, code, message) => {
      vi.mocked(apiFetch).mockImplementation(mockApiFetch)
      uploadError = new ApiError(status, { code, message: 'Server error.' })
      const user = userEvent.setup()
      renderUploader()

      await screen.findByRole('button', { name: 'Odaberi sliku' })
      await user.upload(getFileInput(), imageFile)

      expect(await screen.findByRole('alert')).toHaveTextContent(message)
    },
  )
})
