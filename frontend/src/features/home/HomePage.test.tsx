import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { MemoryRouter } from 'react-router'
import { HomePage } from './HomePage'
import { hr } from '../../i18n/hr'

afterEach(cleanup)

describe('HomePage', () => {
  it('renders the welcome message and both calls to action', async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: hr.home.title })).toBeTruthy()
    expect(screen.getByText(hr.home.description)).toBeTruthy()
    expect(
      screen
        .getByRole('link', { name: 'Prijava s Googleom' })
        .getAttribute('href'),
    ).toBe('/oauth2/authorization/google')
    expect(
      screen
        .getByRole('link', { name: hr.home.browseOpportunities })
        .getAttribute('href'),
    ).toBe('/opportunities')
    await waitFor(() =>
      expect(document.title).toBe(`${hr.home.title} | ${hr.appName}`),
    )
  })
})
