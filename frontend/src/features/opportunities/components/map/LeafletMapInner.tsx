import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { Alert, Box } from '@mui/material'
import { useTheme } from '@mui/material/styles'
import L from 'leaflet'
import {
  MapContainer,
  Marker,
  Popup,
  TileLayer,
  useMap,
  useMapEvents,
} from 'react-leaflet'

import 'leaflet/dist/leaflet.css'
import { hr } from '../../../../i18n/hr'

const CROATIA_CENTER: [number, number] = [45.1, 15.2]
const DEFAULT_ZOOM = 7

type Coordinates = {
  latitude: number
  longitude: number
}

export interface MapMarkerData extends Coordinates {
  id: string | number
  title: string
  popup?: ReactNode
}

const EMPTY_MARKERS: MapMarkerData[] = []

interface MapEventsHandlerProps {
  onClick?: (latitude: number, longitude: number) => void
}

function roundCoordinate(value: number): number {
  return Number(value.toFixed(6))
}

function MapEventsHandler({ onClick }: MapEventsHandlerProps) {
  useMapEvents({
    click(event) {
      onClick?.(
        roundCoordinate(event.latlng.lat),
        roundCoordinate(event.latlng.lng),
      )
    },
  })

  return null
}

interface MapViewportProps {
  center: [number, number]
  fitToMarkers: boolean
  markers: MapMarkerData[]
  zoom: number
}

function MapViewport({
  center,
  fitToMarkers,
  markers,
  zoom,
}: MapViewportProps) {
  const map = useMap()
  const lastViewportKey = useRef<string | null>(null)
  const [latitude, longitude] = center

  const viewportKey =
    fitToMarkers && markers.length > 0
      ? JSON.stringify([
          'markers',
          markers.map(({ latitude, longitude }) => [latitude, longitude]),
        ])
      : JSON.stringify(['center', latitude, longitude, zoom])

  useEffect(() => {
    if (lastViewportKey.current === viewportKey) return

    if (fitToMarkers && markers.length > 1) {
      const bounds = L.latLngBounds(
        markers.map(({ latitude, longitude }) => [latitude, longitude]),
      )
      map.fitBounds(bounds, { padding: [24, 24], maxZoom: 15 })
    } else if (fitToMarkers && markers.length === 1) {
      const [marker] = markers
      map.setView(
        [marker.latitude, marker.longitude],
        Math.max(map.getZoom(), 13),
      )
    } else {
      map.setView([latitude, longitude], zoom)
    }

    lastViewportKey.current = viewportKey
  }, [fitToMarkers, latitude, longitude, map, markers, viewportKey, zoom])

  return null
}

function MapCenterReporter({
  onChange,
}: {
  onChange?: (center: Coordinates) => void
}) {
  const map = useMap()

  useEffect(() => {
    const reportCenter = () => {
      const { lat, lng } = map.getCenter()
      onChange?.({
        latitude: roundCoordinate(lat),
        longitude: roundCoordinate(lng),
      })
    }

    map.on('moveend', reportCenter)
    reportCenter()

    return () => {
      map.off('moveend', reportCenter)
    }
  }, [map, onChange])

  return null
}

export interface LeafletMapInnerProps {
  markers?: MapMarkerData[]
  draggableMarker?: Coordinates | null
  onDraggableChange?: (coordinates: Coordinates) => void
  onMapClick?: (latitude: number, longitude: number) => void
  onViewportCenterChange?: (center: Coordinates) => void
  center?: [number, number]
  zoom?: number
  height?: string | number
  ariaLabel?: string
  fitToMarkers?: boolean
}

export default function LeafletMapInner({
  markers = EMPTY_MARKERS,
  draggableMarker = null,
  onDraggableChange,
  onMapClick,
  onViewportCenterChange,
  center = CROATIA_CENTER,
  zoom = DEFAULT_ZOOM,
  height = 400,
  ariaLabel = hr.map.ariaLabel,
  fitToMarkers = false,
}: Readonly<LeafletMapInnerProps>) {
  const [hasTileError, setHasTileError] = useState(false)
  const markerRef = useRef<L.Marker>(null)

  const theme = useTheme()

  const themedMarkerIcon = useMemo(
    () =>
      L.divIcon({
        className: 'opportunity-map-marker',
        html: `
        <svg
          aria-hidden="true"
          width="32"
          height="40"
          viewBox="0 0 32 40"
          style="filter: drop-shadow(0 2px 3px rgba(28,25,23,.3))"
        >
          <path
            d="M16 1C7.7 1 1 7.7 1 16c0 11 15 23 15 23s15-12 15-23C31 7.7 24.3 1 16 1Z"
            fill="${theme.palette.secondary.main}"
            stroke="${theme.palette.background.paper}"
            stroke-width="2"
          />
        </svg>
      `,
        iconSize: [32, 40],
        iconAnchor: [16, 40],
        popupAnchor: [0, -40],
      }),
    [theme.palette.secondary.main, theme.palette.background.paper],
  )

  const tileEventHandlers = useMemo(
    () => ({
      tileerror: () => setHasTileError(true),
    }),
    [],
  )

  const dragEventHandlers = useMemo(
    () => ({
      dragend: () => {
        const position = markerRef.current?.getLatLng()
        if (!position) return

        onDraggableChange?.({
          latitude: roundCoordinate(position.lat),
          longitude: roundCoordinate(position.lng),
        })
      },
    }),
    [onDraggableChange],
  )

  return (
    <Box
      sx={{ display: 'flex', flexDirection: 'column', gap: 1, width: '100%' }}
    >
      <Box
        role="region"
        aria-label={ariaLabel}
        sx={{
          height,
          width: '100%',
          borderRadius: '12px',
          border: '1px solid',
          borderColor: 'divider',
          overflow: 'hidden',
          backgroundColor: 'background.default',
          '& .leaflet-container': {
            height: '100%',
            width: '100%',
            fontFamily: 'inherit',
          },
          '& .leaflet-popup-content-wrapper': {
            color: theme.palette.text.primary,
            fontFamily: theme.typography.fontFamily,
          },
          '& .leaflet-popup-content': {
            margin: theme.spacing(1.5),
            fontFamily: theme.typography.fontFamily,
            fontSize: theme.typography.body2.fontSize,
            lineHeight: theme.typography.body2.lineHeight,
          },
        }}
      >
        <MapContainer
          center={center}
          zoom={zoom}
          keyboard
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
            maxZoom={19}
            eventHandlers={tileEventHandlers}
          />

          <MapViewport
            center={center}
            fitToMarkers={fitToMarkers}
            markers={markers}
            zoom={zoom}
          />
          <MapEventsHandler onClick={onMapClick} />
          <MapCenterReporter onChange={onViewportCenterChange} />

          {markers.map((marker) => (
            <Marker
              key={marker.id}
              position={[marker.latitude, marker.longitude]}
              icon={themedMarkerIcon}
              title={marker.title}
              alt={marker.title}
            >
              {marker.popup && <Popup>{marker.popup}</Popup>}
            </Marker>
          ))}

          {draggableMarker && (
            <Marker
              ref={markerRef}
              position={[draggableMarker.latitude, draggableMarker.longitude]}
              icon={themedMarkerIcon}
              draggable
              eventHandlers={dragEventHandlers}
              title={hr.map.draggableMarkerTitle}
              alt={hr.map.draggableMarkerTitle}
            />
          )}
        </MapContainer>
      </Box>

      {hasTileError && (
        <Alert severity="warning" sx={{ borderRadius: 2 }}>
          {hr.map.unavailable}
        </Alert>
      )}
    </Box>
  )
}

