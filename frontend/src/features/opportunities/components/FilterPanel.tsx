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

/**
 * Što još treba dopuniti (OPP-02-FE1, 4. korak):
 *
 * - `deadlineFrom` / `deadlineTo`: dva polja datuma (`YYYY-MM-DD`).
 * - Zod validacija uz polja: duljina `q` i `city` najviše 100 znakova te obrnut raspon
 *   roka — poruka ide uz polje i zahtjev se ne šalje.
 *
 * Svaka promjena ide kroz `onChange`, koji u stranici resetira `page` na 1.
 * Do tada su polja samo za čitanje i prikazuju stanje iz URL-a.
 */

type FilterPanelProps = {
  params: OpportunitySearchParams
  onChange: (change: Partial<OpportunitySearchParams>) => void
  onClear: () => void
  activeFilters: boolean
  resultCount: number | undefined
  error: ApiError | undefined
}

type FilterField = {
  key: string
  label: string
  placeholder: string
  value: string
  grow?: number
}

type Timer = ReturnType<typeof setTimeout>

/** Privremeni raspored polja; svako se zamjenjuje pravom kontrolom iz popisa gore. */
function toFields(params: OpportunitySearchParams): FilterField[] {
  const deadline = [params.deadlineFrom, params.deadlineTo]
    .filter((date) => date !== null)
    .join(' – ')

  return [
    {
      key: 'q',
      label: filters.q,
      placeholder: filters.qPlaceholder,
      value: '',
      grow: 2,
    },
    {
      key: 'artFieldIds',
      label: filters.artFields,
      placeholder: filters.anyArtField,
      value: '',
    },
    {
      key: 'typeIds',
      label: filters.types,
      placeholder: filters.anyType,
      value: '',
    },
    {
      key: 'city',
      label: filters.city,
      placeholder: filters.anyCity,
      value: '',
    },
    {
      key: 'locationType',
      label: filters.locationType,
      placeholder: filters.anyLocationType,
      value: '',
    },
    {
      key: 'statuses',
      label: filters.statuses,
      placeholder: hr.opportunityStatus.OPEN,
      value: '',
    },
    {
      key: 'deadline',
      label: filters.deadline,
      placeholder: filters.anyDeadline,
      value: deadline,
    },
  ]
}

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
  const fields = toFields(params)
  const artFields = useArtFields()
  const opportunityTypes = useOpportunityTypes()
  const [draft, setDraft] = useState({ q: params.q, city: params.city })
  const [urlText, setUrlText] = useState({ q: params.q, city: params.city })
  const latestChange = useRef(onChange)
  const commitTimers = useRef<{ q?: Timer; city?: Timer}>({})

  // Greške se izvode iz upisanog teksta, pa ne mogu ostati zalijepljene na staroj vrijednosti.
  const textErrors = {
    q: validateTextFilter('q', draft.q),
    city: validateTextFilter('city', draft.city),
  }

  // Odgođeni upis uvijek koristi zadnji `onChange`
  useEffect(() => {
    latestChange.current = onChange
  })

  if (urlText.q !== params.q || urlText.city !== params.city) {
    setUrlText({ q: params.q, city: params.city })
    setDraft((current) => ({
      q: current.q.trim() === params.q ? current.q : params.q,
      city: current.city.trim() === params.city ? current.city : params.city,
    }))
  }

  useEffect(() => {
    return () => {
      Object.values(commitTimers.current).forEach((timer) =>
          clearTimeout(timer),
      )
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

  function renderPlaceholder(field: FilterField) {
    return (
      <TextField
        key={field.key}
        size="small"
        label={field.label}
        placeholder={field.placeholder}
        value={field.value}
        slotProps={{ input: { readOnly: true } }}
        sx={{ flexGrow: field.grow ?? 1, flexBasis: { xs: '100%', md: 0 } }}
      />
    )
  }

  function renderFields() {
    return fields.map((field) => {
      if (field.key === 'q') return renderText('q')
      if (field.key === 'city') return renderText('city')

      if (field.key === 'artFieldIds') {
        return renderCatalogFilter(
          'artFieldIds',
          filters.artFields,
          filters.anyArtField,
          {
            data: artFields.data?.data ?? [],
            unavailable: artFields.isPending || artFields.isError,
          },
        )
      }

      if (field.key === 'typeIds') {
        return renderCatalogFilter('typeIds', filters.types, filters.anyType, {
          data: opportunityTypes.data?.data ?? [],
          unavailable: opportunityTypes.isPending || opportunityTypes.isError,
        })
      }

      if (field.key === 'locationType') return renderLocationType()
      if (field.key === 'statuses') return renderStatuses()

      return renderPlaceholder(field)
    })
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
