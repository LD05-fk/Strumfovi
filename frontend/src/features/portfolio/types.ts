export type ArtworkImage = {
  id: string
  file: {
    id: string
    originalName: string
    contentType: string
    sizeBytes: number
    url: string
  }
  altText: string | null
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

export type ArtworkInput = Pick<
  Artwork,
  'title' | 'year' | 'technique' | 'dimensions' | 'description' | 'visibility'
>

export type ArtworkPage = {
  data: Artwork[]
  meta: {
    page: number
    pageSize: number
    total: number
  }
}
