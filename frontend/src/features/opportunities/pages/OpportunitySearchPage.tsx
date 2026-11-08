import { Box, Button, Pagination, Stack, Typography } from '@mui/material'
import { useSearchParams } from 'react-router'

import { usePageTitle } from '../../../components/layout/usePageTitle'
import { hr } from '../../../i18n/hr'
import { useOpportunitySearch } from '../opportunityQueries'
import {
  parseSearchParams,
  toSearchQuery,
  hasActiveFilters,
} from '../opportunitySearchParams'
import type { OpportunitySearchParams } from '../types'
import { LoadingState } from '../../../components/state/LoadingState'
import { ErrorState } from '../../../components/state/ErrorState'
import { ApiError } from '../../../api/client'
import { EmptyState } from '../../../components/state/EmptyState'
import { OpportunityCard } from '../components/OpportunityCard'
import { DEFAULT_OPPORTUNITY_SEARCH_PARAMS } from '../types'

export function OpportunitySearchPage() {
  usePageTitle(hr.opportunities.pageTitle)

  const [searchParams, setSearchParams] = useSearchParams()
  const params = parseSearchParams(searchParams)
  const activeFilters = hasActiveFilters(params)
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

  function clearFilters() {
    const { sort, view, ...defaultFilters } = DEFAULT_OPPORTUNITY_SEARCH_PARAMS
    setParams(defaultFilters)
  }

  function renderEmptyAction(outOfRange: boolean) {
    if (outOfRange) {
      return (
        <Button variant="contained" onClick={() => setParams({ page: 1 })}>
          {hr.opportunities.search.firstPage}
        </Button>
      )
    }

    if (!activeFilters) return undefined

    return (
      <Button
        variant="outlined"
        onClick={clearFilters}
        sx={{ bgcolor: 'background.paper' }}
      >
        {hr.opportunities.search.clearFilters}
      </Button>
    )
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

    const { data, meta } = search.data
    const pageCount = Math.max(1, Math.ceil(meta.total / meta.pageSize))

    if (data.length === 0) {
      const outOfRange = meta.total > 0

      return (
        <EmptyState
          title={
            outOfRange
              ? hr.opportunities.search.emptyPageTitle
              : hr.opportunities.search.emptyTitle
          }
          description={
            outOfRange ? undefined : hr.opportunities.search.emptyDescription
          }
          action={renderEmptyAction(outOfRange)}
        />
      )
    }

    // TODO: na view=map ide MapView s oznakama umjesto grida.
    return (
      <Stack spacing={3}>
        <Box
          component="ul"
          sx={{
            listStyle: 'none',
            m: 0,
            p: 0,
            display: 'grid',
            gap: 2,
            gridTemplateColumns: {
              xs: '1fr',
              sm: 'repeat(2, minmax(0, 1fr))',
              md: 'repeat(3, minmax(0, 1fr))',
            },
          }}
        >
          {data.map((opportunity) => (
            <OpportunityCard key={opportunity.id} opportunity={opportunity} />
          ))}
        </Box>

        {pageCount > 1 && (
          <Stack sx={{ alignItems: 'center' }}>
            <Pagination
              count={pageCount}
              page={params.page}
              onChange={(_event, value) => setParams({ page: value })}
              color="primary"
              aria-label={hr.opportunities.search.pagination}
            />
          </Stack>
        )}
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

      {/* TODO: gumb „Očisti filtre” preseliti u FilterPanel kad ta komponenta nastane. */}
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        <Typography color="text.secondary" sx={{ flexGrow: 1 }}>
          {search.data === undefined
            ? ''
            : hr.opportunities.search.resultsCount(search.data.meta.total)}
        </Typography>

        <Button
          variant="text"
          size="small"
          onClick={clearFilters}
          disabled={!activeFilters}
          sx={{ minHeight: { xs: 44, sm: 36 } }}
        >
          {hr.opportunities.search.clearFilters}
        </Button>
      </Stack>

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
          onClick={() => setParams({ statuses: ['CLOSED'] })}
        >
          Samo zatvorene
        </button>
      </Stack>

      {renderBody()}
    </Stack>
  )
}
