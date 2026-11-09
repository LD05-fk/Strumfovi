import {
  Alert,
  Box,
  Button,
  Checkbox,
  Drawer,
  ListItemText,
  MenuItem,
  Stack,
  TextField,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useEffect, useRef, useState } from 'react'

import type { ApiError } from '../../../api/client'
import { useArtFields } from '../../artist-profile/artistProfileQueries'
import { useOpportunityTypes } from '../opportunityQueries'
import {
  validateDeadlineRange,
  validateTextFilter,
  type TextFilterKey,
} from './opportunityFilterSchema'
import { hr } from '../../../i18n/hr'
import type {
  OpportunityLocationType,
  SearchableOpportunityStatus,
  OpportunitySearchParams,
} from '../types'
import {
  OPPORTUNITY_LOCATION_TYPES,
  SEARCHABLE_OPPORTUNITY_STATUSES,
} from '../types'

const filters = hr.opportunities.search.filters

// Tipkanje u `q` i `city` mijenja URL tek kad korisnik stane
const TEXT_DEBOUNCE_MS = 400

type CatalogFilterOption = {
  id: string
  name: string
  active: boolean
}

function toIds(options: CatalogFilterOption[], values: string[]): string[] {
  return options
    .filter((option) => values.includes(option.id))
    .map((option) => option.id)
}

function toLocationType(value: string): OpportunityLocationType | null {
  return OPPORTUNITY_LOCATION_TYPES.includes(value as OpportunityLocationType)
    ? (value as OpportunityLocationType)
    : null
}

function toStatuses(values: string[]): SearchableOpportunityStatus[] {
  return SEARCHABLE_OPPORTUNITY_STATUSES.filter((status) =>
    values.includes(status),
  )
}

type FilterPanelProps = {
  params: OpportunitySearchParams
  onChange: (change: Partial<OpportunitySearchParams>) => void
  onClear: () => void
  activeFilters: boolean
  resultCount: number | undefined
  error: ApiError | undefined
}

// Polja koja se drže u lokalnom stanju prije upisa u URL
const DRAFT_KEYS = ['q', 'city', 'deadlineFrom', 'deadlineTo'] as const

type DraftKey = (typeof DRAFT_KEYS)[number]
type DraftValues = Record<DraftKey, string>
type DeadlineKey = 'deadlineFrom' | 'deadlineTo'

function toDate(value: string): string | null {
  return value === '' ? null : value
}

function toDraftValues(params: OpportunitySearchParams): DraftValues {
  return {
    q: params.q,
    city: params.city,
    deadlineFrom: params.deadlineFrom ?? '',
    deadlineTo: params.deadlineTo ?? '',
  }
}

/**
 * Polje preuzima vrijednost iz URL-a samo kad se stvarno razlikuje od upisane.
 * Razlika u rubnim razmacima ne broji, pa se korisniku ne briše razmak u pisanju.
 */
function syncDraft(current: DraftValues, incoming: DraftValues): DraftValues {
  const next = { ...current }

  for (const key of DRAFT_KEYS) {
    if (current[key].trim() !== incoming[key]) next[key] = incoming[key]
  }

  return next
}

type Timer = ReturnType<typeof setTimeout>

