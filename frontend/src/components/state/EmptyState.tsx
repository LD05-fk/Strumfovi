import { Box, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

type EmptyStateProps = {
  title: string
  description?: string
  action?: ReactNode
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <Box
      sx={{
        textAlign: 'center',
        p: 4,
        border: '2px dashed',
        borderColor: 'divider',
        borderRadius: 3,
        color: 'text.secondary',
      }}
    >
      <Stack spacing={1} sx={{ alignItems: 'center' }}>
        <Typography component="h2" variant="h6" color="text.primary">
          {title}
        </Typography>
        {description && <Typography>{description}</Typography>}
        {action}
      </Stack>
    </Box>
  )
}
