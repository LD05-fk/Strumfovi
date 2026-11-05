import { zodResolver } from '@hookform/resolvers/zod'
import {
  Autocomplete,
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker'
import { useEffect, useRef } from 'react'
import { Controller, useForm, type Control, type Path } from 'react-hook-form'
import { useBlocker, useNavigate } from 'react-router'
import { ApiError } from '../../../api/client'
import { dayjs, OPPORTUNITY_TIME_ZONE } from '../../../app/dayjs'
import { useNotify } from '../../../components/state/useNotify'
import { hr } from '../../../i18n/hr'
import { useSaveOpportunity } from '../opportunityQueries'
import {
  toOpportunityFormValues,
  toOpportunityInput,
  type CatalogOption,
  type CatalogReference,
  type OpportunityFormValues,
} from '../types'
import { createOpportunitySchema } from './opportunitySchema'

type OpportunityFormProps = {
  isCreating: boolean
  initialValues: OpportunityFormValues
  artFields: CatalogOption[]
  opportunityTypes: CatalogOption[]
  existingType?: CatalogReference
  existingArtFields?: CatalogReference[]
  opportunityId?: string
}

type OpportunityDateFieldProps = {
  control: Control<OpportunityFormValues>
  name: 'deadline' | 'eventStart' | 'eventEnd'
  label: string
}

const serverFields = [
  'title',
  'description',
  'typeId',
  'artFieldIds',
  'deadline',
  'criteria',
  'prize',
  'eventStart',
  'eventEnd',
  'location.type',
  'location.streetAddress',
  'location.city',
  'location.postalCode',
  'location.latitude',
  'location.longitude',
  'location.onlineUrl',
] as const satisfies readonly Path<OpportunityFormValues>[]

function mapServerField(field: string): Path<OpportunityFormValues> | null {
  const normalized = field.replace(/\[(\d+)\]/g, '.$1')
  return serverFields.find((candidate) => candidate === normalized) ?? null
}

function OpportunityDateField({
  control,
  name,
  label,
}: OpportunityDateFieldProps) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <DateTimePicker
          label={label}
          timezone={OPPORTUNITY_TIME_ZONE}
          ampm={false}
          value={
            field.value ? dayjs(field.value).tz(OPPORTUNITY_TIME_ZONE) : null
          }
          onChange={(value) => {
            if (!value) {
              field.onChange('')
            } else if (value.isValid()) {
              field.onChange(
                value.tz(OPPORTUNITY_TIME_ZONE).format('YYYY-MM-DDTHH:mm:ssZ'),
              )
            }
          }}
          slotProps={{
            textField: {
              fullWidth: true,
              error: fieldState.invalid,
              helperText: fieldState.error?.message,
              inputRef: field.ref,
              onBlur: field.onBlur,
            },
          }}
        />
      )}
    />
  )
}

