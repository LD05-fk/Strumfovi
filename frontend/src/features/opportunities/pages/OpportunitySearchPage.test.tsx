import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError, apiFetch, type ApiFetchOptions } from '../../../api/client'
import { hr } from '../../../i18n/hr'
import { renderWithProviders } from '../../../test/renderWithProviders'
import type { OpportunitySummary, OpportunitySummaryPage } from '../types'
import { OpportunitySearchPage } from './OpportunitySearchPage'

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>()

  return { ...actual, apiFetch: vi.fn() }
})

const texts = hr.opportunities.search

function opportunity(id: string): OpportunitySummary {
  return {
    id,
    title: `Prilika ${id}`,
    type: { id: 'type-1', code: 'contest', name: 'Natječaj' },
    artFields: [{ id: 'field-1', code: 'painting', name: 'Slikarstvo' }],
    deadline: '2026-11-30T23:59:00+01:00',
    status: 'OPEN',
    locationType: 'PHYSICAL',
    city: 'Zagreb',
    latitude: 45.815,
    longitude: 15.9819,
    eventStart: null,
    organizer: {
      id: 'organizer-1',
      displayName: 'Ana Organizator',
      organizationName: 'Galerija Gradec',
    },
  }
}

const catalog = {
  data: [
    {
      id: 'field-1',
      code: 'painting',
      name: 'Slikarstvo',
      active: true,
      position: 1,
    },
  ],
  meta: { page: 1, pageSize: 100, total: 1 },
}

let searchResponse: OpportunitySummaryPage
let searchError: unknown = null
// Upiti koje je stranica poslala
let searchQueries: string[] = []

async function mockApiFetch<T>(path: string, _options?: ApiFetchOptions) {
  if (path.startsWith('/art-fields') || path.startsWith('/opportunity-types')) {
    return catalog as T
  }

  if (path.startsWith('/opportunities?')) {
    searchQueries.push(path.slice('/opportunities?'.length))
    if (searchError) throw searchError

    return searchResponse as T
  }

  throw new Error(`Neočekivani zahtjev: ${path}`)
}

function renderPage(route = '/opportunities') {
  vi.mocked(apiFetch).mockImplementation(mockApiFetch)

  return renderWithProviders(<OpportunitySearchPage />, { route })
}

function filterBar() {
  return within(screen.getByRole('region', { name: texts.filters.title }))
}

function emptyState(title: string) {
  const heading = screen.getByRole('heading', { name: title })

  return within(heading.parentElement as HTMLElement)
}

beforeEach(() => {
  searchResponse = {
    data: [opportunity('1'), opportunity('2')],
    meta: { page: 1, pageSize: 20, total: 2 },
  }
  searchError = null
  searchQueries = []
})

afterEach(() => {
  vi.clearAllMocks()
})

