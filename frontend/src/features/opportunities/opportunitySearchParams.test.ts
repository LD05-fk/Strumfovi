import { describe, expect, it } from 'vitest'

import {
  hasActiveFilters,
  parseSearchParams,
  toApiQuery,
  toSearchQuery,
} from './opportunitySearchParams'
import {
  DEFAULT_OPPORTUNITY_SEARCH_PARAMS,
  type OpportunitySearchParams,
} from './types'

const artField = '3f1c2a8e-6b1d-4c7e-9a52-1d0e7b4c9f10'
const otherArtField = '3f1c2a8e-6b1d-4c7e-9a52-1d0e7b4c9f11'
const type = 'a1b2c3d4-0001-4e5f-8a9b-000000000001'

// Potpuna pretraga: svaki parametar ima vrijednost različitu od zadane.
const fullSearch: OpportunitySearchParams = {
  q: 'mural',
  artFieldIds: [artField, otherArtField],
  typeIds: [type],
  city: 'Zagreb',
  locationType: 'PHYSICAL',
  statuses: ['OPEN', 'CLOSED'],
  deadlineFrom: '2026-11-01',
  deadlineTo: '2026-12-31',
  sort: '-publishedAt',
  page: 3,
  view: 'map',
}

describe('parseSearchParams', () => {
  it('prazan upit daje zadane vrijednosti', () => {
    expect(parseSearchParams(new URLSearchParams())).toEqual(
      DEFAULT_OPPORTUNITY_SEARCH_PARAMS,
    )
  })

  it('čita sve parametre, uključujući ponovljene', () => {
    const query = new URLSearchParams(
      `q=mural&artFieldIds=${artField}&artFieldIds=${otherArtField}` +
        `&typeIds=${type}&city=Zagreb&locationType=PHYSICAL` +
        '&statuses=OPEN&statuses=CLOSED' +
        '&deadlineFrom=2026-11-01&deadlineTo=2026-12-31' +
        '&sort=-publishedAt&page=3&view=map',
    )

    expect(parseSearchParams(query)).toEqual(fullSearch)
  })

  it('neispravne vrijednosti padaju na zadane umjesto da ruše stranicu', () => {
    const query = new URLSearchParams(
      'q=%20%20mural%20%20&artFieldIds=nije-uuid&typeIds=' +
        '&city=%20Split%20&locationType=MARS&statuses=NEPOSTOJECI' +
        '&deadlineFrom=32.12.2026.&deadlineTo=&sort=cijena&page=-2&view=globus',
    )

    expect(parseSearchParams(query)).toEqual({
      ...DEFAULT_OPPORTUNITY_SEARCH_PARAMS,
      q: 'mural',
      city: 'Split',
    })
  })

  it('izbacuje ponovljene šifrarnike i statuse', () => {
    const query = new URLSearchParams(
      `artFieldIds=${artField}&artFieldIds=${artField}` +
        '&statuses=OPEN&statuses=OPEN',
    )
    const params = parseSearchParams(query)

    expect(params.artFieldIds).toEqual([artField])
    expect(params.statuses).toEqual(['OPEN'])
  })
})

describe('toSearchQuery', () => {
  it('izostavlja zadane vrijednosti', () => {
    const query = toSearchQuery(DEFAULT_OPPORTUNITY_SEARCH_PARAMS)

    expect(query.toString()).toBe('')
  })

  it('zapis pa čitanje vraća isto stanje', () => {
    expect(parseSearchParams(toSearchQuery(fullSearch))).toEqual(fullSearch)
  })

  it('zapis pa čitanje vraća isto stanje i za djelomičnu pretragu', () => {
    const params: OpportunitySearchParams = {
      ...DEFAULT_OPPORTUNITY_SEARCH_PARAMS,
      city: 'Rijeka',
      statuses: ['FINISHED'],
    }

    expect(parseSearchParams(toSearchQuery(params))).toEqual(params)
  })
})

describe('toApiQuery', () => {
  it('popis traži 20 rezultata i zadržava stranicu', () => {
    const query = toApiQuery({ ...fullSearch, view: 'list' })

    expect(query.get('pageSize')).toBe('20')
    expect(query.get('page')).toBe('3')
    expect(query.get('view')).toBeNull()
  })

  it('karta traži 100 rezultata i zanemaruje stranicu', () => {
    const query = toApiQuery(fullSearch)

    expect(query.get('pageSize')).toBe('100')
    expect(query.get('page')).toBeNull()
    expect(query.get('view')).toBeNull()
  })
})

describe('hasActiveFilters', () => {
  it('zadano stanje nema aktivnih filtara', () => {
    expect(hasActiveFilters(DEFAULT_OPPORTUNITY_SEARCH_PARAMS)).toBe(false)
  })

  it('sortiranje, stranica i prikaz nisu filtri', () => {
    const params: OpportunitySearchParams = {
      ...DEFAULT_OPPORTUNITY_SEARCH_PARAMS,
      sort: '-deadline',
      page: 2,
      view: 'map',
    }

    expect(hasActiveFilters(params)).toBe(false)
  })

  it('prepoznaje postavljen filtar', () => {
    expect(
      hasActiveFilters({
        ...DEFAULT_OPPORTUNITY_SEARCH_PARAMS,
        city: 'Zagreb',
      }),
    ).toBe(true)
  })
})
