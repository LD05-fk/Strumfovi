import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { API_ERROR_EVENT, ApiError, apiFetch } from './client'

const fetchMock = vi.fn<typeof fetch>()

function sentInit(): RequestInit {
  return fetchMock.mock.calls[0][1] ?? {}
}

function sentHeaders(): Headers {
  return new Headers(sentInit().headers)
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
  fetchMock.mockReset()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

describe('apiFetch', () => {
  it('šalje CSRF zaglavlje na zahtjevima koji mijenjaju stanje', async () => {
    document.cookie = 'XSRF-TOKEN=abc123'
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    const result = await apiFetch('/opportunities', { method: 'POST' })

    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/opportunities')
    expect(sentHeaders().get('X-XSRF-TOKEN')).toBe('abc123')
    expect(sentInit().credentials).toBe('include')
    expect(result).toBeUndefined()
  })

  it('ne šalje CSRF zaglavlje na GET zahtjevima i vraća JSON', async () => {
    document.cookie = 'XSRF-TOKEN=abc123'
    fetchMock.mockResolvedValue(Response.json({ ok: true }))

    const data = await apiFetch<{ ok: boolean }>('/opportunities')

    expect(data).toEqual({ ok: true })
    expect(sentHeaders().has('X-XSRF-TOKEN')).toBe(false)
  })

  it('opcija json šalje tijelo kao JSON', async () => {
    fetchMock.mockResolvedValue(Response.json({ id: 1 }, { status: 201 }))

    await apiFetch('/opportunities', {
      method: 'POST',
      json: { title: 'Izložba' },
    })

    expect(sentHeaders().get('Content-Type')).toBe('application/json')
    expect(sentInit().body).toBe('{"title":"Izložba"}')
  })

  it('pretvara ErrorResponse u ApiError', async () => {
    fetchMock.mockResolvedValue(
      Response.json(
        {
          error: {
            code: 'VALIDATION_ERROR',
            message: 'Neispravni podaci',
            details: [{ field: 'title', issue: 'obavezno polje' }],
          },
        },
        { status: 400 },
      ),
    )

    const error = await apiFetch('/opportunities').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Neispravni podaci',
      details: [{ field: 'title', issue: 'obavezno polje' }],
    })
  })

  it('emitira događaj za globalno rukovanje autentifikacijskom greškom', async () => {
    const handleApiError = vi.fn()
    window.addEventListener(API_ERROR_EVENT, handleApiError)
    fetchMock.mockResolvedValue(
      Response.json(
        { error: { code: 'ONBOARDING_REQUIRED', message: 'Odaberite ulogu' } },
        { status: 403 },
      ),
    )

    await expect(apiFetch('/opportunities')).rejects.toBeInstanceOf(ApiError)

    expect(handleApiError).toHaveBeenCalledWith(
      expect.objectContaining({
        detail: {
          status: 403,
          code: 'ONBOARDING_REQUIRED',
          path: '/opportunities',
        },
      }),
    )
    window.removeEventListener(API_ERROR_EVENT, handleApiError)
  })

  it('odgovor koji nije JSON postaje ApiError', async () => {
    fetchMock.mockResolvedValue(new Response('<html>prijava</html>'))

    const error = await apiFetch('/opportunities').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 200, code: 'INTERNAL_ERROR' })
  })

  it('nedostupan poslužitelj postaje ApiError', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await apiFetch('/opportunities').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 0, code: 'NETWORK_ERROR' })
  })

  it.each(['POST', 'PUT', 'PATCH', 'DELETE'])(
    'šalje CSRF zaglavlje za metodu %s',
    async (method) => {
      document.cookie = 'XSRF-TOKEN=abc123'
      fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

      await apiFetch('/opportunities', { method })

      expect(sentHeaders().get('X-XSRF-TOKEN')).toBe('abc123')
    },
  )
})
