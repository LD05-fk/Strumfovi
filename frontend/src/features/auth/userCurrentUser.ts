import { useQuery } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../../api/client'
import type { CurrentUser } from './types'

export function useCurrentUser() {
  return useQuery<CurrentUser | null>({
    queryKey: ['me'],
    queryFn: async () => {
      try {
        return await apiFetch<CurrentUser>('/me')
      } catch (error) {
        if(error instanceof ApiError && error.status === 401){
            return null
        }
        throw error
      }
    },
  })
}