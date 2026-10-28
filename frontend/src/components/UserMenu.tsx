import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import {
  Avatar,
  IconButton,
  Menu,
  MenuItem,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material'
import { useEffect, useRef, useState } from 'react'
import { Link as RouterLink } from 'react-router'
import { useCurrentUser } from '../features/auth/useCurrentUser'
import { useLogout } from '../features/auth/useLogout'
import { hasRole } from '../features/auth/hasRole'
import { hr } from '../i18n/hr'
import { GoogleSignInButton } from './GoogleSignInButton'
import { RoleChip } from './state/RoleChip'
import { useNotify } from './state/useNotify'

function getInitials(displayName: string) {
  return displayName
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toLocaleUpperCase('hr-HR'))
    .join('')
}

export function UserMenu() {
  const { data: user, isPending, isError } = useCurrentUser()
  const logout = useLogout()
  const notify = useNotify()

  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const notifiedError = useRef(false)
  const menuOpen = Boolean(anchorEl)

  useEffect(() => {
    if (isError && !notifiedError.current) {
      notify(hr.errors.generic, 'error')
      notifiedError.current = true
    }

    if (!isError) {
      notifiedError.current = false
    }
  }, [isError, notify])

  useEffect(() => {
    if (logout.isError) {
      notify(hr.errors.generic, 'error')
    }
  }, [logout.isError, notify])

  if (isPending) {
    return (
      <Skeleton
        variant="circular"
        width={36}
        height={36}
        data-testid="user-menu-loading"
      />
    )
  }

  if (isError) {
    return null
  }

  if (user === null || user === undefined) {
    return <GoogleSignInButton />
  }

  const closeMenu = () => setAnchorEl(null)

  return (
    <>
      <IconButton
        data-testid="user-menu"
        aria-label="Korisnički izbornik"
        aria-haspopup="menu"
        aria-expanded={menuOpen}
        aria-controls={menuOpen ? 'user-menu-options' : undefined}
        onClick={(event) => setAnchorEl(event.currentTarget)}
        sx={{ p: 0.5 }}
      >
        <Avatar
          src={user.avatarUrl ?? undefined}
          alt={user.displayName}
          sx={{ width: 36, height: 36, bgcolor: 'primary.main' }}
        >
          {getInitials(user.displayName) || <AccountCircleIcon />}
        </Avatar>
      </IconButton>

      <Menu
        id="user-menu-options"
        anchorEl={anchorEl}
        open={menuOpen}
        onClose={closeMenu}
        slotProps={{
          list: {
            'aria-label': 'Korisničke opcije',
          },
        }}
      >
        <Stack spacing={0.5} sx={{ px: 2, py: 1.5, minWidth: 220 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            {user.displayName}
          </Typography>

          <Typography variant="body2" color="text.secondary">
            {user.email}
          </Typography>

          {user.role !== null && <RoleChip role={user.role} />}
        </Stack>

        {hasRole(user, 'ARTIST', 'ORGANIZER') && (
          <MenuItem component={RouterLink} to="/profile" onClick={closeMenu}>
            Profil
          </MenuItem>
        )}

        <MenuItem
          data-testid="logout"
          disabled={logout.isPending}
          onClick={() => {
            closeMenu()
            logout.mutate()
          }}
        >
          Odjava
        </MenuItem>
      </Menu>
    </>
  )
}
