import {
  Alert,
  Avatar,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
} from '@mui/material'
import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useBlocker } from 'react-router'
import { useForm, type Path } from 'react-hook-form'
import { ApiError } from '../../api/client'
import { useNotify } from '../../components/state/useNotify'
import { hr } from '../../i18n/hr'
import { artistProfileSchema } from './artistProfileSchema'
import { useUpdateMyArtistProfile } from './artistProfileQueries'
import { ArtFieldsSelect } from './ArtFieldsSelect'
import { CharacterCountTextField } from './CharacterCountTextField'
import { LinkListEditor } from './LinkListEditor'
import {
  toArtistProfileFormValues,
  toArtistProfileUpdate,
  type ArtFieldOption,
  type ArtistProfile,
  type ArtistProfileFormValues,
} from './types'

type ArtistProfileFormProps = {
  profile: ArtistProfile
  artFields: ArtFieldOption[]
  email?: string
}

function mapServerField(field: string): Path<ArtistProfileFormValues> | null {
  const normalized = field.replace(/\[(\d+)\]/g, '.$1')
  if (
    normalized === 'biography' ||
    normalized === 'artistStatement' ||
    normalized === 'artFieldIds'
  ) {
    return normalized
  }

  if (/^links\.\d+\.(label|url)$/.test(normalized)) {
    return normalized as Path<ArtistProfileFormValues>
  }

  return null
}

export function ArtistProfileForm({
  profile,
  artFields,
  email,
}: ArtistProfileFormProps) {
  const notify = useNotify()
  const updateProfile = useUpdateMyArtistProfile()
  const {
    control,
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isDirty },
  } = useForm<ArtistProfileFormValues>({
    resolver: zodResolver(artistProfileSchema),
    defaultValues: toArtistProfileFormValues(profile),
  })
  const blocker = useBlocker(isDirty)

  useEffect(() => {
    if (!isDirty) return

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty])

  const onSubmit = handleSubmit((values) => {
    updateProfile.mutate(toArtistProfileUpdate(values), {
      onSuccess: (savedProfile) => {
        reset(toArtistProfileFormValues(savedProfile))
        notify(hr.artistProfile.saved, 'success')
      },
      onError: (error) => {
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

        notify(
          error instanceof Error ? error.message : hr.errors.generic,
          'error',
        )
      },
    })
  })

  return (
    <>
      {!profile.biography &&
        !profile.artistStatement &&
        profile.artFields.length === 0 &&
        profile.links.length === 0 && (
          <Alert severity="info" sx={{ mb: 2 }}>
            {hr.artistProfile.emptyProfileHint}
          </Alert>
        )}
      <Card
        component="form"
        noValidate
        onSubmit={onSubmit}
        sx={{ p: 2 }}
      >
        <Stack spacing={2}>
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            <Avatar
              src={profile.avatarUrl ?? undefined}
              alt={profile.displayName}
              sx={{ width: 56, height: 56, bgcolor: 'primary.main' }}
            >
              {profile.displayName.slice(0, 1)}
            </Avatar>
            <Stack>
              <Typography variant="h6">{profile.displayName}</Typography>
              {email && (
                <Typography variant="body2" color="text.secondary">
                  {email}
                </Typography>
              )}
            </Stack>
          </Stack>

          <CharacterCountTextField
            control={control}
            name="biography"
            label={hr.artistProfile.biography}
            disabled={updateProfile.isPending}
          />
          <CharacterCountTextField
            control={control}
            name="artistStatement"
            label={hr.artistProfile.artistStatement}
            disabled={updateProfile.isPending}
          />
          <ArtFieldsSelect
            control={control}
            activeFields={artFields}
            profileFields={profile.artFields}
            disabled={updateProfile.isPending}
          />
          <LinkListEditor
            control={control}
            register={register}
            errors={errors}
            disabled={updateProfile.isPending}
          />
          {errors.links?.message && (
            <Typography role="alert" color="error">
              {errors.links.message}
            </Typography>
          )}
          <Button
            type="submit"
            variant="contained"
            disabled={updateProfile.isPending}
            sx={{ alignSelf: { xs: 'stretch', sm: 'flex-start' } }}
          >
            {updateProfile.isPending
              ? hr.common.loading
              : hr.artistProfile.saveProfile}
          </Button>
        </Stack>
      </Card>

      <Dialog
        open={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        aria-labelledby="profile-leave-title"
        aria-describedby="profile-leave-description"
      >
        <DialogTitle id="profile-leave-title">
          {hr.artistProfile.leaveTitle}
        </DialogTitle>
        <DialogContent id="profile-leave-description">
          {hr.artistProfile.leaveMessage}
        </DialogContent>
        <DialogActions sx={{ flexWrap: 'wrap', p: 2 }}>
          <Button onClick={() => blocker.reset?.()}>
            {hr.artistProfile.continueEditing}
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => blocker.proceed?.()}
          >
            {hr.artistProfile.discardChanges}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
