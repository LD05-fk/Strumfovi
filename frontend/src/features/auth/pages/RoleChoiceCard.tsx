import { Card, CardActionArea, Stack, Typography } from '@mui/material'
import type { KeyboardEventHandler } from 'react'
import type { ReactNode } from 'react'
import type { ChooseRoleFormValues } from './chooseRoleSchema'

type RoleChoiceCardProps = {
  role: ChooseRoleFormValues['role']
  icon: ReactNode
  label: string
  description: string
  selected: boolean
  disabled: boolean
  tabIndex: 0 | -1
  onSelect: (role: ChooseRoleFormValues['role']) => void
  onKeyDown: KeyboardEventHandler<HTMLButtonElement>
}

export function RoleChoiceCard({
  role,
  icon,
  label,
  description,
  selected,
  disabled,
  tabIndex,
  onSelect,
  onKeyDown,
}: RoleChoiceCardProps) {
  return (
    <Card
      variant="outlined"
      sx={{
        height: '100%',
        borderColor: selected ? 'primary.main' : 'divider',
        borderWidth: 2,
        bgcolor: selected ? 'primary.light' : 'background.paper',
        boxShadow: 'none',
      }}
    >
      <CardActionArea
        role="radio"
        aria-checked={selected}
        disabled={disabled}
        tabIndex={tabIndex}
        onClick={() => onSelect(role)}
        onKeyDown={onKeyDown}
        sx={{
          height: '100%',
          p: 2.5,
          textAlign: 'left',
          alignItems: 'stretch',
        }}
      >
        <Stack spacing={1}>
          <span aria-hidden="true">{icon}</span>
          <Typography component="h3" variant="h5">
            {label}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {description}
          </Typography>
        </Stack>
      </CardActionArea>
    </Card>
  )
}
