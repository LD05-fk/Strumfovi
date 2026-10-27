import { Chip } from '@mui/material'
import { statusChip } from '../../app/theme'
import type { UserRole } from '../layout/navigation'

type RoleChipProps = {
  role: UserRole
}

const roleStatus: Record<UserRole, keyof typeof statusChip> = {
  ARTIST: 'FINISHED',
  ORGANIZER: 'SUBMITTED',
  ADMIN: 'PREPARATION',
}

const roleLabels: Record<UserRole, string> = {
  ARTIST: 'Umjetnik',
  ORGANIZER: 'Organizator',
  ADMIN: 'Administrator',
}

export function RoleChip({ role }: RoleChipProps) {
  const colors = statusChip[roleStatus[role]]

  return (
    <Chip
      label={roleLabels[role]}
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
