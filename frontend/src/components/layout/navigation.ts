import { hasRole } from '../../features/auth/hasRole'
import { hr } from '../../i18n/hr'

export type UserRole = 'ARTIST' | 'ORGANIZER' | 'ADMIN'
export type NavigationRole = UserRole | null

export type NavigationItem = {
  label: string
  to: string
}

const navigationItems: Array<{
  item: NavigationItem
  roles?: UserRole[]
}> = [
  { item: { label: hr.navigation.opportunities, to: '/opportunities' } },
  {
    item: { label: hr.navigation.artistProfile, to: '/profile' },
    roles: ['ARTIST'],
  },
  {
    item: { label: hr.navigation.artistPortfolio, to: '/portfolio' },
    roles: ['ARTIST'],
  },
  {
    item: { label: hr.navigation.artistApplications, to: '/applications' },
    roles: ['ARTIST'],
  },
  {
    item: {
      label: hr.navigation.organizerOpportunities,
      to: '/my-opportunities',
    },
    roles: ['ORGANIZER'],
  },
  {
    item: { label: hr.navigation.statistics, to: '/statistics' },
    roles: ['ORGANIZER'],
  },
  {
    item: { label: hr.navigation.administration, to: '/admin' },
    roles: ['ADMIN'],
  },
]

export function getNavigationItems(role: NavigationRole): NavigationItem[] {
  const user = role === null ? null : { role }

  return navigationItems
    .filter(({ roles }) => roles === undefined || hasRole(user, ...roles))
    .map(({ item }) => item)
}
