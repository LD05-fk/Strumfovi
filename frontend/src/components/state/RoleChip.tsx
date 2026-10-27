import { Chip } from '@mui/material'
import { statusChip } from '../../app/theme'
import { hr } from '../../i18n/hr'
import type { UserRole } from '../layout/navigation'

type RoleChipProps = {
  role: UserRole
}

const roleStatus: Record<UserRole, keyof typeof statusChip> = {
  ARTIST: 'FINISHED',
  ORGANIZER: 'SUBMITTED',
  ADMIN: 'PREPARATION',
}

export function RoleChip({ role }: RoleChipProps) {
  const colors = statusChip[roleStatus[role]]

  return (
    <Chip
      label={hr.auth.roles[role]}
      size="small"
      sx={{
        alignSelf: 'flex-start',
        mt: 0.5,
        bgcolor: colors.bg,
        color: colors.fg,
        fontWeight: 600,
      }}
    />
  )
}
