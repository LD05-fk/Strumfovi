import { cleanup, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/client'
import { renderWithProviders } from '../../../test/renderWithProviders'
import { hr } from '../../../i18n/hr'
import { OwnerActionsBar } from './ownerActionsBar'
import type { ComponentProps } from 'react'

const mocks = vi.hoisted(() => ({
  archive: vi.fn(),
  changeStatus: vi.fn(),
  deleteOpportunity: vi.fn(),
  notify: vi.fn(),
}))

vi.mock('../opportunityQueries', () => ({
  useArchiveOpportunity: () => ({
    mutateAsync: mocks.archive,
    isPending: false,
  }),
  useChangeOpportunityStatus: () => ({
    mutateAsync: mocks.changeStatus,
    isPending: false,
  }),
  useDeleteOpportunity: () => ({
    mutateAsync: mocks.deleteOpportunity,
    isPending: false,
  }),
}))

vi.mock('../../../components/state/useNotify', () => ({
  useNotify: () => mocks.notify,
}))

type OwnerActionsBarProps = ComponentProps<typeof OwnerActionsBar>

const opportunity: OwnerActionsBarProps = {
  opportunityId: 'opportunity-1',
  title: 'Prilika za umjetnike',
  status: 'PREPARATION',
  archived: false,
  deadline: '2099-12-01T22:59:00Z',
}

function renderBar(props: Partial<OwnerActionsBarProps> = {}) {
  return renderWithProviders(<OwnerActionsBar {...opportunity} {...props} />, {
    route: '/opportunities/opportunity-1',
  })
}

async function openActions() {
  await userEvent.click(
    screen.getByRole('button', {
      name: hr.opportunities.detail.ownerActions.menu,
    }),
  )
}

async function getActionLabels() {
  await openActions()
  return screen.getAllByRole('menuitem').map((item) => item.textContent)
}

beforeEach(() => {
  mocks.archive.mockReset().mockResolvedValue({})
  mocks.changeStatus.mockReset().mockResolvedValue({})
  mocks.deleteOpportunity.mockReset().mockResolvedValue(undefined)
  mocks.notify.mockReset()
})

afterEach(() => {
  cleanup()
})

describe('OwnerActionsBar', () => {
  it('nudi objavu nacrta, arhiviranje i brisanje te koristi RouterLink za uređivanje', async () => {
    renderBar()

    const labels = await getActionLabels()
    expect(labels).toEqual([
      hr.opportunities.detail.ownerActions.edit,
      hr.opportunities.detail.ownerActions.publish,
      hr.opportunities.detail.ownerActions.archive,
      hr.opportunities.detail.ownerActions.delete,
    ])

    expect(
      screen.getByRole('menuitem', {
        name: hr.opportunities.detail.ownerActions.edit,
      }),
    ).toHaveAttribute('href', '/organizer/opportunities/opportunity-1/edit')
  })

  it('za otvorenu priliku nudi samo zatvaranje prijava među statusnim radnjama', async () => {
    renderBar({ status: 'OPEN' })

    const labels = await getActionLabels()
    expect(labels).toContain(
      hr.opportunities.detail.ownerActions.closeApplications,
    )
    expect(labels).not.toContain(
      hr.opportunities.detail.ownerActions.reopenApplications,
    )
    expect(labels).not.toContain(
      hr.opportunities.detail.ownerActions.startSelection,
    )
  })

  it('zatvorenu priliku prije roka može ponovno otvoriti ili poslati u odabir', async () => {
    renderBar({
      status: 'CLOSED',
      deadline: '2099-12-01T22:59:00Z',
    })

    const labels = await getActionLabels()
    expect(labels).toContain(
      hr.opportunities.detail.ownerActions.reopenApplications,
    )
    expect(labels).toContain(
      hr.opportunities.detail.ownerActions.startSelection,
    )
  })

  it('ne nudi ponovno otvaranje zatvorene prilike nakon roka', async () => {
    renderBar({
      status: 'CLOSED',
      deadline: '2000-01-01T00:00:00Z',
    })

    const labels = await getActionLabels()
    expect(labels).not.toContain(
      hr.opportunities.detail.ownerActions.reopenApplications,
    )
    expect(labels).toContain(
      hr.opportunities.detail.ownerActions.startSelection,
    )
  })

  it.each(['SELECTION', 'FINISHED'] as const)(
    'za status %s ne nudi statusne prijelaze',
    async (status) => {
      renderBar({ status })

      const labels = await getActionLabels()
      expect(labels).toContain(hr.opportunities.detail.ownerActions.archive)
      expect(labels).toContain(hr.opportunities.detail.ownerActions.delete)
      expect(labels).not.toContain(hr.opportunities.detail.ownerActions.publish)
      expect(labels).not.toContain(
        hr.opportunities.detail.ownerActions.closeApplications,
      )
      expect(labels).not.toContain(
        hr.opportunities.detail.ownerActions.startSelection,
      )
    },
  )

  it('za arhiviranu priliku nudi brisanje, ali ne i statusnu promjenu ili arhiviranje', async () => {
    renderBar({ status: 'OPEN', archived: true })

    const labels = await getActionLabels()
    expect(labels).toContain(hr.opportunities.detail.ownerActions.delete)
    expect(labels).not.toContain(hr.opportunities.detail.ownerActions.archive)
    expect(labels).not.toContain(
      hr.opportunities.detail.ownerActions.closeApplications,
    )
  })

  it('traži potvrdu prije promjene statusa i šalje odabrani prijelaz', async () => {
    renderBar()
    await openActions()

    await userEvent.click(
      screen.getByRole('menuitem', {
        name: hr.opportunities.detail.ownerActions.publish,
      }),
    )

    expect(
      await screen.findByRole('dialog', {
        name: hr.opportunities.detail.confirmStatusTitle(
          hr.opportunities.detail.ownerActions.publish,
        ),
      }),
    ).toBeInTheDocument()

    await userEvent.click(
      screen.getByRole('button', {
        name: hr.opportunities.detail.ownerActions.publish,
      }),
    )

    await waitFor(() => {
      expect(mocks.changeStatus).toHaveBeenCalledWith({
        opportunityId: opportunity.opportunityId,
        status: 'OPEN',
      })
    })
  })

  ;(it('nakon HAS_APPLICATIONS nudi arhiviranje umjesto brisanja', async () => {
    mocks.deleteOpportunity.mockRejectedValue(
      new ApiError(409, {
        code: 'HAS_APPLICATIONS',
        message: 'Prilika ima prijave.',
      }),
    )
    renderBar()
    await openActions()

    await userEvent.click(
      screen.getByRole('menuitem', {
        name: hr.opportunities.detail.ownerActions.delete,
      }),
    )
    await userEvent.click(
      screen.getByRole('button', {
        name: hr.opportunities.detail.ownerActions.delete,
      }),
    )

    expect(
      await screen.findByRole('heading', {
        name: hr.opportunities.detail.deleteHasApplicationsTitle,
      }),
    ).toBeInTheDocument()

    await userEvent.click(
      screen.getByRole('button', {
        name: hr.opportunities.detail.ownerActions.archive,
      }),
    )

    await waitFor(() => {
      expect(mocks.archive).toHaveBeenCalledWith(opportunity.opportunityId)
    })
  }),
    it('prikazuje objašnjenje kad je rok istekao pri ponovnom otvaranju', async () => {
      mocks.changeStatus.mockRejectedValue(
        new ApiError(409, {
          code: 'DEADLINE_PASSED',
          message: 'Rok je istekao.',
        }),
      )

      renderBar({ status: 'CLOSED', deadline: '2099-12-01T22:59:00Z' })
      await openActions()

      await userEvent.click(
        screen.getByRole('menuitem', {
          name: hr.opportunities.detail.ownerActions.reopenApplications,
        }),
      )
      await userEvent.click(
        await screen.findByRole('button', {
          name: hr.opportunities.detail.ownerActions.reopenApplications,
        }),
      )

      await waitFor(() => {
        expect(mocks.notify).toHaveBeenCalledWith(
          hr.opportunities.detail.deadlinePassed,
          'error',
        )
      })
    }))

  it('prikazuje pogrešku i osvježava popis i detalj kod nedopuštenog prijelaza', async () => {
    mocks.changeStatus.mockRejectedValue(
      new ApiError(409, {
        code: 'INVALID_STATUS_TRANSITION',
        message: 'Prijelaz nije dopušten.',
      }),
    )

    const { queryClient } = renderBar()
    const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries')

    await openActions()
    await userEvent.click(
      screen.getByRole('menuitem', {
        name: hr.opportunities.detail.ownerActions.publish,
      }),
    )
    await userEvent.click(
      await screen.findByRole('button', {
        name: hr.opportunities.detail.ownerActions.publish,
      }),
    )

    await waitFor(() => {
      expect(mocks.notify).toHaveBeenCalledWith(
        'Prijelaz nije dopušten.',
        'error',
      )
      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: ['my-opportunities'],
      })
      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: ['opportunity', opportunity.opportunityId],
      })
    })
  })

  it('prikazuje poruku API-ja kad organizator još nema odobrenje', async () => {
    mocks.changeStatus.mockRejectedValue(
      new ApiError(403, {
        code: 'ORGANIZER_NOT_APPROVED',
        message: 'Organizator mora biti odobren.',
      }),
    )

    renderBar()
    await openActions()

    await userEvent.click(
      screen.getByRole('menuitem', {
        name: hr.opportunities.detail.ownerActions.publish,
      }),
    )
    await userEvent.click(
      await screen.findByRole('button', {
        name: hr.opportunities.detail.ownerActions.publish,
      }),
    )

    await waitFor(() => {
      expect(mocks.notify).toHaveBeenCalledWith(
        'Organizator mora biti odobren.',
        'error',
      )
    })
  })
})
