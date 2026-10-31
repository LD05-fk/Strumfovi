import { Autocomplete, Box, TextField, Typography } from '@mui/material'
import { Controller, type Control } from 'react-hook-form'
import { hr } from '../../i18n/hr'
import type {
  ArtFieldOption,
  ArtistProfileField,
  ArtistProfileFormValues,
} from './types'

type ArtFieldsSelectProps = {
  control: Control<ArtistProfileFormValues>
  activeFields: ArtFieldOption[]
  profileFields: ArtistProfileField[]
  disabled?: boolean
}

export function ArtFieldsSelect({
  control,
  activeFields,
  profileFields,
  disabled = false,
}: ArtFieldsSelectProps) {
  const activeIds = new Set(activeFields.map((field) => field.id))
  const existingInactiveFields = profileFields
    .filter((field) => !activeIds.has(field.id))
    .map((field) => ({
      ...field,
      active: false,
      position: Number.MAX_SAFE_INTEGER,
    }))
  const options = [...activeFields, ...existingInactiveFields]

  return (
    <Controller
      control={control}
      name="artFieldIds"
      render={({ field, fieldState }) => {
        const selected = options.filter((option) =>
          field.value.includes(option.id),
        )

        const id = 'artist-profile-art-fields'
        return (
          <Box>
            <Typography
              component="label"
              htmlFor={id}
              variant="body2"
              sx={{ display: 'block', mb: 0.5, fontWeight: 600 }}
            >
              {hr.artistProfile.artFields} {hr.artistProfile.upToTen}
            </Typography>
            <Autocomplete<ArtFieldOption, true, false, false>
              id={id}
              multiple
              disableCloseOnSelect
              filterSelectedOptions
              disabled={disabled}
              options={options}
              value={selected}
              getOptionLabel={(option) =>
                option.active
                  ? option.name
                  : `${option.name} (${hr.artistProfile.inactiveField})`
              }
              isOptionEqualToValue={(option, value) => option.id === value.id}
              getOptionDisabled={(option) =>
                !option.active ||
                (field.value.length >= 10 && !field.value.includes(option.id))
              }
              noOptionsText={hr.artistProfile.artFieldsEmpty}
              onChange={(_, values) =>
                field.onChange(values.map((option) => option.id))
              }
              slotProps={{
                chip: {
                  sx: {
                    bgcolor: 'primary.light',
                    color: 'primary.dark',
                    '& .MuiChip-deleteIcon': {
                      color: 'inherit',
                      '&:hover': { color: 'primary.dark' },
                    },
                  },
                },
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  error={fieldState.invalid}
                  helperText={
                    fieldState.error?.message ??
                    `${hr.artistProfile.artFieldsHint} (${field.value.length}/10)`
                  }
                  sx={{
                    '& .MuiInputBase-root': { minHeight: 44 },
                    '& .MuiInputBase-input': { p: 1.25 },
                  }}
                />
              )}
            />
          </Box>
        )
      }}
    />
  )
}
