import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import type { OrganizerProfile, OrganizerProfileUpdate } from './types'

export const organizerProfileQueryKey = ['me', 'organizer-profile'] as const

export function useOrganizerProfile() {
  return useQuery({
    queryKey: organizerProfileQueryKey,
    queryFn: () => apiFetch<OrganizerProfile>('/me/organizer-profile'),
  })
}

export function useUpdateOrganizerProfile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (update: OrganizerProfileUpdate) =>
      apiFetch<OrganizerProfile>('/me/organizer-profile', {
        method: 'PUT',
        json: update,
      }),
    onSuccess: async (profile) => {
      queryClient.setQueryData(organizerProfileQueryKey, profile)
      await queryClient.invalidateQueries({
        queryKey: organizerProfileQueryKey,
      })
    },
  })
}
