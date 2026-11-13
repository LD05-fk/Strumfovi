import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from '../../api/client'
import { createWrapper } from '../../test/renderWithProviders'
import type { MyOpportunityPage } from './types'
import { useMyOpportunities } from './opportunityQueries'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()

  return { ...actual, apiFetch: vi.fn() }
})

const response: MyOpportunityPage = {
  data: [],
  meta: { page: 1, pageSize: 20, total: 0 },
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('useMyOpportunities', () => {
  it('dohvaća stranicu arhiviranih prilika bez statusnog filtra', async () => {
    vi.mocked(apiFetch).mockResolvedValue(response)

    const { result } = renderHook(
      () => useMyOpportunities({ archived: true, page: 1 }),
      { wrapper: createWrapper() },
    )

    await waitFor(() => {
      expect(result.current.data).toEqual(response)
    })

    expect(apiFetch).toHaveBeenCalledWith(
      '/me/opportunities?archived=true&page=1&pageSize=20',
    )
  })

  it('uključuje status u zahtjev kad je statusni filtar odabran', async () => {
    vi.mocked(apiFetch).mockResolvedValue(response)

    const { result } = renderHook(
      () =>
        useMyOpportunities({
          archived: false,
          status: 'CLOSED',
          page: 2,
        }),
      { wrapper: createWrapper() },
    )

    await waitFor(() => {
      expect(result.current.data).toEqual(response)
    })

    expect(apiFetch).toHaveBeenCalledWith(
      '/me/opportunities?archived=false&page=2&pageSize=20&status=CLOSED',
    )
  })
})
