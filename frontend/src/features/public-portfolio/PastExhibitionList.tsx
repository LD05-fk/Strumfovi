import { Box, Paper, Stack, Typography } from '@mui/material'
import type { PastExhibition } from '../artist-profile/types'
import { hr } from '../../i18n/hr'

type PastExhibitionListProps = {
  exhibitions: PastExhibition[]
}

export function PastExhibitionList({
  exhibitions,
}: PastExhibitionListProps) {
  if (exhibitions.length === 0) return null

  return (
    <Box component="section" aria-labelledby="past-exhibitions-heading">
      <Typography
        id="past-exhibitions-heading"
        component="h2"
        variant="h3"
        sx={{ mb: 2 }}
      >
        {hr.publicPortfolio.pastExhibitions}
      </Typography>
      <Stack spacing={2}>
        {exhibitions.map((exhibition) => (
          <Paper key={exhibition.id} sx={{ p: 2 }}>
            <Typography component="h3" variant="h6">
              {exhibition.title}
            </Typography>
            <Typography color="text.secondary">
              {exhibition.year}. · {exhibition.place}
            </Typography>
          </Paper>
        ))}
      </Stack>
    </Box>
  )
}
