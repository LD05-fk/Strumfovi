import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderOptions } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router'
import { theme } from '../app/theme'

interface ExtendedRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
}

export function renderWithProviders(
  ui: ReactElement,
  { route = '/', ...options }: ExtendedRenderOptions = {}
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return {
    queryClient,
    ...render(ui, {
      wrapper: ({ children }) => (
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[route]}>
            <ThemeProvider theme={theme}>
              <CssBaseline />
              {children}
            </ThemeProvider>
          </MemoryRouter>
        </QueryClientProvider>
      ),
      ...options,
    }),
  }
}
