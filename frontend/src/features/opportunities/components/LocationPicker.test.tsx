import { fireEvent, screen } from '@testing-library/react'
import { useForm, useWatch } from 'react-hook-form'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/client'
import { hr } from '../../../i18n/hr'
import { renderWithProviders } from '../../../test/renderWithProviders'
import {
  emptyOpportunityFormValues,
  type OpportunityFormValues,
} from '../types'
import { LocationPicker } from './LocationPicker'

const { mutateAsync, notify } = vi.hoisted(() => ({
  mutateAsync: vi.fn(),
  notify: vi.fn(),
}))

vi.mock('../opportunityQueries', () => ({
  useSearchGeocoding: () => ({ mutateAsync, isPending: false }),
}))

vi.mock('../../../components/state/useNotify', () => ({
  useNotify: () => notify,
}))

vi.mock('./map', () => ({
  DraggableMarkerMap: ({
    onChange,
  }: {
    onChange: (coordinates: { latitude: number; longitude: number }) => void
  }) => (
    <button
      type="button"
      onClick={() => onChange({ latitude: 45.8, longitude: 15.9 })}
    >
      Simuliraj klik na kartu
    </button>
  ),
}))

function LocationPickerTestForm({
  initialValues = {
    ...emptyOpportunityFormValues(),
    location: {
      ...emptyOpportunityFormValues().location,
      streetAddress: 'Ilica 1',
      city: 'Zagreb',
      postalCode: '',
    },
  },
}: {
  initialValues?: OpportunityFormValues
}) {
  const { control, setError, setValue } = useForm<OpportunityFormValues>({
    defaultValues: initialValues,
  })
  const location = useWatch({ control, name: 'location' })

  return (
    <>
      <LocationPicker
        control={control}
        setError={setError}
        setValue={setValue}
      />
      <output data-testid="latitude">{location.latitude}</output>
      <output data-testid="longitude">{location.longitude}</output>
      <output data-testid="city">{location.city}</output>
      <output data-testid="postal-code">{location.postalCode}</output>
    </>
  )
}

describe('LocationPicker', () => {
  beforeEach(() => {
    mutateAsync.mockReset()
    notify.mockReset()
  })

  it('odabire prvi rezultat, postavlja koordinate i popunjava prazan grad i poštanski broj', async () => {
    mutateAsync.mockResolvedValue({
      data: [
        {
          displayName: 'Ilica 1, Zagreb',
          latitude: 45.8123,
          longitude: 15.978,
          city: 'Zagreb',
          postalCode: '10000',
        },
        {
          displayName: 'Ilica 1, druga lokacija',
          latitude: 45.82,
          longitude: 15.99,
          city: 'Zagreb',
          postalCode: '10000',
        },
      ],
    })

    renderWithProviders(<LocationPickerTestForm />)

    fireEvent.click(
      screen.getByRole('button', {
        name: hr.opportunities.locationPicker.findOnMap,
      }),
    )

    expect(
      await screen.findByRole('radio', { name: 'Ilica 1, Zagreb' }),
    ).toBeChecked()
    expect(screen.getByTestId('latitude')).toHaveTextContent('45.8123')
    expect(screen.getByTestId('longitude')).toHaveTextContent('15.978')
    expect(screen.getByTestId('city')).toHaveTextContent('Zagreb')
    expect(screen.getByTestId('postal-code')).toHaveTextContent('10000')

    fireEvent.click(
      screen.getByRole('radio', { name: 'Ilica 1, druga lokacija' }),
    )

    expect(screen.getByTestId('latitude')).toHaveTextContent('45.82')
    expect(screen.getByTestId('longitude')).toHaveTextContent('15.99')
    expect(
      screen.queryByText(hr.opportunities.locationPicker.addressChanged),
    ).not.toBeInTheDocument()
  })

  it('prikazuje poruku kad geokodiranje ne pronađe adresu', async () => {
    mutateAsync.mockResolvedValue({ data: [] })

    renderWithProviders(<LocationPickerTestForm />)

    fireEvent.click(
      screen.getByRole('button', {
        name: hr.opportunities.locationPicker.findOnMap,
      }),
    )

    expect(
      await screen.findByText(hr.opportunities.locationPicker.noResults),
    ).toBeInTheDocument()
  })

  it('prikazuje posebnu poruku za HTTP 502', async () => {
    mutateAsync.mockRejectedValue(
      new ApiError(502, {
        code: 'GEOCODING_UNAVAILABLE',
        message: 'Geokodiranje nije dostupno.',
      }),
    )

    renderWithProviders(<LocationPickerTestForm />)

    fireEvent.click(
      screen.getByRole('button', {
        name: hr.opportunities.locationPicker.findOnMap,
      }),
    )

    expect(
      await screen.findByText(hr.opportunities.locationPicker.unavailable),
    ).toBeInTheDocument()
  })

  it('prikazuje posebnu poruku za HTTP 429', async () => {
    mutateAsync.mockRejectedValue(
      new ApiError(429, {
        code: 'RATE_LIMITED',
        message: 'Previše zahtjeva.',
      }),
    )

    renderWithProviders(<LocationPickerTestForm />)

    fireEvent.click(
      screen.getByRole('button', {
        name: hr.opportunities.locationPicker.findOnMap,
      }),
    )

    expect(
      await screen.findByText(hr.opportunities.locationPicker.rateLimited),
    ).toBeInTheDocument()
  })

  it('ručni klik na karti postavlja koordinate oznake', () => {
    renderWithProviders(<LocationPickerTestForm />)

    fireEvent.click(
      screen.getByRole('button', { name: 'Simuliraj klik na kartu' }),
    )

    expect(screen.getByTestId('latitude')).toHaveTextContent('45.8')
    expect(screen.getByTestId('longitude')).toHaveTextContent('15.9')
  })
})
