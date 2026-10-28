import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { CssBaseline, ThemeProvider } from '@mui/material'
import App from './App.tsx'
import { theme } from './app/theme'
import { apiFetch } from './api/client.ts'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './app/queryClient.ts'

async function bootstrap() {
  // Osiguraj CSRF kolačić prije pokretanja aplikacije.
  try {
    await apiFetch('/auth/csrf')
  } catch {
    // Aplikacija se i dalje treba pokrenuti ako backend nije dostupan.
    // Pojedinačni zahtjevi mogu prikazati odgovarajuću grešku.
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <App />
        </ThemeProvider>
      </QueryClientProvider>
    </StrictMode>,
  )
}

void bootstrap()
