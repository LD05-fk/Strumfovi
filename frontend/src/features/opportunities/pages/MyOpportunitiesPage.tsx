import {
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Stack,
  Tab,
  Tabs,
  Typography,
} from '@mui/material'
import { useSearchParams, Link as RouterLink } from 'react-router'
import { dayjs, OPPORTUNITY_TIME_ZONE } from '../../../app/dayjs'
import { ApiError } from '../../../api/client'
import { usePageTitle } from '../../../components/layout/usePageTitle'
import { EmptyState } from '../../../components/state/EmptyState'
import { ErrorState } from '../../../components/state/ErrorState'
import { LoadingState } from '../../../components/state/LoadingState'
import { StatusChip } from '../../../components/state/StatusChip'
import { hr } from '../../../i18n/hr'
import { OwnerActionsBar } from '../components/ownerActionsBar'
import { useMyOpportunities } from '../opportunityQueries'
import type { OpportunityStatus } from '../types'

const statuses: OpportunityStatus[] = [
  'PREPARATION',
  'OPEN',
  'CLOSED',
  'SELECTION',
  'FINISHED',
]

function readPage(value: string | null): number {
  const page = Number(value)
  return Number.isSafeInteger(page) && page > 0 ? page : 1
}

function readStatus(value: string | null): OpportunityStatus | undefined {
  return statuses.find((status) => status === value)
}

function formatDeadline(value: string): string {
  return dayjs(value).tz(OPPORTUNITY_TIME_ZONE).format('DD.MM.YYYY.')
}

export function MyOpportunitiesPage() {
  usePageTitle(hr.opportunities.my.title)

  const [searchParams, setSearchParams] = useSearchParams()
  const archived = searchParams.get('archived') === 'true'
  const page = readPage(searchParams.get('page'))
  const status = readStatus(searchParams.get('status'))
  const opportunities = useMyOpportunities({ archived, status, page })

  function updateParams(updates: Record<string, string | null>) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)

      for (const [key, value] of Object.entries(updates)) {
        if (value === null) next.delete(key)
        else next.set(key, value)
      }

      return next
    })
  }

  const pageCount = opportunities.data
    ? Math.ceil(
        opportunities.data.meta.total / opportunities.data.meta.pageSize,
      )
    : 0

  return (
    <Stack spacing={2} sx={{ maxWidth: 1200, mx: 'auto' }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
      >
        <Typography component="h1" variant="h1">
          {hr.opportunities.my.title}
        </Typography>
        <Button
          component={RouterLink}
          to="/organizer/opportunities/new"
          variant="contained"
        >
          {hr.opportunities.my.newOpportunity}
        </Button>
      </Stack>

      <Tabs
        value={archived ? 'archived' : 'active'}
        aria-label={hr.opportunities.my.title}
        onChange={(_, value: 'active' | 'archived') =>
          updateParams({
            archived: value === 'archived' ? 'true' : null,
            page: null,
          })
        }
      >
        <Tab value="active" label={hr.opportunities.my.active} />
        <Tab value="archived" label={hr.opportunities.my.archived} />
      </Tabs>

      <FormControl sx={{ maxWidth: { sm: 280 } }}>
        <InputLabel id="opportunity-status-filter-label">
          {hr.opportunities.my.statusFilter}
        </InputLabel>
        <Select
          labelId="opportunity-status-filter-label"
          label={hr.opportunities.my.statusFilter}
          value={status ?? ''}
          onChange={(event) =>
            updateParams({
              status: event.target.value || null,
              page: null,
            })
          }
        >
          <MenuItem value="">{hr.opportunities.my.allStatuses}</MenuItem>
          {statuses.map((item) => (
            <MenuItem key={item} value={item}>
              {hr.opportunityStatus[item]}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      {opportunities.isPending ? (
        <LoadingState lines={5} />
      ) : opportunities.isError ? (
        <ErrorState
          error={
            opportunities.error instanceof ApiError
              ? opportunities.error
              : undefined
          }
          onRetry={() => void opportunities.refetch()}
        />
      ) : opportunities.data.data.length === 0 ? (
        <EmptyState
          title={hr.opportunities.my.emptyTitle}
          description={hr.opportunities.my.emptyDescription}
          action={
            <Button
              component={RouterLink}
              to="/organizer/opportunities/new"
              variant="contained"
            >
              {hr.opportunities.my.newOpportunity}
            </Button>
          }
        />
      ) : (
        <>
          <Box
            component="ul"
            sx={{
              listStyle: 'none',
              p: 0,
              m: 0,
              display: 'grid',
              gap: 1.5,
            }}
          >
            {opportunities.data.data.map((item) => (
              <Card component="li" key={item.id}>
                <CardContent
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: {
                      xs: 'minmax(0, 1fr) auto',
                      sm: 'minmax(0, 2fr) repeat(4, minmax(0, 1fr)) auto',
                    },
                    gap: 2,
                    alignItems: 'center',
                  }}
                >
                  <Stack spacing={0.5} sx={{ minWidth: 0 }}>
                    <Button
                      component={RouterLink}
                      to={`/opportunities/${item.id}`}
                      variant="text"
                      sx={{
                        justifyContent: 'flex-start',
                        textAlign: 'left',
                        p: 0,
                        minWidth: 0,
                      }}
                    >
                      {item.title}
                    </Button>
                    {item.hiddenByAdmin && (
                      <Typography variant="caption" color="error">
                        {hr.opportunities.my.hiddenByAdmin}
                      </Typography>
                    )}
                  </Stack>

                  <Stack spacing={0.5}>
                    <Typography variant="caption" color="text.secondary">
                      {hr.opportunities.my.statusFilter}
                    </Typography>
                    <StatusChip type="opportunity" status={item.status} />
                  </Stack>

                  <Stack spacing={0.5}>
                    <Typography variant="caption" color="text.secondary">
                      {hr.opportunities.my.deadline}
                    </Typography>
                    <Typography component="time" dateTime={item.deadline}>
                      {formatDeadline(item.deadline)}
                    </Typography>
                  </Stack>

                  <Stack spacing={0.5}>
                    <Typography variant="caption" color="text.secondary">
                      {hr.opportunities.my.type}
                    </Typography>
                    <Typography>{item.type.name}</Typography>
                  </Stack>

                  <Stack spacing={0.5}>
                    <Typography variant="caption" color="text.secondary">
                      {hr.opportunities.my.applications}
                    </Typography>
                    <Typography>{item.applicationCounts.total}</Typography>
                  </Stack>

                  <OwnerActionsBar
                    opportunityId={item.id}
                    title={item.title}
                    status={item.status}
                    archived={item.archived}
                    deadline={item.deadline}
                  />
                </CardContent>
              </Card>
            ))}
          </Box>

          {pageCount > 1 && (
            <Stack sx={{ alignItems: 'center' }}>
              <Pagination
                count={pageCount}
                page={page}
                onChange={(_, nextPage) =>
                  updateParams({ page: String(nextPage) })
                }
                aria-label={hr.opportunities.my.pagination}
              />
            </Stack>
          )}
        </>
      )}
    </Stack>
  )
}
