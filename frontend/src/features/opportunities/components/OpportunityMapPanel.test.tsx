import { screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { hr } from '../../../i18n/hr'
import { renderWithProviders } from '../../../test/renderWithProviders'
import type { OpportunitySummary } from '../types'
import { OpportunityMapPanel } from './OpportunityMapPanel'

vi.mock('./map', async () => {
  const React = await import('react')

  return {
    MapView: (props: {
      markers: { id: string; title: string; popup?: React.ReactNode }[]
      ariaLabel?: string
    }) =>
      React.createElement(
        'div',
        { 'data-testid': 'karta', 'aria-label': props.ariaLabel },
        props.markers.map((marker) =>
          React.createElement(
            'div',
            { key: marker.id, 'data-testid': 'oznaka' },
            marker.title,
            marker.popup,
          ),
        ),
      ),
  }
})

const texts = hr.opportunities.search.map

function opportunity(
  id: string,
  coordinates: { latitude: number; longitude: number } | null,
): OpportunitySummary {
  return {
    id,
    title: `Prilika ${id}`,
    type: { id: 'type-1', code: 'contest', name: 'Natječaj' },
    artFields: [{ id: 'field-1', code: 'painting', name: 'Slikarstvo' }],
    deadline: '2026-11-30T23:59:00+01:00',
    status: 'OPEN',
    locationType: coordinates === null ? 'ONLINE' : 'PHYSICAL',
    city: coordinates === null ? null : 'Zagreb',
    latitude: coordinates?.latitude ?? null,
    longitude: coordinates?.longitude ?? null,
    eventStart: null,
    organizer: {
      id: 'organizer-1',
      displayName: 'Ana Organizator',
      organizationName: 'Galerija Gradec',
    },
  }
}

const zagreb = { latitude: 45.815, longitude: 15.9819 }
const rijeka = { latitude: 45.3271, longitude: 14.4422 }

describe('OpportunityMapPanel', () => {
  it('crta oznake samo za prilike s koordinatama, a kartice za sve', () => {
    const opportunities = [
      opportunity('1', zagreb),
      opportunity('2', null),
      opportunity('3', rijeka),
    ]

    renderWithProviders(
      <OpportunityMapPanel opportunities={opportunities} total={3} />,
    )

    expect(screen.getAllByTestId('oznaka')).toHaveLength(2)
    expect(screen.getByTestId('karta')).toHaveAttribute(
      'aria-label',
      texts.ariaLabel,
    )
    expect(screen.getAllByRole('heading', { name: /^Prilika/ })).toHaveLength(3)
  })

  it('javlja koliko prilika nema lokaciju', () => {
    renderWithProviders(
      <OpportunityMapPanel
        opportunities={[opportunity('1', zagreb), opportunity('2', null)]}
        total={2}
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent(
      texts.missingLocation(1),
    )
  })

  it('javlja da je prikazan samo dio rezultata', () => {
    renderWithProviders(
      <OpportunityMapPanel
        opportunities={[opportunity('1', zagreb)]}
        total={5}
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent(texts.truncated(1))
  })

  it('bez obavijesti kad su sve prilike na karti', () => {
    renderWithProviders(
      <OpportunityMapPanel
        opportunities={[opportunity('1', zagreb), opportunity('2', rijeka)]}
        total={2}
      />,
    )

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('skočni opis vodi na detalj prilike', () => {
    renderWithProviders(
      <OpportunityMapPanel
        opportunities={[opportunity('1', zagreb)]}
        total={1}
      />,
    )

    const marker = screen.getByTestId('oznaka')

    expect(
      within(marker).getByRole('link', {
        name: hr.opportunities.search.card.detailsAriaLabel('Prilika 1'),
      }),
    ).toHaveAttribute('href', '/opportunities/1')
  })
})
