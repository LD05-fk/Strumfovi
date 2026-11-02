export type OpportunityLocationType = 'PHYSICAL' | 'ONLINE'

export type CatalogReference = {
  id: string
  code: string
  name: string
}

export type CatalogOption = CatalogReference & {
  active: boolean
  position: number
}

export type OpportunityFormValues = {
  title: string
  description: string
  typeId: string
  artFieldIds: string[]
  deadline: string
  criteria: string
  prize: string
  eventStart: string
  eventEnd: string
  location: {
    type: OpportunityLocationType
    streetAddress: string
    city: string
    postalCode: string
    latitude: string
    longitude: string
    onlineUrl: string
  }
}

export type OpportunityInput = {
  title: string
  description: string
  typeId: string
  artFieldIds: string[]
  deadline: string
  criteria: string | null
  prize: string | null
  location: {
    type: OpportunityLocationType
    streetAddress: string | null
    city: string | null
    postalCode: string | null
    latitude: number | null
    longitude: number | null
    onlineUrl: string | null
  }
  eventStart: string | null
  eventEnd: string | null
}

export function toOpportunityInput(
  values: OpportunityFormValues,
): OpportunityInput {
  const location: OpportunityInput['location'] =
    values.location.type === 'PHYSICAL'
      ? {
          type: 'PHYSICAL',
          streetAddress: values.location.streetAddress.trim(),
          city: values.location.city.trim(),
          postalCode: values.location.postalCode.trim() || null,
          latitude: Number(values.location.latitude),
          longitude: Number(values.location.longitude),
          onlineUrl: null,
        }
      : {
          type: 'ONLINE',
          streetAddress: null,
          city: null,
          postalCode: null,
          latitude: null,
          longitude: null,
          onlineUrl: values.location.onlineUrl.trim(),
        }

  return {
    title: values.title.trim(),
    description: values.description.trim(),
    typeId: values.typeId,
    artFieldIds: [...values.artFieldIds],
    deadline: values.deadline,
    criteria: values.criteria.trim() || null,
    prize: values.prize.trim() || null,
    location,
    eventStart: values.eventStart || null,
    eventEnd: values.eventEnd || null,
  }
}
