import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Alert, Box, Button, Paper, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useParams, useSearchParams } from 'react-router'
import { ApiError } from '../../api/client'
import { ErrorState } from '../../components/state/ErrorState'
import { LoadingState } from '../../components/state/LoadingState'
import { ShareLinkButton } from '../artist-profile/ShareLinkButton'
import { useCurrentUser } from '../auth/useCurrentUser'
import { hr } from '../../i18n/hr'
import { ArtistHeader } from './ArtistHeader'
import { ArtworkGallery } from './ArtworkGallery'
import { ParticipationList } from './ParticipationList'
import { PastExhibitionList } from './PastExhibitionList'
import {
  usePublicArtist,
  usePublicArtworks,
} from './publicPortfolioQueries'

function requestedPage(value: string | null) {
  if (!value || !/^\d+$/.test(value)) return 1
  const page = Number(value)
  return Number.isSafeInteger(page) && page > 0 ? page : 1
}

function isNotFound(error: unknown) {
  return (
    error instanceof ApiError &&
    (error.status === 404 || error.code === 'NOT_FOUND')
  )
}

function PublicArtistNotFound() {
  return (
    <Stack
      spacing={2}
      sx={{
        alignItems: 'center',
        textAlign: 'center',
        mt: 4,
        p: { xs: 3, sm: 6 },
        border: 1,
        borderStyle: 'dashed',
        borderColor: 'divider',
        borderRadius: 1.5,
        bgcolor: 'background.paper',
      }}
    >
      <Typography component="span" aria-hidden="true" sx={{ fontSize: 40 }}>
        🔍
      </Typography>
      <Typography component="h1" variant="h4">
        {hr.publicPortfolio.notFound}
      </Typography>
      <Typography color="text.secondary">
        {hr.publicPortfolio.profileUnavailable}
      </Typography>
      <Button component={RouterLink} to="/opportunities" variant="contained">
        {hr.publicPortfolio.browseOpportunities}
      </Button>
    </Stack>
  )
}

export function PublicPortfolioPage() {
  const { artistId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = requestedPage(searchParams.get('page'))
  const queryClient = useQueryClient()
  const profile = usePublicArtist(artistId)
  const artworks = usePublicArtworks(artistId, page)
  const currentUser = useCurrentUser()
  const user = currentUser.data

  useEffect(() => {
    document.title = `${profile.data?.displayName ?? hr.publicPortfolio.pageTitleFallback} – ${hr.appName}`
  }, [profile.data?.displayName])

  if (profile.isPending || (artworks.isPending && !artworks.data)) {
    return (
      <Box sx={{ width: '100%', maxWidth: 1200, mx: 'auto' }}>
        <LoadingState lines={7} />
      </Box>
    )
  }

  if (profile.isError && isNotFound(profile.error)) {
    return (
      <Box sx={{ width: '100%', maxWidth: 1000, mx: 'auto' }}>
        <PublicArtistNotFound />
      </Box>
    )
  }

  if (profile.isError || artworks.isError || !profile.data || !artworks.data) {
    const error = profile.error ?? artworks.error
    return (
      <Box sx={{ width: '100%', maxWidth: 1000, mx: 'auto' }}>
        <ErrorState
          error={error instanceof ApiError ? error : undefined}
          onRetry={() => {
            void queryClient.invalidateQueries({
              queryKey: ['artists', artistId],
            })
          }}
        />
      </Box>
    )
  }

  const isOwner = user?.id === artistId
  const changePage = (nextPage: number) => {
    const nextParams = new URLSearchParams(searchParams)
    if (nextPage === 1) {
      nextParams.delete('page')
    } else {
      nextParams.set('page', String(nextPage))
    }
    setSearchParams(nextParams)
  }

  return (
    <Stack spacing={3} sx={{ width: '100%', maxWidth: 1200, mx: 'auto' }}>
      {isOwner && (
        <Paper
          role="status"
          sx={{
            p: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 1,
            flexWrap: 'wrap',
          }}
        >
          <Typography sx={{ fontWeight: 600 }}>
            {hr.publicPortfolio.ownerNotice}
          </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
            <Button
              component={RouterLink}
              to="/profile"
              variant="contained"
            >
              {hr.publicPortfolio.editProfile}
            </Button>
            <ShareLinkButton userId={artistId} />
          </Stack>
        </Paper>
      )}
      <ArtistHeader profile={profile.data} />
      <ArtworkGallery
        artworks={artworks.data.data}
        page={page}
        dataPage={artworks.data.meta.page}
        total={artworks.data.meta.total}
        pageSize={artworks.data.meta.pageSize}
        onPageChange={changePage}
      />
      <PastExhibitionList exhibitions={profile.data.pastExhibitions} />
      <ParticipationList participations={profile.data.participations} />
      {artworks.isFetching && (
        <Alert severity="info" role="status">
          {hr.common.loading}
        </Alert>
      )}
    </Stack>
  )
}
