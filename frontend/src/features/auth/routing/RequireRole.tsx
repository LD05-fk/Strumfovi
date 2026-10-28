import type { ReactNode } from 'react'
import { Navigate, Outlet } from 'react-router'
import type { UserRole } from '../../../components/layout/navigation'
import { ForbiddenPage } from '../../../components/state/ForbiddenPage'
import { hasRole } from '../hasRole'
import { LoginRequiredPanel } from './LoginRequiredPanel'
import { SessionGate } from './SessionGate'

type RequireRoleProps = {
  roles: UserRole[]
  children?: ReactNode
}

export function RequireRole({ roles, children }: RequireRoleProps) {
  return (
    <SessionGate>
      {(user) => {
        if (!user) {
          return <LoginRequiredPanel />
        }

        if (user.status === 'ONBOARDING') {
          return <Navigate to="/onboarding" replace />
        }

        if (!hasRole(user, ...roles)) {
          return <ForbiddenPage />
        }

        return children ?? <Outlet />
      }}
    </SessionGate>
  )
}
