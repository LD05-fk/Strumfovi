import AddIcon from '@mui/icons-material/Add'
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward'
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined'
import {
  Box,
  Button,
  IconButton,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import {
  type Control,
  useFieldArray,
  type UseFormRegister,
  type FieldErrors,
} from 'react-hook-form'
import { hr } from '../../i18n/hr'
import type { ArtistProfileFormValues } from './types'

type LinkListEditorProps = {
  control: Control<ArtistProfileFormValues>
  register: UseFormRegister<ArtistProfileFormValues>
  errors: FieldErrors<ArtistProfileFormValues>
  disabled?: boolean
}

export function LinkListEditor({
  control,
  register,
  errors,
  disabled = false,
}: LinkListEditorProps) {
  const { fields, append, remove, move } = useFieldArray({
    control,
    name: 'links',
  })

  return (
    <Stack spacing={2}>
      <Box>
        <Typography component="h2" variant="body2" sx={{ fontWeight: 600 }}>
          {hr.artistProfile.links} {hr.artistProfile.upToTen}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {hr.artistProfile.linksHint} ({fields.length}/10)
        </Typography>
      </Box>

      {fields.length === 0 && (
        <Typography variant="body2" color="text.secondary">
          {hr.artistProfile.noLinks}
        </Typography>
      )}

      {fields.map((link, index) => {
        const labelError = errors.links?.[index]?.label?.message
        const urlError = errors.links?.[index]?.url?.message

        return (
          <Stack
            key={link.id}
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1}
            sx={{ alignItems: { xs: 'stretch', sm: 'flex-start' } }}
          >
            <TextField
              {...register(`links.${index}.label`)}
              label={hr.artistProfile.linkLabel}
              disabled={disabled}
              error={Boolean(labelError)}
              helperText={labelError}
              slotProps={{
                htmlInput: {
                  maxLength: 60,
                  'aria-label': `${hr.artistProfile.linkLabel} ${index + 1}`,
                },
              }}
              sx={{
                flex: 1,
                minWidth: 0,
                '& .MuiInputBase-root': { minHeight: 44 },
                '& .MuiInputBase-input': { p: 1.25 },
              }}
            />
            <Box sx={{ flex: 2, minWidth: 0, pt: { xs: 2, sm: 0 } }}>
              <TextField
                {...register(`links.${index}.url`)}
                label={hr.artistProfile.linkUrl}
                type="url"
                disabled={disabled}
                error={Boolean(urlError)}
                helperText={urlError}
                slotProps={{
                  htmlInput: {
                    maxLength: 1024,
                    'aria-label': `${hr.artistProfile.linkUrl} ${index + 1}`,
                  },
                }}
                sx={{
                  width: '100%',
                  '& .MuiInputBase-root': { minHeight: 44 },
                  '& .MuiInputBase-input': { p: 1.25 },
                }}
              />
            </Box>
            <Stack direction="row" spacing={0.5}>
              <IconButton
                type="button"
                aria-label={`${hr.artistProfile.moveLinkUp} ${index + 1}`}
                disabled={disabled || index === 0}
                onClick={() => move(index, index - 1)}
                sx={{ minWidth: 44, minHeight: 44 }}
              >
                <ArrowUpwardIcon />
              </IconButton>
              <IconButton
                type="button"
                aria-label={`${hr.artistProfile.moveLinkDown} ${index + 1}`}
                disabled={disabled || index === fields.length - 1}
                onClick={() => move(index, index + 1)}
                sx={{ minWidth: 44, minHeight: 44 }}
              >
                <ArrowDownwardIcon />
              </IconButton>
              <IconButton
                type="button"
                aria-label={`${hr.artistProfile.removeLink} ${index + 1}`}
                disabled={disabled}
                onClick={() => remove(index)}
                sx={{ minWidth: 44, minHeight: 44, color: 'error.main' }}
              >
                <DeleteOutlineIcon />
              </IconButton>
            </Stack>
          </Stack>
        )
      })}

      <Button
        type="button"
        variant="outlined"
        startIcon={<AddIcon />}
        disabled={disabled || fields.length >= 10}
        onClick={() => append({ label: '', url: '' })}
        sx={{ alignSelf: 'flex-start', minHeight: 36, px: 1.5 }}
      >
        {hr.artistProfile.addLink}
      </Button>
      {fields.length >= 10 && (
        <Typography role="status" variant="caption" color="text.secondary">
          {hr.artistProfile.addLinkLimit}
        </Typography>
      )}
    </Stack>
  )
}
