import type { UserRole } from '../../components/layout/navigation'
import type { CurrentUser } from './types'

export function hasRole(
  user: Pick<CurrentUser, 'role'> | null | undefined,
  ...roles: UserRole[]
): boolean {
  return user?.role != null && roles.includes(user.role)
}
