export type ArtistProfileField = {
  id: string
  code: string
  name: string
}

export type ArtistProfileLink = {
  label: string | null
  url: string
}

export type ArtistProfile = {
  userId: string
  displayName: string
  avatarUrl: string | null
  biography: string | null
  artistStatement: string | null
  artFields: ArtistProfileField[]
  links: ArtistProfileLink[]
}

export type PastExhibition = {
  id: string
  title: string
  year: number
  place: string
}

export type ArtistPublicProfile = ArtistProfile & {
  pastExhibitions: PastExhibition[]
}

export type PastExhibitionInput = {
  title: string
  year: number
  place: string
}

export type ArtFieldOption = ArtistProfileField & {
  active: boolean
  position: number
}

export type ArtFieldPage = {
  data: ArtFieldOption[]
  meta: {
    page: number
    pageSize: number
    total: number
  }
}

export type ArtistProfileFormValues = {
  biography: string
  artistStatement: string
  artFieldIds: string[]
  links: Array<{
    label: string
    url: string
  }>
}

export type ArtistProfileUpdate = {
  biography: string | null
  artistStatement: string | null
  artFieldIds: string[]
  links: ArtistProfileLink[]
}

export function toArtistProfileFormValues(
  profile: ArtistProfile,
): ArtistProfileFormValues {
  return {
    biography: profile.biography ?? '',
    artistStatement: profile.artistStatement ?? '',
    artFieldIds: profile.artFields.map((field) => field.id),
    links: profile.links.map((link) => ({
      label: link.label ?? '',
      url: link.url,
    })),
  }
}

export function toArtistProfileUpdate(
  values: ArtistProfileFormValues,
): ArtistProfileUpdate {
  const normalize = (value: string) => value.trim() || null

  return {
    biography: normalize(values.biography),
    artistStatement: normalize(values.artistStatement),
    artFieldIds: [...values.artFieldIds],
    links: values.links.map((link) => ({
      label: normalize(link.label),
      url: link.url.trim(),
    })),
  }
}
