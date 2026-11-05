import { Chip } from '@mui/material'
import type { Participation } from '../artist-profile/types'
import { hr } from '../../i18n/hr'

type ParticipationBadgeProps = {
  participation: Participation
}

export function ParticipationBadge({
  participation,
}: ParticipationBadgeProps) {
  return (
    <Chip
      label={hr.publicPortfolio.participationBadge}
      color="success"
      aria-label={`${hr.publicPortfolio.participationBadge}: ${participation.title}`}
    />
  )
}
