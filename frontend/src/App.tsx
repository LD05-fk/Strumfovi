import { Stack, Typography } from '@mui/material'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AppLayout } from './components/layout/AppLayout'
import { usePageTitle } from './components/layout/usePageTitle'
import { ErrorState } from './components/state/ErrorState'
import { ForbiddenPage } from './components/state/ForbiddenPage'
import { NotFoundPage } from './components/state/NotFoundPage'
import { NotifyProvider } from './components/state/NotifyProvider'
import { HomePage } from './features/home/HomePage'
import { AuthErrorHandler } from './features/auth/routing/AuthErrorHandler'
import { OnboardingOnly } from './features/auth/routing/OnboardingOnly'
import { RequireRole } from './features/auth/routing/RequireRole'
import { hr } from './i18n/hr'
import { ChooseRolePage } from './features/auth/pages/ChooseRolePage'

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
    <Stack
      spacing={2}
      sx={{ alignItems: 'center', textAlign: 'center', pt: 4 }}
    >
      <Typography component="h1" variant="h1">
        {hr.errors.errorTitle}
      </Typography>
      <ErrorState onRetry={() => window.location.reload()} />
    </Stack>
  )
}

function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="opportunities/*" element={<Page title="Prilike" />} />
        <Route
          path="my-opportunities"
          element={
            <RequireRole roles={['ORGANIZER']}>
              <Page title="Moje prilike" />
            </RequireRole>
          }
        />
        <Route
          path="statistics"
          element={
            <RequireRole roles={['ORGANIZER']}>
              <Page title="Statistika" />
            </RequireRole>
          }
        />
        <Route
          path="opportunities/:opportunityId"
          element={<Page title="Prilika" />}
        />
        <Route
          path="opportunities/:opportunityId/apply"
          element={
            <RequireRole roles={['ARTIST']}>
              <Page title="Prijava na priliku" />
            </RequireRole>
          }
        />
        <Route path="artists/:artistId" element={<Page title="Umjetnik" />} />
        <Route
          path="onboarding"
          element={
            <OnboardingOnly>
              <ChooseRolePage />
            </OnboardingOnly>
          }
        />
        <Route
          path="profile"
          element={
            <RequireRole roles={['ARTIST', 'ORGANIZER']}>
              <Page title="Profil" />
            </RequireRole>
          }
        />
        <Route
          path="portfolio/*"
          element={
            <RequireRole roles={['ARTIST']}>
              <Page title="Portfolio" />
            </RequireRole>
          }
        />
        <Route
          path="applications/*"
          element={
            <RequireRole roles={['ARTIST']}>
              <Page title="Moje prijave" />
            </RequireRole>
          }
        />
        <Route
          path="organizer/*"
          element={
            <RequireRole roles={['ORGANIZER']}>
              <Page title="Organizatorski portal" />
            </RequireRole>
          }
        />
        <Route
          path="admin/*"
          element={
            <RequireRole roles={['ADMIN']}>
              <Page title="Administracija" />
            </RequireRole>
          }
        />
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
        <AuthErrorHandler />
        <AppRoutes />
      </BrowserRouter>
    </NotifyProvider>
  )
}

export default App
