import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  Chip,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useForm, useWatch } from 'react-hook-form'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import { statusChip } from '../../app/theme'
import { useNotify } from '../../components/state/useNotify'
import { hr } from '../../i18n/hr'
import { useCurrentUser } from '../auth/useCurrentUser'
import { organizerProfileSchema } from './organizerProfileSchema'
import { toOrganizerProfileUpdate, type OrganizerProfile } from './types'
import { useUpdateOrganizerProfile } from './organizerProfileQueries'

type OrganizerProfileFormProps = {
  profile: OrganizerProfile
}

export function OrganizerProfileForm({ profile }: OrganizerProfileFormProps) {
  const notify = useNotify()
  const [saved, setSaved] = useState(false)
  const { data: user } = useCurrentUser()
  const updateProfile = useUpdateOrganizerProfile()
  const {
    control,
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(organizerProfileSchema),
    defaultValues: {
      organizationName: profile.organizationName ?? '',
    },
  })
  const organizationName = useWatch({
    control,
    name: 'organizationName',
  })

  const onSubmit = handleSubmit((values) => {
    updateProfile.mutate(toOrganizerProfileUpdate(values), {
      onSuccess: (savedProfile) => {
        reset({ organizationName: savedProfile.organizationName ?? '' })
        setSaved(true)
      },
      onError: (error) => {
        if (
          error instanceof ApiError &&
          error.status === 400 &&
          error.code === 'VALIDATION_ERROR'
        ) {
          const fieldErrors = error.details.filter(
            (detail) => detail.field === 'organizationName',
          )

          for (const detail of fieldErrors) {
            setError('organizationName', {
              type: 'server',
              message: detail.issue,
            })
          }

          if (
            fieldErrors.length === error.details.length &&
            fieldErrors.length
          ) {
            return
          }
        }

        notify(
          error instanceof Error ? error.message : hr.errors.generic,
          'error',
        )
      },
    })
  })

  const approvalLabel =
    profile.organizerApproval === 'APPROVED'
      ? hr.organizerProfile.approved
      : hr.organizerProfile.pendingApproval
  const approvalColors =
    profile.organizerApproval === 'APPROVED'
      ? statusChip.ACCEPTED
      : statusChip.UNDER_REVIEW

  return (
    <Box sx={{ width: '100%', maxWidth: 640, mx: 'auto' }}>
      <Typography component="h1" variant="h2" sx={{ mb: 2 }}>
        {hr.organizerProfile.pageTitle}
      </Typography>

      {saved && (
        <Alert severity="success" role="status" sx={{ mb: 2 }}>
          {hr.organizerProfile.saved}
        </Alert>
      )}

      <Card
        component="form"
        noValidate
        onSubmit={onSubmit}
        sx={{ p: { xs: 2, sm: 3 } }}
      >
        <Stack spacing={2}>
          <Stack
            direction="row"
            spacing={2}
            sx={{ alignItems: 'center', minWidth: 0 }}
          >
            <Avatar
              src={user?.avatarUrl ?? undefined}
              alt={profile.displayName}
              aria-label={profile.displayName}
              role="img"
              sx={{ width: 56, height: 56, bgcolor: 'primary.main' }}
            >
              {profile.displayName
                .split(/\s+/)
                .slice(0, 2)
                .map((part) => part[0])
                .join('')}
            </Avatar>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="h6" noWrap>
                {profile.organizationName ??
                  hr.organizerProfile.organizationNameMissing}
              </Typography>
              <Typography variant="body2" color="text.secondary" noWrap>
                {user?.email}
              </Typography>
            </Box>
            <Chip
              label={approvalLabel}
              size="small"
              sx={{
                flexShrink: 0,
                bgcolor: approvalColors.bg,
                color: approvalColors.fg,
                fontSize: '0.75rem',
                lineHeight: '1.25rem',
              }}
            />
          </Stack>

          <Box>
            <Typography
              component="label"
              htmlFor="organizer-display-name"
              variant="body2"
              sx={{ display: 'block', mb: 0.5, fontWeight: 600 }}
            >
              {hr.organizerProfile.displayName}
            </Typography>
            <TextField
              id="organizer-display-name"
              value={profile.displayName}
              fullWidth
              slotProps={{ htmlInput: { readOnly: true } }}
              sx={{
                '& .MuiInputBase-root': {
                  bgcolor: 'action.disabledBackground',
                },
              }}
            />
          </Box>

          <Box>
            <Typography
              component="label"
              htmlFor="organizationName"
              variant="body2"
              sx={{ display: 'block', mb: 0.5, fontWeight: 600 }}
            >
              {hr.organizerProfile.organizationName}
            </Typography>
            <TextField
              id="organizationName"
              placeholder={hr.organizerProfile.organizationNamePlaceholder}
              fullWidth
              error={Boolean(errors.organizationName)}
              helperText={
                errors.organizationName?.message ??
                hr.organizerProfile.organizationNameCharacterCount(
                  organizationName.length,
                )
              }
              slotProps={{
                htmlInput: {
                  'aria-describedby': 'organizationName-helper-text',
                  'aria-invalid': Boolean(errors.organizationName),
                },
              }}
              {...register('organizationName', {
                onChange: () => setSaved(false),
              })}
            />
            {profile.organizerApproval === 'PENDING' && (
              <Alert severity="info">{hr.organizerProfile.pendingHint}</Alert>
            )}
          </Box>

          <Button
            type="submit"
            variant="contained"
            disabled={updateProfile.isPending}
            sx={{ alignSelf: { xs: 'stretch', sm: 'flex-start' } }}
          >
            {updateProfile.isPending
              ? hr.organizerProfile.saving
              : hr.organizerProfile.saveProfile}
          </Button>
        </Stack>
      </Card>
    </Box>
  )
}