export function FilterPanel({
  params,
  onChange,
  onClear,
  activeFilters,
  resultCount,
  error,
}: FilterPanelProps) {
  const theme = useTheme()
  const desktop = useMediaQuery(theme.breakpoints.up('md'))
  const [drawerOpen, setDrawerOpen] = useState(false)
  const closeDrawer = () => setDrawerOpen(false)
  const artFields = useArtFields()
  const opportunityTypes = useOpportunityTypes()
  const urlValues = toDraftValues(params)
  const [draft, setDraft] = useState(urlValues)
  const [urlSeen, setUrlSeen] = useState(urlValues)
  const latestChange = useRef(onChange)
  const commitTimers = useRef<{ q?: Timer; city?: Timer }>({})

  // Greške se izvode iz upisanog teksta, pa ne mogu ostati zalijepljene na staroj vrijednosti.
  const textErrors = {
    q: validateTextFilter('q', draft.q),
    city: validateTextFilter('city', draft.city),
  }
  const deadlineError = validateDeadlineRange(
    toDate(draft.deadlineFrom),
    toDate(draft.deadlineTo),
  )

  // Odgođeni upis uvijek koristi zadnji `onChange`
  useEffect(() => {
    latestChange.current = onChange
  })

  if (DRAFT_KEYS.some((key) => urlSeen[key] !== urlValues[key])) {
    setUrlSeen(urlValues)
    setDraft((current) => syncDraft(current, urlValues))
  }

  useEffect(() => {
    const timers = commitTimers.current

    return () => {
      Object.values(timers).forEach((timer) => clearTimeout(timer))
    }
  }, [])

  function handleTextChange(key: TextFilterKey, value: string) {
    setDraft((current) => ({ ...current, [key]: value }))
    clearTimeout(commitTimers.current[key])

    // Neispravna vrijednost ostaje u polju uz poruku, ali se ne šalje.
    if (validateTextFilter(key, value) !== undefined) return

    commitTimers.current[key] = setTimeout(() => {
      latestChange.current({ [key]: value.trim() })
    }, TEXT_DEBOUNCE_MS)
  }

  function renderLocationType() {
    return (
      <TextField
        key="locationType"
        select
        size="small"
        label={filters.locationType}
        value={params.locationType ?? ''}
        onChange={(event) =>
          onChange({ locationType: toLocationType(event.target.value) })
        }
        sx={{ flexGrow: 1, flexBasis: { xs: '100%', md: 0 } }}
      >
        <MenuItem value="">{filters.anyLocationType}</MenuItem>
        {OPPORTUNITY_LOCATION_TYPES.map((type) => (
          <MenuItem key={type} value={type}>
            {hr.opportunityLocationType[type]}
          </MenuItem>
        ))}
      </TextField>
    )
  }

  function renderCatalogFilter(
    key: 'artFieldIds' | 'typeIds',
    label: string,
    emptyLabel: string,
    catalog: { data: CatalogFilterOption[]; unavailable: boolean },
  ) {
    const selectedIds = params[key]
    const options = catalog.data.filter(
      (option) => option.active || selectedIds.includes(option.id),
    )

    return (
      <TextField
        key={key}
        select
        size="small"
        label={label}
        value={selectedIds}
        disabled={catalog.unavailable}
        onChange={(event) =>
          onChange({
            [key]: toIds(options, event.target.value as unknown as string[]),
          })
        }
        slotProps={{
          inputLabel: { shrink: true },
          select: {
            multiple: true,
            displayEmpty: true,
            renderValue: (selected) => {
              const ids = selected as string[]

              if (ids.length === 0) return emptyLabel

              return options
                .filter((option) => ids.includes(option.id))
                .map((option) => option.name)
                .join(', ')
            },
          },
        }}
        sx={{ flexGrow: 1, flexBasis: { xs: '100%', md: 0 } }}
      >
        {options.map((option) => (
          <MenuItem key={option.id} value={option.id} sx={{ py: 0 }}>
            <Checkbox
              checked={selectedIds.includes(option.id)}
              tabIndex={-1}
              disableRipple
            />
            <ListItemText
              primary={
                option.active
                  ? option.name
                  : `${option.name} (${hr.opportunities.form.inactive})`
              }
            />
          </MenuItem>
        ))}
      </TextField>
    )
  }

  function handleDeadlineChange(key: DeadlineKey, value: string) {
    const next = { ...draft, [key]: value }
    setDraft(next)

    // Neispravan raspon ostaje u poljima uz poruku, ali se ne šalje.
    if (validateDeadlineRange(toDate(next.deadlineFrom), toDate(next.deadlineTo)) !== undefined) return

    onChange({
      deadlineFrom: toDate(next.deadlineFrom),
      deadlineTo: toDate(next.deadlineTo),
    })
  }

  function renderText(key: TextFilterKey) {
    const field =
      key === 'q'
        ? { label: filters.q, placeholder: filters.qPlaceholder, grow: 2 }
        : { label: filters.city, placeholder: filters.anyCity, grow: 1 }

    return (
      <TextField
        key={key}
        size="small"
        label={field.label}
        placeholder={field.placeholder}
        value={draft[key]}
        onChange={(event) => handleTextChange(key, event.target.value)}
        error={textErrors[key] !== undefined}
        helperText={textErrors[key]}
        sx={{ flexGrow: field.grow, flexBasis: { xs: '100%', md: 0 } }}
      />
    )
  }

  function renderStatuses() {
    // Prazan odabir nema značenja: parseSearchParams bi ga vratio na zadani ['OPEN'],
    // pa se zadnji preostali status ne može odznačiti.
    const locked = params.statuses.length === 1

    return (
      <TextField
        key="statuses"
        select
        size="small"
        label={filters.statuses}
        value={params.statuses}
        onChange={(event) => {
          const statuses = toStatuses(event.target.value as unknown as string[])

          if (statuses.length > 0) onChange({ statuses })
        }}
        slotProps={{
          select: {
            multiple: true,
            renderValue: (selected) =>
              toStatuses(selected as string[])
                .map((status) => hr.opportunityStatus[status])
                .join(', '),
          },
        }}
        sx={{ flexGrow: 1, flexBasis: { xs: '100%', md: 0 } }}
      >
        {SEARCHABLE_OPPORTUNITY_STATUSES.map((status) => {
          const selected = params.statuses.includes(status)

          return (
            <MenuItem
              key={status}
              value={status}
              disabled={selected && locked}
              sx={{ py: 0 }}
            >
              <Checkbox checked={selected} tabIndex={-1} disableRipple />
              <ListItemText primary={hr.opportunityStatus[status]} />
            </MenuItem>
          )
        })}
      </TextField>
    )
  }

  function renderDeadlineField(key: DeadlineKey, label: string) {
    const first = key === 'deadlineFrom'

    return (
      <TextField
        key={key}
        type="date"
        size="small"
        label={label}
        value={draft[key]}
        onChange={(event) => handleDeadlineChange(key, event.target.value)}
        error={first && deadlineError !== undefined}
        helperText={first ? deadlineError : undefined}
        slotProps={{ inputLabel: { shrink: true } }}
        sx={{ flexGrow: 1, minWidth: 0 }}
      />
    )
  }

  function renderDeadline() {
    return (
      <Stack
        key="deadline"
        direction="row"
        spacing={1}
        sx={{ flexGrow: 1, flexBasis: { xs: '100%', md: 0 }, minWidth: 0 }}
      >
        {renderDeadlineField('deadlineFrom', filters.deadlineFrom)}
        {renderDeadlineField('deadlineTo', filters.deadlineTo)}
      </Stack>
    )
  }

  function renderFields() {
    return [
      renderText('q'),
      renderCatalogFilter('artFieldIds', filters.artFields, filters.anyArtField, {
          data: artFields.data?.data ?? [],
          unavailable: artFields.isPending || artFields.isError,
      }),
      renderCatalogFilter('typeIds', filters.types, filters.anyType, {
        data: opportunityTypes.data?.data ?? [],
        unavailable: opportunityTypes.isPending || opportunityTypes.isError,
      }),
      renderText('city'),
      renderLocationType(),
      renderStatuses(),
      renderDeadline(),
    ]
  }

  function renderClearButton() {
    return (
      <Button
        variant="text"
        size="small"
        onClick={onClear}
        disabled={!activeFilters}
      >
        {hr.opportunities.search.clearFilters}
      </Button>
    )
  }

  return (
    <Stack spacing={2}>
      {error !== undefined && <Alert severity="error">{error.message}</Alert>}

      <Box
        component="section"
        aria-label={filters.title}
        sx={{
          display: { xs: 'none', md: 'flex' },
          gap: 2,
          alignItems: 'center',
          flexWrap: 'wrap',
        }}
      >
        {renderFields()}
        {renderClearButton()}
      </Box>

      <Box sx={{ display: { xs: 'flex', md: 'none' } }}>
        <Button
          variant="outlined"
          aria-haspopup="dialog"
          onClick={() => setDrawerOpen(true)}
        >
          {filters.title}
        </Button>
      </Box>

      <Drawer
        anchor="bottom"
        open={drawerOpen && !desktop}
        onClose={closeDrawer}
        transitionDuration={0}
        ModalProps={{ keepMounted: true }}
        slotProps={{
          paper: {
            sx: { borderRadius: '16px 16px 0 0', p: 2, maxHeight: '90vh' },
          },
        }}
      >
        <Stack component="section" aria-label={filters.title} spacing={2}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Typography component="h2" variant="h6" sx={{ flexGrow: 1 }}>
              {filters.title}
            </Typography>
            {renderClearButton()}
          </Stack>

          {renderFields()}

          {/* TODO: dok se novi broj učitava, zadržati zadnji poznati (keepPreviousData). */}
          <Button variant="contained" fullWidth onClick={closeDrawer}>
            {filters.apply(resultCount ?? 0)}
          </Button>
        </Stack>
      </Drawer>
    </Stack>
  )
}
