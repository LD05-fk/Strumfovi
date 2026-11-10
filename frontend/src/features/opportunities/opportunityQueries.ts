import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import { toApiQuery } from './opportunitySearchParams'
import type {
  OpportunitySearchParams,
  OpportunitySummaryPage,
} from './types'

import type {
  CatalogOption,
  OpportunityDetail,
  OpportunityInput,
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
