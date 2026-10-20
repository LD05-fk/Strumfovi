export type UserRole = 'ARTIST' | 'ORGANIZER' | 'ADMIN'
export type NavigationRole = UserRole | null

export type NavigationItem = {
  label: string
  to: string
}

const navigationItems: Record<string, NavigationItem[]> = {
  visitor: [{ label: 'Prilike', to: '/opportunities' }],
  ARTIST: [
    { label: 'Prilike', to: '/opportunities' },
    { label: 'Portfolio', to: '/portfolio' },
    { label: 'Moje prijave', to: '/applications' },
  ],
  ORGANIZER: [
    { label: 'Prilike', to: '/opportunities' },
    { label: 'Moje prilike', to: '/my-opportunities' },
    { label: 'Statistika', to: '/statistics' },
  ],
  ADMIN: [
    { label: 'Prilike', to: '/opportunities' },
    { label: 'Administracija', to: '/admin' },
  ],
}

export function getNavigationItems(role: NavigationRole): NavigationItem[] {
  return navigationItems[role ?? 'visitor']
}
