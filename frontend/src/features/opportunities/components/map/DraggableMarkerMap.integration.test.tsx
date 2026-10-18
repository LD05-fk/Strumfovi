import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DraggableMarkerMap from './DraggableMarkerMap'
import { hr } from '../../../../i18n/hr'

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

    render(
      <DraggableMarkerMap
        value={null}
        onChange={onChange}
        center={{ latitude: 45.1, longitude: 15.2 }}
      />,
    )

    await waitFor(() => {
      const region = screen.getByRole('region', { name: hr.map.ariaLabel })
      expect(region.querySelector('.leaflet-container')).not.toBeNull()
    })

    const region = screen.getByRole('region', { name: hr.map.ariaLabel })
    const mapElement = region.querySelector('.leaflet-container')

    if (!mapElement) throw new Error('Leaflet karta nije renderirana.')

    fireEvent.click(mapElement, { clientX: 400, clientY: 250 })

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledTimes(1)
    })

    const coordinates = onChange.mock.calls[0]?.[0]
    expect(coordinates).toBeDefined()
    if (!coordinates) throw new Error('Koordinate nisu poslane u onChange.')

    expect(Number.isFinite(coordinates.latitude)).toBe(true)
    expect(Number.isFinite(coordinates.longitude)).toBe(true)
    expect(coordinates.latitude).not.toBe(45.1)
    expect(coordinates.longitude).not.toBe(15.2)
    expect(coordinates.latitude).toBeGreaterThanOrEqual(-90)
    expect(coordinates.latitude).toBeLessThanOrEqual(90)
    expect(coordinates.longitude).toBeGreaterThanOrEqual(-180)
    expect(coordinates.longitude).toBeLessThanOrEqual(180)
  })
})
