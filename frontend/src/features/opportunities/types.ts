import { dayjs, OPPORTUNITY_TIME_ZONE } from '../../app/dayjs'

export type OpportunityLocationType = 'PHYSICAL' | 'ONLINE'

export const OPPORTUNITY_LOCATION_TYPES =
  ['PHYSICAL', 'ONLINE'] as const satisfies readonly OpportunityLocationType[]

export type OpportunityStatus =
  | 'PREPARATION'
  | 'OPEN'
  | 'CLOSED'
  | 'SELECTION'
  | 'FINISHED'

export type CatalogReference = {
  id: string
  code: string
  name: string
}

export type CatalogOption = CatalogReference & {
  active: boolean
  position: number
}

export type OpportunityFormValues = {
  title: string
  description: string
  typeId: string
  artFieldIds: string[]
  deadline: string
  criteria: string
  prize: string
  eventStart: string
  eventEnd: string
  location: {
    type: OpportunityLocationType
    streetAddress: string
    city: string
    postalCode: string
    latitude: string
    longitude: string
    onlineUrl: string
  }
}

export type OpportunityInput = {
  title: string
  description: string
  typeId: string
  artFieldIds: string[]
  deadline: string
  criteria: string | null
  prize: string | null
  location: {
    type: OpportunityLocationType
    streetAddress: string | null
    city: string | null
    postalCode: string | null
    latitude: number | null
    longitude: number | null
    onlineUrl: string | null
  }
  eventStart: string | null
  eventEnd: string | null
}

export function toOpportunityInput(
  values: OpportunityFormValues,
): OpportunityInput {
  const location: OpportunityInput['location'] =
    values.location.type === 'PHYSICAL'
      ? {
          type: 'PHYSICAL',
          streetAddress: values.location.streetAddress.trim(),
          city: values.location.city.trim(),
          postalCode: values.location.postalCode.trim() || null,
          latitude: Number(values.location.latitude),
          longitude: Number(values.location.longitude),
          onlineUrl: null,
        }
      : {
          type: 'ONLINE',
          streetAddress: null,
          city: null,
          postalCode: null,
          latitude: null,
          longitude: null,
          onlineUrl: values.location.onlineUrl.trim(),
        }

  return {
    title: values.title.trim(),
    description: values.description.trim(),
    typeId: values.typeId,
    artFieldIds: [...values.artFieldIds],
    deadline: values.deadline,
    criteria: values.criteria.trim() || null,
    prize: values.prize.trim() || null,
    location,
    eventStart: values.eventStart || null,
    eventEnd: values.eventEnd || null,
  }
}

export type OpportunityDetail = {
  id: string
  title: string
  description: string
  type: CatalogReference
  artFields: CatalogReference[]
  deadline: string
  criteria: string | null
  prize: string | null
  location: {
    type: OpportunityLocationType
    streetAddress: string | null
    city: string | null
    postalCode: string | null
    latitude: number | null
    longitude: number | null
    onlineUrl: string | null
  }
  eventStart: string | null
  eventEnd: string | null
  status: OpportunityStatus
  archived: boolean
  hiddenByAdmin: boolean
  organizer: {
    id: string
    displayName: string
    organizationName: string | null
  }
  publishedAt: string | null
  createdAt: string
  updatedAt: string
  hasResults: boolean
  myApplicationId: string | null
}

function toZagrebDateTime(value: string | null): string {
  if (value === null) return ''

  const date = dayjs(value)
  if (!date.isValid()) {
    throw new Error(`Invalid opportunity date received from API: ${value}`)
  }

  return date
    .tz(OPPORTUNITY_TIME_ZONE)
    .format('YYYY-MM-DDTHH:mm:ssZ')
}

export function toOpportunityFormValues(
  opportunity: OpportunityDetail,
): OpportunityFormValues {
  return {
    title: opportunity.title,
    description: opportunity.description,
    typeId: opportunity.type.id,
    artFieldIds: opportunity.artFields.map((field) => field.id),
    deadline: toZagrebDateTime(opportunity.deadline),
    criteria: opportunity.criteria ?? '',
    prize: opportunity.prize ?? '',
    eventStart: toZagrebDateTime(opportunity.eventStart),
    eventEnd: toZagrebDateTime(opportunity.eventEnd),
    location: {
      type: opportunity.location.type,
      streetAddress: opportunity.location.streetAddress ?? '',
      city: opportunity.location.city ?? '',
      postalCode: opportunity.location.postalCode ?? '',
      latitude:
        opportunity.location.latitude === null
          ? ''
          : String(opportunity.location.latitude),
      longitude:
        opportunity.location.longitude === null
          ? ''
          : String(opportunity.location.longitude),
      onlineUrl: opportunity.location.onlineUrl ?? '',
    },
  }
}

export function emptyOpportunityFormValues(): OpportunityFormValues {
  return {
    title: '',
    description: '',
    typeId: '',
    artFieldIds: [],
    deadline: '',
    criteria: '',
    prize: '',
    eventStart: '',
    eventEnd: '',
    location: {
      type: 'PHYSICAL',
      streetAddress: '',
      city: '',
      postalCode: '',
      latitude: '',
      longitude: '',
      onlineUrl: '',
    },
  }
}

export const SEARCHABLE_OPPORTUNITY_STATUSES = [
  'OPEN',
  'CLOSED',
  'SELECTION',
  'FINISHED',
] as const satisfies readonly OpportunityStatus[]

export type SearchableOpportunityStatus = (typeof SEARCHABLE_OPPORTUNITY_STATUSES)[number]

export const OPPORTUNITY_SORT_VALUES = [
  'deadline',
  '-deadline',
  'publishedAt',
  '-publishedAt',
] as const

export type OpportunitySort = (typeof OPPORTUNITY_SORT_VALUES)[number]

export const OPPORTUNITY_VIEWS = ['list', 'map'] as const

export type OpportunityView = (typeof OPPORTUNITY_VIEWS)[number]

export type OpportunitySearchParams = {
  q: string
  artFieldIds: string[]
  typeIds: string[]
  city: string
  locationType: OpportunityLocationType | null
  statuses: SearchableOpportunityStatus[]
  deadlineFrom: string | null
  deadlineTo: string | null
  sort: OpportunitySort
  page: number
  view: OpportunityView
}

export const DEFAULT_OPPORTUNITY_SEARCH_PARAMS: OpportunitySearchParams = {
  q: '',
  artFieldIds: [],
  typeIds: [],
  city: '',
  locationType: null,
  statuses: ['OPEN'],
  deadlineFrom: null,
  deadlineTo: null,
  sort: 'deadline',
  page: 1,
  view: 'list',
}

export type OpportunitySummary = {
  id: string
  title: string
  type: CatalogReference
  artFields: CatalogReference[]
  deadline: string
  status: OpportunityStatus
  locationType: OpportunityLocationType
  city: string | null
  latitude: number | null
  longitude: number | null
  eventStart: string | null
  organizer: {
    id: string
    displayName: string
    organizationName: string | null
  }
}

export type OpportunitySummaryPage = {
  data: OpportunitySummary[]
  meta: {
    page: number
    pageSize: number
    total: number
  }
}

export type MyOpportunitySummary = OpportunitySummary & {
  archived: boolean
  hiddenByAdmin: boolean
  applicationCounts: {
    total: number
    submitted: number
    underReview: number
    needsInfo: number
    accepted: number
    rejected: number
  }
}

export type MyOpportunityPage = {
  data: MyOpportunitySummary[]
  meta: {
    page: number
    pageSize: number
    total: number
  }
}
