import { z } from 'zod'
import type { OpportunitySearchParams } from './types.ts'
import {
  SEARCHABLE_OPPORTUNITY_STATUSES,
  DEFAULT_OPPORTUNITY_SEARCH_PARAMS,
  OPPORTUNITY_SORT_VALUES,
  OPPORTUNITY_LOCATION_TYPES,
  OPPORTUNITY_VIEWS,
} from './types.ts'

const uuidSchema = z.uuid()
const dateSchema = z.iso.date()
const MAP_PAGE_SIZE = 100
const LIST_PAGE_SIZE = 20

function unique<T>(values: T[]): T[] {
  return [...new Set(values)]
}

function parseUuidList(query: URLSearchParams, key: string): string[] {
  return unique(
    query.getAll(key).filter((id) => uuidSchema.safeParse(id).success)
  )
}

function pickOption<T extends string>(value: string | null, allowed: readonly T[]): T | null {
  return allowed.includes(value as T) ? (value as T) : null
}

function parseDate(value: string | null): string | null {
  return value !== null && dateSchema.safeParse(value).success ? value : null
}

function parsePage(value: string | null): number {
  const page = Number(value)

  return Number.isInteger(page) && page >= 1 ? page : DEFAULT_OPPORTUNITY_SEARCH_PARAMS.page
}

export function parseSearchParams(query: URLSearchParams): OpportunitySearchParams {
  const defaults = DEFAULT_OPPORTUNITY_SEARCH_PARAMS
  const statuses = unique(query
    .getAll('statuses')
    .filter((status) =>
      SEARCHABLE_OPPORTUNITY_STATUSES.includes(status as (typeof SEARCHABLE_OPPORTUNITY_STATUSES)[number]))
    ) as OpportunitySearchParams['statuses']

  return {
    q: query.get('q')?.trim() ?? defaults.q,
    artFieldIds: parseUuidList(query, 'artFieldIds'),
    typeIds: parseUuidList(query, 'typeIds'),
    city: query.get('city')?.trim() ?? defaults.city,
    locationType: pickOption(
      query.get('locationType'),
      OPPORTUNITY_LOCATION_TYPES,
    ),
    statuses: statuses.length > 0 ? statuses : [...defaults.statuses],
    deadlineFrom: parseDate(query.get('deadlineFrom')),
    deadlineTo: parseDate(query.get('deadlineTo')),
    sort:
      pickOption(query.get('sort'), OPPORTUNITY_SORT_VALUES) ?? defaults.sort,
    page: parsePage(query.get('page')),
    view: pickOption(query.get('view'), OPPORTUNITY_VIEWS) ?? defaults.view,
  }
}

/** Zadani status (`OPEN`) se ne upisuje u URL jer je zadan i na API-ju. */
function isDefaultStatuses(statuses: OpportunitySearchParams['statuses']): boolean {
  return statuses.length === 1 && statuses[0] === 'OPEN'
}

export function toSearchQuery(params: OpportunitySearchParams): URLSearchParams {
  const defaults = DEFAULT_OPPORTUNITY_SEARCH_PARAMS
  const query = new URLSearchParams()

  if (params.q !== '') query.set('q', params.q)
  params.artFieldIds.forEach((id) => query.append('artFieldIds', id))
  params.typeIds.forEach((id) => query.append('typeIds', id))
  if (params.city !== '') query.set('city', params.city)
  if (params.locationType !== null) query.set('locationType', params.locationType)
  if (!isDefaultStatuses(params.statuses)) {
    params.statuses.forEach((status) => query.append('statuses', status))
  }
  if (params.deadlineFrom !== null) query.set('deadlineFrom', params.deadlineFrom)
  if (params.deadlineTo !== null) query.set('deadlineTo', params.deadlineTo)
  if (params.sort !== defaults.sort) query.set('sort', params.sort)
  if (params.page !== defaults.page) query.set('page', String(params.page))
  if (params.view !== defaults.view) query.set('view', params.view)

  return query
}

export function toApiQuery(params: OpportunitySearchParams): URLSearchParams {
  const query = toSearchQuery(params)

  query.delete('view')
  const pageSize = params.view === 'map' ? MAP_PAGE_SIZE : LIST_PAGE_SIZE
  query.set('pageSize', String(pageSize))

  return query
}
