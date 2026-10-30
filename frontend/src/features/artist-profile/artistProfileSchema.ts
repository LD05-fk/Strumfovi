import { z } from 'zod'
import { hr } from '../../i18n/hr'

const isValidHttpUrl = (value: string) => {
  const trimmed = value.trim()
  if (!/^https?:\/\//i.test(trimmed)) return false

  try {
    const url = new URL(trimmed)
    return (
      (url.protocol === 'http:' || url.protocol === 'https:') &&
      url.hostname.length > 0
    )
  } catch {
    return false
  }
}

const artistLinkSchema = z.object({
  label: z.string().refine((value) => value.trim().length <= 60, {
    message: hr.artistProfile.validation.linkLabelMaxLength,
  }),
  url: z
    .string()
    .refine((value) => value.trim().length > 0, {
      message: hr.artistProfile.validation.linkUrlRequired,
    })
    .refine((value) => value.trim().length <= 1024, {
      message: hr.artistProfile.validation.linkUrlMaxLength,
    })
    .refine(isValidHttpUrl, {
      message: hr.artistProfile.validation.linkUrlFormat,
    }),
})

export const artistProfileSchema = z.object({
  biography: z.string().refine((value) => value.trim().length <= 5000, {
    message: hr.artistProfile.validation.biographyMaxLength,
  }),
  artistStatement: z.string().refine((value) => value.trim().length <= 5000, {
    message: hr.artistProfile.validation.artistStatementMaxLength,
  }),
  artFieldIds: z
    .array(z.string().uuid())
    .max(10, hr.artistProfile.validation.artFieldsMaxCount)
    .refine((ids) => new Set(ids).size === ids.length, {
      message: hr.artistProfile.validation.artFieldsUnique,
    }),
  links: z
    .array(artistLinkSchema)
    .max(10, hr.artistProfile.validation.linksMaxCount),
})

export type ArtistProfileSchemaValues = z.infer<typeof artistProfileSchema>
