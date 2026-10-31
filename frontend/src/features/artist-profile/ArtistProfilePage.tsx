import { Box, Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'
import { ApiError } from '../../api/client'
import { ErrorState } from '../../components/state/ErrorState'
import { LoadingState } from '../../components/state/LoadingState'
import { usePageTitle } from '../../components/layout/usePageTitle'
import { useCurrentUser } from '../auth/useCurrentUser'
import { hr } from '../../i18n/hr'
import { useArtFields, useMyArtistProfile } from './artistProfileQueries'
import { ArtistProfileForm } from './ArtistProfileForm'
import { ShareLinkButton } from './ShareLinkButton'

export function ArtistProfilePage() {
  usePageTitle(hr.artistProfile.pageTitle)
  const profile = useMyArtistProfile()
  const artFields = useArtFields()
  const { data: user } = useCurrentUser()

  if (profile.isPending || artFields.isPending) {
    return <LoadingState lines={5} />
  }

  if (profile.isError || artFields.isError) {
    const error = profile.error ?? artFields.error
    return (
      <Box sx={{ width: '100%', maxWidth: 1000, mx: 'auto' }}>
        <ErrorState
          error={error instanceof ApiError ? error : undefined}
          onRetry={() => {
            void profile.refetch()
            void artFields.refetch()
          }}
        />
      </Box>
    )
  }

  if (!profile.data || !artFields.data) {
    return (
      <Box sx={{ width: '100%', maxWidth: 1000, mx: 'auto' }}>
        <ErrorState
          message={hr.errors.generic}
          onRetry={() => {
            void profile.refetch()
            void artFields.refetch()
          }}
        />
      </Box>
    )
  }

  return (
    <Box sx={{ width: '100%', maxWidth: 1000, mx: 'auto' }}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{
          alignItems: { xs: 'stretch', md: 'center' },
          justifyContent: 'space-between',
          mb: 2,
        }}
      >
        <Typography component="h1" variant="h2">
          {hr.artistProfile.pageTitle}
        </Typography>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{ alignItems: { xs: 'stretch', sm: 'center' } }}
        >
          <Button
            component={RouterLink}
            to={`/artists/${profile.data.userId}`}
            variant="outlined"
            sx={{
              minHeight: 36,
              px: 1.5,
              borderWidth: 2,
              '&:hover': { borderWidth: 2 },
            }}
          >
            {hr.artistProfile.viewPublicProfile}
          </Button>
          <ShareLinkButton userId={profile.data.userId} />
        </Stack>
      </Stack>
      <ArtistProfileForm
        profile={profile.data}
        artFields={artFields.data.data}
        email={user?.email}
      />
    </Box>
  )
}
