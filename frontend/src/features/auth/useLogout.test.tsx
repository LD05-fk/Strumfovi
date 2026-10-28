import { render, renderHook, waitFor } from '@testing-library/react'
import { vi, it, expect } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import { MemoryRouter, useLocation } from 'react-router'
import { useLogout } from './useLogout'
import { NotifyProvider } from '../../components/state/NotifyProvider'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return {
    ...actual,
    apiFetch: vi.fn(),
  }
})

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
}

function LocationDisplay() {
  const location = useLocation()

  return <div data-testid="location">{location.pathname}</div>
}

function LogoutTrigger() {
  const logout = useLogout()

  return <button onClick={() => logout.mutate()}>Logout</button>
}

function createWrapper(queryClient: QueryClient, route = '/') {
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <NotifyProvider>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[route]}>
            <LocationDisplay />
            {children}
          </MemoryRouter>
        </QueryClientProvider>
      </NotifyProvider>
    )
  }
}

it('salje POST request na /auth/logout', async () => {
  const queryClient = createTestQueryClient()
  vi.mocked(apiFetch).mockResolvedValue(undefined)

  const { result } = renderHook(() => useLogout(), {
    wrapper: createWrapper(queryClient),
  })

  result.current.mutate()

  await waitFor(() => {
    expect(apiFetch).toHaveBeenCalledWith('/auth/logout', {
      method: 'POST',
    })
  })
})

it('cisti query cache nakon logouta', async () => {
  const queryClient = createTestQueryClient()

  queryClient.setQueryData(['private-data'], { secret: 'test' })

  vi.mocked(apiFetch).mockResolvedValue(undefined)

  const { result } = renderHook(() => useLogout(), {
    wrapper: createWrapper(queryClient),
  })

  result.current.mutate()

  await waitFor(() => {
    expect(queryClient.getQueryData(['private-data'])).toBeUndefined()
  })
})

it('navigira na home page nakon logouta', async () => {
  const queryClient = createTestQueryClient()

  vi.mocked(apiFetch).mockResolvedValue(undefined)

  const { getByTestId, getByRole } = render(<LogoutTrigger />, {
    wrapper: createWrapper(queryClient, '/profile'),
  })

  getByRole('button', { name: 'Logout' }).click()

  await waitFor(() => {
    expect(getByTestId('location')).toHaveTextContent('/')
  })
})

it('zadrzava cache i rutu ako logout ne uspije', async () => {
  const queryClient = createTestQueryClient()

  queryClient.setQueryData(['private-data'], { secret: 'test' })

  vi.mocked(apiFetch).mockRejectedValue(new Error('Logout failed'))

  const { getByTestId, getByRole } = render(<LogoutTrigger />, {
    wrapper: createWrapper(queryClient, '/profile'),
  })

  getByRole('button', { name: 'Logout' }).click()

  await waitFor(() => {
    expect(apiFetch).toHaveBeenCalledWith('/auth/logout', {
      method: 'POST',
    })
  })

  expect(queryClient.getQueryData(['private-data'])).toEqual({
    secret: 'test',
  })

  expect(getByTestId('location')).toHaveTextContent('/profile')
})
