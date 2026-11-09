import { z } from 'zod'

import { hr } from '../../../i18n/hr'

const MAX_TEXT_LENGTH = 100

export const textFilterSchema = z.object({
  q: z.string().max(MAX_TEXT_LENGTH, hr.opportunities.validation.queryMax),
  city: z.string().max(MAX_TEXT_LENGTH, hr.opportunities.validation.cityMax),
})

export type TextFilterKey = keyof typeof textFilterSchema.shape

/** Vraća poruku greške za polje ili `undefined` ako je vrijednost ispravna. */
export function validateTextFilter(
  key: TextFilterKey,
  value: string,
): string | undefined {
  const result = textFilterSchema.shape[key].safeParse(value)

  return result.success ? undefined : result.error.issues[0]?.message
}

/**
 * Raspon roka. Oblik datuma ne provjeravamo ovdje: polja su `type="date"`, a
 * vrijednosti iz URL-a već je provjerio `parseSearchParams`. Obrnut raspon API
 * vraća kao 400 s poljem `deadlineFrom`, pa i poruka ide uz to polje.
 */
export const deadlineRangeSchema = z
  .object({
    deadlineFrom: z.string().nullable(),
    deadlineTo: z.string().nullable(),
  })
  .refine(
    ({ deadlineFrom, deadlineTo }) =>
      deadlineFrom === null ||
      deadlineTo === null ||
      deadlineFrom <= deadlineTo,
    {
      message: hr.opportunities.validation.deadlineRange,
      path: ['deadlineFrom'],
    },
  )

/** Vraća poruku za polje "Rok od" ili `undefined` ako je raspon ispravan. */
export function validateDeadlineRange(deadlineFrom: string | null, deadlineTo: string | null): string | undefined {
  const result = deadlineRangeSchema.safeParse({ deadlineFrom, deadlineTo })

  return result.success ? undefined : result.error.issues[0]?.message
}
