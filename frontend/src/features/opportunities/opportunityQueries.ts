import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import { toApiQuery } from './opportunitySearchParams'
import type {
  CatalogOption,
  MyOpportunityPage,
  OpportunityDetail,
  OpportunityInput,
  OpportunitySearchParams,
  OpportunityStatus,
  OpportunitySummaryPage,
} from './types'

type CatalogPage = {
  data: CatalogOption[]
  meta: {
    page: number
    pageSize: number
    total: number
  }
}

type SaveOpportunityVariables =
  | { kind: 'create'; input: OpportunityInput }
  | {
      kind: 'update'
      opportunityId: string
      input: OpportunityInput
    }

type UseOpportunitySearchOptions = {
  /** `false` dok klijentska validacija filtara ne prođe - zahtjev se ne šalje. */
  enabled?: boolean
}

export function useSaveOpportunity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (variables: SaveOpportunityVariables) => {
      if (variables.kind === 'create') {
        return apiFetch<OpportunityDetail>('/opportunities', {
          method: 'POST',
          json: variables.input,
        })
      }

      return apiFetch<OpportunityDetail>(
        `/opportunities/${variables.opportunityId}`,
        {
          method: 'PUT',
          json: variables.input,
        },
      )
    },
    onSuccess: async (opportunity) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: opportunityQueryKey(opportunity.id),
        }),
        queryClient.invalidateQueries({
          queryKey: ['my-opportunities'],
        }),
      ])
    },
  })
}

export const opportunityQueryKey = (id: string) => ['opportunity', id] as const

export function useOpportunity(id: string) {
  return useQuery({
    queryKey: opportunityQueryKey(id),
    queryFn: () => apiFetch<OpportunityDetail>(`/opportunities/${id}`),
    enabled: id.length > 0,
  })
}

export type MyOpportunitiesParams = {
  archived: boolean
  status?: OpportunityStatus
  page: number
}

export function useMyOpportunities({
  archived,
  status,
  page,
}: MyOpportunitiesParams) {
  const params = new URLSearchParams({
    archived: String(archived),
    page: String(page),
    pageSize: '20',
  })

  if (status) params.set('status', status)

  return useQuery({
    queryKey: ['my-opportunities', { archived, status, page }],
    queryFn: () =>
      apiFetch<MyOpportunityPage>(`/me/opportunities?${params.toString()}`),
    placeholderData: keepPreviousData,
  })
}

type OpportunityStatusTarget = Extract<
  OpportunityStatus,
  'OPEN' | 'CLOSED' | 'SELECTION'
>

async function invalidateOpportunityQueries(
  queryClient: ReturnType<typeof useQueryClient>,
  opportunityId: string,
) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['my-opportunities'] }),
    queryClient.invalidateQueries({
      queryKey: opportunityQueryKey(opportunityId),
    }),
  ])
}

export function useChangeOpportunityStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      opportunityId,
      status,
    }: {
      opportunityId: string
      status: OpportunityStatusTarget
    }) =>
      apiFetch<OpportunityDetail>(`/opportunities/${opportunityId}/status`, {
        method: 'POST',
        json: { status },
      }),
    onSuccess: (opportunity) =>
      invalidateOpportunityQueries(queryClient, opportunity.id),
  })
}

export function useArchiveOpportunity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (opportunityId: string) =>
      apiFetch<OpportunityDetail>(`/opportunities/${opportunityId}/archive`, {
        method: 'POST',
      }),
    onSuccess: (opportunity) =>
      invalidateOpportunityQueries(queryClient, opportunity.id),
  })
}

export function useDeleteOpportunity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (opportunityId: string) =>
      apiFetch<void>(`/opportunities/${opportunityId}`, {
        method: 'DELETE',
      }),
    onSuccess: (_result, opportunityId) =>
      invalidateOpportunityQueries(queryClient, opportunityId),
  })
}

export function useOpportunityTypes() {
  return useQuery({
    queryKey: ['opportunity-types', { pageSize: 100 }],
    queryFn: () => apiFetch<CatalogPage>('/opportunity-types?pageSize=100'),
  })
}

export function useOpportunitySearch(
  params: OpportunitySearchParams,
  { enabled = true }: UseOpportunitySearchOptions = {},
) {
  const query = toApiQuery(params).toString()

  return useQuery({
    queryKey: ['opportunities', 'search', query],
    queryFn: () => apiFetch<OpportunitySummaryPage>(`/opportunities?${query}`),
    placeholderData: keepPreviousData,
    enabled: enabled,
  })
}

export type GeocodingCandidate = {
  displayName: string
  latitude: number
  longitude: number
  city?: string | null
  postalCode?: string | null
}

type GeocodingResponse = {
  data: GeocodingCandidate[]
}

type GeocodingSearchInput = {
  street: string
  city: string
  postalCode: string
}

export function useSearchGeocoding() {
  return useMutation({
    mutationFn: ({ street, city, postalCode }: GeocodingSearchInput) => {
      const params = new URLSearchParams({ street, city })
      if (postalCode.trim()) params.set('postalCode', postalCode)

      return apiFetch<GeocodingResponse>(
        `/geocoding/search?${params.toString()}`,
      )
    },
  })
}
