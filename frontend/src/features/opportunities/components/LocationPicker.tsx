import { useState } from 'react'
import {
  Alert,
  Box,
  Button,
  FormControl,
  FormControlLabel,
  FormLabel,
  Radio,
  RadioGroup,
  Stack,
  TextField,
} from '@mui/material'
import {
  Controller,
  useWatch,
  type Control,
  type Path,
  type UseFormSetError,
  type UseFormSetValue,
} from 'react-hook-form'
import { ApiError } from '../../../api/client'
import { useNotify } from '../../../components/state/useNotify'
import { hr } from '../../../i18n/hr'
import {
  useSearchGeocoding,
  type GeocodingCandidate,
} from '../opportunityQueries'
import type { OpportunityFormValues } from '../types'
import { DraggableMarkerMap } from './map'

type LocationPickerProps = {
  control: Control<OpportunityFormValues>
  setError: UseFormSetError<OpportunityFormValues>
  setValue: UseFormSetValue<OpportunityFormValues>
}

const DEFAULT_CENTER = { latitude: 45.1, longitude: 15.2 }

const geocodingFields: Record<string, Path<OpportunityFormValues>> = {
  street: 'location.streetAddress',
  streetAddress: 'location.streetAddress',
  city: 'location.city',
  postalCode: 'location.postalCode',
}

