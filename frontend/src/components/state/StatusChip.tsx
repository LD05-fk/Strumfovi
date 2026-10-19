import { Chip } from '@mui/material'
import { hr } from '../../i18n/hr'

export type OpportunityStatus = keyof typeof hr.opportunityStatus
export type ApplicationStatus = keyof typeof hr.applicationStatus

type StatusChipProps =
  | { type: 'opportunity'; status: OpportunityStatus }
  | { type: 'application'; status: ApplicationStatus }

type ChipColor = { background: string; foreground: string }

const opportunityStatusColors: Record<OpportunityStatus, ChipColor> = {
  PREPARATION: { background: '#E7E5E4', foreground: '#44403C' },
  OPEN: { background: '#DCFCE7', foreground: '#166534' },
  CLOSED: { background: '#FEE2E2', foreground: '#991B1B' },
  SELECTION: { background: '#FEF3C7', foreground: '#92400E' },
  FINISHED: { background: '#EEF2FF', foreground: '#312E81' },
}

const applicationStatusColors: Record<ApplicationStatus, ChipColor> = {
  SUBMITTED: { background: '#DBEAFE', foreground: '#1E40AF' },
  UNDER_REVIEW: { background: '#FEF3C7', foreground: '#92400E' },
  NEEDS_INFO: { background: '#FEF3C7', foreground: '#92400E' },
  ACCEPTED: { background: '#DCFCE7', foreground: '#166534' },
  REJECTED: { background: '#FEE2E2', foreground: '#991B1B' },
  WITHDRAWN: { background: '#E7E5E4', foreground: '#44403C' },
}

export function StatusChip(props: StatusChipProps) {
  const { label, colors } =
    props.type === 'opportunity'
      ? {
          label: hr.opportunityStatus[props.status],
          colors: opportunityStatusColors[props.status],
        }
      : {
          label: hr.applicationStatus[props.status],
          colors: applicationStatusColors[props.status],
        }

  return (
    <Chip
      label={label}
      size="small"
      sx={{
        bgcolor: colors.background,
        color: colors.foreground,
        fontWeight: 600,
      }}
    />
  )
}
