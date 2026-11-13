import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch } from '../../../api/client'
import { theme } from '../../../app/theme'
import { hr } from '../../../i18n/hr'
import type { CurrentUser } from '../../auth/types'
import type { OpportunityDetail } from '../types'
import { OpportunityDetailPage } from './OpportunityDetailPage'
import { NotifyProvider } from '../../../components/state/NotifyProvider'

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>()
  return { ...actual, apiFetch: vi.fn() }
})

vi.mock('../components/map/MapView', () => ({
  default: ({ ariaLabel }: { ariaLabel: string }) => (
    <div role="region" aria-label={ariaLabel}>
      Karta
    </div>
  ),
}))

let opportunity: OpportunityDetail
let currentUser: CurrentUser | null
let opportunityError: ApiError | null

function makeOpportunity(): OpportunityDetail {
  return {
    id: 'opportunity-1',
    title: 'Prilika za umjetnike',
    description: 'Opis prilike.',
    type: { id: 'type-1', code: 'natjecaj', name: 'Natječaj' },
    artFields: [{ id: 'field-1', code: 'slikarstvo', name: 'Slikarstvo' }],
    deadline: '2026-12-01T22:59:00Z',
    criteria: null,
    prize: null,
    location: {
      type: 'PHYSICAL',
      streetAddress: 'Ilica 1',
      city: 'Zagreb',
      postalCode: '10000',
      latitude: 45.813,
      longitude: 15.977,
      onlineUrl: null,
    },
    eventStart: null,
    eventEnd: null,
    status: 'OPEN',
    archived: false,
    hiddenByAdmin: false,
    organizer: {
      id: 'organizer-1',
      displayName: 'Test organizator',
      organizationName: 'Galerija',
    },
    publishedAt: null,
    createdAt: '2026-01-01T10:00:00Z',
    updatedAt: '2026-01-01T10:00:00Z',
    hasResults: false,
    myApplicationId: null,
  }
}

function makeUser(): CurrentUser {
  return {
    id: 'organizer-1',
    displayName: 'Test organizator',
    email: 'organizer@example.com',
    avatarUrl: null,
    role: 'ORGANIZER',
    status: 'ACTIVE',
    organizerApproval: 'APPROVED',
  }
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const router = createMemoryRouter(
    [
      {
        path: '/opportunities/:opportunityId',
        element: <OpportunityDetailPage />,
      },
    ],
    { initialEntries: ['/opportunities/opportunity-1'] },
  )

  return render(
    <NotifyProvider>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <RouterProvider router={router} />
        </ThemeProvider>
      </QueryClientProvider>
    </NotifyProvider>,
  )
}

beforeEach(() => {
  opportunity = makeOpportunity()
  currentUser = null
  opportunityError = null

  vi.mocked(apiFetch).mockImplementation(
    async <T,>(path: string): Promise<T> => {
      if (path === '/me') {
        if (!currentUser) {
          throw new ApiError(401, {
            code: 'UNAUTHENTICATED',
            message: 'Korisnik nije prijavljen',
          })
        }
        return currentUser as T
      }

      if (path === '/opportunities/opportunity-1') {
        if (opportunityError) throw opportunityError
        return opportunity as T
      }

      throw new Error(`Neočekivan API poziv: ${path}`)
    },
  )
})

afterEach(() => {
  cleanup()
  vi.resetAllMocks()
})

describe('OpportunityDetailPage', () => {
  it('prikazuje adresu, nepomičnu kartu i OpenStreetMap poveznicu za fizičku lokaciju', async () => {
    renderPage()

    expect(await screen.findByText('Ilica 1, 10000 Zagreb')).toBeInTheDocument()
    expect(
      screen.getByRole('region', { name: hr.map.ariaLabel }),
    ).toBeInTheDocument()

    const osmLink = screen.getByRole('link', { name: /OpenStreetMapu/ })
    expect(osmLink).toHaveAttribute(
      'href',
      'https://www.openstreetmap.org/?mlat=45.813&mlon=15.977#map=17/45.813/15.977',
    )
    expect(osmLink).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('prikazuje sigurnu vanjsku poveznicu i ne prikazuje kartu za online lokaciju', async () => {
    opportunity.location = {
      type: 'ONLINE',
      streetAddress: null,
      city: null,
      postalCode: null,
      latitude: null,
      longitude: null,
      onlineUrl: 'https://example.com/dogadjanje',
    }

    renderPage()

    const onlineLink = await screen.findByRole('link', {
      name: /example\.com\/dogadjanje/,
    })
    expect(onlineLink).toHaveAttribute('href', 'https://example.com/dogadjanje')
    expect(onlineLink).toHaveAttribute('target', '_blank')
    expect(onlineLink).toHaveAttribute('rel', 'noopener noreferrer')
    expect(
      screen.queryByRole('region', { name: hr.map.ariaLabel }),
    ).not.toBeInTheDocument()
  })

  it('vlasniku prikazuje privatnu oznaku za priliku u pripremi i oznake arhive', async () => {
    currentUser = makeUser()
    opportunity.status = 'PREPARATION'
    opportunity.archived = true
    opportunity.hiddenByAdmin = true

    renderPage()

    expect(
      await screen.findByText(hr.opportunities.detail.preparationPrivate),
    ).toBeInTheDocument()
    expect(
      screen.getByText(hr.opportunities.detail.archived),
    ).toBeInTheDocument()
    expect(
      screen.getByText(hr.opportunities.detail.hiddenByAdmin),
    ).toBeInTheDocument()
  })

  it('prikazuje placeholder rezultata ispod detalja kad prilika ima rezultate', async () => {
    opportunity.hasResults = true

    renderPage()

    expect(
      await screen.findByRole('heading', {
        name: hr.opportunities.detail.results,
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(hr.opportunities.detail.resultsPlaceholder),
    ).toBeInTheDocument()
  })

  it('na 404 prikazuje poruku i poveznicu na popis prilika', async () => {
    opportunityError = new ApiError(404, {
      code: 'NOT_FOUND',
      message: 'Prilika nije pronađena',
    })

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      hr.opportunities.detail.notFound,
    )
    expect(
      screen.getByRole('link', {
        name: hr.opportunities.detail.allOpportunities,
      }),
    ).toHaveAttribute('href', '/opportunities')
  })
})
