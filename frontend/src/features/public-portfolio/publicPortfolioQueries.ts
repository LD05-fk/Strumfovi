import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import type { ArtworkPage, PublicArtistProfile } from './types'

export const publicArtistQueryKey = (artistId: string) =>
  ['artists', artistId] as const

export const publicArtworksQueryKey = (artistId: string, page: number) =>
  ['artists', artistId, 'artworks', page] as const

export function usePublicArtist(artistId: string) {
  return useQuery({
    queryKey: publicArtistQueryKey(artistId),
    queryFn: () => apiFetch<PublicArtistProfile>(`/artists/${artistId}`),
    enabled: Boolean(artistId),
  })
}

export function usePublicArtworks(artistId: string, page: number) {
  return useQuery({
    queryKey: publicArtworksQueryKey(artistId, page),
    queryFn: () =>
      apiFetch<ArtworkPage>(
        `/artists/${artistId}/artworks?page=${page}&pageSize=24`,
        { credentials: 'omit' },
      ),
    enabled: Boolean(artistId),
  })
}
