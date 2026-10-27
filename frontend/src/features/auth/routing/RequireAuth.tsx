import type { ReactNode } from 'react'
import { Navigate, Outlet } from 'react-router'
import { LoginRequiredPanel } from './LoginRequiredPanel'
import { SessionGate } from './SessionGate'

type RequireAuthProps = {
  children?: ReactNode
}

export function RequireAuth({ children }: RequireAuthProps) {
  return (
    <SessionGate>
      {(user) => {
        if (!user) {
          return <LoginRequiredPanel />
        }

        if (user.status === 'ONBOARDING') {
          return <Navigate to="/onboarding" replace />
        }

        return children ?? <Outlet />
      }}
    </SessionGate>
  )
}
