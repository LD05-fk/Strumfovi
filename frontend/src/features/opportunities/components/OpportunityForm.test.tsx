import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/client'
import { OpportunityForm } from './OpportunityForm'
import type { Dayjs } from 'dayjs'
import { toOpportunityFormValues, type OpportunityDetail } from '../types'
import { createOpportunitySchema } from './opportunitySchema'

const { mutateAsync, searchMutateAsync, notify } = vi.hoisted(() => ({
  mutateAsync: vi.fn(),
  searchMutateAsync: vi.fn(),
  notify: vi.fn(),
}))

vi.mock('../opportunityQueries', () => ({
  useSaveOpportunity: () => ({ mutateAsync }),
  useSearchGeocoding: () => ({
    mutateAsync: searchMutateAsync,
    isPending: false,
  }),
}))

vi.mock('../../../components/state/useNotify', () => ({
  useNotify: () => notify,
}))

vi.mock('@mui/x-date-pickers/DateTimePicker', async () => {
  const { dayjs, OPPORTUNITY_TIME_ZONE } = await import('../../../app/dayjs')

  return {
    DateTimePicker: ({
      label,
      onChange,
    }: {
      label: string
      onChange: (value: Dayjs | null) => void
    }) => (
      <button
        type="button"
        aria-label={`Odaberi ${label}`}
        onClick={() =>
          onChange(dayjs.tz('2099-01-10 23:00', OPPORTUNITY_TIME_ZONE))
        }
      >
        {label}
      </button>
    ),
  }
})

const typeId = '00000000-0000-4000-8000-000000000001'
const artFieldId = '00000000-0000-4000-8000-000000000002'

function renderForm() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  const router = createMemoryRouter(
    [
      {
        path: '*',
        element: (
          <OpportunityForm
            isCreating
            initialValues={{
              title: 'Prilika za umjetnike',
              description: 'Opis prilike.',
              typeId,
              artFieldIds: [artFieldId],
              deadline: '2099-01-10T23:00:00+01:00',
              criteria: '',
              prize: '',
              eventStart: '',
              eventEnd: '',
              location: {
                type: 'PHYSICAL',
                streetAddress: 'Ilica 1',
                city: 'Zagreb',
                postalCode: '10000',
                latitude: '45.813',
                longitude: '15.977',
                onlineUrl: '',
              },
            }}
            artFields={[
              {
                id: artFieldId,
                code: 'slikarstvo',
                name: 'Slikarstvo',
                active: true,
                position: 1,
              },
            ]}
            opportunityTypes={[
              {
                id: typeId,
                code: 'natjecaj',
                name: 'Natječaj',
                active: true,
                position: 1,
              },
            ]}
          />
        ),
      },
    ],
    { initialEntries: ['/organizer/opportunities/new'] },
  )

  return render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('OpportunityForm', () => {
  beforeEach(() => {
    mutateAsync.mockReset()
    searchMutateAsync.mockReset()
    notify.mockReset()
  })

  it('prikazuje API 400 grešku uz polje na koje se odnosi', async () => {
    mutateAsync.mockRejectedValue(
      new ApiError(400, {
        code: 'VALIDATION_ERROR',
        message: 'Podaci nisu ispravni.',
        details: [{ field: 'title', issue: 'Naslov je već zauzet.' }],
      }),
    )

    renderForm()

    fireEvent.click(screen.getByRole('button', { name: 'Spremi' }))

    expect(await screen.findByText('Naslov je već zauzet.')).toBeInTheDocument()
    expect(mutateAsync).toHaveBeenCalledOnce()
    expect(notify).not.toHaveBeenCalled()
  })
  it('šalje odabrano zagrebačko vrijeme s ISO-8601 pomakom', async () => {
    mutateAsync.mockResolvedValue({
      id: '00000000-0000-4000-8000-000000000003',
      title: 'Prilika za umjetnike',
      description: 'Opis prilike.',
      type: {
        id: typeId,
        code: 'natjecaj',
        name: 'Natječaj',
      },
      artFields: [
        {
          id: artFieldId,
          code: 'slikarstvo',
          name: 'Slikarstvo',
        },
      ],
      deadline: '2099-01-10T23:00:00+01:00',
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
      status: 'PREPARATION',
      archived: false,
      hiddenByAdmin: false,
      organizer: {
        id: '00000000-0000-4000-8000-000000000004',
        displayName: 'Organizator',
        organizationName: null,
      },
      publishedAt: null,
      createdAt: '2098-01-01T12:00:00+01:00',
      updatedAt: '2098-01-01T12:00:00+01:00',
      hasResults: false,
      myApplicationId: null,
    })

    renderForm()

    fireEvent.click(screen.getByRole('button', { name: 'Odaberi Rok prijave' }))
    fireEvent.click(screen.getByRole('button', { name: 'Spremi' }))

    await waitFor(() => {
      expect(mutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: 'create',
          input: expect.objectContaining({
            deadline: '2099-01-10T23:00:00+01:00',
          }),
        }),
      )
    })
  })
  it('normalizira API datume u zagrebačko vrijeme pri uređivanju', () => {
    const opportunity: OpportunityDetail = {
      id: '00000000-0000-4000-8000-000000000003',
      title: 'Prilika za umjetnike',
      description: 'Opis prilike.',
      type: {
        id: typeId,
        code: 'natjecaj',
        name: 'Natječaj',
      },
      artFields: [
        {
          id: artFieldId,
          code: 'slikarstvo',
          name: 'Slikarstvo',
        },
      ],
      deadline: '2099-01-10T22:00:00.123Z',
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
      eventStart: '2099-07-10T21:00:00.123Z',
      eventEnd: '2099-07-10T22:00:00.456Z',
      status: 'PREPARATION',
      archived: false,
      hiddenByAdmin: false,
      organizer: {
        id: '00000000-0000-4000-8000-000000000004',
        displayName: 'Organizator',
        organizationName: null,
      },
      publishedAt: null,
      createdAt: '2098-01-01T12:00:00+01:00',
      updatedAt: '2098-01-01T12:00:00+01:00',
      hasResults: false,
      myApplicationId: null,
    }

    const values = toOpportunityFormValues(opportunity)

    expect(values.deadline).toBe('2099-01-10T23:00:00+01:00')
    expect(values.eventStart).toBe('2099-07-10T23:00:00+02:00')
    expect(values.eventEnd).toBe('2099-07-11T00:00:00+02:00')
    expect(createOpportunitySchema(false).safeParse(values).success).toBe(true)
  })
})
