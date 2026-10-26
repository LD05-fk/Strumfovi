import { hasRole } from '../../features/auth/hasRole'

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
  { item: { label: 'Prilike', to: '/opportunities' } },
  { item: { label: 'Portfolio', to: '/portfolio' }, roles: ['ARTIST'] },
  { item: { label: 'Moje prijave', to: '/applications' }, roles: ['ARTIST'] },
  {
    item: { label: 'Moje prilike', to: '/my-opportunities' },
    roles: ['ORGANIZER'],
  },
  { item: { label: 'Statistika', to: '/statistics' }, roles: ['ORGANIZER'] },
  { item: { label: 'Administracija', to: '/admin' }, roles: ['ADMIN'] },
]

export function getNavigationItems(role: NavigationRole): NavigationItem[] {
  const user = role === null ? null : { role }

  return navigationItems
    .filter(({ roles }) => roles === undefined || hasRole(user, ...roles))
    .map(({ item }) => item)
}
