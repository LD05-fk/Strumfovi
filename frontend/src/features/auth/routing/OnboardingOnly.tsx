import type { ReactNode } from 'react'
import { Navigate, Outlet } from 'react-router'
import { hasRole } from '../hasRole'
import { SessionGate } from './SessionGate'

type OnboardingOnlyProps = {
  children?: ReactNode
}

export function OnboardingOnly({ children }: OnboardingOnlyProps) {
  return (
    <SessionGate>
      {(user) =>
        user?.status === 'ONBOARDING' ? (
          (children ?? <Outlet />)
        ) : (
          <Navigate
            to={hasRole(user, 'ARTIST', 'ORGANIZER') ? '/profile' : '/'}
            replace
          />
        )
      }
    </SessionGate>
  )
}
