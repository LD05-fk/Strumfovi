import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'
import { hr } from '../../i18n/hr'
import { usePageTitle } from '../layout/usePageTitle'

export function ForbiddenPage() {
  usePageTitle('403')

  return (
    <Stack
      spacing={2}
      sx={{ alignItems: 'center', textAlign: 'center', pt: 4 }}
    >
      <Typography component="p" variant="h1" color="primary.main">
        403
      </Typography>
      <Typography component="h1" variant="h4">
        {hr.errors.forbidden}
      </Typography>
      <Button component={RouterLink} to="/opportunities" variant="contained">
        {hr.common.back}
      </Button>
    </Stack>
  )
}
