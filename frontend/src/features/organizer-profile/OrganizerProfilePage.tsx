import { Box } from '@mui/material'
import { ApiError } from '../../api/client'
import { usePageTitle } from '../../components/layout/usePageTitle'
import { ErrorState } from '../../components/state/ErrorState'
import { LoadingState } from '../../components/state/LoadingState'
import { hr } from '../../i18n/hr'
import { useOrganizerProfile } from './organizerProfileQueries'
import { OrganizerProfileForm } from './OrganizerProfileForm'

export function OrganizerProfilePage() {
  usePageTitle(hr.organizerProfile.pageTitle)
  const profile = useOrganizerProfile()

  if (profile.isPending) {
    return <LoadingState lines={4} />
  }

  if (profile.isError || !profile.data) {
    return (
      <Box sx={{ width: '100%', maxWidth: 640, mx: 'auto' }}>
        <ErrorState
          error={profile.error instanceof ApiError ? profile.error : undefined}
          onRetry={() => void profile.refetch()}
        />
      </Box>
    )
  }
  return <OrganizerProfileForm profile={profile.data} />
}
