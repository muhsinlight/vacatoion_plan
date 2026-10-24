import { describe, expect, it } from 'vitest'
import { displayName, initials } from './user'

describe('user labels', () => {
  it('joins present name parts', () => {
    expect(displayName(null)).toBe('')
    expect(displayName({ firstName: 'Ada', lastName: '' })).toBe('Ada')
    expect(displayName({ firstName: 'Ada', lastName: 'Lovelace' })).toBe('Ada Lovelace')
  })

  it('uses Turkish uppercase for initials', () => {
    expect(initials({ firstName: 'ışık', lastName: 'öz' })).toBe('IÖ')
    expect(initials({ firstName: 'ihsan' })).toBe('İH')
    expect(initials(null)).toBe('')
  })
})
