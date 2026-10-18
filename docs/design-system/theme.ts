import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  spacing: 8,
  breakpoints: { values: { xs: 0, sm: 600, md: 900, lg: 1200, xl: 1536 } },
  palette: {
    primary: { main: '#4338CA', dark: '#3730A3', light: '#EEF2FF', contrastText: '#FFFFFF' },
    secondary: { main: '#BE185D', contrastText: '#FFFFFF' },
    error: { main: '#B91C1C' },
    warning: { main: '#B45309' },
    success: { main: '#15803D' },
    info: { main: '#1D4ED8' },
    background: { default: '#FAFAF9', paper: '#FFFFFF' },
    text: { primary: '#1C1917', secondary: '#57534E' },
    divider: '#D6D3D1',
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: '"Inter", system-ui, sans-serif',
    h1: { fontSize: '2.25rem', lineHeight: '2.75rem', fontWeight: 700, '@media (max-width:599.95px)': { fontSize: '1.75rem', lineHeight: '2.25rem' } },
    h2: { fontSize: '1.875rem', lineHeight: '2.375rem', fontWeight: 700, '@media (max-width:599.95px)': { fontSize: '1.5rem', lineHeight: '2rem' } },
    h3: { fontSize: '1.5rem', lineHeight: '2rem', fontWeight: 600 },
    h4: { fontSize: '1.25rem', lineHeight: '1.75rem', fontWeight: 600 },
    h5: { fontSize: '1.125rem', lineHeight: '1.625rem', fontWeight: 600 },
    h6: { fontSize: '1rem', lineHeight: '1.5rem', fontWeight: 600 },
    body1: { fontSize: '1rem', lineHeight: '1.5rem' },
    body2: { fontSize: '0.875rem', lineHeight: '1.25rem' },
    button: { fontSize: '0.875rem', lineHeight: '1.25rem', fontWeight: 600, textTransform: 'none' },
    caption: { fontSize: '0.75rem', lineHeight: '1rem' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { minHeight: 44, borderRadius: 8 } },
    },
    MuiCard: {
      styleOverrides: {
        root: { borderRadius: 12, boxShadow: '0 1px 3px rgba(28,25,23,.08)' },
      },
    },
    MuiChip: { styleOverrides: { root: { borderRadius: 16, fontWeight: 600 } } },
    MuiDialog: { styleOverrides: { paper: { borderRadius: 16 } } },
    MuiCssBaseline: {
      styleOverrides: {
        ':focus-visible': { outline: '3px solid #4338CA', outlineOffset: 2 },
      },
    },
  },
});
