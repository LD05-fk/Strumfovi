import {
  cleanup,
  fireEvent,
  screen,
  waitFor,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DraggableMarkerMap from './DraggableMarkerMap'
import { hr } from '../../../../i18n/hr'
import { renderWithProviders } from '../../../../test/renderWithProviders'

beforeEach(() => {
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(800)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(500)
  vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(800)
  vi.spyOn(HTMLElement.prototype, 'offsetHeight', 'get').mockReturnValue(500)
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue(
    new DOMRect(0, 0, 800, 500),
  )
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('DraggableMarkerMap integracija s Leafletom', () => {
  it('klik na stvarnu Leaflet kartu poziva onChange s koordinatama', async () => {
    const onChange = vi.fn()

    renderWithProviders(
      <DraggableMarkerMap
        value={null}
        onChange={onChange}
        center={{ latitude: 45.1, longitude: 15.2 }}
      />,
    )

    await screen.findByRole('link', { name: 'OpenStreetMap' })

    const region = screen.getByRole('region', { name: hr.map.ariaLabel })
    const mapElement = region.firstElementChild

    if (!mapElement) throw new Error('Leaflet karta nije renderirana.')

    fireEvent.click(mapElement, { clientX: 400, clientY: 250 })

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledTimes(1)
    })

    const coordinates = onChange.mock.calls[0]?.[0]
    expect(coordinates).toBeDefined()
    if (!coordinates) throw new Error('Koordinate nisu poslane u onChange.')

    expect(coordinates).toEqual({ latitude: 45.096791, longitude: 15.205078 })

    fireEvent.click(mapElement, { clientX: 600, clientY: 250 })

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledTimes(2)
    })

    const edgeCoordinates = onChange.mock.calls[1]?.[0]
    expect(edgeCoordinates).toBeDefined()
    if (!edgeCoordinates) throw new Error('Rubne koordinate nisu poslane.')

    expect(edgeCoordinates).toEqual({
      latitude: 45.096791,
      longitude: 17.402344,
    })
  })
})
