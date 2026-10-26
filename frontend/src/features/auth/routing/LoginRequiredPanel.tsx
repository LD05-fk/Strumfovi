import { Paper, Stack, Typography } from '@mui/material'
import { GoogleSignInButton } from '../../../components/GoogleSignInButton'
import { hr } from '../../../i18n/hr'

export function LoginRequiredPanel() {
  return (
    <Paper
      data-testid="login-required"
      component="section"
      aria-labelledby="login-required-title"
      elevation={0}
      sx={{
        mx: 'auto',
        maxWidth: 520,
        p: { xs: 2, sm: 3 },
        border: 1,
        borderColor: 'divider',
      }}
    >
      <Stack spacing={2} sx={{ alignItems: 'center', textAlign: 'center' }}>
        <Typography id="login-required-title" component="h1" variant="h4">
          {hr.auth.loginRequiredTitle}
        </Typography>
        <Typography color="text.secondary">
          {hr.auth.loginRequiredDescription}
        </Typography>
        <GoogleSignInButton />
      </Stack>
    </Paper>
  )
}
