// Svi pozivi backenda idu kroz apiFetch: osnovna adresa, kolačić sesije, CSRF, JSON i obrada grešaka na jednom mjestu.

// Omotnica greške iz API-ja: { "error": { "code", "message", "details"? } }
export type ApiErrorDetail = { field: string; issue: string }

type ErrorBody = {
  code: string
  message: string
  details?: ApiErrorDetail[]
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: ApiErrorDetail[]

  constructor(status: number, body: ErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = status
    this.code = body.code
    this.details = body.details ?? []
  }
}

export const API_ERROR_EVENT = 'artconnect:api-error'

export type ApiErrorEventDetail = {
  status: number
  code: string
  path: string
}

export type ApiFetchOptions = Omit<RequestInit, 'body'> & {
  // podaci koji se šalju kao JSON (apiFetch postavi Content-Type i JSON.stringify)
  json?: unknown
  // ostala tijela (npr. FormData za učitavanje datoteka) idu bez promjene
  body?: BodyInit | null
}

const BASE_URL = '/api/v1'
const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS']

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((part) => part.startsWith(`${name}=`))
    ?.slice(name.length + 1)
}

function unexpected(status: number, message: string): ApiError {
  return new ApiError(status, { code: 'INTERNAL_ERROR', message })
}

function emitApiError(error: ApiError, path: string): void {
  if (typeof window === 'undefined') return

  window.dispatchEvent(
    new CustomEvent<ApiErrorEventDetail>(API_ERROR_EVENT, {
      detail: { status: error.status, code: error.code, path },
    }),
  )
}

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  const { json, ...init } = options
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  if (json !== undefined) {
    headers.set('Content-Type', 'application/json')
    init.body = JSON.stringify(json)
  }

  // Spring Security: zahtjevi koji mijenjaju stanje šalju vrijednost kolačića XSRF-TOKEN u zaglavlju
  const token = readCookie('XSRF-TOKEN')
  if (token && !SAFE_METHODS.includes(method)) {
    headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
  }

  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      method,
      headers,
      credentials: 'include',
    })
  } catch {
    // mreža ili backend nedostupni (fetch tada ne vraća odgovor)
    throw new ApiError(0, {
      code: 'NETWORK_ERROR',
      message:
        'Poslužitelj nije dostupan. Provjerite vezu i pokušajte ponovno.',
    })
  }

  const text = await response.text()
  let body: unknown
  try {
    body = text ? JSON.parse(text) : undefined
  } catch {
    // npr. HTML stranica umjesto JSON-a (preusmjeravanje, greška proxyja)
    const apiError = unexpected(
      response.status,
      `Neočekivan odgovor poslužitelja (HTTP ${response.status})`,
    )
    if (!response.ok) emitApiError(apiError, path)
    throw apiError
  }

  if (!response.ok) {
    const error = (body as { error?: ErrorBody } | undefined)?.error
    const apiError = error?.code
      ? new ApiError(response.status, error)
      : unexpected(
          response.status,
          `Neočekivan odgovor poslužitelja (HTTP ${response.status})`,
        )

    emitApiError(apiError, path)
    throw apiError
  }

  return body as T
}
