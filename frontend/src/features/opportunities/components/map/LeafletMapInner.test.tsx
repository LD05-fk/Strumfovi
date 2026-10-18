import type { ReactNode } from 'react'
import { act, cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type L from 'leaflet'
import LeafletMapInner from './LeafletMapInner'
import { hr } from '../../../../i18n/hr'
import { renderWithProviders } from '../../../../test/renderWithProviders'

const leafletHarness = vi.hoisted(() => ({
  mapClick: null as
    ((event: { latlng: { lat: number; lng: number } }) => void) | null,
  map: {
    getZoom: vi.fn(() => 7),
    setView: vi.fn(),
    fitBounds: vi.fn(),
    getCenter: vi.fn(() => ({ lat: 45.1, lng: 15.2 })),
    on: vi.fn(),
    off: vi.fn(),
  },
}))

vi.mock('react-leaflet', async () => {
  const React = await import('react')
  const Leaflet = await import('leaflet')
  const markerInstance = Leaflet.marker([45.1, 15.2])

  return {
    MapContainer: ({ children }: { children: ReactNode }) =>
      React.createElement('div', null, children),

    TileLayer: ({
      eventHandlers,
    }: {
      eventHandlers?: {
        tileerror?: (event: L.TileErrorEvent) => void
        tileload?: (event: L.TileEvent) => void
        tileunload?: (event: L.TileEvent) => void
      }
    }) => {
      const tiles = React.useMemo(
        () => ({
          failedA: document.createElement('img'),
          failedB: document.createElement('img'),
          healthy: document.createElement('img'),
        }),
        [],
      )
      const tileLayer = Leaflet.tileLayer('')
      const event = (tile: HTMLImageElement) => ({
        type: 'tileload',
        popup: null,
        tile,
        coords: Object.assign(Leaflet.point(0, 0), { z: 0 }),
        target: tileLayer,
        sourceTarget: tileLayer,
        propagatedFrom: null,
        layer: tileLayer,
      })

      return React.createElement(
        React.Fragment,
        null,
        React.createElement(
          'button',
          {
            type: 'button',
            onClick: () =>
              eventHandlers?.tileerror?.({
                ...event(tiles.failedA),
                type: 'tileerror',
                error: new Error('Tile A failed'),
              }),
          },
          'Simuliraj pogrešku pločice A',
        ),
        React.createElement(
          'button',
          {
            type: 'button',
            onClick: () =>
              eventHandlers?.tileerror?.({
                ...event(tiles.failedB),
                type: 'tileerror',
                error: new Error('Tile B failed'),
              }),
          },
          'Simuliraj pogrešku pločice B',
        ),
        React.createElement(
          'button',
          {
            type: 'button',
            onClick: () => eventHandlers?.tileload?.(event(tiles.failedA)),
          },
          'Simuliraj oporavak pločice A',
        ),
        React.createElement(
          'button',
          {
            type: 'button',
            onClick: () => eventHandlers?.tileload?.(event(tiles.failedB)),
          },
          'Simuliraj oporavak pločice B',
        ),
        React.createElement(
          'button',
          {
            type: 'button',
            onClick: () => eventHandlers?.tileload?.(event(tiles.healthy)),
          },
          'Simuliraj učitavanje druge pločice',
        ),
        React.createElement(
          'button',
          {
            type: 'button',
            onClick: () => eventHandlers?.tileunload?.(event(tiles.failedB)),
          },
          'Simuliraj uklanjanje pločice B',
        ),
      )
    },

    Marker: React.forwardRef(
      (
        {
          title,
          alt,
          children,
          draggable,
          eventHandlers,
        }: {
          title?: string
          alt?: string
          children?: ReactNode
          draggable?: boolean
          eventHandlers?: { dragend?: () => void }
        },
        ref: React.ForwardedRef<L.Marker>,
      ) => {
        React.useImperativeHandle(ref, () => markerInstance)

        return React.createElement(
          React.Fragment,
          null,
          React.createElement(
            'div',
            { role: 'img', title, 'aria-label': alt },
            children,
          ),
          draggable &&
            React.createElement(
              'button',
              {
                type: 'button',
                onClick: () => {
                  markerInstance.setLatLng([45.12345678, 15.98765432])
                  eventHandlers?.dragend?.()
                },
              },
              'Simuliraj povlačenje oznake',
            ),
        )
      },
    ),

    Popup: ({ children }: { children: ReactNode }) =>
      React.createElement('div', null, children),

    useMap: () => leafletHarness.map,

    useMapEvents: (handlers: {
      click?: (event: { latlng: { lat: number; lng: number } }) => void
    }) => {
      leafletHarness.mapClick = handlers.click ?? null
    },
  }
})

beforeEach(() => {
  vi.clearAllMocks()
  leafletHarness.mapClick = null
})

afterEach(cleanup)

describe('LeafletMapInner', () => {
  it('zaokružuje koordinate klika na šest decimala prije poziva callbacka', () => {
    const onMapClick = vi.fn()

    renderWithProviders(<LeafletMapInner onMapClick={onMapClick} />)

    expect(leafletHarness.mapClick).not.toBeNull()

    act(() => {
      leafletHarness.mapClick?.({
        latlng: { lat: 45.12345678, lng: 15.98765432 },
      })
    })

    expect(onMapClick).toHaveBeenCalledWith(45.123457, 15.987654)
  })

  it('prikazuje oznake i prilagođava prikaz kada je uključeno fitToMarkers', () => {
    renderWithProviders(
      <LeafletMapInner
        fitToMarkers
        markers={[
          {
            id: 'prilika-1',
            latitude: 45.1,
            longitude: 15.2,
            title: 'Izložba u Zagrebu',
          },
          {
            id: 'prilika-2',
            latitude: 44.1,
            longitude: 15.3,
            title: 'Radionica u Zadru',
          },
        ]}
      />,
    )

    expect(screen.getByRole('region', { name: hr.map.ariaLabel })).toBeTruthy()
    expect(screen.getByRole('img', { name: 'Izložba u Zagrebu' })).toBeTruthy()
    expect(screen.getByRole('img', { name: 'Radionica u Zadru' })).toBeTruthy()
    expect(leafletHarness.map.fitBounds).toHaveBeenCalledWith(
      expect.anything(),
      { padding: [24, 24], maxZoom: 15 },
    )
  })

  it('zadržava upozorenje dok se ne oporave sve neuspjele pločice', async () => {
    const user = userEvent.setup()

    renderWithProviders(<LeafletMapInner />)

    await user.click(
      screen.getByRole('button', { name: 'Simuliraj pogrešku pločice A' }),
    )
    await user.click(
      screen.getByRole('button', { name: 'Simuliraj pogrešku pločice B' }),
    )

    expect(screen.getByRole('alert').textContent).toContain(hr.map.unavailable)
    expect(screen.getByRole('region', { name: hr.map.ariaLabel })).toBeTruthy()

    await user.click(
      screen.getByRole('button', { name: 'Simuliraj učitavanje druge pločice' }),
    )
    expect(screen.getByRole('alert')).toBeTruthy()

    await user.click(
      screen.getByRole('button', { name: 'Simuliraj oporavak pločice A' }),
    )
    expect(screen.getByRole('alert')).toBeTruthy()

    await user.click(
      screen.getByRole('button', { name: 'Simuliraj oporavak pločice B' }),
    )
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('na povlačenju oznake javlja nove koordinate zaokružene na šest decimala', async () => {
    const user = userEvent.setup()
    const onDraggableChange = vi.fn()

    renderWithProviders(
      <LeafletMapInner
        draggableMarker={{ latitude: 45.1, longitude: 15.2 }}
        onDraggableChange={onDraggableChange}
      />,
    )

    await user.click(
      screen.getByRole('button', { name: 'Simuliraj povlačenje oznake' }),
    )

    expect(onDraggableChange).toHaveBeenCalledWith({
      latitude: 45.123457,
      longitude: 15.987654,
    })
  })

  it('uklanja pogrešku kad se neuspjela pločica ukloni iz karte', async () => {
    const user = userEvent.setup()

    renderWithProviders(<LeafletMapInner />)

    await user.click(
      screen.getByRole('button', { name: 'Simuliraj pogrešku pločice B' }),
    )
    expect(screen.getByRole('alert')).toBeTruthy()

    await user.click(
      screen.getByRole('button', { name: 'Simuliraj uklanjanje pločice B' }),
    )
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
