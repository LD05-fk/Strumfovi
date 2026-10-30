import { Box, TextField, Typography } from '@mui/material'
import { Controller, type Control } from 'react-hook-form'
import { hr } from '../../i18n/hr'
import type { ArtistProfileFormValues } from './types'

type CharacterCountTextFieldProps = {
  control: Control<ArtistProfileFormValues>
  name: 'biography' | 'artistStatement'
  label: string
  disabled?: boolean
}

export function CharacterCountTextField({
  control,
  name,
  label,
  disabled = false,
}: CharacterCountTextFieldProps) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => {
        const id = `artist-profile-${name}`
        return (
          <Box>
            <Typography
              component="label"
              htmlFor={id}
              variant="body2"
              sx={{ display: 'block', mb: 0.5, fontWeight: 600 }}
            >
              {label}
            </Typography>
            <TextField
              {...field}
              id={id}
              disabled={disabled}
              fullWidth
              multiline
              minRows={3}
              error={fieldState.invalid}
              helperText={
                fieldState.error?.message ??
                hr.artistProfile.characterCount(field.value.length)
              }
              slotProps={{ htmlInput: { maxLength: 5000 } }}
              sx={{
                '& .MuiInputBase-root': {
                  minHeight: 96,
                  alignItems: 'flex-start',
                },
                '& .MuiInputBase-inputMultiline': { p: 1.25 },
              }}
            />
          </Box>
        )
      }}
    />
  )
}
