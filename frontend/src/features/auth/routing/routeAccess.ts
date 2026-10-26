import { matchPath } from 'react-router'

const protectedRoutePatterns = [
  '/profile',
  '/portfolio/*',
  '/applications/*',
  '/opportunities/:opportunityId/apply',
  '/organizer/*',
  '/admin/*',
  '/my-opportunities',
  '/statistics',
]

export function isProtectedRoutePath(pathname: string): boolean {
  return protectedRoutePatterns.some((path) =>
    matchPath({ path, end: false }, pathname),
  )
}
