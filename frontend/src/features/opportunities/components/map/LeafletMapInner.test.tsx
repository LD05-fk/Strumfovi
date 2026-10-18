import type { ReactNode } from 'react'
import { act, cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import LeafletMapInner from './LeafletMapInner'
import { hr } from '../../../../i18n/hr'

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

  return {
    MapContainer: ({ children }: { children: ReactNode }) =>
      React.createElement('div', null, children),

    TileLayer: ({
      eventHandlers,
    }: {
      eventHandlers?: { tileerror?: () => void }
    }) =>
      React.createElement(
        'button',
        {
          type: 'button',
          onClick: () => eventHandlers?.tileerror?.(),
        },
        'Simuliraj pogrešku pločica',
      ),

    Marker: ({
      title,
      alt,
      children,
    }: {
      title?: string
      alt?: string
      children?: ReactNode
    }) =>
      React.createElement(
        'div',
        { role: 'img', title, 'aria-label': alt },
        children,
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

    render(<LeafletMapInner onMapClick={onMapClick} />)

    expect(leafletHarness.mapClick).not.toBeNull()

    act(() => {
      leafletHarness.mapClick?.({
        latlng: { lat: 45.12345678, lng: 15.98765432 },
      })
    })

    expect(onMapClick).toHaveBeenCalledWith(45.123457, 15.987654)
  })

  it('prikazuje oznake i prilagođava prikaz kada je uključeno fitToMarkers', () => {
    render(
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

  it('prikazuje upozorenje na pogrešku pločica, a kartu ostavlja dostupnom', async () => {
    const user = userEvent.setup()

    render(<LeafletMapInner />)

    await user.click(
      screen.getByRole('button', { name: 'Simuliraj pogrešku pločica' }),
    )

    expect(screen.getByRole('alert').textContent).toContain(hr.map.unavailable)
    expect(screen.getByRole('region', { name: hr.map.ariaLabel })).toBeTruthy()
  })
})
