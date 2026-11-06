import { z } from 'zod'
import { hr } from '../../../i18n/hr'

const hasOffsetDateTime = (value: string) => {
  const match = value.match(
    /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})([+-])(\d{2}):(\d{2})$/,
  )

  if (!match) return false

  const [, year, month, day, hour, minute, second, , offsetHour, offsetMinute] =
    match
  const yearNumber = Number(year)
  const monthNumber = Number(month)
  const dayNumber = Number(day)
  const hourNumber = Number(hour)
  const minuteNumber = Number(minute)
  const secondNumber = Number(second)
  const offsetHourNumber = Number(offsetHour)
  const offsetMinuteNumber = Number(offsetMinute)

  if (monthNumber < 1 || monthNumber > 12) return false

  const isLeapYear =
    yearNumber % 4 === 0 && (yearNumber % 100 !== 0 || yearNumber % 400 === 0)
  const daysInMonth = [
    31,
    isLeapYear ? 29 : 28,
    31,
    30,
    31,
    30,
    31,
    31,
    30,
    31,
    30,
    31,
  ][monthNumber - 1]

  return (
    dayNumber >= 1 &&
    dayNumber <= daysInMonth &&
    hourNumber <= 23 &&
    minuteNumber <= 59 &&
    secondNumber <= 59 &&
    offsetHourNumber <= 14 &&
    offsetMinuteNumber <= 59 &&
    (offsetHourNumber !== 14 || offsetMinuteNumber === 0) &&
    Number.isFinite(Date.parse(value))
  )
}

const isHttpUrl = (value: string) => {
  try {
    const url = new URL(value.trim())
    return (
      (url.protocol === 'http:' || url.protocol === 'https:') &&
      url.hostname.length > 0
    )
  } catch {
    return false
  }
}

const dateField = z
  .string()
  .refine((value) => value === '' || hasOffsetDateTime(value), {
    message: hr.opportunities.validation.dateFormat,
  })

export function createOpportunitySchema(isCreating: boolean) {
  return z
    .object({
      title: z
        .string()
        .trim()
        .min(3, hr.opportunities.validation.titleMin)
        .max(200, hr.opportunities.validation.titleMax),

      description: z
        .string()
        .trim()
        .min(1, hr.opportunities.validation.descriptionRequired)
        .max(10000, hr.opportunities.validation.descriptionMax),

      typeId: z.string().uuid(hr.opportunities.validation.typeRequired),

      artFieldIds: z
        .array(z.string().uuid())
        .min(1, hr.opportunities.validation.artFieldsMin)
        .max(10, hr.opportunities.validation.artFieldsMax)
        .refine((ids) => new Set(ids).size === ids.length, {
          message: hr.opportunities.validation.artFieldsUnique,
        }),

      deadline: dateField
        .refine((value) => value !== '', {
          message: hr.opportunities.validation.deadlineRequired,
        })
        .refine((value) => !isCreating || Date.parse(value) > Date.now(), {
          message: hr.opportunities.validation.deadlineFuture,
        }),

      criteria: z.string().max(5000, hr.opportunities.validation.criteriaMax),
      prize: z.string().max(2000, hr.opportunities.validation.prizeMax),

      eventStart: dateField,
      eventEnd: dateField,

      location: z.object({
        type: z.enum(['PHYSICAL', 'ONLINE']),
        streetAddress: z
          .string()
          .max(200, hr.opportunities.validation.addressMax),
        city: z.string().max(100, hr.opportunities.validation.cityMax),
        postalCode: z
          .string()
          .max(10, hr.opportunities.validation.postalCodeMax),
        latitude: z.string(),
        longitude: z.string(),
        onlineUrl: z
          .string()
          .max(1024, hr.opportunities.validation.onlineUrlMax),
      }),
    })
    .superRefine((values, context) => {
      if (values.eventEnd && !values.eventStart) {
        context.addIssue({
          code: 'custom',
          path: ['eventEnd'],
          message: hr.opportunities.validation.eventStartRequired,
        })
      } else if (
        values.eventStart &&
        values.eventEnd &&
        Date.parse(values.eventEnd) < Date.parse(values.eventStart)
      ) {
        context.addIssue({
          code: 'custom',
          path: ['eventEnd'],
          message: hr.opportunities.validation.eventEndBeforeStart,
        })
      }

      if (values.location.type === 'PHYSICAL') {
        if (!values.location.streetAddress.trim()) {
          context.addIssue({
            code: 'custom',
            path: ['location', 'streetAddress'],
            message: hr.opportunities.validation.addressRequired,
          })
        }

        if (!values.location.city.trim()) {
          context.addIssue({
            code: 'custom',
            path: ['location', 'city'],
            message: hr.opportunities.validation.cityRequired,
          })
        }

        const coordinates = [
          {
            field: 'latitude',
            min: -90,
            max: 90,
            message: hr.opportunities.validation.latitudeRange,
          },
          {
            field: 'longitude',
            min: -180,
            max: 180,
            message: hr.opportunities.validation.longitudeRange,
          },
        ] as const

        for (const coordinate of coordinates) {
          const raw = values.location[coordinate.field]
          const number = Number(raw)

          if (
            raw.trim() === '' ||
            !Number.isFinite(number) ||
            number < coordinate.min ||
            number > coordinate.max
          ) {
            context.addIssue({
              code: 'custom',
              path: ['location', coordinate.field],
              message: coordinate.message,
            })
          }
        }
      } else if (
        !values.location.onlineUrl.trim() ||
        !isHttpUrl(values.location.onlineUrl)
      ) {
        context.addIssue({
          code: 'custom',
          path: ['location', 'onlineUrl'],
          message: hr.opportunities.validation.onlineUrlFormat,
        })
      }
    })
}
