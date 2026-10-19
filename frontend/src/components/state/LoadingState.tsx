import { Box, Skeleton, Stack } from '@mui/material'
import { hr } from '../../i18n/hr'

type LoadingStateProps = {
  lines?: number
}

export function LoadingState({ lines = 3 }: LoadingStateProps) {
  return (
    <Stack
      role="status"
      aria-label={hr.common.loading}
      aria-busy="true"
      spacing={1}
    >
      {Array.from({ length: lines }, (_, index) => (
        <Box key={index}>
          <Skeleton
            variant="rounded"
            height={index === 0 ? 120 : 16}
            width={index === 1 ? '70%' : '100%'}
          />
        </Box>
      ))}
    </Stack>
  )
}
