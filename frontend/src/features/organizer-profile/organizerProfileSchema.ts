import { z } from 'zod'
import { hr } from '../../i18n/hr'

export const organizerProfileSchema = z.object({
  organizationName: z
    .string()
    .trim()
    .min(1, hr.organizerProfile.organizationNameRequired)
    .max(200, hr.organizerProfile.organizationNameTooLong),
})

export type OrganizerProfileFormValues = z.infer<typeof organizerProfileSchema>
