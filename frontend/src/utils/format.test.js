import { describe, expect, it } from 'vitest'
import { fieldText, formatMoney, isoDate } from './format'

describe('fieldText', () => {
  it('translates known validation messages', () => {
    expect(fieldText('must not be blank')).toBe('Bu alan zorunlu')
    expect(fieldText('size must be between 8 and 72')).toBe('Şifre 8 ile 72 karakter arasında olmalı')
  })

  it('keeps unknown text and blanks empty input', () => {
    expect(fieldText('')).toBe('')
    expect(fieldText('özel hata')).toBe('özel hata')
  })
})

describe('formatMoney', () => {
  it('renders a single amount or a range in TRY', () => {
    expect(formatMoney(1500, 1500)).toBe('1.500 TRY')
    expect(formatMoney(1000, 2500)).toBe('1.000 – 2.500 TRY')
    expect(formatMoney(10, 20, 'EUR')).toBe('10 – 20 EUR')
  })

  it('uses a dash when either bound is missing', () => {
    expect(formatMoney(null, 10)).toBe('—')
    expect(formatMoney(10, undefined)).toBe('—')
  })
})

describe('isoDate', () => {
  it('returns a calendar date', () => {
    expect(isoDate(0)).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})
