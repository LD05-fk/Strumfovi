import { Stack, Typography } from '@mui/material'
import { useSearchParams } from 'react-router'

import { usePageTitle} from '../../../components/layout/usePageTitle'
import { hr } from "../../../i18n/hr"
import { useOpportunitySearch } from '../opportunityQueries'
import {
  parseSearchParams,
  toSearchQuery,
} from '../opportunitySearchParams'
import type { OpportunitySearchParams } from '../types'
import { LoadingState } from '../../../components/state/LoadingState'
import { ErrorState } from '../../../components/state/ErrorState'
import { ApiError } from '../../../api/client'
import { EmptyState } from '../../../components/state/EmptyState.tsx'
import { StatusChip } from '../../../components/state/StatusChip.tsx'

export function OpportunitySearchPage() {
  usePageTitle(hr.opportunities.pageTitle)

  const [searchParams, setSearchParams] = useSearchParams()
  const params = parseSearchParams(searchParams)
  const search = useOpportunitySearch(params)

  function setParams(change: Partial<OpportunitySearchParams>) {
    const keepsPage = 'page' in change
    const next: OpportunitySearchParams = {
      ...params,
      ...change,
      page: keepsPage ? (change.page ?? params.page) : 1,
    }

    setSearchParams(toSearchQuery(next))
  }

  function renderBody() {
    if (search.isPending) return <LoadingState />

    if (search.isError) {
      // TODO: ApiError sa statusom 400 ide kao Alert uz filtre, ne ovdje.
      return (
        <ErrorState
          error={search.error instanceof ApiError ? search.error : undefined}
          onRetry={() => void search.refetch()}
        />
      )
    }

    if (search.data.meta.total === 0) {
      // TODO: akciju „Očisti filtre” prikazati samo kad hasActiveFilters(params).
      return (
        <EmptyState
          title={hr.opportunities.search.emptyTitle}
          description={hr.opportunities.search.emptyDescription}
        />
      )
    }

    // TODO: zamijeniti gridom OpportunityCard komponenti (view=list) i MapView-om s markerima (view=map).
    return (
      <Stack component="ul" spacing={1} sx={{ listStyle: 'none', p: 0 }}>
        {search.data.data.map((opportunity) => (
          <Stack
            key={opportunity.id}
            component="li"
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center' }}
          >
            <StatusChip type="opportunity" status={opportunity.status} />
            <Typography>{opportunity.title}</Typography>
            <Typography color="text.secondary">
              {opportunity.deadline}
            </Typography>
          </Stack>
        ))}
      </Stack>
    )
  }

  return (
    <Stack spacing={2}>
      <Typography component="h1" variant="h1">
        {hr.opportunities.pageTitle}
      </Typography>

      {/* TODO: ListMapToggle desno u zaglavlju, FilterPanel ispod (Drawer na xs),
          Select za sortiranje, Pagination pod rezultatima (pageCount). */}

      {search.data !== undefined && (
        <Typography color="text.secondary">
          {hr.opportunities.search.resultsCount(search.data.meta.total)}
        </Typography>
      )}

      {/* Privremeno, za provjeru URL-a i povijesti */}
      <Stack direction="row" spacing={1}>
        <button
          type="button"
          onClick={() => setParams({ sort: '-publishedAt' })}
        >
          Sortiraj po objavi
        </button>
        <button
          type="button"
          onClick={() => setParams({ page: params.page + 1 })}
        >
          Sljedeća stranica
        </button>
        <button
          type="button"
          onClick={() => setParams({ statuses: ['CLOSED'] })}
        >
          Samo zatvorene
        </button>
      </Stack>

      {renderBody()}
    </Stack>
  )
}
