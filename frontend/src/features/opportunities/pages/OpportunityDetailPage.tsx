import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router'
import { ApiError } from '../../../api/client'
import { usePageTitle } from '../../../components/layout/usePageTitle'
import { ErrorState } from '../../../components/state/ErrorState'
import { LoadingState } from '../../../components/state/LoadingState'
import { hr } from '../../../i18n/hr'
import { useCurrentUser } from '../../auth/useCurrentUser'
import { useOpportunity } from '../opportunityQueries'
import { dayjs, OPPORTUNITY_TIME_ZONE } from '../../../app/dayjs'
import { statusChip } from '../../../app/theme'
import { Link as MuiLink } from '@mui/material'
import MapView from '../components/map/MapView'
import { OwnerActionsBar } from '../components/ownerActionsBar'

function formatZagrebDateTime(value: string) {
  return dayjs(value).tz(OPPORTUNITY_TIME_ZONE).format('DD.MM.YYYY. HH:mm')
}

export function OpportunityDetailPage() {
  const { opportunityId } = useParams()
  const opportunity = useOpportunity(opportunityId ?? '')
  const currentUser = useCurrentUser()

  usePageTitle(opportunity.data?.title ?? hr.opportunities.detail.defaultTitle)

  if (opportunity.isPending) {
    return <LoadingState lines={5} />
  }

  if (opportunity.isError) {
    if (
      opportunity.error instanceof ApiError &&
      opportunity.error.status === 404
    ) {
      return (
        <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
          <Alert severity="error" role="alert">
            {hr.opportunities.detail.notFound}
          </Alert>
          <Button component={RouterLink} to="/opportunities" variant="outlined">
            {hr.opportunities.detail.allOpportunities}
          </Button>
        </Stack>
      )
    }

    return (
      <ErrorState
        error={
          opportunity.error instanceof ApiError ? opportunity.error : undefined
        }
        onRetry={() => void opportunity.refetch()}
      />
    )
  }

  if (!opportunity.data) {
    return <ErrorState message={hr.errors.generic} />
  }

  const isOwner = currentUser.data?.id === opportunity.data.organizer.id

  const detail = opportunity.data
  const status = statusChip[detail.status]

  const location = detail.location
  const hasCoordinates =
    location.latitude !== null && location.longitude !== null
  const address = [
    location.streetAddress,
    [location.postalCode, location.city].filter(Boolean).join(' '),
  ]
    .filter(Boolean)
    .join(', ')

  return (
    <Stack spacing={2} sx={{ maxWidth: 1200, mx: 'auto' }}>
      {isOwner && (
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{
            p: 2,
            bgcolor: 'background.paper',
            border: 1,
            borderColor: 'divider',
            borderRadius: 2,
            alignItems: { xs: 'stretch', sm: 'center' },
          }}
        >
          <Typography sx={{ fontWeight: 600 }}>
            {hr.opportunities.detail.owner}
          </Typography>
          <Chip
            label={
              detail.status === 'PREPARATION'
                ? hr.opportunities.detail.preparationPrivate
                : hr.opportunityStatus[detail.status]
            }
            sx={{
              bgcolor: status.bg,
              color: status.fg,
              alignSelf: 'flex-start',
            }}
          />
          {detail.archived && <Chip label={hr.opportunities.detail.archived} />}
          {detail.hiddenByAdmin && (
            <Chip label={hr.opportunities.detail.hiddenByAdmin} />
          )}
          <Box sx={{ flexGrow: 1 }} />
          <OwnerActionsBar
            opportunityId={detail.id}
            title={detail.title}
            status={detail.status}
            archived={detail.archived}
            deadline={detail.deadline}
          />
        </Stack>
      )}

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '1fr',
            md: 'minmax(0, 2fr) minmax(280px, 1fr)',
          },
          gap: 2,
          alignItems: 'start',
        }}
      >
        <Card>
          <CardContent>
            <Stack direction="row" spacing={1} sx={{ mb: 1, flexWrap: 'wrap' }}>
              <Chip
                label={hr.opportunityStatus[detail.status]}
                sx={{ bgcolor: status.bg, color: status.fg }}
              />
              <Chip label={detail.type.name} variant="outlined" />
            </Stack>

            <Typography component="h1" variant="h1" sx={{ mb: 1 }}>
              {detail.title}
            </Typography>
            <Typography color="text.secondary" sx={{ mb: 3 }}>
              {detail.organizer.displayName}
              {detail.organizer.organizationName
                ? ` · ${detail.organizer.organizationName}`
                : ''}
            </Typography>

            <Typography component="h2" variant="h5" sx={{ mb: 1 }}>
              {hr.opportunities.detail.description}
            </Typography>
            <Typography sx={{ whiteSpace: 'pre-line', mb: 3 }}>
              {detail.description}
            </Typography>

            {detail.criteria && (
              <>
                <Typography component="h2" variant="h5" sx={{ mb: 1 }}>
                  {hr.opportunities.detail.criteria}
                </Typography>
                <Typography sx={{ whiteSpace: 'pre-line', mb: 3 }}>
                  {detail.criteria}
                </Typography>
              </>
            )}

            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
              {detail.artFields.map((field) => (
                <Chip key={field.id} label={field.name} />
              ))}
            </Stack>
          </CardContent>
        </Card>

        <Stack spacing={2}>
          <Card>
            <CardContent>
              <Typography component="h2" variant="h6">
                {hr.opportunities.form.deadline}
              </Typography>
              <Typography variant="h5" sx={{ mb: 2 }}>
                {formatZagrebDateTime(detail.deadline)}
              </Typography>

              {detail.prize && (
                <>
                  <Typography component="h2" variant="h6">
                    {hr.opportunities.detail.prize}
                  </Typography>
                  <Typography sx={{ whiteSpace: 'pre-line', mb: 2 }}>
                    {detail.prize}
                  </Typography>
                </>
              )}

              {detail.eventStart && (
                <>
                  <Typography component="h2" variant="h6">
                    {hr.opportunities.detail.event}
                  </Typography>
                  <Typography>
                    {formatZagrebDateTime(detail.eventStart)}
                    {detail.eventEnd
                      ? ` - ${formatZagrebDateTime(detail.eventEnd)}`
                      : ''}
                  </Typography>
                </>
              )}

              {!isOwner && (
                <Button variant="contained" fullWidth disabled sx={{ mt: 2 }}>
                  {hr.opportunities.detail.apply}
                </Button>
              )}
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <Typography component="h2" variant="h6" sx={{ mb: 1 }}>
                {hr.opportunities.form.location}
              </Typography>

              {location.type === 'PHYSICAL' ? (
                <>
                  <Typography sx={{ mb: 1 }}>{address}</Typography>
                  {hasCoordinates ? (
                    <>
                      <MapView
                        markers={[
                          {
                            id: detail.id,
                            title: detail.title,
                            latitude: location.latitude!,
                            longitude: location.longitude!,
                          },
                        ]}
                        height={180}
                        fitToMarkers
                        ariaLabel={hr.map.ariaLabel}
                      />
                      <MuiLink
                        href={`https://www.openstreetmap.org/?mlat=${location.latitude}&mlon=${location.longitude}#map=17/${location.latitude}/${location.longitude}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        sx={{ display: 'inline-block', mt: 1 }}
                      >
                        {hr.map.openInOsm} ↗
                      </MuiLink>
                    </>
                  ) : (
                    <Typography color="text.secondary">
                      {hr.map.unavailable}
                    </Typography>
                  )}
                </>
              ) : location.onlineUrl ? (
                <MuiLink
                  href={location.onlineUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {location.onlineUrl} ↗
                </MuiLink>
              ) : null}
            </CardContent>
          </Card>
        </Stack>
      </Box>
      {detail.hasResults && (
        <Card>
          <CardContent>
            <Typography component="h2" variant="h5" sx={{ mb: 1 }}>
              {hr.opportunities.detail.results}
            </Typography>
            <Typography color="text.secondary">
              {hr.opportunities.detail.resultsPlaceholder}
            </Typography>
          </CardContent>
        </Card>
      )}
    </Stack>
  )
}
