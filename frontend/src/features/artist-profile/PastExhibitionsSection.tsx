import {
  Alert,
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Skeleton,
  Stack,
  TextField,
  Typography,
  useMediaQuery,
} from '@mui/material'
import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm, type Path } from 'react-hook-form'
import { ApiError } from '../../api/client'
import { ErrorState } from '../../components/state/ErrorState'
import { useNotify } from '../../components/state/useNotify'
import { hr } from '../../i18n/hr'
import {
  useArtistPublicProfile,
  useCreatePastExhibition,
  useDeletePastExhibition,
  useUpdatePastExhibition,
} from './artistProfileQueries'
import {
  currentZagrebYear,
  pastExhibitionSchema,
  type PastExhibitionFormValues,
} from './pastExhibitionSchema'
import type { PastExhibition } from './types'

type DialogMode =
  { type: 'create' } | { type: 'edit'; exhibition: PastExhibition } | null

function mapServerField(field: string): Path<PastExhibitionFormValues> | null {
  return field === 'title' || field === 'year' || field === 'place'
    ? field
    : null
}

function ExhibitionSkeleton() {
  return (
    <Card
      role="status"
      aria-label={hr.common.loading}
      sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}
    >
      <Box sx={{ flex: 1 }}>
        <Skeleton width="45%" height={30} />
        <Skeleton width="25%" height={24} />
      </Box>
      <Skeleton width={64} height={36} />
      <Skeleton width={64} height={36} />
    </Card>
  )
}

