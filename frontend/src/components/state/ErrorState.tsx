import { Alert, Button, Stack } from '@mui/material'
import type { ApiError } from '../../api/client'
import { hr } from '../../i18n/hr'

type ErrorStateProps = {
  error?: ApiError | null
  message?: string
  onRetry?: () => void
}

export function ErrorState({ error, message, onRetry }: ErrorStateProps) {
  const errorMessage = message ?? error?.message ?? hr.errors.generic

  return (
    <Stack spacing={2} sx={{ alignItems: 'center', textAlign: 'center' }}>
      <Alert severity="error" role="alert">
        {errorMessage}
      </Alert>
      {onRetry && (
        <Button variant="contained" onClick={onRetry}>
          {hr.common.retry}
        </Button>
      )}
    </Stack>
  )
}
