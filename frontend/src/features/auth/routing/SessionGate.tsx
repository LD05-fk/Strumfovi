import type { ReactNode } from 'react'
import { ApiError } from '../../../api/client'
import { ErrorState } from '../../../components/state/ErrorState'
import { LoadingState } from '../../../components/state/LoadingState'
import { useCurrentUser } from '../useCurrentUser'
import type { CurrentUser } from '../types'

type SessionGateProps = {
  children: (user: CurrentUser | null) => ReactNode
}

export function SessionGate({ children }: SessionGateProps) {
  const query = useCurrentUser()

  if (query.isPending) {
    return <LoadingState lines={2} />
  }

  if (query.isError) {
    return (
      <ErrorState
        error={query.error instanceof ApiError ? query.error : undefined}
        onRetry={() => void query.refetch()}
      />
    )
  }

  return children(query.data ?? null)
}
