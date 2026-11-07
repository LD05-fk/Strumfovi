import { Box, Paper, Stack, Typography } from '@mui/material'
import type { Participation } from '../artist-profile/types'
import { hr } from '../../i18n/hr'
import { ParticipationBadge } from './ParticipationBadge'

type ParticipationListProps = {
  participations?: Participation[]
}

export function ParticipationList({
  participations,
}: ParticipationListProps) {
  if (!participations?.length) return null

  return (
    <Box component="section" aria-labelledby="participations-heading">
      <Typography
        id="participations-heading"
        component="h2"
        variant="h3"
        sx={{ mb: 2 }}
      >
        {hr.publicPortfolio.participations}
      </Typography>
      <Stack spacing={2}>
        {participations.map((participation) => (
          <Paper
            key={participation.id}
            sx={{
              p: 2,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 2,
              flexWrap: 'wrap',
            }}
          >
            <Box>
              <Typography component="h3" variant="h6">
                {participation.title}
              </Typography>
              <Typography color="text.secondary">
                {participation.organizerName} · {participation.year}.
              </Typography>
            </Box>
            <ParticipationBadge participation={participation} />
          </Paper>
        ))}
      </Stack>
    </Box>
  )
}