export function PastExhibitionsSection({ userId }: { userId: string }) {
  const notify = useNotify()
  const profile = useArtistPublicProfile(userId)
  const createExhibition = useCreatePastExhibition(userId)
  const updateExhibition = useUpdatePastExhibition(userId)
  const deleteExhibition = useDeletePastExhibition(userId)
  const [dialogMode, setDialogMode] = useState<DialogMode>(null)
  const [deleteTarget, setDeleteTarget] = useState<PastExhibition | null>(null)
  const isMobile = useMediaQuery((theme) => theme.breakpoints.down('sm'))
  const currentYear = currentZagrebYear()
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<PastExhibitionFormValues>({
    resolver: zodResolver(pastExhibitionSchema),
    defaultValues: { title: '', year: currentYear, place: '' },
  })

  useEffect(() => {
    if (!dialogMode) return
    reset(
      dialogMode.type === 'edit'
        ? {
            title: dialogMode.exhibition.title,
            year: dialogMode.exhibition.year,
            place: dialogMode.exhibition.place,
          }
        : { title: '', year: currentYear, place: '' },
    )
  }, [currentYear, dialogMode, reset])

  function openCreateDialog() {
    createExhibition.reset()
    updateExhibition.reset()
    setDialogMode({ type: 'create' })
  }

  function openEditDialog(exhibition: PastExhibition) {
    createExhibition.reset()
    updateExhibition.reset()
    setDialogMode({ type: 'edit', exhibition })
  }

  const save = handleSubmit((values) => {
    const callbacks = { onSuccess: onSaved, onError: onSaveError }
    if (dialogMode?.type === 'edit') {
      updateExhibition.mutate(
        { id: dialogMode.exhibition.id, values },
        callbacks,
      )
    } else {
      createExhibition.mutate(values, callbacks)
    }
  })

  function onSaved() {
    setDialogMode(null)
    notify(hr.artistProfile.pastExhibitions.saved, 'success')
  }

  function onSaveError(error: Error) {
    if (error instanceof ApiError && error.status === 404) {
      setDialogMode(null)
      notify(hr.artistProfile.pastExhibitions.notFound, 'error')
      return
    }

    if (
      error instanceof ApiError &&
      error.status === 400 &&
      error.code === 'VALIDATION_ERROR'
    ) {
      let mappedErrors = 0
      let unmappedErrors = 0
      for (const detail of error.details) {
        const field = mapServerField(detail.field)
        if (!field) {
          unmappedErrors += 1
          continue
        }
        setError(field, { type: 'server', message: detail.issue })
        mappedErrors += 1
      }
      if (mappedErrors > 0 && unmappedErrors === 0) return
    }

    notify(error.message || hr.errors.generic, 'error')
  }

  const isSaving = createExhibition.isPending || updateExhibition.isPending

  if (profile.isPending) {
    return (
      <Box component="section" aria-labelledby="past-exhibitions-heading">
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{ alignItems: { xs: 'stretch', sm: 'center' }, mb: 2, mt: 3 }}
        >
          <Typography id="past-exhibitions-heading" component="h2" variant="h3">
            {hr.artistProfile.pastExhibitions.heading}
          </Typography>
          <Button
            disabled
            variant="contained"
            sx={{ ml: { sm: 'auto !important' } }}
          >
            {hr.artistProfile.pastExhibitions.add}
          </Button>
        </Stack>
        <Stack spacing={2}>
          <ExhibitionSkeleton />
          <ExhibitionSkeleton />
        </Stack>
      </Box>
    )
  }

  if (profile.isError || !profile.data) {
    return (
      <Box
        component="section"
        aria-labelledby="past-exhibitions-heading"
        sx={{ mt: 3 }}
      >
        <Typography
          id="past-exhibitions-heading"
          component="h2"
          variant="h3"
          sx={{ mb: 2 }}
        >
          {hr.artistProfile.pastExhibitions.heading}
        </Typography>
        <ErrorState
          error={profile.error instanceof ApiError ? profile.error : undefined}
          message={
            profile.error instanceof ApiError
              ? undefined
              : hr.artistProfile.pastExhibitions.loadError
          }
          onRetry={() => void profile.refetch()}
        />
      </Box>
    )
  }

  return (
    <Box
      component="section"
      aria-labelledby="past-exhibitions-heading"
      sx={{ mt: 3 }}
    >
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{ alignItems: { xs: 'stretch', sm: 'center' }, mb: 2 }}
      >
        <Typography id="past-exhibitions-heading" component="h2" variant="h3">
          {hr.artistProfile.pastExhibitions.heading}
        </Typography>
        <Button
          variant="contained"
          onClick={openCreateDialog}
          sx={{ ml: { sm: 'auto !important' } }}
        >
          {hr.artistProfile.pastExhibitions.add}
        </Button>
      </Stack>

      {profile.data.pastExhibitions.length === 0 ? (
        <Alert severity="info">{hr.artistProfile.pastExhibitions.empty}</Alert>
      ) : (
        <Stack spacing={2}>
          {profile.data.pastExhibitions.map((exhibition) => (
            <Card
              key={exhibition.id}
              sx={{
                p: 2,
                display: 'flex',
                alignItems: { xs: 'flex-start', sm: 'center' },
                flexDirection: { xs: 'column', sm: 'row' },
                gap: { xs: 1.5, sm: 2 },
              }}
            >
              <Box sx={{ flex: 1 }}>
                <Typography component="h3" variant="h5">
                  {exhibition.title}
                </Typography>
                <Typography color="text.secondary">
                  {exhibition.year}. · {exhibition.place}
                </Typography>
              </Box>
              <Stack
                direction="row"
                spacing={1}
                sx={{ alignSelf: { xs: 'flex-end', sm: 'center' } }}
              >
                <Button
                  onClick={() => openEditDialog(exhibition)}
                  aria-label={`${hr.common.edit}: ${exhibition.title}`}
                >
                  {hr.common.edit}
                </Button>
                <Button
                  color="error"
                  onClick={() => setDeleteTarget(exhibition)}
                  aria-label={`${hr.common.delete}: ${exhibition.title}`}
                >
                  {hr.common.delete}
                </Button>
              </Stack>
            </Card>
          ))}
        </Stack>
      )}

      <Dialog
        open={dialogMode !== null}
        onClose={() => {
          if (!isSaving) setDialogMode(null)
        }}
        fullScreen={isMobile}
        fullWidth
        maxWidth="sm"
        aria-labelledby="past-exhibition-dialog-title"
        sx={{
          '& .MuiDialog-paper': {
            p: { xs: 1, sm: 2 },
            borderRadius: { xs: 0, sm: 2 },
          },
        }}
      >
        <Box
          component="form"
          noValidate
          onSubmit={(event) => {
            void save(event)
          }}
        >
          <DialogTitle id="past-exhibition-dialog-title">
            {dialogMode?.type === 'edit'
              ? hr.artistProfile.pastExhibitions.editDialogTitle
              : hr.artistProfile.pastExhibitions.addDialogTitle}
          </DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ pt: 1 }}>
              <TextField
                {...register('title')}
                label={hr.artistProfile.pastExhibitions.title}
                placeholder={hr.artistProfile.pastExhibitions.titlePlaceholder}
                error={Boolean(errors.title)}
                helperText={errors.title?.message}
                disabled={isSaving}
                required
                fullWidth
              />
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                  gap: 2,
                }}
              >
                <TextField
                  {...register('year', { valueAsNumber: true })}
                  label={hr.artistProfile.pastExhibitions.year}
                  type="number"
                  slotProps={{
                    htmlInput: { min: 1900, max: currentYear, step: 1 },
                  }}
                  error={Boolean(errors.year)}
                  helperText={errors.year?.message}
                  disabled={isSaving}
                  required
                  fullWidth
                />
                <TextField
                  {...register('place')}
                  label={hr.artistProfile.pastExhibitions.place}
                  error={Boolean(errors.place)}
                  helperText={errors.place?.message}
                  disabled={isSaving}
                  required
                  fullWidth
                />
              </Box>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ flexWrap: 'wrap', p: 2 }}>
            <Button
              variant="outlined"
              onClick={() => setDialogMode(null)}
              disabled={isSaving}
            >
              {hr.common.cancel}
            </Button>
            <Button type="submit" variant="contained" disabled={isSaving}>
              {isSaving ? hr.common.loading : hr.common.save}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Dialog
        open={deleteTarget !== null}
        disableRestoreFocus
        onClose={() => {
          if (!deleteExhibition.isPending) setDeleteTarget(null)
        }}
        role="alertdialog"
        aria-labelledby="delete-exhibition-title"
        aria-describedby="delete-exhibition-description"
      >
        <DialogTitle id="delete-exhibition-title">
          {hr.artistProfile.pastExhibitions.deleteTitle}
        </DialogTitle>
        <DialogContent id="delete-exhibition-description">
          {deleteTarget &&
            hr.artistProfile.pastExhibitions.deleteDescription(
              deleteTarget.title,
            )}
        </DialogContent>
        <DialogActions sx={{ flexWrap: 'wrap', p: 2 }}>
          <Button
            variant="outlined"
            onClick={() => setDeleteTarget(null)}
            disabled={deleteExhibition.isPending}
          >
            {hr.common.cancel}
          </Button>
          <Button
            color="error"
            variant="contained"
            disabled={deleteExhibition.isPending}
            onClick={() => {
              if (!deleteTarget) return
              deleteExhibition.mutate(deleteTarget.id, {
                onSuccess: () => {
                  setDeleteTarget(null)
                  notify(hr.artistProfile.pastExhibitions.deleted, 'success')
                },
                onError: (error) => {
                  if (error instanceof ApiError && error.status === 404) {
                    setDeleteTarget(null)
                    notify(hr.artistProfile.pastExhibitions.notFound, 'error')
                  } else {
                    notify(error.message || hr.errors.generic, 'error')
                  }
                },
              })
            }}
          >
            {deleteExhibition.isPending ? hr.common.loading : hr.common.delete}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
