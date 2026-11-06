import type {
  ArtistPublicProfile,
  Participation,
} from '../artist-profile/types'

export type PublicArtistProfile = ArtistPublicProfile & {
  participations?: Participation[]
}

export type ArtworkImage = {
  id: string
  file: {
    id: string
    originalName: string
    contentType: string
    sizeBytes: number
    url: string
  }
  altText: string
}

export type Artwork = {
  id: string
  artistId: string
  title: string
  year: number
  technique: string
  dimensions: string | null
  description: string | null
  visibility: 'PUBLIC' | 'PRIVATE'
  hiddenByAdmin: boolean
  images: ArtworkImage[]
  createdAt: string
}

export type ArtworkPage = {
  data: Artwork[]
  meta: {
    page: number
    pageSize: number
    total: number
  }
}
