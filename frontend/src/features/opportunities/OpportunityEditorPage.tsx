import { Alert, Box, Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router'
import { ApiError } from '../../api/client'
import { usePageTitle } from '../../components/layout/usePageTitle'
import { ErrorState } from '../../components/state/ErrorState'
import { LoadingState } from '../../components/state/LoadingState'
import { useArtFields } from '../artist-profile/artistProfileQueries'
import { hr } from '../../i18n/hr'
import { emptyOpportunityFormValues, toOpportunityFormValues } from './types'
import { useOpportunity, useOpportunityTypes } from './opportunityQueries'
import { OpportunityForm } from './components/OpportunityForm'

type OpportunityEditorPageProps = {
  mode: 'create' | 'edit'
}

export function OpportunityEditorPage({ mode }: OpportunityEditorPageProps) {
  const { opportunityId } = useParams()
  const isCreating = mode === 'create'

  usePageTitle(
    isCreating
      ? hr.opportunities.form.createTitle
      : hr.opportunities.form.editTitle,
  )

  const artFields = useArtFields()
  const opportunityTypes = useOpportunityTypes()
  const opportunity = useOpportunity(isCreating ? '' : (opportunityId ?? ''))

  if (
    artFields.isPending ||
    opportunityTypes.isPending ||
    (!isCreating && opportunity.isPending)
  ) {
    return <LoadingState lines={5} />
  }

  if (!isCreating && opportunity.error) {
    const error = opportunity.error

    if (
      error instanceof ApiError &&
      (error.status === 403 || error.status === 404)
    ) {
      return (
        <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
          <Alert severity="error" role="alert">
            {error.status === 403
              ? hr.opportunities.form.accessDenied
              : hr.opportunities.form.notFound}
          </Alert>
          <Button
            component={RouterLink}
            to="/my-opportunities"
            variant="outlined"
          >
            {hr.navigation.organizerOpportunities}
          </Button>
        </Stack>
      )
    }

    return (
      <ErrorState
        error={error instanceof ApiError ? error : undefined}
        onRetry={() => void opportunity.refetch()}
      />
    )
  }

  const catalogError = artFields.error ?? opportunityTypes.error

  if (catalogError) {
    return (
      <ErrorState
        error={catalogError instanceof ApiError ? catalogError : undefined}
        onRetry={() => {
          void artFields.refetch()
          void opportunityTypes.refetch()
        }}
      />
    )
  }

  if (!artFields.data || !opportunityTypes.data) {
    return <ErrorState message={hr.errors.generic} />
  }

  const commonProps = {
    artFields: artFields.data.data,
    opportunityTypes: opportunityTypes.data.data,
  }

  if (isCreating) {
    return (
      <Box sx={{ width: '100%', maxWidth: 800, mx: 'auto' }}>
        <Typography component="h1" variant="h2" sx={{ mb: 2 }}>
          {hr.opportunities.form.createTitle}
        </Typography>
        <OpportunityForm
          {...commonProps}
          isCreating
          initialValues={emptyOpportunityFormValues()}
        />
      </Box>
    )
  }

  if (!opportunity.data) {
    return <ErrorState message={hr.errors.generic} />
  }

  return (
    <Box sx={{ width: '100%', maxWidth: 800, mx: 'auto' }}>
      <Typography component="h1" variant="h2" sx={{ mb: 2 }}>
        {hr.opportunities.form.editTitle}
      </Typography>
      <OpportunityForm
        {...commonProps}
        isCreating={false}
        opportunityId={opportunity.data.id}
        initialValues={toOpportunityFormValues(opportunity.data)}
        existingType={opportunity.data.type}
        existingArtFields={opportunity.data.artFields}
      />
    </Box>
  )
}
