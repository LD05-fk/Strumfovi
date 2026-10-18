import MenuIcon from '@mui/icons-material/Menu'
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Drawer,
  IconButton,
  Link as MuiLink,
  Menu,
  MenuItem,
  Stack,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useState } from 'react'
import { Link as RouterLink, Outlet } from 'react-router'
import { hr } from '../../i18n/hr'
import { GoogleMark } from './GoogleMark'
import { NavItems } from './NavItems'
import type { NavigationRole } from './navigation'

export type { NavigationRole, UserRole } from './navigation'

type AppLayoutProps = {
  role?: NavigationRole
  onSignOut?: () => void
}

export function AppLayout({ role = null, onSignOut }: AppLayoutProps) {
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [accountAnchor, setAccountAnchor] = useState<HTMLElement | null>(null)
  const theme = useTheme()
  const desktop = useMediaQuery(theme.breakpoints.up('md'))
  const closeDrawer = () => setDrawerOpen(false)

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <MuiLink
        href="#main-content"
        sx={{
          position: 'absolute',
          zIndex: (theme) => theme.zIndex.tooltip + 1,
          top: 0,
          left: 0,
          p: 1.5,
          bgcolor: 'background.paper',
          transform: 'translateY(-150%)',
          '&:focus': { transform: 'translateY(0)' },
        }}
      >
        Preskoči na sadržaj
      </MuiLink>

      <AppBar
        position="static"
        color="inherit"
        elevation={0}
        sx={{ borderBottom: 1, borderColor: 'divider' }}
      >
        <Toolbar
          sx={{
            width: '100%',
            minHeight: '64px !important',
            gap: 1,
            px: { xs: 2, sm: 3, md: 3 },
          }}
        >
          {!desktop && (
            <IconButton
              aria-label="Otvori izbornik"
              aria-expanded={drawerOpen}
              aria-controls="mobile-navigation"
              onClick={() => setDrawerOpen(true)}
              edge="start"
              sx={{ minWidth: 44, minHeight: 44 }}
            >
              <MenuIcon />
            </IconButton>
          )}
          <Typography
            component={RouterLink}
            to="/"
            variant="h6"
            sx={{
              color: 'primary.main',
              fontWeight: 700,
              textDecoration: 'none',
              whiteSpace: 'nowrap',
            }}
          >
            {hr.appName}
          </Typography>

          {desktop && (
            <Box component="nav" aria-label="Glavna navigacija" sx={{ ml: 2 }}>
              <NavItems role={role} />
            </Box>
          )}

          <Box sx={{ flexGrow: 1 }} />
          {role === null ? (
            <Button
              component="a"
              href="/oauth2/authorization/google"
              variant="outlined"
              size="small"
              aria-label="Prijava s Googleom"
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
          ) : (
            <IconButton
              aria-label="Korisnički izbornik"
              aria-haspopup="true"
              aria-expanded={Boolean(accountAnchor)}
              onClick={(event) => setAccountAnchor(event.currentTarget)}
              sx={{ p: 0.5 }}
            >
              <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main' }}>
                {role === 'ARTIST' ? 'UM' : role === 'ORGANIZER' ? 'OR' : 'AD'}
              </Avatar>
            </IconButton>
          )}
          {role !== null && (
            <Menu
              anchorEl={accountAnchor}
              open={Boolean(accountAnchor)}
              onClose={() => setAccountAnchor(null)}
            >
              <MenuItem
                component={RouterLink}
                to="/profile"
                onClick={() => setAccountAnchor(null)}
              >
                Profil
              </MenuItem>
              {onSignOut && (
                <MenuItem
                  onClick={() => {
                    onSignOut()
                    setAccountAnchor(null)
                  }}
                >
                  Odjava
                </MenuItem>
              )}
            </Menu>
          )}
        </Toolbar>
      </AppBar>

      <Drawer
        anchor="left"
        open={drawerOpen}
        onClose={closeDrawer}
        transitionDuration={0}
        ModalProps={{ keepMounted: true }}
      >
        <Box
          id="mobile-navigation"
          component="nav"
          aria-label="Mobilna navigacija"
          sx={{ width: 280, p: 2 }}
        >
          <Stack spacing={2}>
            <Typography
              variant="h6"
              color="primary.main"
              sx={{ fontWeight: 700 }}
            >
              {hr.appName}
            </Typography>
            <NavItems role={role} vertical onNavigate={closeDrawer} />
            {role === null ? (
              <Button
                component="a"
                href="/oauth2/authorization/google"
                variant="outlined"
                fullWidth
                sx={{
                  color: 'text.primary',
                  borderColor: '#78716C',
                  '&:hover': { borderColor: 'text.primary' },
                }}
              >
                <GoogleMark />
                <Box component="span" sx={{ ml: 1 }}>
                  Prijava s Googleom
                </Box>
              </Button>
            ) : (
              <>
                <Button
                  component={RouterLink}
                  to="/profile"
                  onClick={closeDrawer}
                  sx={{ justifyContent: 'flex-start' }}
                >
                  Profil
                </Button>
                {onSignOut && (
                  <Button
                    onClick={() => {
                      onSignOut()
                      closeDrawer()
                    }}
                    sx={{ justifyContent: 'flex-start' }}
                  >
                    Odjava
                  </Button>
                )}
              </>
            )}
          </Stack>
        </Box>
      </Drawer>

      <Box
        component="main"
        id="main-content"
        tabIndex={-1}
        sx={{
          flex: 1,
          width: '100%',
          px: { xs: 2, sm: 3 },
          py: { xs: 3, sm: 4 },
        }}
      >
        <Outlet />
      </Box>

      <Box
        component="footer"
        sx={{
          px: { xs: 2, sm: 3, md: 4 },
          py: 2,
          borderTop: 1,
          borderColor: 'divider',
          color: 'text.secondary',
          fontSize: '0.75rem',
        }}
      >
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={{ xs: 0.5, sm: 1 }}
          sx={{
            width: '100%',
            justifyContent: 'space-between',
          }}
        >
          <span>© 2026 ArtConnect</span>
          <span>Kontakt · Uvjeti korištenja · Privatnost</span>
        </Stack>
      </Box>
    </Box>
  )
}
