import { describe, expect, it } from 'vitest'

import { hr } from '../../../i18n/hr'
import {
  validateDeadlineRange,
  validateTextFilter,
} from './opportunityFilterSchema'

const validation = hr.opportunities.validation

describe('validateTextFilter', () => {
  it('prihvaća prazan tekst i tekst do 100 znakova', () => {
    expect(validateTextFilter('q', '')).toBeUndefined()
    expect(validateTextFilter('q', 'a'.repeat(100))).toBeUndefined()
    expect(validateTextFilter('city', 'Zagreb')).toBeUndefined()
  })

  it('odbija tekst duži od 100 znakova, s porukom uz pravo polje', () => {
    expect(validateTextFilter('q', 'a'.repeat(101))).toBe(validation.queryMax)
    expect(validateTextFilter('city', 'a'.repeat(101))).toBe(validation.cityMax)
  })
})

describe('validateDeadlineRange', () => {
  it('prihvaća ispravan raspon, isti datum i nepotpun raspon', () => {
    expect(validateDeadlineRange('2026-11-01', '2026-12-31')).toBeUndefined()
    expect(validateDeadlineRange('2026-11-01', '2026-11-01')).toBeUndefined()
    expect(validateDeadlineRange('2026-11-01', null)).toBeUndefined()
    expect(validateDeadlineRange(null, '2026-12-31')).toBeUndefined()
    expect(validateDeadlineRange(null, null)).toBeUndefined()
  })

  it('odbija obrnut raspon', () => {
    expect(validateDeadlineRange('2026-12-31', '2026-11-01')).toBe(
      validation.deadlineRange,
    )
  })
})
