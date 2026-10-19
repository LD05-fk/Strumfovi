import { Box } from '@mui/material'

export function GoogleMark() {
  return (
    <Box
      component="span"
      aria-hidden="true"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 16,
        height: 16,
        fontSize: 16,
        lineHeight: 1,
        fontWeight: 700,
        fontFamily: 'Arial, sans-serif',
        background:
          'conic-gradient(from -45deg, #4285F4 0deg 90deg, #34A853 90deg 180deg, #FBBC05 180deg 270deg, #EA4335 270deg)',
        backgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
      }}
    >
      G
    </Box>
  )
}
