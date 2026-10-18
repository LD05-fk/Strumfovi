import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'
import { hr } from '../../i18n/hr'
import { usePageTitle } from '../layout/usePageTitle'

export function NotFoundPage() {
  usePageTitle('404')

  return (
    <Stack
      spacing={2}
      sx={{ alignItems: 'center', textAlign: 'center', pt: 4 }}
    >
      <Typography component="p" variant="h1" color="primary.main">
        404
      </Typography>
      <Typography component="h1" variant="h4">
        {hr.errors.notFound}
      </Typography>
      <Typography color="text.secondary">
        {hr.errors.pageNotFoundDescription}
      </Typography>
      <Button component={RouterLink} to="/opportunities" variant="contained">
        Na početnu
      </Button>
    </Stack>
  )
}
