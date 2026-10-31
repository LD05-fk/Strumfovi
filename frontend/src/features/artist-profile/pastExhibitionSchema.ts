import { z } from 'zod'
import { hr } from '../../i18n/hr'

export function currentZagrebYear(): number {
  return Number(
    new Intl.DateTimeFormat('en', {
      timeZone: 'Europe/Zagreb',
      year: 'numeric',
    }).format(new Date()),
  )
}

export const pastExhibitionSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, hr.artistProfile.pastExhibitions.validation.required)
    .max(200, hr.artistProfile.pastExhibitions.validation.maxLength),
  year: z
    .number({ error: hr.artistProfile.pastExhibitions.validation.required })
    .int(hr.artistProfile.pastExhibitions.validation.yearInteger)
    .min(1900, hr.artistProfile.pastExhibitions.validation.yearMin)
    .refine((year) => year <= currentZagrebYear(), {
      message: hr.artistProfile.pastExhibitions.validation.yearFuture,
    }),
  place: z
    .string()
    .trim()
    .min(1, hr.artistProfile.pastExhibitions.validation.required)
    .max(200, hr.artistProfile.pastExhibitions.validation.maxLength),
})

export type PastExhibitionFormValues = z.input<typeof pastExhibitionSchema>
