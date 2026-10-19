import { Box, Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'
import { GoogleMark } from '../../components/layout/GoogleMark'
import { usePageTitle } from '../../components/layout/usePageTitle'
import { hr } from '../../i18n/hr'

export function HomePage() {
  usePageTitle(hr.home.title)

  return (
    <Box
      sx={{
        pt: { xs: 5.5, sm: 5, md: 6 },
        pb: 4,
        textAlign: 'center',
      }}
    >
      <Typography
        component="h1"
        variant="h1"
        sx={{
          mb: 0.5,
          fontSize: { xs: '1.75rem', sm: '2.25rem' },
          lineHeight: { xs: '2.25rem', sm: '2.75rem' },
        }}
      >
        {hr.home.title}
      </Typography>
      <Typography
        component="p"
        sx={{
          m: 0,
          color: 'text.secondary',
          fontSize: { xs: '1.125rem', sm: '1.25rem' },
          lineHeight: { xs: '1.75rem', sm: '1.75rem' },
        }}
      >
        {hr.home.description}
      </Typography>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1.5}
        sx={{ mt: 4, px: 1, justifyContent: 'center', alignItems: 'center' }}
      >
        <Button
          component="a"
          href="/oauth2/authorization/google"
          variant="outlined"
          sx={{
            minWidth: { xs: 194, sm: 194 },
            minHeight: 44,
            color: 'text.primary',
            borderColor: '#78716C',
            bgcolor: 'background.paper',
            '&:hover': {
              borderColor: 'text.primary',
              bgcolor: 'background.paper',
            },
          }}
        >
          <GoogleMark />
          <Box component="span" sx={{ ml: 1 }}>
            Prijava s Googleom
          </Box>
        </Button>
        <Button
          component={RouterLink}
          to="/opportunities"
          variant="outlined"
          sx={{
            minWidth: 151,
            minHeight: 44,
            borderWidth: 2,
            '&:hover': { borderWidth: 2 },
          }}
        >
          {hr.home.browseOpportunities}
        </Button>
      </Stack>
    </Box>
  )
}
