import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import type { PublicSettings } from './types'

export const publicSettingsQueryKey = ['settings', 'public'] as const

export function usePublicSettings(enabled = true) {
  return useQuery({
    queryKey: publicSettingsQueryKey,
    queryFn: () => apiFetch<PublicSettings>('/settings/public'),
    enabled,
  })
}
