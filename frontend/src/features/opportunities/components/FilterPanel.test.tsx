import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiFetch, type ApiFetchOptions } from '../../../api/client'
import { hr } from '../../../i18n/hr'
import { renderWithProviders } from '../../../test/renderWithProviders'
import {
  DEFAULT_OPPORTUNITY_SEARCH_PARAMS,
  type OpportunitySearchParams,
} from '../types'
import { FilterPanel } from './FilterPanel'

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>()

  return { ...actual, apiFetch: vi.fn() }
})

const texts = hr.opportunities.search

const catalog = {
  data: [
    {
      id: 'field-1',
      code: 'painting',
      name: 'Slikarstvo',
      active: true,
      position: 1,
    },
  ],
  meta: { page: 1, pageSize: 100, total: 1 },
}

async function mockApiFetch<T>(path: string, _options?: ApiFetchOptions) {
  if (path.startsWith('/art-fields') || path.startsWith('/opportunity-types')) {
    return catalog as T
  }

  throw new Error(`Neočekivani zahtjev: ${path}`)
}

function renderPanel(params: Partial<OpportunitySearchParams> = {}) {
  vi.mocked(apiFetch).mockImplementation(mockApiFetch)
  const onChange = vi.fn()
  const onClear = vi.fn()
  const merged = { ...DEFAULT_OPPORTUNITY_SEARCH_PARAMS, ...params }

  renderWithProviders(
    <FilterPanel
      params={merged}
      onChange={onChange}
      onClear={onClear}
      activeFilters={Object.keys(params).length > 0}
      resultCount={12}
      error={undefined}
    />,
  )

  return { onChange, onClear }
}

function filterBar() {
  return within(screen.getByRole('region', { name: texts.filters.title }))
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('FilterPanel', () => {
  it('dodaje odabrani status postojećima', async () => {
    const user = userEvent.setup()
    const { onChange } = renderPanel()

    await user.click(filterBar().getByLabelText(texts.filters.statuses))
    await user.click(
      await screen.findByRole('option', { name: hr.opportunityStatus.CLOSED }),
    )

    expect(onChange).toHaveBeenCalledWith({ statuses: ['OPEN', 'CLOSED'] })
  })

  it('zadnji preostali status se ne može odznačiti', async () => {
    const user = userEvent.setup()
    const { onChange } = renderPanel()

    await user.click(filterBar().getByLabelText(texts.filters.statuses))
    const open = await screen.findByRole('option', {
      name: hr.opportunityStatus.OPEN,
    })

    // Onemogućena stavka ne prima klik (pointer-events: none), pa pretraga nikad ne ostane bez statusa.
    expect(open).toHaveAttribute('aria-disabled', 'true')
    expect(onChange).not.toHaveBeenCalled()
  })

  it('čišćenje filtara je dostupno tek kad filtri postoje', async () => {
    const user = userEvent.setup()
    const { onClear } = renderPanel({ city: 'Zagreb' })
    const clear = filterBar().getByRole('button', { name: texts.clearFilters })

    await user.click(clear)
    expect(onClear).toHaveBeenCalledOnce()
  })

  it('bez filtara je gumb za čišćenje onemogućen', () => {
    renderPanel()

    expect(
      filterBar().getByRole('button', { name: texts.clearFilters }),
    ).toBeDisabled()
  })
})
