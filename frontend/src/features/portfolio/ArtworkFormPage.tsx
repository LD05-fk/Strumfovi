import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert,
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormHelperText,
  FormLabel,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import {
  Link as RouterLink,
  useBlocker,
  useNavigate,
  useParams,
} from 'react-router'
import { useForm, useWatch, type Path } from 'react-hook-form'
import { ApiError } from '../../api/client'
import { usePageTitle } from '../../components/layout/usePageTitle'
import { ErrorState } from '../../components/state/ErrorState'
import { LoadingState } from '../../components/state/LoadingState'
import { useNotify } from '../../components/state/useNotify'
import { useCurrentUser } from '../auth/useCurrentUser'
import { hr } from '../../i18n/hr'
import {
  artworkSchema,
  type ArtworkFormSubmission,
  type ArtworkFormValues,
} from './artworkSchema'
import {
  useArtwork,
  useCreateArtwork,
  useDeleteArtwork,
  useUpdateArtwork,
} from './portfolioQueries'
import type { Artwork } from './types'
import { ConfirmDialog } from './ConfirmDialog'
import { ArtworkImageUploader } from './components/ArtworkImageUploader'

function formValuesFromArtwork(artwork: Artwork): ArtworkFormValues {
  return {
    title: artwork.title,
    year: String(artwork.year),
    technique: artwork.technique,
    dimensions: artwork.dimensions ?? '',
    description: artwork.description ?? '',
    visibility: artwork.visibility,
  }
}

function serverField(field: string): Path<ArtworkFormValues> | null {
  if (
    field === 'title' ||
    field === 'year' ||
    field === 'technique' ||
    field === 'dimensions' ||
    field === 'description' ||
    field === 'visibility'
  ) {
    return field
  }

  return null
}

function ArtworkNotFound() {
  return (
    <Stack
      spacing={2}
      sx={{ alignItems: 'center', textAlign: 'center', py: 4 }}
    >
      <Typography component="h1" variant="h2">
        {hr.portfolio.form.notFoundTitle}
      </Typography>
      <Typography color="text.secondary">
        {hr.portfolio.form.notFoundDescription}
      </Typography>
      <Button component={RouterLink} to="/portfolio" variant="contained">
        {hr.portfolio.form.backToPortfolio}
      </Button>
    </Stack>
  )
}

type ArtworkFormProps = {
  artwork?: Artwork
  artistId: string
}