export function LocationPicker({
  control,
  setError,
  setValue,
}: LocationPickerProps) {
  const location = useWatch({ control, name: 'location' })
  const search = useSearchGeocoding()
  const notify = useNotify()
  const [candidates, setCandidates] = useState<GeocodingCandidate[]>([])
  const [selectedCandidate, setSelectedCandidate] = useState('')
  const [searchMessage, setSearchMessage] = useState('')
  const [searchedAddress, setSearchedAddress] = useState<string | null>(null)

  const addressKey = [
    location.streetAddress.trim(),
    location.city.trim(),
    location.postalCode.trim(),
  ].join('\u0000')

  const latitude = Number(location.latitude)
  const longitude = Number(location.longitude)
  const hasMarker =
    location.latitude.trim() !== '' &&
    location.longitude.trim() !== '' &&
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180

  const marker = hasMarker ? { latitude, longitude } : null
  const addressChanged =
    marker !== null &&
    searchedAddress !== null &&
    searchedAddress !== addressKey

  const setCoordinates = (coordinates: {
    latitude: number
    longitude: number
  }) => {
    setValue('location.latitude', String(coordinates.latitude), {
      shouldDirty: true,
      shouldValidate: true,
    })
    setValue('location.longitude', String(coordinates.longitude), {
      shouldDirty: true,
      shouldValidate: true,
    })
  }

  const selectCandidate = (candidate: GeocodingCandidate, index: number) => {
    setSelectedCandidate(String(index))
    setCoordinates({
      latitude: candidate.latitude,
      longitude: candidate.longitude,
    })

    if (!location.city.trim() && candidate.city) {
      setValue('location.city', candidate.city, {
        shouldDirty: true,
        shouldValidate: true,
      })
    }

    if (!location.postalCode.trim() && candidate.postalCode) {
      setValue('location.postalCode', candidate.postalCode, {
        shouldDirty: true,
        shouldValidate: true,
      })
    }
  }

  const searchAddress = async () => {
    setSearchMessage('')
    setCandidates([])
    setSelectedCandidate('')

    try {
      const result = await search.mutateAsync({
        street: location.streetAddress.trim(),
        city: location.city.trim(),
        postalCode: location.postalCode.trim(),
      })

      setCandidates(result.data)
      setSearchedAddress(addressKey)

      if (result.data.length === 0) {
        setSearchMessage(hr.opportunities.locationPicker.noResults)
        return
      }

      selectCandidate(result.data[0], 0)
    } catch (error) {
      if (!(error instanceof ApiError)) {
        notify(
          error instanceof Error ? error.message : hr.errors.generic,
          'error',
        )
        return
      }

      if (error.status === 429 && error.code === 'RATE_LIMITED') {
        setSearchMessage(hr.opportunities.locationPicker.rateLimited)
        return
      }

      if (error.status === 502 && error.code === 'GEOCODING_UNAVAILABLE') {
        setSearchMessage(hr.opportunities.locationPicker.unavailable)
        return
      }

      if (error.status === 400 && error.code === 'VALIDATION_ERROR') {
        let mappedCount = 0

        for (const detail of error.details) {
          const field = geocodingFields[detail.field]
          if (!field) continue

          setError(field, { type: 'server', message: detail.issue })
          mappedCount += 1
        }

        if (mappedCount === error.details.length && mappedCount > 0) return
      }

      setSearchMessage(error.message)
    }
  }

  return (
    <Stack spacing={2}>
      <Controller
        control={control}
        name="location.type"
        render={({ field }) => (
          <FormControl>
            <FormLabel>{hr.opportunities.locationPicker.type}</FormLabel>
            <RadioGroup
              row
              value={field.value}
              onChange={(_, value) => field.onChange(value)}
            >
              <FormControlLabel
                value="PHYSICAL"
                control={<Radio />}
                label={hr.opportunities.locationPicker.physical}
              />
              <FormControlLabel
                value="ONLINE"
                control={<Radio />}
                label={hr.opportunities.locationPicker.online}
              />
            </RadioGroup>
          </FormControl>
        )}
      />

      {location.type === 'PHYSICAL' ? (
        <>
          <Controller
            control={control}
            name="location.streetAddress"
            render={({ field, fieldState }) => (
              <TextField
                {...field}
                label={hr.opportunities.locationPicker.street}
                error={fieldState.invalid}
                helperText={fieldState.error?.message}
                fullWidth
                slotProps={{ htmlInput: { maxLength: 200 } }}
              />
            )}
          />

          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
              gap: 2,
            }}
          >
            <Controller
              control={control}
              name="location.postalCode"
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label={hr.opportunities.locationPicker.postalCode}
                  error={fieldState.invalid}
                  helperText={fieldState.error?.message}
                  fullWidth
                  slotProps={{ htmlInput: { maxLength: 10 } }}
                />
              )}
            />

            <Controller
              control={control}
              name="location.city"
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label={hr.opportunities.locationPicker.city}
                  error={fieldState.invalid}
                  helperText={fieldState.error?.message}
                  fullWidth
                  slotProps={{ htmlInput: { maxLength: 100 } }}
                />
              )}
            />
          </Box>

          <Button
            type="button"
            variant="outlined"
            disabled={
              search.isPending ||
              location.streetAddress.trim().length < 3 ||
              location.city.trim().length < 2
            }
            onClick={searchAddress}
          >
            {search.isPending
              ? hr.opportunities.locationPicker.searching
              : hr.opportunities.locationPicker.findOnMap}
          </Button>

          {searchMessage && <Alert severity="info">{searchMessage}</Alert>}

          {candidates.length > 0 && (
            <FormControl>
              <FormLabel>
                {hr.opportunities.locationPicker.candidates}
              </FormLabel>
              <RadioGroup
                value={selectedCandidate}
                onChange={(_, value) => {
                  const index = Number(value)
                  const candidate = candidates[index]
                  if (candidate) selectCandidate(candidate, index)
                }}
              >
                {candidates.map((candidate, index) => (
                  <FormControlLabel
                    key={`${candidate.latitude},${candidate.longitude}`}
                    value={String(index)}
                    control={<Radio />}
                    label={candidate.displayName}
                  />
                ))}
              </RadioGroup>
            </FormControl>
          )}

          {addressChanged && (
            <Alert severity="info">
              {hr.opportunities.locationPicker.addressChanged}
            </Alert>
          )}

          <Controller
            control={control}
            name="location.latitude"
            render={({ fieldState: latitudeState }) => (
              <Controller
                control={control}
                name="location.longitude"
                render={({ fieldState: longitudeState }) => (
                  <>
                    {(latitudeState.error || longitudeState.error) && (
                      <Alert severity="error">
                        {latitudeState.error?.message ??
                          longitudeState.error?.message}
                      </Alert>
                    )}

                    <DraggableMarkerMap
                      value={marker}
                      center={marker ?? DEFAULT_CENTER}
                      onChange={setCoordinates}
                    />
                  </>
                )}
              />
            )}
          />
        </>
      ) : (
        <Controller
          control={control}
          name="location.onlineUrl"
          render={({ field, fieldState }) => (
            <TextField
              {...field}
              label={hr.opportunities.locationPicker.onlineUrl}
              error={fieldState.invalid}
              helperText={fieldState.error?.message}
              fullWidth
              slotProps={{ htmlInput: { maxLength: 1024 } }}
            />
          )}
        />
      )}
    </Stack>
  )
}
