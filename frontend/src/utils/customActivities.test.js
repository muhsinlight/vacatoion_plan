import { describe, expect, it } from 'vitest'
import { addCustomActivityName, appendCustomActivity, toggleActivityId } from './customActivities'
import { safeExternalUrl } from './safeUrl'

describe('safeExternalUrl', () => {
  it('accepts only https addresses', () => {
    expect(safeExternalUrl(' https://ornek.com/yol ')).toBe('https://ornek.com/yol')
    expect(safeExternalUrl('http://ornek.com')).toBeNull()
    expect(safeExternalUrl('javascript:alert(1)')).toBeNull()
    expect(safeExternalUrl('')).toBeNull()
    expect(safeExternalUrl(null)).toBeNull()
  })
})

describe('custom activities', () => {
  it('adds a trimmed name once', () => {
    expect(addCustomActivityName(['Müze'], '  Kale ')).toEqual(['Müze', 'Kale'])
    expect(addCustomActivityName(['Kale'], 'Kale')).toEqual(['Kale'])
    expect(addCustomActivityName(['Kale'], '   ')).toEqual(['Kale'])
  })

  it('inserts an activity before lunch on the first open day', () => {
    const itinerary = [
      { items: [{ type: 'MEAL', title: 'Kahvaltı' }, { type: 'ACTIVITY', title: 'Kale' }, { type: 'MEAL', title: 'Öğle yemeği' }] },
      { items: [{ type: 'MEAL', title: 'Kahvaltı' }, { type: 'MEAL', title: 'Öğle yemeği' }] },
    ]
    const next = appendCustomActivity(itinerary, 'Hamam')
    expect(itinerary[0].items.map((item) => item.title)).toEqual(['Kahvaltı', 'Kale', 'Öğle yemeği'])
    expect(next[0].items.map((item) => item.title)).toEqual(['Kahvaltı', 'Kale', 'Öğle yemeği'])
    expect(next[1].items.map((item) => item.title)).toEqual(['Kahvaltı', 'Hamam', 'Öğle yemeği'])
    expect(appendCustomActivity(next, 'Hamam')).toBe(next)
  })

  it('toggles an activity id', () => {
    expect(toggleActivityId(['a'], 'b')).toEqual(['a', 'b'])
    expect(toggleActivityId(['a', 'b'], 'a')).toEqual(['b'])
  })
})