describe('OpportunitySearchPage', () => {
  it('prikazuje rezultate i šalje filtre iz URL-a', async () => {
    renderPage('/opportunities?city=Zagreb&statuses=CLOSED&sort=-deadline')

    expect(await screen.findByText(texts.resultsCount(2))).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Prilika 1' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Prilika 2' }),
    ).toBeInTheDocument()

    const query = new URLSearchParams(searchQueries[0])
    expect(query.get('city')).toBe('Zagreb')
    expect(query.getAll('statuses')).toEqual(['CLOSED'])
    expect(query.get('sort')).toBe('-deadline')
    expect(query.get('pageSize')).toBe('20')
  })

  it('promjena filtra vraća pretragu na prvu stranicu', async () => {
    const user = userEvent.setup()
    const { router } = renderPage('/opportunities?page=3&city=Zagreb')

    await screen.findByText(texts.resultsCount(2))

    await user.click(filterBar().getByLabelText(texts.filters.locationType))
    await user.click(
      await screen.findByRole('option', {
        name: hr.opportunityLocationType.ONLINE,
      }),
    )

    await waitFor(() => {
      const query = new URLSearchParams(router.state.location.search)
      expect(query.get('locationType')).toBe('ONLINE')
      expect(query.get('page')).toBeNull()
      expect(query.get('city')).toBe('Zagreb')
    })
  })

  it('prazna pretraga bez filtara ne nudi čišćenje filtara', async () => {
    searchResponse = { data: [], meta: { page: 1, pageSize: 20, total: 0 } }
    renderPage()

    expect(
      await screen.findByRole('heading', { name: texts.emptyTitle }),
    ).toBeInTheDocument()
    expect(screen.getByText(texts.emptyDescription)).toBeInTheDocument()
    expect(
      emptyState(texts.emptyTitle).queryByRole('button', {
        name: texts.clearFilters,
      }),
    ).not.toBeInTheDocument()
  })

  it('prazna pretraga s filtrima nudi čišćenje koje zadržava sortiranje i prikaz', async () => {
    searchResponse = { data: [], meta: { page: 1, pageSize: 20, total: 0 } }
    const user = userEvent.setup()
    const { router } = renderPage(
      '/opportunities?city=Vis&sort=-deadline&view=map',
    )

    expect(
      await screen.findByRole('heading', { name: texts.emptyTitle }),
    ).toBeInTheDocument()

    await user.click(
      emptyState(texts.emptyTitle).getByRole('button', {
        name: texts.clearFilters,
      }),
    )

    await waitFor(() => {
      const query = new URLSearchParams(router.state.location.search)
      expect(query.get('city')).toBeNull()
      expect(query.get('sort')).toBe('-deadline')
      expect(query.get('view')).toBe('map')
    })
  })

  it('greška 400 ide uz filtre, bez ponovnog pokušaja', async () => {
    searchError = new ApiError(400, {
      code: 'VALIDATION_ERROR',
      message: 'Neispravna kombinacija filtara.',
    })
    renderPage('/opportunities?city=Zagreb')

    expect(
      await screen.findByText('Neispravna kombinacija filtara.'),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: hr.common.retry }),
    ).not.toBeInTheDocument()
  })

  it('ostale greške nude ponovni pokušaj', async () => {
    searchError = new ApiError(500, {
      code: 'INTERNAL_ERROR',
      message: 'Dogodila se neočekivana greška.',
    })
    renderPage()

    expect(
      await screen.findByRole('button', { name: hr.common.retry }),
    ).toBeInTheDocument()
  })

  it('straničenje upisuje stranicu u URL', async () => {
    searchResponse = {
      data: [opportunity('1')],
      meta: { page: 2, pageSize: 20, total: 40 },
    }
    const user = userEvent.setup()
    const { router } = renderPage('/opportunities')

    await screen.findByText(texts.resultsCount(40))

    const pagination = within(
      screen.getByRole('navigation', { name: texts.pagination }),
    )
    await user.click(pagination.getByRole('button', { name: /2/ }))

    await waitFor(() => {
      expect(
        new URLSearchParams(router.state.location.search).get('page'),
      ).toBe('2')
    })
  })
})

describe('OpportunitySearchPage – odgoda upisa', () => {
  it('ključna riječ se ne šalje dok korisnik tipka', async () => {
    const user = userEvent.setup()
    const { router } = renderPage()

    await screen.findByText(texts.resultsCount(2))

    await user.type(filterBar().getByLabelText(texts.filters.q), 'mural')

    // Odmah nakon tipkanja URL je netaknut i nije poslan upit s ključnom riječi.
    expect(router.state.location.search).toBe('')
    expect(searchQueries.some((query) => query.includes('q=mural'))).toBe(false)

    await waitFor(
      () => {
        expect(new URLSearchParams(router.state.location.search).get('q')).toBe(
          'mural',
        )
      },
      { timeout: 2000 },
    )

    // Jedan upit s ključnom riječi, a ne jedan po znaku.
    await waitFor(() => {
      expect(
        searchQueries.filter((query) => query.includes('q=mural')),
      ).toHaveLength(1)
    })
  })
})
