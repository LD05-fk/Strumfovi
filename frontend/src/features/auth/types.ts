import type { UserRole } from "../../components/layout/navigation"

export type UserStatus =
  | 'INVITED'
  | 'ONBOARDING'
  | 'ACTIVE'
  | 'BLOCKED'
  | 'DELETED'

export type OrganizerApproval = 'PENDING' | 'APPROVED'

export interface CurrentUser {
  id: string
  displayName: string
  email: string
  avatarUrl: string | null
  role: UserRole | null
  status: UserStatus
  organizerApproval: OrganizerApproval | null
}
