import { cleanup, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/client'
import { renderWithProviders } from '../../../test/renderWithProviders'
import { hr } from '../../../i18n/hr'
import type { MyOpportunityPage, MyOpportunitySummary } from '../types'
import { MyOpportunitiesPage } from './MyOpportunitiesPage'

const mocks = vi.hoisted(() => ({
  useMyOpportunities: vi.fn(),
  refetch: vi.fn(),
}))

vi.mock('../opportunityQueries', () => ({
  useMyOpportunities: mocks.useMyOpportunities,
}))

vi.mock('../components/ownerActionsBar', () => ({
  OwnerActionsBar: ({ opportunityId }: { opportunityId: string }) => (
    <button type="button" aria-label={`Radnje ${opportunityId}`}>
      Radnje
    </button>
  ),
}))

const myTexts = hr.opportunities.my

function makeOpportunity(
  overrides: Partial<MyOpportunitySummary> = {},
): MyOpportunitySummary {
  return {
    id: 'opportunity-1',
    title: 'Prilika za umjetnike',
    type: { id: 'type-1', code: 'natjecaj', name: 'Natječaj' },
    artFields: [],
    deadline: '2026-12-01T22:59:00Z',
    status: 'OPEN',
    locationType: 'PHYSICAL',
    city: 'Zagreb',
    latitude: 45.815,
    longitude: 15.9819,
    eventStart: null,
    organizer: {
      id: 'organizer-1',
      displayName: 'Test organizator',
      organizationName: 'Galerija',
    },
    archived: false,
    hiddenByAdmin: false,
    applicationCounts: {
      total: 3,
      submitted: 3,
      underReview: 0,
      needsInfo: 0,
      accepted: 0,
      rejected: 0,
    },
    ...overrides,
  }
}

function makePage(
  data: MyOpportunitySummary[] = [makeOpportunity()],
  total = data.length,
): MyOpportunityPage {
  return {
    data,
    meta: { page: 1, pageSize: 20, total },
  }
}

function setSuccessfulQuery(data = makePage()) {
  mocks.useMyOpportunities.mockReturnValue({
    data,
    isPending: false,
    isError: false,
    isSuccess: true,
    isPlaceholderData: false,
    error: null,
    refetch: mocks.refetch,
  })
}

function renderPage(route = '/my-opportunities') {
  return renderWithProviders(<MyOpportunitiesPage />, { route })
}

beforeEach(() => {
  mocks.refetch.mockReset()
  setSuccessfulQuery()
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('MyOpportunitiesPage', () => {
  it('prikazuje podatke prilike i poveznice na detalje i radnje', () => {
    renderPage()

    expect(
      screen.getByRole('heading', { name: myTexts.title }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Prilika za umjetnike' }),
    ).toHaveAttribute('href', '/opportunities/opportunity-1')
    expect(screen.getByText('Otvorena')).toBeInTheDocument()
    expect(screen.getByText('01.12.2026.')).toBeInTheDocument()
    expect(screen.getByText('Natječaj')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Radnje opportunity-1' }),
    ).toBeInTheDocument()
  })

  it('prikazuje oznaku kada je prilika skrivena od administratora', () => {
    setSuccessfulQuery(makePage([makeOpportunity({ hiddenByAdmin: true })]))

    renderPage()

    expect(screen.getByText(myTexts.hiddenByAdmin)).toBeInTheDocument()
  })

  it('čita arhivu, status i stranicu iz URL-a i šalje ih query hooku', () => {
    renderPage('/my-opportunities?archived=true&status=CLOSED&page=2')

    expect(mocks.useMyOpportunities).toHaveBeenLastCalledWith({
      archived: true,
      status: 'CLOSED',
      page: 2,
    })
    expect(screen.getByRole('tab', { name: myTexts.archived })).toHaveAttribute(
      'aria-selected',
      'true',
    )
  })

  it('promjenom kartice arhive ažurira URL i vraća se na prvu stranicu', async () => {
    const user = userEvent.setup()
    const { router } = renderPage('/my-opportunities?page=3')

    await user.click(screen.getByRole('tab', { name: myTexts.archived }))

    await waitFor(() => {
      expect(router.state.location.search).toContain('archived=true')
      expect(router.state.location.search).not.toContain('page=')
      expect(mocks.useMyOpportunities).toHaveBeenLastCalledWith({
        archived: true,
        status: undefined,
        page: 1,
      })
    })
  })

  it('promjenom statusa postavlja URL filtar i resetira stranicu', async () => {
    const user = userEvent.setup()
    const { router } = renderPage('/my-opportunities?page=3')

    await user.click(
      screen.getByRole('combobox', { name: myTexts.statusFilter }),
    )
    await user.click(
      await screen.findByRole('option', { name: hr.opportunityStatus.CLOSED }),
    )

    await waitFor(() => {
      expect(router.state.location.search).toContain('status=CLOSED')
      expect(router.state.location.search).not.toContain('page=')
      expect(mocks.useMyOpportunities).toHaveBeenLastCalledWith({
        archived: false,
        status: 'CLOSED',
        page: 1,
      })
    })
  })

  it('omogućuje straničenje i ažurira page parametar', async () => {
    const user = userEvent.setup()
    setSuccessfulQuery(makePage([makeOpportunity()], 45))
    const { router } = renderPage()

    await user.click(screen.getByRole('button', { name: 'Idi na stranicu 2' }))

    await waitFor(() => {
      expect(router.state.location.search).toContain('page=2')
      expect(mocks.useMyOpportunities).toHaveBeenLastCalledWith({
        archived: false,
        status: undefined,
        page: 2,
      })
    })
  })

  it('prikazuje prazno stanje i poveznicu za novu priliku', () => {
    setSuccessfulQuery(makePage([], 0))
    renderPage()

    expect(screen.getByText(myTexts.emptyTitle)).toBeInTheDocument()
    const newOpportunityLinks = screen.getAllByRole('link', {
      name: myTexts.newOpportunity,
    })

    expect(newOpportunityLinks).toHaveLength(2)
    expect(newOpportunityLinks[0]).toHaveAttribute(
      'href',
      '/organizer/opportunities/new',
    )
    expect(newOpportunityLinks[1]).toHaveAttribute(
      'href',
      '/organizer/opportunities/new',
    )
  })

  it.each([
    '/my-opportunities?archived=true',
    '/my-opportunities?status=CLOSED',
  ])('za %s bez rezultata ne tvrdi da organizator nema prilika', (route) => {
    setSuccessfulQuery(makePage([], 0))
    renderPage(route)

    expect(screen.getByText(myTexts.emptyFilteredTitle)).toBeInTheDocument()
    expect(screen.queryByText(myTexts.emptyTitle)).not.toBeInTheDocument()
  })

  it('s prazne stranice iza zadnje vraća na zadnju postojeću stranicu', async () => {
    setSuccessfulQuery(makePage([], 21))
    const { router } = renderPage('/my-opportunities?archived=true&page=3')

    await waitFor(() => {
      expect(router.state.location.search).toBe('?archived=true&page=2')
    })
    expect(
      screen.queryByText(myTexts.emptyFilteredTitle),
    ).not.toBeInTheDocument()
  })

  it('prikazuje stanje učitavanja', () => {
    mocks.useMyOpportunities.mockReturnValue({
      data: undefined,
      isPending: true,
      isError: false,
      error: null,
      refetch: mocks.refetch,
    })

    renderPage()

    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true')
  })

  it('prikazuje API pogrešku i omogućuje ponovni pokušaj', async () => {
    const user = userEvent.setup()
    mocks.useMyOpportunities.mockReturnValue({
      data: undefined,
      isPending: false,
      isError: true,
      error: new ApiError(500, {
        code: 'INTERNAL_ERROR',
        message: 'Greška poslužitelja.',
      }),
      refetch: mocks.refetch,
    })

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Greška poslužitelja.',
    )
    await user.click(screen.getByRole('button', { name: hr.common.retry }))
    expect(mocks.refetch).toHaveBeenCalledOnce()
  })
})
