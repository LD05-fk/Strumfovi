import { z } from 'zod'

export const chooseRoleSchema = z.object({
  role: z.enum(['ARTIST', 'ORGANIZER']),
})

export type ChooseRoleFormValues = z.infer<typeof chooseRoleSchema>
