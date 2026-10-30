import { useRef } from 'react'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material'
import AccountBalanceOutlined from '@mui/icons-material/AccountBalanceOutlined'
import PaletteOutlined from '@mui/icons-material/PaletteOutlined'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { statusChip } from '../../../app/theme'
import { usePageTitle } from '../../../components/layout/usePageTitle'
import { ApiError } from '../../../api/client'
import { hr } from '../../../i18n/hr'
import { useChooseRole } from '../useChooseRole'
import { chooseRoleSchema, type ChooseRoleFormValues } from './chooseRoleSchema'
import { RoleChoiceCard } from './RoleChoiceCard'

const roles = ['ARTIST', 'ORGANIZER'] as const

export function ChooseRolePage() {
  usePageTitle(hr.auth.onboardingTitle)
  const groupRef = useRef<HTMLDivElement>(null)
  const form = useForm<ChooseRoleFormValues>({
    resolver: zodResolver(chooseRoleSchema),
    defaultValues: {},
  })
  const chooseRole = useChooseRole()
  const selectedRole = useWatch({ control: form.control, name: 'role' })

  function selectRole(role: ChooseRoleFormValues['role']) {
    chooseRole.reset()
    form.setValue('role', role, { shouldValidate: true })
  }

  function handleRoleKeyDown(event: React.KeyboardEvent<HTMLButtonElement>) {
    if (
      event.key !== 'ArrowRight' &&
      event.key !== 'ArrowDown' &&
      event.key !== 'ArrowLeft' &&
      event.key !== 'ArrowUp'
    ) {
      return
    }

    event.preventDefault()

    const cards = groupRef.current?.querySelectorAll<HTMLButtonElement>(
      '[role="radio"]:not(:disabled)',
    )
    if (!cards?.length) return
    const currentIndex = Array.from(cards).indexOf(event.currentTarget)
    if (currentIndex < 0) return
    const step =
      event.key === 'ArrowRight' || event.key === 'ArrowDown' ? 1 : -1
    const nextIndex = (currentIndex + step + cards.length) % cards.length
    const nextRole = roles[nextIndex]

    selectRole(nextRole)
    cards[nextIndex].focus()
  }

  function onSubmit(values: ChooseRoleFormValues) {
    chooseRole.mutate(values)
  }

  const errorMessage =
    chooseRole.error instanceof ApiError
      ? chooseRole.error.message
      : chooseRole.error
        ? hr.errors.generic
        : null
  return (
    <Box sx={{ maxWidth: 720, mx: 'auto' }}>
      <Stack
        component="form"
        onSubmit={form.handleSubmit(onSubmit)}
      >
        <Typography component="h1" variant="h2" sx={{ mb: 0.5 }}>
          {hr.auth.chooseRoleHeading}
        </Typography>

        <Typography color="text.secondary" sx={{ m: 0 }}>
          {hr.auth.chooseRoleDescription}
        </Typography>

        <Box
          ref={groupRef}
          role="radiogroup"
          aria-label={hr.auth.onboardingTitle}
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
            gap: 2,
            mt: 3,
          }}
        >
          {roles.map((role) => (
            <RoleChoiceCard
              key={role}
              role={role}
              icon={
                role === 'ARTIST' ? (
                  <PaletteOutlined fontSize="large" />
                ) : (
                  <AccountBalanceOutlined fontSize="large" />
                )
              }
              label={hr.auth.roles[role]}
              description={hr.auth.roleDescriptions[role]}
              selected={selectedRole === role}
              disabled={chooseRole.isPending}
              tabIndex={(selectedRole ?? 'ARTIST') === role ? 0 : -1}
              onSelect={selectRole}
              onKeyDown={handleRoleKeyDown}
            />
          ))}
        </Box>
        <Alert
          severity="warning"
          sx={{
            mt: 3,
            mb: 2,
            p: 1.5,
            bgcolor: statusChip.SELECTION.bg,
            color: statusChip.SELECTION.fg,
            '& .MuiAlert-icon': { color: statusChip.SELECTION.fg },
          }}
        >
          {hr.auth.roleSelectionNotice}
        </Alert>
        {errorMessage && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorMessage}
          </Alert>
        )}
        <Button
          type="submit"
          variant="contained"
          disabled={!selectedRole || chooseRole.isPending}
          aria-busy={chooseRole.isPending}
          startIcon={
            chooseRole.isPending ? (
              <CircularProgress color="inherit" size={18} />
            ) : undefined
          }
          sx={{
            alignSelf: { xs: 'stretch', sm: 'flex-start' },
            px: 2.5,
          }}
        >
          {chooseRole.isPending ? hr.auth.savingRole : hr.auth.confirmRole}
        </Button>
      </Stack>
    </Box>
  )
}
