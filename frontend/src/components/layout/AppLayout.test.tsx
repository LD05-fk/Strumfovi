import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import { AppLayout } from './AppLayout'
import { NavItems } from './NavItems'
import { getNavigationItems, type NavigationRole } from './navigation'

afterEach(cleanup)

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
  function renderLayout(role: NavigationRole = null) {
    return render(
      <MemoryRouter initialEntries={['/opportunities']}>
        <Routes>
          <Route element={<AppLayout role={role} />}>
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
    renderLayout('ARTIST')

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
    renderLayout('ARTIST')

    await user.click(screen.getByRole('button', { name: 'Otvori izbornik' }))
    await user.click(screen.getByRole('link', { name: 'Portfolio' }))

    await waitFor(() => {
      expect(
        screen.queryByRole('navigation', { name: 'Mobilna navigacija' }),
      ).toBeNull()
    })
  })
})
