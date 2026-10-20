import { Box, Button, Stack } from '@mui/material'
import { Link as RouterLink, useLocation } from 'react-router'
import { getNavigationItems, type NavigationRole } from './navigation'

type NavItemsProps = {
  role: NavigationRole
  onNavigate?: () => void
  vertical?: boolean
}

export function NavItems({
  role,
  onNavigate,
  vertical = false,
}: NavItemsProps) {
  const location = useLocation()
  const items = getNavigationItems(role)

  return (
    <Stack
      component="ul"
      direction={vertical ? 'column' : 'row'}
      spacing={0.5}
      sx={{ listStyle: 'none', p: 0, m: 0 }}
    >
      {items.map((item) => {
        const selected =
          location.pathname === item.to ||
          (location.pathname === '/' && item.to === '/opportunities')
        return (
          <Box component="li" key={item.to}>
            <Button
              component={RouterLink}
              to={item.to}
              onClick={onNavigate}
              aria-current={selected ? 'page' : undefined}
              sx={{
                justifyContent: 'flex-start',
                color: selected ? '#312E81' : 'text.primary',
                bgcolor: selected ? 'primary.light' : 'transparent',
                px: 1.5,
                width: vertical ? '100%' : 'auto',
                '&:hover': { bgcolor: 'primary.light' },
              }}
            >
              {item.label}
            </Button>
          </Box>
        )
      })}
    </Stack>
  )
}
