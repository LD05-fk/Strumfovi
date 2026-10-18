import { lazy, Suspense, useCallback, useMemo, useState } from 'react'
import { Box, Button, Typography } from '@mui/material'
import { hr } from '../../../../i18n/hr'

const LeafletMapInner = lazy(() => import('./LeafletMapInner'))

export interface DraggableMarkerMapProps {
  value: { latitude: number; longitude: number } | null
  onChange: (value: { latitude: number; longitude: number }) => void
  center: { latitude: number; longitude: number }
  height?: string | number
  ariaLabel?: string
}

function roundCoordinate(value: number): number {
  return Number(value.toFixed(6))
}

export default function DraggableMarkerMap({
  value,
  onChange,
  center,
  height = 240,
  ariaLabel = hr.map.ariaLabel,
}: Readonly<DraggableMarkerMapProps>) {
  const centerLatitude = center.latitude
  const centerLongitude = center.longitude

  const [viewportState, setViewportState] = useState(() => ({
    sourceLatitude: centerLatitude,
    sourceLongitude: centerLongitude,
    viewportCenter: {
      latitude: centerLatitude,
      longitude: centerLongitude,
    },
  }))

  const viewportCenter =
    viewportState.sourceLatitude === centerLatitude &&
    viewportState.sourceLongitude === centerLongitude
      ? viewportState.viewportCenter
      : center

  const mapCenter = useMemo<[number, number]>(
    () => [centerLatitude, centerLongitude],
    [centerLatitude, centerLongitude],
  )

  const handleMapClick = useCallback(
    (latitude: number, longitude: number) => {
      onChange({ latitude, longitude })
    },
    [onChange],
  )

  const handleViewportCenterChange = useCallback(
    (newCenter: { latitude: number; longitude: number }) => {
      setViewportState({
        sourceLatitude: centerLatitude,
        sourceLongitude: centerLongitude,
        viewportCenter: newCenter,
      })
    },
    [centerLatitude, centerLongitude],
  )

  const setMarkerAtViewportCenter = () => {
    onChange({
      latitude: roundCoordinate(viewportCenter.latitude),
      longitude: roundCoordinate(viewportCenter.longitude),
    })
  }

  return (
    <Box
      sx={{ display: 'flex', flexDirection: 'column', gap: 1, width: '100%' }}
    >
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
          center={mapCenter}
          height={height}
          draggableMarker={value}
          onDraggableChange={onChange}
          onMapClick={handleMapClick}
          onViewportCenterChange={handleViewportCenterChange}
          ariaLabel={ariaLabel}
        />
      </Suspense>

      <Box
        sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}
      >
        <Button variant="outlined" onClick={setMarkerAtViewportCenter}>
          {hr.map.setCenterMarker}
        </Button>
        <Typography variant="body2" color="text.secondary">
          {value ? hr.map.dragHint : hr.map.noMarkerHint}
        </Typography>
      </Box>
    </Box>
  )
}
