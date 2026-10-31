import type { OrganizerApproval } from '../auth/types'
import type { OrganizerProfileFormValues } from './organizerProfileSchema'

export type OrganizerProfile = {
  userId: string
  displayName: string
  organizationName: string | null
  organizerApproval: OrganizerApproval
}

export type OrganizerProfileUpdate = {
  organizationName: string | null
}

export function toOrganizerProfileUpdate(
  values: OrganizerProfileFormValues,
): OrganizerProfileUpdate {
  return {
    organizationName: values.organizationName || null,
  }
}
