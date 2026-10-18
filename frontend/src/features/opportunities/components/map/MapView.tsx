import { lazy, Suspense } from 'react'
import { Box } from '@mui/material'
import { hr } from '../../../../i18n/hr'
import type { MapMarkerData } from './LeafletMapInner'

const LeafletMapInner = lazy(() => import('./LeafletMapInner'))

export interface MapViewProps {
  markers: MapMarkerData[]
  height?: string | number
  fitToMarkers?: boolean
  ariaLabel?: string
}

export default function MapView({
  markers,
  height = 400,
  fitToMarkers = false,
  ariaLabel = hr.map.ariaLabel,
}: Readonly<MapViewProps>) {
  return (
    <Suspense
      fallback={
        <Box
          role="region"
          aria-label={ariaLabel}
          aria-busy="true"
          sx={{
            width: '100%',
            height,
            borderRadius: '12px',
            backgroundColor: 'background.default',
            border: '1px solid',
            borderColor: 'divider',
          }}
        />
      }
    >
      <LeafletMapInner
        markers={markers}
        height={height}
        fitToMarkers={fitToMarkers}
        ariaLabel={ariaLabel}
      />
    </Suspense>
  )
}
