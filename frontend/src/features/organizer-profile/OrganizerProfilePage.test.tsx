import { fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, type ApiFetchOptions } from '../../api/client'
import { NotifyProvider } from '../../components/state/NotifyProvider'
import { renderWithProviders } from '../../test/renderWithProviders'
import type { CurrentUser } from '../auth/types'
import { OrganizerProfilePage } from './OrganizerProfilePage'
import type { OrganizerProfile, OrganizerProfileUpdate } from './types'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, apiFetch: vi.fn() }
})

const profile: OrganizerProfile = {
  userId: '00000000-0000-4000-8000-000000000001',
  displayName: 'Goran Grgić',
  organizationName: 'Galerija Gradec',
  organizerApproval: 'PENDING',
}

const currentUser: CurrentUser = {
  id: profile.userId,
  displayName: profile.displayName,
  email: 'galerija.gradec@gmail.com',
  avatarUrl: null,
  role: 'ORGANIZER',
  status: 'ACTIVE',
  organizerApproval: 'PENDING',
}

let profileResponse = profile
let savedUpdate: OrganizerProfileUpdate | undefined
let updateError: ApiError | undefined

async function mockApiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  if (path === '/me') {
    return currentUser as T
  }

  if (path !== '/me/organizer-profile') {
    throw new Error(`Unexpected API request: ${path}`)
  }

  if (options.method === 'PUT') {
    if (updateError) {
      throw updateError
    }

    savedUpdate = options.json as OrganizerProfileUpdate
    profileResponse = { ...profileResponse, ...savedUpdate }
  }

  return profileResponse as T
}

function renderPage() {
  return renderWithProviders(
    <NotifyProvider>
      <OrganizerProfilePage />
    </NotifyProvider>,
    { route: '/profile' },
  )
}

afterEach(() => {
  vi.clearAllMocks()
  profileResponse = profile
  savedUpdate = undefined
  updateError = undefined
})

describe('OrganizerProfilePage', () => {
  it('shows a loading state while the profile request is pending', () => {
    vi.mocked(apiFetch).mockReturnValue(new Promise<never>(() => {}))
    renderPage()

    expect(
      screen.getByRole('status', { name: 'Učitavanje…' }),
    ).toBeInTheDocument()
  })

  it('saves the organization name and shows a success message', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderPage()

    const organizationName = await screen.findByRole('textbox', {
      name: 'Naziv organizacije',
    })
    expect(screen.getByRole('img', { name: 'Goran Grgić' })).toBeInTheDocument()
    expect(screen.getByText('Galerija Gradec')).toBeInTheDocument()
    expect(
      await screen.findByText('galerija.gradec@gmail.com'),
    ).toBeInTheDocument()
    expect(screen.getByText('Čeka odobrenje')).toBeInTheDocument()
    expect(
      screen.getByText(
        'Dok administrator ne odobri vaš račun, ne možete objavljivati prilike.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('15/200')).toBeInTheDocument()
    expect(
      screen.getByRole('textbox', {
        name: 'Ime i prezime (s Google računa)',
      }),
    ).toHaveAttribute('readonly')

    await user.clear(organizationName)
    await user.type(organizationName, 'Galerija Novo')
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(await screen.findByText('Profil spremljen')).toBeInTheDocument()
    expect(savedUpdate).toEqual({ organizationName: 'Galerija Novo' })
  })

  it('sends null when the organization name is cleared', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderPage()

    const organizationName = await screen.findByRole('textbox', {
      name: 'Naziv organizacije',
    })
    await user.clear(organizationName)
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(await screen.findByText('Profil spremljen')).toBeInTheDocument()
    expect(savedUpdate).toEqual({ organizationName: null })
  })

  it('rejects organization names longer than 200 characters without saving', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    const user = userEvent.setup()
    renderPage()

    const organizationName = await screen.findByRole('textbox', {
      name: 'Naziv organizacije',
    })
    await user.clear(organizationName)
    fireEvent.change(organizationName, {
      target: { value: 'a'.repeat(201) },
    })
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(
      await screen.findByText(
        'Naziv organizacije smije imati najviše 200 znakova.',
      ),
    ).toBeInTheDocument()
    expect(savedUpdate).toBeUndefined()
  })

  it('shows server validation details beside the organization name field', async () => {
    vi.mocked(apiFetch).mockImplementation(mockApiFetch)
    updateError = new ApiError(400, {
      code: 'VALIDATION_ERROR',
      message: 'Neispravni podaci.',
      details: [
        {
          field: 'organizationName',
          issue: 'Naziv organizacije nije prihvaćen.',
        },
      ],
    })
    const user = userEvent.setup()
    renderPage()

    const organizationName = await screen.findByRole('textbox', {
      name: 'Naziv organizacije',
    })
    await user.type(organizationName, ' Novo')
    await user.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(
      await screen.findByText('Naziv organizacije nije prihvaćen.'),
    ).toBeInTheDocument()
    expect(organizationName).toHaveAttribute('aria-invalid', 'true')
  })
})
