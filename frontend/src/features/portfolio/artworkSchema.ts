import { z } from 'zod'
import { hr } from '../../i18n/hr'

function currentYearInZagreb() {
  return Number(
    new Intl.DateTimeFormat('en-US', {
      timeZone: 'Europe/Zagreb',
      year: 'numeric',
    }).format(new Date()),
  )
}

const optionalText = (maxLength: number, message: string) =>
  z
    .string()
    .trim()
    .max(maxLength, message)
    .transform((value) => value || null)

const artworkFieldsSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, hr.portfolio.form.validation.required)
    .max(200, hr.portfolio.form.validation.titleMaxLength),
  year: z
    .string()
    .trim()
    .min(1, hr.portfolio.form.validation.yearRequired)
    .regex(/^\d+$/, hr.portfolio.form.validation.yearInteger)
    .refine(
      (year) => Number(year) >= 1900,
      hr.portfolio.form.validation.yearMin,
    )
    .refine(
      (year) => Number(year) <= currentYearInZagreb(),
      hr.portfolio.form.validation.yearFuture,
    ),
  technique: z
    .string()
    .trim()
    .min(1, hr.portfolio.form.validation.required)
    .max(200, hr.portfolio.form.validation.techniqueMaxLength),
  dimensions: optionalText(
    100,
    hr.portfolio.form.validation.dimensionsMaxLength,
  ),
  description: optionalText(
    5000,
    hr.portfolio.form.validation.descriptionMaxLength,
  ),
  visibility: z.enum(['PUBLIC', 'PRIVATE']),
})

export const artworkSchema = artworkFieldsSchema.transform((values) => ({
  title: values.title,
  year: Number(values.year),
  technique: values.technique,
  dimensions: values.dimensions ?? null,
  description: values.description ?? null,
  visibility: values.visibility,
}))

export type ArtworkFormValues = z.input<typeof artworkSchema>
export type ArtworkFormSubmission = z.output<typeof artworkSchema>
