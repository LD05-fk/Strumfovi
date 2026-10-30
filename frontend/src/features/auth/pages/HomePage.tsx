import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink, useSearchParams } from 'react-router'
import type { ReactNode } from 'react'
import { ApiError } from '../../../api/client'
import { GoogleSignInButton } from '../../../components/GoogleSignInButton'
import { ErrorState } from '../../../components/state/ErrorState'
import { LoadingState } from '../../../components/state/LoadingState'
import { usePageTitle } from '../../../components/layout/usePageTitle'
import { hr } from '../../../i18n/hr'
import { useCurrentUser } from '../useCurrentUser'

type HomeCardProps = {
  title: string
  description: string
  href: string
  actionLabel: string
}

function HomeCard({ title, description, href, actionLabel }: HomeCardProps) {
  return (
    <Card sx={{ flex: 1, minWidth: 0, textAlign: 'left' }}>
      <CardContent>
        <Typography component="h2" variant="h5" sx={{ mb: 0.5 }}>
          {title}
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
          {description}
        </Typography>
        <Button component={RouterLink} to={href} variant="contained">
          {actionLabel}
        </Button>
      </CardContent>
    </Card>
  )
}

export function HomePage() {
  const [searchParams] = useSearchParams()
  const { data: user, isPending, isError, error, refetch } = useCurrentUser()

  usePageTitle(hr.home.title)

  let content: ReactNode

  if (isPending) {
    content = <LoadingState lines={2} />
  } else if (isError) {
    content = (
      <ErrorState
        error={error instanceof ApiError ? error : undefined}
        onRetry={() => void refetch()}
      />
    )
  } else if (user) {
    content = (
      <>
        <Typography
          component="h1"
          variant="h1"
          sx={{ mb: 0.5, textAlign: 'left' }}
        >
          {hr.auth.home.greeting.replace('{name}', user.displayName)}
        </Typography>
        <Typography
          component="p"
          variant="h5"
          color="text.secondary"
          sx={{ mb: 4, textAlign: 'left', fontWeight: 400 }}
        >
          {hr.auth.home.subtitle}
        </Typography>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{ alignItems: 'stretch' }}
        >
          <HomeCard
            title={hr.home.browseOpportunities}
            description={hr.auth.home.browseOpportunitiesDescription}
            href="/opportunities"
            actionLabel={hr.auth.home.opportunitiesAction}
          />
          {user.role === 'ORGANIZER' && (
            <HomeCard
              title={hr.auth.home.myOpportunities}
              description={hr.auth.home.myOpportunitiesDescription}
              href="/organizer/opportunities"
              actionLabel={hr.auth.home.myOpportunities}
            />
          )}
          {user.role === 'ARTIST' && (
            <HomeCard
              title={hr.auth.home.myPortfolio}
              description={hr.auth.home.myPortfolioDescription}
              href="/portfolio"
              actionLabel={hr.auth.home.portfolioAction}
            />
          )}
        </Stack>
      </>
    )
  } else {
    content = (
      <>
        {searchParams.get('login') === 'error' && (
          <Alert severity="error" sx={{ mb: 3, textAlign: 'left' }}>
            {hr.auth.home.loginError}
          </Alert>
        )}
        <Typography component="h1" variant="h1" sx={{ mb: 0.5 }}>
          {hr.home.title}
        </Typography>
        <Typography
          component="p"
          variant="h5"
          sx={{ m: 0, color: 'text.secondary' }}
        >
          {hr.home.description}
        </Typography>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5}
          sx={{ mt: 4, justifyContent: 'center', alignItems: 'center' }}
        >
          <GoogleSignInButton />
          <Button component={RouterLink} to="/opportunities" variant="outlined">
            {hr.home.browseOpportunities}
          </Button>
        </Stack>
      </>
    )
  }

  return (
    <Box
      sx={{
        width: '100%',
        maxWidth: (theme) => theme.spacing(90),
        mx: user ? 0 : 'auto',
        pt: { xs: 3, sm: 3, md: 4 },
        pb: 4,
        textAlign: user ? 'left' : 'center',
      }}
    >
      {content}
    </Box>
  )
}
