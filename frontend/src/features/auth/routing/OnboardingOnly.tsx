import type { ReactNode } from 'react'
import { Navigate, Outlet } from 'react-router'
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
          <Navigate to="/" replace />
        )
      }
    </SessionGate>
  )
}
