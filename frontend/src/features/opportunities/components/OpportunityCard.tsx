import { Button, Card, CardContent, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'

import { dayjs, OPPORTUNITY_TIME_ZONE } from '../../../app/dayjs'
import { StatusChip } from '../../../components/state/StatusChip'
import { hr } from '../../../i18n/hr'
import type { OpportunitySummary } from '../types'

function formatDeadline(deadline: string): string {
  return dayjs(deadline).tz(OPPORTUNITY_TIME_ZONE).format('DD.MM.YYYY.')
}

type OpportunityCardProps = {
  opportunity: OpportunitySummary
}

export function OpportunityCard({ opportunity }: OpportunityCardProps) {
  const organizer = opportunity.organizer.organizationName ?? opportunity.organizer.displayName
  const place = opportunity.locationType === 'ONLINE'
      ? hr.opportunities.search.card.online
      : opportunity.city

  return (
    <Card
      component="li"
      sx={{
        display: 'flex',
        flexDirection: 'column',
        '&:hover': { boxShadow: '0 4px 12px rgba(28,25,23,.12)' },
      }}
    >
      <CardContent
        sx={{
          display: 'flex',
          flexDirection: 'column',
          flexGrow: 1,
          gap: 1,
        }}
      >
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <StatusChip type="opportunity" status={opportunity.status} />
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ ml: 'auto' }}
          >
            {opportunity.type.name}
          </Typography>
        </Stack>

        <Typography component="h2" variant="h4">
          {opportunity.title}
        </Typography>

        <Typography variant="body2" color="text.secondary">
          {place === null ? organizer : `${organizer} · ${place}`}
        </Typography>

        <Typography variant="body2" color="text.secondary">
          {hr.opportunities.search.card.deadlineLabel}{' '}
          <Typography
            component="time"
            variant="body2"
            dateTime={opportunity.deadline}
            sx={{ fontWeight: 700, color: 'text.primary' }}
          >
            {formatDeadline(opportunity.deadline)}
          </Typography>
        </Typography>

        <Button
          component={RouterLink}
          to={`/opportunities/${opportunity.id}`}
          variant="outlined"
          size="small"
          aria-label={hr.opportunities.search.card.detailsAriaLabel(opportunity.title)}
          sx={{
            alignSelf: 'flex-start',
            mt: 'auto',
            bgcolor: 'background.paper',
          }}
        >
          {hr.opportunities.search.card.details}
        </Button>
      </CardContent>
    </Card>
  )
}
