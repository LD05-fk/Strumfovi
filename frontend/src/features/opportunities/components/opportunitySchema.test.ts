import { describe, expect, it } from 'vitest'
import { createOpportunitySchema } from './opportunitySchema'

const typeId = '00000000-0000-4000-8000-000000000001'
const artFieldId = '00000000-0000-4000-8000-000000000002'

const validValues = {
  title: 'Prilika za umjetnike',
  description: 'Opis prilike.',
  typeId,
  artFieldIds: [artFieldId],
  deadline: '2099-01-10T23:00:00+01:00',
  criteria: '',
  prize: '',
  eventStart: '',
  eventEnd: '',
  location: {
    type: 'PHYSICAL' as const,
    streetAddress: 'Ilica 1',
    city: 'Zagreb',
    postalCode: '10000',
    latitude: '45.813',
    longitude: '15.977',
    onlineUrl: '',
  },
}

describe('createOpportunitySchema', () => {
  it('prihvaća valjane vrijednosti', () => {
    expect(createOpportunitySchema(true).safeParse(validValues).success).toBe(
      true,
    )
  })

  it('provjerava ograničenja duljine naslova i opisa', () => {
    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        title: 'ab',
      }).success,
    ).toBe(false)

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        title: 'x'.repeat(201),
      }).success,
    ).toBe(false)

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        description: 'x'.repeat(10001),
      }).success,
    ).toBe(false)
  })

  it('zahtijeva 1–10 jedinstvenih područja umjetnosti', () => {
    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        artFieldIds: [],
      }).success,
    ).toBe(false)

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        artFieldIds: [artFieldId, artFieldId],
      }).success,
    ).toBe(false)

    const elevenIds = Array.from(
      { length: 11 },
      (_, index) =>
        `00000000-0000-4000-8000-${String(index + 10).padStart(12, '0')}`,
    )

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        artFieldIds: elevenIds,
      }).success,
    ).toBe(false)
  })

  it('rok mora biti u budućnosti pri stvaranju, ali ne i pri uređivanju', () => {
    const pastDeadline = {
      ...validValues,
      deadline: '2000-01-10T23:00:00+01:00',
    }

    expect(createOpportunitySchema(true).safeParse(pastDeadline).success).toBe(
      false,
    )
    expect(createOpportunitySchema(false).safeParse(pastDeadline).success).toBe(
      true,
    )
  })

  it('kraj događanja zahtijeva početak i ne može biti prije njega', () => {
    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        eventEnd: '2027-01-02T10:00:00+01:00',
      }).success,
    ).toBe(false)

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        eventStart: '2027-01-02T12:00:00+01:00',
        eventEnd: '2027-01-02T10:00:00+01:00',
      }).success,
    ).toBe(false)

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        eventStart: '2027-01-02T10:00:00+01:00',
        eventEnd: '2027-01-02T10:00:00+01:00',
      }).success,
    ).toBe(true)
  })
  it('zahtijeva valjan ISO-8601 datum s vremenskim pomakom', () => {
    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        deadline: '2099-01-10T23:00:00+01:00',
      }).success,
    ).toBe(true)

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        deadline: '2099-07-10T23:00:00+02:00',
      }).success,
    ).toBe(true)

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        deadline: '2099-01-10T23:00:00',
      }).success,
    ).toBe(false)

    expect(
      createOpportunitySchema(true).safeParse({
        ...validValues,
        deadline: '2099-02-30T23:00:00+01:00',
      }).success,
    ).toBe(false)
  })
})