export function OpportunityForm({
  isCreating,
  initialValues,
  artFields,
  opportunityTypes,
  existingType,
  existingArtFields = [],
  opportunityId,
}: OpportunityFormProps) {
  const {
    control,
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<OpportunityFormValues>({
    resolver: zodResolver(createOpportunitySchema(isCreating)),
    defaultValues: initialValues,
  })

  const saveOpportunity = useSaveOpportunity()
  const navigate = useNavigate()
  const notify = useNotify()
  const allowNavigationRef = useRef(false)
  const blocker = useBlocker(() => isDirty && !allowNavigationRef.current)

  useEffect(() => {
    if (!isDirty) return

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty])

  const submitForm = async (values: OpportunityFormValues) => {
    const input = toOpportunityInput(values)

    try {
      if (!isCreating && !opportunityId) {
        notify(hr.errors.generic, 'error')
        return
      }

      const saved = isCreating
        ? await saveOpportunity.mutateAsync({ kind: 'create', input })
        : await saveOpportunity.mutateAsync({
            kind: 'update',
            opportunityId: opportunityId!,
            input,
          })

      allowNavigationRef.current = true
      reset(toOpportunityFormValues(saved))
      notify(hr.opportunities.form.saved, 'success')
      navigate(`/opportunities/${saved.id}`)
    } catch (error) {
      if (
        error instanceof ApiError &&
        error.status === 400 &&
        error.code === 'VALIDATION_ERROR'
      ) {
        let mappedErrors = 0
        let unmappedErrors = 0

        for (const detail of error.details) {
          const field = mapServerField(detail.field)
          if (!field) {
            unmappedErrors += 1
            continue
          }

          setError(field, { type: 'server', message: detail.issue })
          mappedErrors += 1
        }

        if (mappedErrors > 0 && unmappedErrors === 0) return
      }

      notify(
        error instanceof Error ? error.message : hr.errors.generic,
        'error',
      )
    }
  }

  const typeOptions = [
    ...opportunityTypes,
    ...(existingType &&
    !opportunityTypes.some((option) => option.id === existingType.id)
      ? [
          {
            ...existingType,
            active: false,
            position: Number.MAX_SAFE_INTEGER,
          },
        ]
      : []),
  ]

  const artFieldOptions = [
    ...artFields,
    ...existingArtFields
      .filter(
        (existing) => !artFields.some((option) => option.id === existing.id),
      )
      .map((field) => ({
        ...field,
        active: false,
        position: Number.MAX_SAFE_INTEGER,
      })),
  ]

  const inactiveArtFieldIds = new Set(
    artFieldOptions
      .filter((option) => !option.active)
      .map((option) => option.id),
  )

  return (
    <>
      <Card
        component="form"
        noValidate
        onSubmit={handleSubmit(submitForm)}
        sx={{ p: { xs: 2, sm: 3 } }}
      >
        <Stack spacing={2}>
          <TextField
            {...register('title')}
            label={hr.opportunities.form.title}
            error={Boolean(errors.title)}
            helperText={errors.title?.message}
            fullWidth
          />

          <TextField
            {...register('description')}
            label={hr.opportunities.form.description}
            error={Boolean(errors.description)}
            helperText={errors.description?.message}
            fullWidth
            multiline
            minRows={4}
          />

          <Controller
            control={control}
            name="typeId"
            render={({ field, fieldState }) => {
              const selectedType =
                typeOptions.find((option) => option.id === field.value) ?? null

              return (
                <Autocomplete
                  options={typeOptions}
                  value={selectedType}
                  getOptionLabel={(option) =>
                    option.active
                      ? option.name
                      : `${option.name} (${hr.opportunities.form.inactive})`
                  }
                  getOptionDisabled={(option) => !option.active}
                  isOptionEqualToValue={(option, value) =>
                    option.id === value.id
                  }
                  disableClearable={Boolean(
                    selectedType && !selectedType.active,
                  )}
                  noOptionsText={hr.opportunities.form.typesEmpty}
                  onChange={(_, option) => field.onChange(option?.id ?? '')}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={hr.opportunities.form.opportunityType}
                      error={fieldState.invalid}
                      helperText={fieldState.error?.message}
                    />
                  )}
                />
              )
            }}
          />

          <Controller
            control={control}
            name="artFieldIds"
            render={({ field, fieldState }) => {
              const selected = artFieldOptions.filter((option) =>
                field.value.includes(option.id),
              )

              return (
                <Autocomplete
                  multiple
                  disableCloseOnSelect
                  options={artFieldOptions}
                  value={selected}
                  getOptionLabel={(option) =>
                    option.active
                      ? option.name
                      : `${option.name} (${hr.opportunities.form.inactive})`
                  }
                  getOptionDisabled={(option) =>
                    !option.active ||
                    (field.value.length >= 10 &&
                      !field.value.includes(option.id))
                  }
                  isOptionEqualToValue={(option, value) =>
                    option.id === value.id
                  }
                  noOptionsText={hr.artistProfile.artFieldsEmpty}
                  onChange={(_, options) => {
                    const selectedIds = options.map((option) => option.id)
                    const retainedInactiveIds = field.value.filter((id) =>
                      inactiveArtFieldIds.has(id),
                    )
                    field.onChange([
                      ...new Set([...selectedIds, ...retainedInactiveIds]),
                    ])
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={hr.opportunities.form.artFields}
                      error={fieldState.invalid}
                      helperText={
                        fieldState.error?.message ??
                        `${hr.artistProfile.artFieldsHint} (${field.value.length}/10)`
                      }
                    />
                  )}
                />
              )
            }}
          />

          <OpportunityDateField
            control={control}
            name="deadline"
            label={hr.opportunities.form.deadline}
          />

          <OpportunityDateField
            control={control}
            name="eventStart"
            label={hr.opportunities.form.eventStart}
          />

          <OpportunityDateField
            control={control}
            name="eventEnd"
            label={hr.opportunities.form.eventEnd}
          />

          <TextField
            {...register('criteria')}
            label={hr.opportunities.form.criteria}
            error={Boolean(errors.criteria)}
            helperText={errors.criteria?.message}
            fullWidth
            multiline
            minRows={3}
            slotProps={{ htmlInput: { maxLength: 5000 } }}
          />

          <TextField
            {...register('prize')}
            label={hr.opportunities.form.prize}
            error={Boolean(errors.prize)}
            helperText={errors.prize?.message}
            fullWidth
            multiline
            minRows={2}
            slotProps={{ htmlInput: { maxLength: 2000 } }}
          />

          <Box
            component="section"
            aria-labelledby="opportunity-location-heading"
            sx={{
              p: 2,
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1,
            }}
          >
            <Typography
              id="opportunity-location-heading"
              component="h2"
              variant="h6"
            >
              {hr.opportunities.form.location}
            </Typography>
            <Typography color="text.secondary">
              {hr.opportunities.form.locationPlaceholder}
            </Typography>
          </Box>

          <Box
            component="section"
            aria-labelledby="application-requirements-heading"
            sx={{
              p: 2,
              border: '2px dashed',
              borderColor: 'divider',
              borderRadius: 1,
              boxShadow: 'none',
            }}
          >
            <Typography
              id="application-requirements-heading"
              component="h2"
              variant="h6"
            >
              {hr.opportunities.form.applicationRequirements}
            </Typography>
            <Typography color="text.secondary">
              {hr.opportunities.form.applicationRequirementsPlaceholder}
            </Typography>
          </Box>

          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1}
            sx={{ alignItems: { xs: 'stretch', sm: 'center' } }}
          >
            <Button type="submit" variant="contained" disabled={isSubmitting}>
              {hr.common.save}
            </Button>
            <Button
              type="button"
              variant="outlined"
              disabled={isSubmitting}
              onClick={() => navigate('/my-opportunities')}
            >
              {hr.common.cancel}
            </Button>
          </Stack>
        </Stack>
      </Card>

      <Dialog
        open={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        aria-labelledby="opportunity-leave-title"
        aria-describedby="opportunity-leave-description"
      >
        <DialogTitle id="opportunity-leave-title">
          {hr.opportunities.form.leaveTitle}
        </DialogTitle>
        <DialogContent id="opportunity-leave-description">
          {hr.opportunities.form.leaveMessage}
        </DialogContent>
        <DialogActions sx={{ flexWrap: 'wrap', p: 2 }}>
          <Button onClick={() => blocker.reset?.()}>
            {hr.opportunities.form.continueEditing}
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => {
              allowNavigationRef.current = true
              blocker.proceed?.()
            }}
          >
            {hr.opportunities.form.discardChanges}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
