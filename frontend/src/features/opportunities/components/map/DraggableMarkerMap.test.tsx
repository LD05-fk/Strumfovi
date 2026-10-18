import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import DraggableMarkerMap from './DraggableMarkerMap'
import { hr } from '../../../../i18n/hr'

vi.mock('./LeafletMapInner', async () => {
  const React = await import('react')

  return {
    default: (props: {
      onMapClick?: (latitude: number, longitude: number) => void
      onViewportCenterChange?: (center: {
        latitude: number
        longitude: number
      }) => void
    }) =>
      React.createElement(
        React.Fragment,
        null,
        React.createElement(
          'button',
          {
            type: 'button',
            onClick: () => props.onMapClick?.(45.123457, 15.987654),
          },
          'Simuliraj klik na kartu',
        ),
        React.createElement(
          'button',
          {
            type: 'button',
            onClick: () =>
              props.onViewportCenterChange?.({
                latitude: 45.12345678,
                longitude: 15.98765432,
              }),
          },
          'Simuliraj pomak karte',
        ),
      ),
  }
})

afterEach(cleanup)

describe('DraggableMarkerMap', () => {
  it('prosljeđuje koordinate klika kroz onChange', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <DraggableMarkerMap
        value={null}
        onChange={onChange}
        center={{ latitude: 45.1, longitude: 15.2 }}
      />,
    )

    await user.click(
      await screen.findByRole('button', { name: 'Simuliraj klik na kartu' }),
    )

    expect(onChange).toHaveBeenCalledWith({
      latitude: 45.123457,
      longitude: 15.987654,
    })
  })

  it('postavlja oznaku u trenutačno središte karte zaokruženo na šest decimala', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <DraggableMarkerMap
        value={null}
        onChange={onChange}
        center={{ latitude: 45.1, longitude: 15.2 }}
      />,
    )

    await user.click(
      await screen.findByRole('button', { name: 'Simuliraj pomak karte' }),
    )
    await user.click(
      screen.getByRole('button', { name: hr.map.setCenterMarker }),
    )

    expect(onChange).toHaveBeenCalledWith({
      latitude: 45.123457,
      longitude: 15.987654,
    })
  })

  it('koristi ažurirani center prop nakon promjene lokacije obrasca', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    const { rerender } = render(
      <DraggableMarkerMap
        value={null}
        onChange={onChange}
        center={{ latitude: 45.1, longitude: 15.2 }}
      />,
    )

    rerender(
      <DraggableMarkerMap
        value={null}
        onChange={onChange}
        center={{ latitude: 46.1234567, longitude: 16.7654321 }}
      />,
    )

    await user.click(
      screen.getByRole('button', { name: hr.map.setCenterMarker }),
    )

    expect(onChange).toHaveBeenCalledWith({
      latitude: 46.123457,
      longitude: 16.765432,
    })
  })
})
