import { useMemo } from 'react'
import { Alert, Box, Link, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'

import { hr } from '../../../i18n/hr'
import type { OpportunitySummary } from '../types'
import { OpportunityCard } from './OpportunityCard'
import { MapView } from './map'
import type { MapMarkerData } from './map'

const MAP_HEIGHT = 420
const texts = hr.opportunities.search.map
const cardTexts = hr.opportunities.search.card

type OpportunityMapPanelProps = {
  opportunities: OpportunitySummary[]
  total: number
}

export function OpportunityMapPanel({
  opportunities,
  total,
}: OpportunityMapPanelProps) {
  const markers = useMemo<MapMarkerData[]>(
    () =>
      // prilike bez koordinata (online i nepotpune) nemaju što prikazati na karti
      opportunities.flatMap((opportunity) => {
        const { latitude, longitude } = opportunity
        if (latitude === null || longitude === null) return []

        return [
          {
            id: opportunity.id,
            title: opportunity.title,
            latitude,
            longitude,
            popup: (
              <Stack spacing={0.5}>
                <Typography component="p" variant="subtitle2">
                  {opportunity.title}
                </Typography>
                <Link
                  component={RouterLink}
                  to={`/opportunities/${opportunity.id}`}
                  aria-label={cardTexts.detailsAriaLabel(opportunity.title)}
                  sx={{ fontWeight: 600 }}
                >
                  {cardTexts.details}
                </Link>
              </Stack>
            ),
          },
        ]
      }),
    [opportunities],
  )

  const missingLocationCount = opportunities.length - markers.length
  const notices = [
    total > opportunities.length ? texts.truncated(opportunities.length) : null,
    missingLocationCount > 0 ? texts.missingLocation(missingLocationCount) : null,
  ].filter((notice) => notice !== null)

  return (
    <Stack spacing={2}>
      {notices.length > 0 && <Alert severity="info">{notices.join(' ')}</Alert>}

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' },
          alignItems: 'start',
        }}
      >
        <MapView
          markers={markers}
          height={MAP_HEIGHT}
          fitToMarkers={true}
          ariaLabel={texts.ariaLabel}
        />

        <Box
          component="ul"
          sx={{
            listStyle: 'none',
            m: 0,
            p: 0,
            display: 'grid',
            gap: 2,
            maxHeight: { md: MAP_HEIGHT },
            overflowY: { md: 'auto' },
          }}
        >
          {opportunities.map((opportunity) => (
            <OpportunityCard key={opportunity.id} opportunity={opportunity} />
          ))}
        </Box>
      </Box>
    </Stack>
  )
}
