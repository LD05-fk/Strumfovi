import AddIcon from '@mui/icons-material/Add'
import {
  Box,
  Button,
  Pagination,
  Skeleton,
  Stack,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useEffect, useState } from 'react'
import { Link as RouterLink, useSearchParams } from 'react-router'
import { ApiError } from '../../api/client'
import { usePageTitle } from '../../components/layout/usePageTitle'
import { EmptyState } from '../../components/state/EmptyState'
import { ErrorState } from '../../components/state/ErrorState'
import { useNotify } from '../../components/state/useNotify'
import { useCurrentUser } from '../auth/useCurrentUser'
import { hr } from '../../i18n/hr'
import { ArtworkCard } from './ArtworkCard'
import { ConfirmDialog } from './ConfirmDialog'
import { useArtistArtworks, useDeleteArtwork } from './portfolioQueries'
import type { Artwork } from './types'

function readPage(searchParams: URLSearchParams) {
  const page = Number(searchParams.get('page') ?? 1)
  return Number.isSafeInteger(page) && page > 0 ? page : 1
}

function ArtworkGridSkeleton({ count }: { count: number }) {
  return (
    <Box
      aria-label={hr.common.loading}
      aria-busy="true"
      role="status"
      sx={{
        display: 'grid',
        gridTemplateColumns: {
          xs: 'repeat(2, minmax(0, 1fr))',
          sm: 'repeat(3, minmax(0, 1fr))',
          md: 'repeat(4, minmax(0, 1fr))',
        },
        gap: 2,
      }}
    >
      {Array.from({ length: count }, (_, index) => (
        <Box
          key={index}
          sx={{
            p: 2,
            borderRadius: 1.5,
            bgcolor: 'background.paper',
            boxShadow: 1,
          }}
        >
          <Skeleton variant="rounded" sx={{ aspectRatio: '4 / 3' }} />
          <Skeleton width="70%" sx={{ mt: 1 }} />
          <Skeleton width="45%" />
          <Skeleton width="35%" />
        </Box>
      ))}
    </Box>
  )
}

export function MyPortfolioPage() {
  usePageTitle(hr.portfolio.pageTitle)
  const theme = useTheme()
  const isTablet = useMediaQuery(theme.breakpoints.up('sm'))
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'))
  const notify = useNotify()
  const { data: user, isPending: userPending } = useCurrentUser()
  const [searchParams, setSearchParams] = useSearchParams()
  const [selectedArtwork, setSelectedArtwork] = useState<Artwork | null>(null)
  const page = readPage(searchParams)
  const columns = isDesktop ? 4 : isTablet ? 3 : 2
  const pageSize = columns * 2
  const artworks = useArtistArtworks(user?.id ?? '', page, pageSize)
  const pageCount = artworks.data
    ? Math.ceil(artworks.data.meta.total / pageSize)
    : 0

  useEffect(() => {
    if (!artworks.data || artworks.data.meta.total === 0 || page <= pageCount) {
      return
    }

    setSearchParams(
      (currentParams) => {
        const nextParams = new URLSearchParams(currentParams)
        nextParams.set('page', String(pageCount))
        return nextParams
      },
      { replace: true },
    )
  }, [artworks.data, page, pageCount, setSearchParams])

  const deleteArtwork = useDeleteArtwork(
    user?.id ?? '',
    () => {
      const isLastOnPage = page > 1 && (artworks.data?.data.length ?? 0) === 1
      setSelectedArtwork(null)
      notify(hr.portfolio.deleted, 'success')
      if (isLastOnPage) {
        setSearchParams({ page: String(page - 1) })
      }
    },
    (error) => {
      notify(
        error instanceof ApiError ? error.message : hr.portfolio.deleteError,
        'error',
      )
    },
  )

  if (userPending || !user?.id || artworks.isPending) {
    return <ArtworkGridSkeleton count={pageSize} />
  }

  if (artworks.isError) {
    return (
      <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
        <ErrorState
          error={
            artworks.error instanceof ApiError ? artworks.error : undefined
          }
          message={
            artworks.error instanceof ApiError
              ? undefined
              : hr.portfolio.loadError
          }
          onRetry={() => void artworks.refetch()}
        />
      </Box>
    )
  }

  if (!artworks.data) {
    return (
      <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
        <ErrorState
          message={hr.portfolio.loadError}
          onRetry={() => void artworks.refetch()}
        />
      </Box>
    )
  }

  if (artworks.data.meta.total > 0 && page > pageCount) {
    return <ArtworkGridSkeleton count={pageSize} />
  }

  return (
    <Box sx={{ width: '100%', maxWidth: 1200, mx: 'auto' }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{
          alignItems: { xs: 'stretch', sm: 'center' },
          justifyContent: 'space-between',
          mb: 2,
        }}
      >
        <Typography component="h1" variant="h2">
          {hr.portfolio.pageTitle}
        </Typography>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{ alignItems: 'stretch' }}
        >
          <Button
            component={RouterLink}
            to={`/artists/${encodeURIComponent(user.id)}`}
            variant="outlined"
            sx={{ borderWidth: 2, '&:hover': { borderWidth: 2 } }}
          >
            {hr.portfolio.viewPublic}
          </Button>
          <Button
            component={RouterLink}
            to="/portfolio/artworks/new"
            variant="contained"
            startIcon={<AddIcon />}
          >
            {hr.portfolio.addArtwork}
          </Button>
        </Stack>
      </Stack>

      {artworks.data.meta.total === 0 ? (
        <EmptyState
          title={
            <>
              <Typography
                component="span"
                aria-hidden="true"
                sx={{ display: 'block', fontSize: '3rem', lineHeight: 1 }}
              >
                🎨
              </Typography>
              <br />
              {hr.portfolio.emptyTitle}
            </>
          }
          description={hr.portfolio.emptyDescription}
          action={
            <Button
              component={RouterLink}
              to="/portfolio/artworks/new"
              variant="contained"
              startIcon={<AddIcon />}
            >
              {hr.portfolio.addFirstArtwork}
            </Button>
          }
        />
      ) : (
        <>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: {
                xs: 'repeat(2, minmax(0, 1fr))',
                sm: 'repeat(3, minmax(0, 1fr))',
                md: 'repeat(4, minmax(0, 1fr))',
              },
              gap: 2,
            }}
          >
            {artworks.data.data.map((artwork) => (
              <ArtworkCard
                key={artwork.id}
                artwork={artwork}
                deletePending={deleteArtwork.isPending}
                onDelete={setSelectedArtwork}
              />
            ))}
          </Box>
          {pageCount > 1 && (
            <Stack sx={{ alignItems: 'center', mt: 3 }}>
              <Pagination
                count={pageCount}
                page={Math.min(page, pageCount)}
                onChange={(_, selectedPage) => {
                  setSearchParams({ page: String(selectedPage) })
                }}
                aria-label={hr.portfolio.pagination}
                color="primary"
              />
            </Stack>
          )}
        </>
      )}

      <ConfirmDialog
        open={selectedArtwork !== null}
        title={hr.portfolio.deleteTitle}
        description={
          selectedArtwork
            ? hr.portfolio.deleteDescription(selectedArtwork.title)
            : ''
        }
        confirmLabel={hr.common.delete}
        isPending={deleteArtwork.isPending}
        onCancel={() => setSelectedArtwork(null)}
        onConfirm={() => {
          if (selectedArtwork) {
            deleteArtwork.mutate(selectedArtwork.id)
          }
        }}
      />
    </Box>
  )
}
