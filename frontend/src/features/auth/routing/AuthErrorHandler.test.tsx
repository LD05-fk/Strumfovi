import { QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { queryClient } from '../../../app/queryClient'
import { API_ERROR_EVENT, type ApiErrorEventDetail } from '../../../api/client'
import { AuthErrorHandler } from './AuthErrorHandler'
import { isProtectedRoutePath } from './routeAccess'

function LocationDisplay() {
  const location = useLocation()
  return <div data-testid="location">{location.pathname}</div>
}

function renderHandler(route: string) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <AuthErrorHandler />
        <LocationDisplay />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function dispatchApiError(detail: ApiErrorEventDetail) {
  fireEvent(
    window,
    new CustomEvent<ApiErrorEventDetail>(API_ERROR_EVENT, { detail }),
  )
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('AuthErrorHandler', () => {
  it('navigira na onboarding za 403 ONBOARDING_REQUIRED', async () => {
    renderHandler('/opportunities')

    dispatchApiError({
      status: 403,
      code: 'ONBOARDING_REQUIRED',
      path: '/opportunities',
    })

    await waitFor(() => {
      expect(screen.getByTestId('location')).toHaveTextContent('/onboarding')
    })
  })

  it('invalidira sesiju nakon 401 na zaštićenoj ruti', async () => {
    const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries')
    renderHandler('/profile')

    dispatchApiError({
      status: 401,
      code: 'UNAUTHENTICATED',
      path: '/applications',
    })

    await waitFor(() => {
      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: ['me'],
      })
    })
  })

  it('ne invalidira sesiju na javnoj ruti ni za 401 poziva /me', () => {
    const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries')
    renderHandler('/opportunities')

    dispatchApiError({
      status: 401,
      code: 'UNAUTHENTICATED',
      path: '/opportunities',
    })
    dispatchApiError({
      status: 401,
      code: 'UNAUTHENTICATED',
      path: '/me',
    })

    expect(invalidateQueries).not.toHaveBeenCalled()
  })
})

describe('isProtectedRoutePath', () => {
  it.each([
    ['/profile', true],
    ['/portfolio', true],
    ['/portfolio/artworks/new', true],
    ['/applications', true],
    ['/applications/123', true],
    ['/opportunities/123/apply', true],
    ['/organizer/opportunities', true],
    ['/admin', true],
    ['/admin/users', true],
    ['/opportunities', false],
    ['/artists/123', false],
    ['/onboarding', false],
  ])('prepoznaje %s kao zaštićenu putanju: %s', (path, expected) => {
    expect(isProtectedRoutePath(path)).toBe(expected)
  })
})
