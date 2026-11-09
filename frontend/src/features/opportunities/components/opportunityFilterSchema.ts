import { z } from 'zod'

import { hr } from '../../../i18n/hr'

const MAX_TEXT_LENGTH = 100

export const textFilterSchema = z.object({
  q: z.string().max(MAX_TEXT_LENGTH, hr.opportunities.validation.queryMax),
  city: z.string().max(MAX_TEXT_LENGTH, hr.opportunities.validation.cityMax),
})

export type TextFilterKey = keyof typeof textFilterSchema.shape

/** Vraća poruku greške za polje ili `undefined` ako je vrijednost ispravna. */
export function validateTextFilter(key: TextFilterKey, value: string): string | undefined {
  const result = textFilterSchema.shape[key].safeParse(value)

  return result.success ? undefined : result.error.issues[0]?.message
}
