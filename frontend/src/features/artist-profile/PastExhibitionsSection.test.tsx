import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, type ApiFetchOptions } from '../../api/client'
import { NotifyProvider } from '../../components/state/NotifyProvider'
import { renderWithProviders } from '../../test/renderWithProviders'
import { PastExhibitionsSection } from './PastExhibitionsSection'
import { currentZagrebYear } from './pastExhibitionSchema'
import type {
  ArtistPublicProfile,
  PastExhibition,
  PastExhibitionInput,
} from './types'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, apiFetch: vi.fn() }
})

const userId = '00000000-0000-4000-8000-000000000001'
const original: PastExhibition = {
  id: '00000000-0000-4000-8000-000000000002',
  title: 'Izložba „Svjetlo”',
  year: 2024,
  place: 'Zagreb',
}
let exhibitions: PastExhibition[] = []
let lastInput: PastExhibitionInput | undefined
let saveError: ApiError | undefined

async function mockApiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  if (path === `/artists/${userId}`) {
    const response: ArtistPublicProfile = {
      userId,
      displayName: 'Ana Umjetnica',
      avatarUrl: null,
      biography: null,
      artistStatement: null,
      artFields: [],
      links: [],
      pastExhibitions: exhibitions,
    }
    return response as T
  }
  if (path === '/past-exhibitions' && options.method === 'POST') {
    if (saveError) throw saveError
    lastInput = options.json as PastExhibitionInput
    const saved = { id: 'new-exhibition', ...lastInput }
    exhibitions = [...exhibitions, saved]
    return saved as T
  }
  if (path.startsWith('/past-exhibitions/') && options.method === 'PUT') {
    lastInput = options.json as PastExhibitionInput
    const id = path.split('/').at(-1)
    exhibitions = exhibitions.map((exhibition) =>
      exhibition.id === id ? { id, ...lastInput } : exhibition,
    )
    return exhibitions.find((exhibition) => exhibition.id === id) as T
  }
  if (path.startsWith('/past-exhibitions/') && options.method === 'DELETE') {
    const id = path.split('/').at(-1)
    exhibitions = exhibitions.filter((exhibition) => exhibition.id !== id)
    return undefined as T
  }
  throw new Error(`Unexpected API request: ${path}`)
}

function renderSection() {
  return renderWithProviders(
    <NotifyProvider>
      <PastExhibitionsSection userId={userId} />
    </NotifyProvider>,
    { route: '/profile' },
  )
}

afterEach(() => {
  vi.clearAllMocks()
  exhibitions = []
  lastInput = undefined
  saveError = undefined
})

describe('PastExhibitionsSection', () => {
  it('creates an exhibition and refreshes the list', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderSection()

    await user.click(await screen.findByRole('button', { name: '+ Dodaj' }))
    await user.type(
      screen.getByRole('textbox', { name: 'Naslov' }),
      'Nova radionica',
    )
    await user.type(screen.getByRole('textbox', { name: 'Mjesto' }), 'Rijeka')
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(
      await screen.findByRole('heading', { name: 'Nova radionica' }),
    ).toBeInTheDocument()
    expect(lastInput).toEqual({
      title: 'Nova radionica',
      year: currentZagrebYear(),
      place: 'Rijeka',
    })
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Izložba spremljena',
    )
  })

  it('shows local validation errors without sending an invalid form', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderSection()

    await user.click(await screen.findByRole('button', { name: '+ Dodaj' }))
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(await screen.findAllByText('Ovo polje je obavezno.')).toHaveLength(2)
    expect(lastInput).toBeUndefined()
    expect(apiFetch).not.toHaveBeenCalledWith(
      '/past-exhibitions',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('rejects a year later than the current year in Zagreb', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderSection()

    await user.click(await screen.findByRole('button', { name: '+ Dodaj' }))
    await user.type(
      screen.getByRole('textbox', { name: 'Naslov' }),
      'Nova izložba',
    )
    await user.type(screen.getByRole('textbox', { name: 'Mjesto' }), 'Zagreb')
    const year = screen.getByRole('spinbutton', { name: 'Godina' })
    await user.clear(year)
    await user.type(year, String(currentZagrebYear() + 1))
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(
      await screen.findByText('Godina ne smije biti u budućnosti.'),
    ).toBeInTheDocument()
    expect(apiFetch).not.toHaveBeenCalledWith(
      '/past-exhibitions',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('maps server validation errors to the matching field', async () => {
    saveError = new ApiError(400, {
      code: 'VALIDATION_ERROR',
      message: 'Neispravni podaci.',
      details: [{ field: 'title', issue: 'Naslov nije dopušten.' }],
    })
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderSection()

    await user.click(await screen.findByRole('button', { name: '+ Dodaj' }))
    await user.type(
      screen.getByRole('textbox', { name: 'Naslov' }),
      'Naziv radionice',
    )
    await user.type(screen.getByRole('textbox', { name: 'Mjesto' }), 'Zagreb')
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(await screen.findByText('Naslov nije dopušten.')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Naslov' })).toHaveAttribute(
      'aria-invalid',
      'true',
    )
  })

  it('edits an exhibition', async () => {
    exhibitions = [original]
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderSection()

    await user.click(
      await screen.findByRole('button', { name: 'Uredi: Izložba „Svjetlo”' }),
    )
    const dialog = screen.getByRole('dialog', {
      name: 'Uredi izložbu ili projekt',
    })
    const title = within(dialog).getByRole('textbox', { name: 'Naslov' })
    await user.clear(title)
    await user.type(title, 'Nova izložba')
    await user.click(within(dialog).getByRole('button', { name: 'Spremi' }))

    expect(
      await screen.findByRole('heading', { name: 'Nova izložba' }),
    ).toBeInTheDocument()
    expect(lastInput?.title).toBe('Nova izložba')
    expect(apiFetch).toHaveBeenCalledWith(
      `/past-exhibitions/${original.id}`,
      expect.objectContaining({ method: 'PUT' }),
    )
  })

  it('deletes an exhibition after confirmation', async () => {
    exhibitions = [original]
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderSection()

    await user.click(
      await screen.findByRole('button', { name: 'Obriši: Izložba „Svjetlo”' }),
    )
    const dialog = screen.getByRole('alertdialog', {
      name: 'Obrisati izložbu?',
    })
    expect(
      within(dialog).getByText(/bit će trajno uklonjena/),
    ).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Obriši' }))

    await waitFor(() => {
      expect(
        screen.getByText('Još niste dodali prethodne izložbe'),
      ).toBeInTheDocument()
    })
    expect(apiFetch).toHaveBeenCalledWith(
      `/past-exhibitions/${original.id}`,
      expect.objectContaining({ method: 'DELETE' }),
    )
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Izložba obrisana',
    )
  })
})
