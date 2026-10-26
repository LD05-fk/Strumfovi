import {
  cleanup,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useCurrentUser } from '../../features/auth/useCurrentUser'
import { useLogout } from '../../features/auth/useLogout'
import type { CurrentUser } from '../../features/auth/types'
import { useNotify } from '../state/useNotify'
import { AppLayout } from './AppLayout'
import { NavItems } from './NavItems'
import { getNavigationItems, type NavigationRole } from './navigation'

vi.mock('../../features/auth/useCurrentUser', () => ({
  useCurrentUser: vi.fn(),
}))

vi.mock('../../features/auth/useLogout', () => ({
  useLogout: vi.fn(),
}))

vi.mock('../state/useNotify', () => ({
  useNotify: vi.fn(),
}))

afterEach(cleanup)

const mockUser: CurrentUser = {
  id: '123',
  displayName: 'Testni Korisnik',
  email: 'test@example.com',
  avatarUrl: null,
  role: 'ARTIST',
  status: 'ACTIVE',
  organizerApproval: null,
}

const mockLogout = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(useCurrentUser).mockReturnValue({
    data: null,
    isPending: false,
    isError: false,
  } as ReturnType<typeof useCurrentUser>)
  vi.mocked(useLogout).mockReturnValue({
    mutate: mockLogout,
    isPending: false,
    isError: false,
  } as unknown as ReturnType<typeof useLogout>)
  vi.mocked(useNotify).mockReturnValue(vi.fn())
})

const roleCases: Array<{
  role: NavigationRole
  expected: string[]
}> = [
  { role: null, expected: ['Prilike'] },
  {
    role: 'ARTIST',
    expected: ['Prilike', 'Portfolio', 'Moje prijave'],
  },
  {
    role: 'ORGANIZER',
    expected: ['Prilike', 'Moje prilike', 'Statistika'],
  },
  { role: 'ADMIN', expected: ['Prilike', 'Administracija'] },
]

describe('role-based navigation', () => {
  it.each(roleCases)(
    'shows the expected links for $role',
    ({ role, expected }) => {
      expect(getNavigationItems(role).map((item) => item.label)).toEqual(
        expected,
      )

      const { container } = render(
        <MemoryRouter initialEntries={['/opportunities']}>
          <NavItems role={role} />
        </MemoryRouter>,
      )

      expect(
        Array.from(container.querySelectorAll('a')).map(
          (link) => link.textContent,
        ),
      ).toEqual(expected)
      expect(
        Array.from(container.querySelectorAll('a')).every(
          (link) => link.tabIndex === 0,
        ),
      ).toBe(true)
    },
  )
})

describe('AppLayout', () => {
  function renderLayout() {
    return render(
      <MemoryRouter initialEntries={['/opportunities']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="opportunities" element={<h1>Prilike</h1>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )
  }

  it('provides a skip link, main content and footer', () => {
    renderLayout()

    expect(
      screen.getByRole('link', { name: 'Preskoči na sadržaj' }),
    ).toBeTruthy()
    expect(screen.getByRole('main').id).toBe('main-content')
    expect(screen.getByRole('contentinfo')).toBeTruthy()
  })

  it('opens the mobile drawer and closes it with Escape', async () => {
    const user = userEvent.setup()
    vi.mocked(useCurrentUser).mockReturnValue({
      data: mockUser,
      isPending: false,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)
    renderLayout()

    await user.click(screen.getByRole('button', { name: 'Otvori izbornik' }))
    expect(
      screen.getByRole('navigation', { name: 'Mobilna navigacija' }),
    ).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Portfolio' })).toBeTruthy()

    await user.keyboard('{Escape}')
    await waitFor(() => {
      expect(
        screen.queryByRole('navigation', { name: 'Mobilna navigacija' }),
      ).toBeNull()
    })
  })

  it('closes the drawer after choosing a navigation link', async () => {
    const user = userEvent.setup()
    vi.mocked(useCurrentUser).mockReturnValue({
      data: mockUser,
      isPending: false,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)
    renderLayout()

    await user.click(screen.getByRole('button', { name: 'Otvori izbornik' }))
    await user.click(screen.getByRole('link', { name: 'Portfolio' }))

    await waitFor(() => {
      expect(
        screen.queryByRole('navigation', { name: 'Mobilna navigacija' }),
      ).toBeNull()
    })
  })

  it('renders UserMenu and role-based navigation for the signed-in user', async () => {
    const user = userEvent.setup()
    vi.mocked(useCurrentUser).mockReturnValue({
      data: mockUser,
      isPending: false,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)
    renderLayout()

    expect(screen.getByTestId('user-menu')).toBeInTheDocument()
    expect(screen.queryByTestId('google-sign-in')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Otvori izbornik' }))
    expect(
      within(
        screen.getByRole('navigation', { name: 'Mobilna navigacija' }),
      ).getByRole('link', { name: 'Portfolio' }),
    ).toBeInTheDocument()

    await user.click(screen.getByTestId('user-menu'))
    expect(await screen.findByTestId('logout')).toBeInTheDocument()
  })

  it('shows Google sign-in in the mobile drawer to a visitor', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByRole('button', { name: 'Otvori izbornik' }))

    const mobileNavigation = screen.getByRole('navigation', {
      name: 'Mobilna navigacija',
    })
    expect(
      mobileNavigation.querySelector('[data-testid="google-sign-in"]'),
    ).toHaveAttribute('href', '/oauth2/authorization/google')
  })

  it('shows profile only to artists and organizers in the mobile drawer', async () => {
    const user = userEvent.setup()
    vi.mocked(useCurrentUser).mockReturnValue({
      data: { ...mockUser, role: 'ADMIN' },
      isPending: false,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)
    renderLayout()

    await user.click(screen.getByRole('button', { name: 'Otvori izbornik' }))

    const mobileNavigation = screen.getByRole('navigation', {
      name: 'Mobilna navigacija',
    })
    expect(
      mobileNavigation.querySelector('a[href="/profile"]'),
    ).not.toBeInTheDocument()
    expect(
      within(mobileNavigation).getByRole('button', { name: 'Odjava' }),
    ).toBeInTheDocument()
  })

  it('logs out from the mobile drawer', async () => {
    const user = userEvent.setup()
    vi.mocked(useCurrentUser).mockReturnValue({
      data: mockUser,
      isPending: false,
      isError: false,
    } as ReturnType<typeof useCurrentUser>)
    renderLayout()

    await user.click(screen.getByRole('button', { name: 'Otvori izbornik' }))
    await user.click(
      within(
        screen.getByRole('navigation', { name: 'Mobilna navigacija' }),
      ).getByRole('button', { name: 'Odjava' }),
    )

    expect(mockLogout).toHaveBeenCalledTimes(1)
  })
})