function ArtworkForm({ artwork, artistId }: ArtworkFormProps) {
  const isNew = artwork === undefined
  const artworkId = artwork?.id ?? ''
  const title = isNew ? hr.portfolio.form.newTitle : hr.portfolio.form.editTitle
  usePageTitle(title)

  const notify = useNotify()
  const navigate = useNavigate()
  const [pendingNavigation, setPendingNavigation] = useState<{
    to: string
    replace: boolean
  } | null>(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const createArtwork = useCreateArtwork(artistId)
  const updateArtwork = useUpdateArtwork(artistId, artworkId)
  const deleteArtwork = useDeleteArtwork(artistId)
  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    setValue,
    formState: { errors, isDirty, dirtyFields },
  } = useForm<ArtworkFormValues, unknown, ArtworkFormSubmission>({
    resolver: zodResolver(artworkSchema),
    defaultValues: artwork
      ? formValuesFromArtwork(artwork)
      : {
          title: '',
          year: '',
          technique: '',
          dimensions: '',
          description: '',
          visibility: 'PRIVATE',
        },
  })
  const description = useWatch({ control, name: 'description' }) ?? ''
  const artworkTitle = useWatch({ control, name: 'title' }) ?? ''
  const visibility = useWatch({ control, name: 'visibility' }) ?? 'PRIVATE'
  const hasDirtyFields = Object.keys(dirtyFields).length > 0
  const blocker = useBlocker(isDirty)

  useEffect(() => {
    if (artwork) {
      reset(formValuesFromArtwork(artwork), {
        keepDirtyValues: hasDirtyFields,
      })
    }
  }, [artwork, hasDirtyFields, reset])

  useEffect(() => {
    if (!isDirty) return

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty])

  useEffect(() => {
    if (pendingNavigation && !isDirty) {
      const { to, replace } = pendingNavigation
      setPendingNavigation(null)
      navigate(to, { replace })
    }
  }, [isDirty, navigate, pendingNavigation])

  const handleSaveError = (error: unknown) => {
    if (
      error instanceof ApiError &&
      error.status === 400 &&
      error.code === 'VALIDATION_ERROR'
    ) {
      let mappedErrors = 0
      for (const detail of error.details) {
        const field = serverField(detail.field)
        if (!field) continue
        setError(field, { type: 'server', message: detail.issue })
        mappedErrors += 1
      }

      if (mappedErrors === error.details.length && mappedErrors > 0) return
    }

    notify(error instanceof Error ? error.message : hr.errors.generic, 'error')
  }

  const onSubmit = handleSubmit((values) => {
    const onSuccess = (savedArtwork: Artwork) => {
      reset(formValuesFromArtwork(savedArtwork))
      notify(hr.portfolio.form.saved, 'success')
      if (isNew) {
        setPendingNavigation({
          to: `/portfolio/artworks/${encodeURIComponent(savedArtwork.id)}`,
          replace: true,
        })
      }
    }

    if (isNew) {
      createArtwork.mutate(values, {
        onSuccess,
        onError: handleSaveError,
      })
    } else {
      updateArtwork.mutate(values, {
        onSuccess,
        onError: handleSaveError,
      })
    }
  })

  const saving = createArtwork.isPending || updateArtwork.isPending

  return (
    <>
      <Box sx={{ width: '100%', maxWidth: 1200, mx: 'auto' }}>
        <Typography component="h1" variant="h2" sx={{ mb: 2 }}>
          {title}
        </Typography>

        {artwork?.hiddenByAdmin && (
          <Alert severity="warning" role="status" sx={{ mb: 2 }}>
            {hr.portfolio.form.hiddenByAdmin}
          </Alert>
        )}

        <Box
          component="form"
          noValidate
          onSubmit={onSubmit}
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: 'minmax(0, 1fr)', md: '3fr 2fr' },
            alignItems: 'start',
            gap: 2,
          }}
        >
          <Card sx={{ p: { xs: 2, sm: 3 } }}>
            <Stack spacing={2}>
              <TextField
                id="artwork-title"
                label={hr.portfolio.form.title}
                required
                fullWidth
                error={Boolean(errors.title)}
                helperText={errors.title?.message ?? ' '}
                slotProps={{
                  htmlInput: {
                    'aria-describedby': 'artwork-title-helper-text',
                    'aria-invalid': Boolean(errors.title),
                    maxLength: 200,
                  },
                }}
                {...register('title')}
              />

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  id="artwork-year"
                  label={hr.portfolio.form.year}
                  type="number"
                  required
                  fullWidth
                  error={Boolean(errors.year)}
                  helperText={errors.year?.message ?? ' '}
                  slotProps={{
                    htmlInput: {
                      'aria-describedby': 'artwork-year-helper-text',
                      'aria-invalid': Boolean(errors.year),
                      inputMode: 'numeric',
                      step: 1,
                    },
                  }}
                  {...register('year')}
                />
                <TextField
                  id="artwork-technique"
                  label={hr.portfolio.form.technique}
                  required
                  fullWidth
                  error={Boolean(errors.technique)}
                  helperText={errors.technique?.message ?? ' '}
                  slotProps={{
                    htmlInput: {
                      'aria-describedby': 'artwork-technique-helper-text',
                      'aria-invalid': Boolean(errors.technique),
                      maxLength: 200,
                    },
                  }}
                  {...register('technique')}
                />
              </Stack>

              <TextField
                id="artwork-dimensions"
                label={hr.portfolio.form.dimensions}
                fullWidth
                error={Boolean(errors.dimensions)}
                helperText={errors.dimensions?.message ?? ' '}
                slotProps={{
                  htmlInput: {
                    'aria-describedby': 'artwork-dimensions-helper-text',
                    'aria-invalid': Boolean(errors.dimensions),
                    maxLength: 100,
                  },
                }}
                {...register('dimensions')}
              />

              <TextField
                id="artwork-description"
                label={hr.portfolio.form.description}
                fullWidth
                multiline
                minRows={4}
                error={Boolean(errors.description)}
                helperText={
                  errors.description?.message ??
                  hr.portfolio.form.descriptionCount(description.length)
                }
                slotProps={{
                  htmlInput: {
                    'aria-describedby': 'artwork-description-helper-text',
                    'aria-invalid': Boolean(errors.description),
                    maxLength: 5000,
                  },
                }}
                {...register('description')}
              />

              <FormControl component="fieldset" required>
                <FormLabel component="legend">
                  {hr.portfolio.form.visibility}
                </FormLabel>
                <ToggleButtonGroup
                  exclusive
                  size="small"
                  color="primary"
                  value={visibility}
                  aria-label={hr.portfolio.form.visibility}
                  aria-describedby="artwork-visibility-helper-text"
                  aria-invalid={Boolean(errors.visibility)}
                  onChange={(_, value: 'PUBLIC' | 'PRIVATE' | null) => {
                    if (value) {
                      setValue('visibility', value, {
                        shouldDirty: true,
                        shouldValidate: true,
                      })
                    }
                  }}
                >
                  <ToggleButton value="PUBLIC">
                    {hr.portfolio.form.public}
                  </ToggleButton>
                  <ToggleButton value="PRIVATE">
                    {hr.portfolio.form.private}
                  </ToggleButton>
                </ToggleButtonGroup>
                <FormHelperText
                  id="artwork-visibility-helper-text"
                  error={Boolean(errors.visibility)}
                >
                  {errors.visibility?.message ??
                    hr.portfolio.form.visibilityHint}
                </FormHelperText>
              </FormControl>

              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1}
                sx={{ alignItems: { xs: 'stretch', sm: 'center' } }}
              >
                <Button
                  type="submit"
                  variant="contained"
                  disabled={saving || deleteArtwork.isPending}
                >
                  {saving ? hr.portfolio.form.saving : hr.portfolio.form.save}
                </Button>
                <Button
                  component={RouterLink}
                  to="/portfolio"
                  color="inherit"
                  variant="outlined"
                  disabled={saving || deleteArtwork.isPending}
                >
                  {hr.common.cancel}
                </Button>
                {!isNew && (
                  <>
                    <Box sx={{ flexGrow: 1 }} />
                    <Button
                      color="error"
                      variant="text"
                      disabled={saving || deleteArtwork.isPending}
                      onClick={() => setDeleteDialogOpen(true)}
                    >
                      {hr.common.delete}
                    </Button>
                  </>
                )}
              </Stack>
            </Stack>
          </Card>
          <ArtworkImageUploader
            artwork={artwork}
            artistId={artistId}
            title={artworkTitle}
          />
        </Box>
      </Box>

      <ConfirmDialog
        open={deleteDialogOpen}
        title={hr.portfolio.form.deleteTitle}
        description={hr.portfolio.form.deleteDescription(artwork?.title ?? '')}
        confirmLabel={hr.common.delete}
        isPending={deleteArtwork.isPending}
        onCancel={() => setDeleteDialogOpen(false)}
        onConfirm={() => {
          if (!artworkId) return
          deleteArtwork.mutate(artworkId, {
            onSuccess: () => {
              setDeleteDialogOpen(false)
              notify(hr.portfolio.form.deleted, 'success')
              reset(
                artwork
                  ? formValuesFromArtwork(artwork)
                  : {
                      title: '',
                      year: '',
                      technique: '',
                      dimensions: '',
                      description: '',
                      visibility: 'PRIVATE',
                    },
              )
              setPendingNavigation({ to: '/portfolio', replace: false })
            },
            onError: (error) => {
              notify(
                error instanceof Error
                  ? error.message
                  : hr.portfolio.form.deleteError,
                'error',
              )
            },
          })
        }}
      />

      <Dialog
        open={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        aria-labelledby="artwork-leave-title"
        aria-describedby="artwork-leave-description"
      >
        <DialogTitle id="artwork-leave-title">
          {hr.portfolio.form.leaveTitle}
        </DialogTitle>
        <DialogContent id="artwork-leave-description">
          {hr.portfolio.form.leaveMessage}
        </DialogContent>
        <DialogActions sx={{ flexWrap: 'wrap', p: 2 }}>
          <Button onClick={() => blocker.reset?.()}>
            {hr.portfolio.form.continueEditing}
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => blocker.proceed?.()}
          >
            {hr.portfolio.form.discardChanges}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

