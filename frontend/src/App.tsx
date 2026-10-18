import { Typography } from '@mui/material'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AppLayout } from './components/layout/AppLayout'
import { usePageTitle } from './components/layout/usePageTitle'
import { ErrorState } from './components/state/ErrorState'
import { ForbiddenPage } from './components/state/ForbiddenPage'
import { NotFoundPage } from './components/state/NotFoundPage'
import { NotifyProvider } from './components/state/NotifyProvider'
import { HomePage } from './features/home/HomePage'
import { hr } from './i18n/hr'

type PageProps = {
  title: string
}

function Page({ title }: PageProps) {
  usePageTitle(title)

  return (
    <Typography component="h1" variant="h1">
      {title}
    </Typography>
  )
}

function ErrorPage() {
  usePageTitle(hr.errors.errorTitle)

  return (
    <>
      <Typography component="h1" variant="h1" sx={{ mb: 2 }}>
        {hr.errors.errorTitle}
      </Typography>
      <ErrorState onRetry={() => window.location.reload()} />
    </>
  )
}

function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="opportunities" element={<Page title="Prilike" />} />
        <Route path="portfolio" element={<Page title="Portfolio" />} />
        <Route path="applications" element={<Page title="Moje prijave" />} />
        <Route
          path="my-opportunities"
          element={<Page title="Moje prilike" />}
        />
        <Route path="statistics" element={<Page title="Statistika" />} />
        <Route path="admin" element={<Page title="Administracija" />} />
        <Route path="profile" element={<Page title="Profil" />} />
        <Route path="403" element={<ForbiddenPage />} />
        <Route path="error" element={<ErrorPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

function App() {
  return (
    <NotifyProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </NotifyProvider>
  )
}

export default App
