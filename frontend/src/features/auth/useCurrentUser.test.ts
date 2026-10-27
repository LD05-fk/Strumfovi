import { renderHook, waitFor } from '@testing-library/react'
import { vi, it, expect, afterEach } from 'vitest'
import { createWrapper } from '../../test/renderWithProviders'
import { apiFetch } from '../../api/client'
import { useCurrentUser } from './useCurrentUser'
import type { CurrentUser } from './types'
import { ApiError } from '../../api/client'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return {
    ...actual,
    apiFetch: vi.fn(),
  }
})

const mockUser: CurrentUser = {
  id: '123',
  displayName: 'Testni korisnik',
  email: 'test@example.com',
  avatarUrl: null,
  role: 'ARTIST',
  status: 'ACTIVE',
  organizerApproval: null,
}

afterEach(() => {
  vi.clearAllMocks()
})

it('vraća trenutno prijavljenog korisnika', async () => {
  vi.mocked(apiFetch).mockResolvedValue(mockUser)

  const { result } = renderHook(() => useCurrentUser(), {
    wrapper: createWrapper(),
  })
  await waitFor(() => {
    expect(result.current.data).toEqual(mockUser)
  })
})

it('vraća null kada korisnik nije prijavljen', async () => {
  vi.mocked(apiFetch).mockRejectedValue(
    new ApiError(401, {
      code: 'UNAUTHENTICATED',
      message: 'Korisnik nije prijavljen',
    }),
  )

  const { result } = renderHook(() => useCurrentUser(), {
    wrapper: createWrapper(),
  })

  await waitFor(() => {
    expect(result.current.isSuccess).toBe(true)
    expect(result.current.data).toBeNull()
  })
})
