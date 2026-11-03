import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../../api/client'
import type {
  ArtFieldPage,
  ArtistProfile,
  ArtistProfileUpdate,
  ArtistPublicProfile,
  PastExhibitionInput,
} from './types'

export const artistProfileQueryKey = ['me', 'artist-profile'] as const

export function useMyArtistProfile() {
  return useQuery({
    queryKey: artistProfileQueryKey,
    queryFn: () => apiFetch<ArtistProfile>('/me/artist-profile'),
  })
}

export function useArtistPublicProfile(userId: string) {
  return useQuery({
    queryKey: ['artists', userId],
    queryFn: () => apiFetch<ArtistPublicProfile>(`/artists/${userId}`),
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

export function useCreatePastExhibition(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: PastExhibitionInput) =>
      apiFetch('/past-exhibitions', { method: 'POST', json: input }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['artists', userId] })
    },
  })
}

export function useUpdatePastExhibition(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: PastExhibitionInput }) =>
      apiFetch(`/past-exhibitions/${id}`, {
        method: 'PUT',
        json: values,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['artists', userId] })
    },
    onError: async (error) => {
      if (error instanceof ApiError && error.status === 404) {
        await queryClient.invalidateQueries({ queryKey: ['artists', userId] })
      }
    },
  })
}

export function useDeletePastExhibition(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/past-exhibitions/${id}`, { method: 'DELETE' }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['artists', userId] })
    },
    onError: async (error) => {
      if (error instanceof ApiError && error.status === 404) {
        await queryClient.invalidateQueries({ queryKey: ['artists', userId] })
      }
    },
  })
}
