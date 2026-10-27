import { Box, Button } from '@mui/material'
import { GoogleMark } from './layout/GoogleMark'

export function GoogleSignInButton() {
  return (
    <Button
      component="a"
      href="/oauth2/authorization/google"
      variant="outlined"
      size="small"
      aria-label="Prijava s Googleom"
      data-testid="google-sign-in"
      sx={{
        minWidth: { xs: 40, sm: 0 },
        minHeight: 36,
        px: { xs: 1, sm: 2 },
        whiteSpace: 'nowrap',
        color: 'text.primary',
        bgcolor: 'background.paper',
        borderColor: '#78716C',
        '&:hover': {
          borderColor: 'text.primary',
          bgcolor: 'background.paper',
        },
      }}
    >
      <GoogleMark />
      <Box
        component="span"
        sx={{ display: { xs: 'none', sm: 'inline' }, ml: 1 }}
      >
        Prijava s Googleom
      </Box>
    </Button>
  )
}
