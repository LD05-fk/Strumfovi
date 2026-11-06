import { Chip } from '@mui/material'
import { hr } from '../../i18n/hr'

type VisibilityChipProps = {
  visibility: 'PUBLIC' | 'PRIVATE'
}

export function VisibilityChip({ visibility }: VisibilityChipProps) {
  const isPublic = visibility === 'PUBLIC'

  return (
    <Chip
      label={isPublic ? hr.portfolio.public : hr.portfolio.private}
      size="small"
      sx={{
        bgcolor: isPublic ? 'success.main' : 'text.secondary',
        color: 'common.white',
        fontWeight: 600,
      }}
    />
  )
}

export function HiddenByAdminBadge() {
  return (
    <Chip
      label={hr.portfolio.hiddenByAdmin}
      size="small"
      sx={{
        bgcolor: 'warning.main',
        color: 'common.white',
        fontWeight: 600,
      }}
    />
  )
}
