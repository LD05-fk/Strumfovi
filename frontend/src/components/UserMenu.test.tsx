import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useCurrentUser } from '../features/auth/useCurrentUser'
import { useLogout } from '../features/auth/useLogout'
import type { CurrentUser } from '../features/auth/types'
import { useNotify } from './state/useNotify'
import { UserMenu } from './UserMenu'

vi.mock('../features/auth/useCurrentUser', () => ({
  useCurrentUser: vi.fn(),
}))

vi.mock('../features/auth/useLogout', () => ({
  useLogout: vi.fn(),
}))

vi.mock('./state/useNotify', () => ({
  useNotify: vi.fn(),
}))

vi.mock('./GoogleSignInButton', () => ({
  GoogleSignInButton: () => (
    <a href="/oauth2/authorization/google" data-testid="google-sign-in">
      Prijava putem Googlea
    </a>
  ),
}))

const mockUser: CurrentUser = {
  id: '123',
  displayName: 'Testni Korisnik',
  email: 'test@example.com',
  avatarUrl: null,
  role: 'ARTIST',
  status: 'ACTIVE',
  organizerApproval: null,
}

const mockNotify = vi.fn()
const mockMutate = vi.fn()

function renderUserMenu() {
  return render(
    <MemoryRouter>
      <UserMenu />
    </MemoryRouter>,
  )
}

describe('UserMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    vi.mocked(useCurrentUser).mockReturnValue({
      data: mockUser,
      isPending: false,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)

    vi.mocked(useLogout).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useLogout>)

    vi.mocked(useNotify).mockReturnValue(mockNotify)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('prikazuje indikator učitavanja dok se korisnik dohvaća', () => {
    vi.mocked(useCurrentUser).mockReturnValue({
      data: undefined,
      isPending: true,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)

    renderUserMenu()

    expect(screen.getByTestId('user-menu-loading')).toBeInTheDocument()
    expect(screen.queryByTestId('user-menu')).not.toBeInTheDocument()
  })

  it('prikazuje gumb za prijavu kada korisnik nije prijavljen', () => {
    vi.mocked(useCurrentUser).mockReturnValue({
      data: null,
      isPending: false,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)

    renderUserMenu()

    expect(screen.getByTestId('google-sign-in')).toHaveAttribute(
      'href',
      '/oauth2/authorization/google',
    )
    expect(screen.queryByTestId('user-menu')).not.toBeInTheDocument()
  })

  it('prikazuje avatar korisnika kada je prijavljen', () => {
    renderUserMenu()

    expect(screen.getByTestId('user-menu')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Korisnički izbornik' }),
    ).toBeInTheDocument()
    expect(screen.getByText('TK')).toBeInTheDocument()
  })

  it('otvara izbornik s podacima i opcijama korisnika', async () => {
    renderUserMenu()

    fireEvent.click(screen.getByTestId('user-menu'))

    expect(await screen.findByText('Testni Korisnik')).toBeInTheDocument()
    expect(screen.getByText('test@example.com')).toBeInTheDocument()
    expect(screen.getByText('Umjetnik')).toBeInTheDocument()

    expect(screen.getByRole('menuitem', { name: 'Profil' })).toHaveAttribute(
      'href',
      '/profile',
    )

    expect(screen.getByTestId('logout')).toBeInTheDocument()
  })

  it('poziva logout kada korisnik odabere odjavu', async () => {
    renderUserMenu()

    fireEvent.click(screen.getByTestId('user-menu'))
    fireEvent.click(await screen.findByTestId('logout'))

    expect(mockMutate).toHaveBeenCalledTimes(1)
  })

  it('onemogućuje odjavu dok je zahtjev u tijeku', async () => {
    vi.mocked(useLogout).mockReturnValue({
      mutate: mockMutate,
      isPending: true,
      isError: false,
    } as unknown as ReturnType<typeof useLogout>)

    renderUserMenu()

    fireEvent.click(screen.getByTestId('user-menu'))

    const logoutItem = await screen.findByTestId('logout')

    expect(logoutItem).toHaveAttribute('aria-disabled', 'true')

    fireEvent.click(logoutItem)

    expect(mockMutate).not.toHaveBeenCalled()
  })

  it('prikazuje obavijest ako dohvat korisnika ne uspije', async () => {
    vi.mocked(useCurrentUser).mockReturnValue({
      data: undefined,
      isPending: false,
      isError: true,
    } as ReturnType<typeof useCurrentUser>)

    renderUserMenu()

    await waitFor(() => {
      expect(mockNotify).toHaveBeenCalledWith(expect.any(String), 'error')
    })

    expect(screen.queryByTestId('user-menu')).not.toBeInTheDocument()
    expect(screen.queryByTestId('google-sign-in')).not.toBeInTheDocument()
  })

  it('prikazuje sliku avatara kada avatarUrl postoji', () => {
    vi.mocked(useCurrentUser).mockReturnValue({
      data: {
        ...mockUser,
        avatarUrl: 'https://example.com/avatar.png',
      },
      isPending: false,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)

    renderUserMenu()

    expect(
      screen.getByRole('img', { name: 'Testni Korisnik' }),
    ).toHaveAttribute('src', 'https://example.com/avatar.png')
  })
})
