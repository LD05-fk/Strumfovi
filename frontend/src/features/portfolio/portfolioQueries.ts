import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../../api/client'
import type { Artwork, ArtworkImage, ArtworkInput, ArtworkPage } from './types'

export const artworkQueryKey = (artworkId: string) =>
  ['artworks', artworkId] as const

export const artistArtworksQueryKey = (
  artistId: string,
  page: number,
  pageSize: number,
) => ['artists', artistId, 'artworks', page, pageSize] as const

export function useArtwork(artworkId: string) {
  return useQuery({
    queryKey: artworkQueryKey(artworkId),
    queryFn: () =>
      apiFetch<Artwork>(`/artworks/${encodeURIComponent(artworkId)}`),
    enabled: artworkId.length > 0,
  })
}

export function useArtistArtworks(
  artistId: string,
  page: number,
  pageSize: number,
) {
  return useQuery({
    queryKey: artistArtworksQueryKey(artistId, page, pageSize),
    queryFn: () =>
      apiFetch<ArtworkPage>(
        `/artists/${encodeURIComponent(artistId)}/artworks?page=${page}&pageSize=${pageSize}`,
      ),
    enabled: artistId.length > 0,
  })
}

export function useCreateArtwork(artistId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: ArtworkInput) =>
      apiFetch<Artwork>('/artworks', { method: 'POST', json: input }),
    onSuccess: async (artwork) => {
      queryClient.setQueryData(artworkQueryKey(artwork.id), artwork)
      await queryClient.invalidateQueries({
        queryKey: ['artists', artistId, 'artworks'],
      })
    },
  })
}

export function useUpdateArtwork(artistId: string, artworkId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: ArtworkInput) =>
      apiFetch<Artwork>(`/artworks/${encodeURIComponent(artworkId)}`, {
        method: 'PUT',
        json: input,
      }),
    onSuccess: async (artwork) => {
      queryClient.setQueryData(artworkQueryKey(artwork.id), artwork)
      await queryClient.invalidateQueries({
        queryKey: ['artists', artistId, 'artworks'],
      })
    },
  })
}

type ArtworkImageRequest = {
  body: FormData
  signal?: AbortSignal
}

export function useUploadArtworkImage(artistId: string, artworkId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ body, signal }: ArtworkImageRequest) =>
      apiFetch<ArtworkImage>(
        `/artworks/${encodeURIComponent(artworkId)}/images`,
        { method: 'POST', body, signal },
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: artworkQueryKey(artworkId) }),
        queryClient.invalidateQueries({
          queryKey: ['artists', artistId, 'artworks'],
        }),
      ])
    },
  })
}

export function useDeleteArtworkImage(artistId: string, artworkId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (imageId: string) =>
      apiFetch<void>(
        `/artworks/${encodeURIComponent(artworkId)}/images/${encodeURIComponent(imageId)}`,
        { method: 'DELETE' },
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: artworkQueryKey(artworkId) }),
        queryClient.invalidateQueries({
          queryKey: ['artists', artistId, 'artworks'],
        }),
      ])
    },
  })
}

export function useDeleteArtwork(
  artistId: string,
  onSuccess?: () => void,
  onError?: (error: unknown) => void,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (artworkId: string) =>
      apiFetch<void>(`/artworks/${encodeURIComponent(artworkId)}`, {
        method: 'DELETE',
      }),
    onSuccess: async () => {
      onSuccess?.()
      await queryClient.invalidateQueries({
        queryKey: ['artists', artistId, 'artworks'],
      })
    },
    onError: async (error) => {
      onError?.(error)
      if (error instanceof ApiError && error.status === 404) {
        await queryClient.invalidateQueries({
          queryKey: ['artists', artistId, 'artworks'],
        })
      }
    },
  })
}
