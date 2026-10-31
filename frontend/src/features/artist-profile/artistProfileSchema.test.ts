import { describe, expect, it } from 'vitest'
import { artistProfileSchema } from './artistProfileSchema'

const validLink = (index: number) => ({
  label: `Link ${index}`,
  url: `https://example.test/${index}`,
})

const validValues = {
  biography: '',
  artistStatement: '',
  artFieldIds: [],
  links: [],
}

describe('artistProfileSchema', () => {
  it('accepts valid HTTP(S) URLs and rejects malformed or unsupported URLs', () => {
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        links: [
          { label: 'HTTP', url: 'http://example.test' },
          { label: 'HTTPS', url: 'https://example.test/path' },
        ],
      }).success,
    ).toBe(true)

    for (const url of [
      'example.test',
      'ftp://example.test/file',
      'https://',
      'https://?query=value',
    ]) {
      expect(
        artistProfileSchema.safeParse({
          ...validValues,
          links: [{ label: '', url }],
        }).success,
      ).toBe(false)
    }
  })

  it('accepts the exact limit of 10 links and rejects 11', () => {
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        links: Array.from({ length: 10 }, (_, index) => validLink(index)),
      }).success,
    ).toBe(true)
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        links: Array.from({ length: 11 }, (_, index) => validLink(index)),
      }).success,
    ).toBe(false)
  })

  it('enforces text, field-count, and field-uniqueness limits', () => {
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        biography: 'a'.repeat(5000),
      }).success,
    ).toBe(true)
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        biography: 'a'.repeat(5001),
      }).success,
    ).toBe(false)

    const ids = Array.from(
      { length: 11 },
      (_, index) =>
        `00000000-0000-4000-8000-${index.toString().padStart(12, '0')}`,
    )
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        artFieldIds: ids.slice(0, 10),
      }).success,
    ).toBe(true)
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        artFieldIds: ids,
      }).success,
    ).toBe(false)
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        artFieldIds: [ids[0], ids[0]],
      }).success,
    ).toBe(false)
  })

  it('accepts 60-character labels and 1024-character HTTP(S) URLs only', () => {
    const urlPrefix = 'https://example.test/'
    const maxLengthUrl = `${urlPrefix}${'a'.repeat(1024 - urlPrefix.length)}`
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        links: [{ label: 'x'.repeat(60), url: maxLengthUrl }],
      }).success,
    ).toBe(true)
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        links: [{ label: 'x'.repeat(61), url: maxLengthUrl }],
      }).success,
    ).toBe(false)
    expect(
      artistProfileSchema.safeParse({
        ...validValues,
        links: [{ label: 'Label', url: `${maxLengthUrl}a` }],
      }).success,
    ).toBe(false)
  })
})
