import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import type { ArtFieldPage, ArtistProfile, ArtistProfileUpdate } from './types'

export const artistProfileQueryKey = ['me', 'artist-profile'] as const

export function useMyArtistProfile() {
  return useQuery({
    queryKey: artistProfileQueryKey,
    queryFn: () => apiFetch<ArtistProfile>('/me/artist-profile'),
  })
}

export function useArtFields() {
  return useQuery({
    queryKey: ['art-fields', { pageSize: 100 }],
    queryFn: () => apiFetch<ArtFieldPage>('/art-fields?pageSize=100'),
  })
}

export function useUpdateMyArtistProfile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (update: ArtistProfileUpdate) =>
      apiFetch<ArtistProfile>('/me/artist-profile', {
        method: 'PUT',
        json: update,
      }),
    onSuccess: async (profile) => {
      queryClient.setQueryData(artistProfileQueryKey, profile)
      await queryClient.invalidateQueries({
        queryKey: ['artists', profile.userId],
      })
    },
  })
}
