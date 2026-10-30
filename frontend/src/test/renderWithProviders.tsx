import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderOptions } from '@testing-library/react'
import type { PropsWithChildren, ReactElement } from 'react'
import { createMemoryRouter, MemoryRouter, RouterProvider } from 'react-router'
import { theme } from '../app/theme'

interface ExtendedRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  route?: string
}

export function renderWithProviders(
  ui: ReactElement,
  { route = '/', ...options }: ExtendedRenderOptions = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const router = createMemoryRouter([{ path: '*', element: ui }], {
    initialEntries: [route],
  })

  return {
    queryClient,
    router,
    ...render(<RouterProvider router={router} />, {
      wrapper: ({ children }) => (
        <QueryClientProvider client={queryClient}>
          <ThemeProvider theme={theme}>
            <CssBaseline />
            {children}
          </ThemeProvider>
        </QueryClientProvider>
      ),
      ...options,
    }),
  }
}

export function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ThemeProvider theme={theme}>
            <CssBaseline />
            {children}
          </ThemeProvider>
        </MemoryRouter>
      </QueryClientProvider>
    )
  }
}