export function ArtworkFormPage() {
  const { artworkId = '' } = useParams()
  const isNew = artworkId.length === 0
  const currentUser = useCurrentUser()
  const artworkQuery = useArtwork(artworkId)
  const title = isNew ? hr.portfolio.form.newTitle : hr.portfolio.form.editTitle
  usePageTitle(title)

  if (currentUser.isPending || (!isNew && artworkQuery.isPending)) {
    return <LoadingState lines={5} />
  }

  if (currentUser.isError) {
    return (
      <Box sx={{ maxWidth: 720, mx: 'auto' }}>
        <ErrorState
          error={
            currentUser.error instanceof ApiError
              ? currentUser.error
              : undefined
          }
          onRetry={() => void currentUser.refetch()}
        />
      </Box>
    )
  }

  if (!currentUser.data?.id) {
    return (
      <Box sx={{ maxWidth: 720, mx: 'auto' }}>
        <ErrorState message={hr.errors.generic} />
      </Box>
    )
  }

  if (!isNew && artworkQuery.isError) {
    if (
      artworkQuery.error instanceof ApiError &&
      artworkQuery.error.status === 404
    ) {
      return (
        <Box sx={{ maxWidth: 720, mx: 'auto' }}>
          <ArtworkNotFound />
        </Box>
      )
    }

    return (
      <Box sx={{ maxWidth: 720, mx: 'auto' }}>
        <ErrorState
          error={
            artworkQuery.error instanceof ApiError
              ? artworkQuery.error
              : undefined
          }
          onRetry={() => void artworkQuery.refetch()}
        />
      </Box>
    )
  }

  if (!isNew && !artworkQuery.data) {
    return (
      <Box sx={{ maxWidth: 720, mx: 'auto' }}>
        <ErrorState
          message={hr.portfolio.loadError}
          onRetry={() => void artworkQuery.refetch()}
        />
      </Box>
    )
  }

  if (!isNew && artworkQuery.data.artistId !== currentUser.data.id) {
    return (
      <Box sx={{ maxWidth: 720, mx: 'auto' }}>
        <ArtworkNotFound />
      </Box>
    )
  }

  return (
    <ArtworkForm artwork={artworkQuery.data} artistId={currentUser.data.id} />
  )
}
