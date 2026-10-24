import { beforeEach, describe, expect, it } from 'vitest'
import {
  emptyTripDraft,
  markPlanStepsSeen,
  patchTripDraft,
  planStepsSeen,
  readTripDraft,
  sanitizeTripDraft,
  TRIP_DRAFT_KEY,
} from './tripDraft'

describe('trip draft', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('drops unknown modes and out-of-range adults', () => {
    const draft = sanitizeTripDraft({
      originCity: 'ankara',
      adults: 99,
      transportMode: 'BOAT',
      accommodationType: 'HOTEL',
      travelStyle: 'COMFORT',
      totalBudget: -5,
      perPersonBudget: '2500',
      customActivityNames: ['  ', 'Kale', 3],
    })
    expect(draft.originCity).toBe('ankara')
    expect(draft.adults).toBe(20)
    expect(draft.transportMode).toBe(emptyTripDraft().transportMode)
    expect(draft.accommodationType).toBe('HOTEL')
    expect(draft.travelStyle).toBe('COMFORT')
    expect(draft.totalBudget).toBe('')
    expect(draft.perPersonBudget).toBe('2500')
    expect(draft.customActivityNames).toEqual(['Kale'])
  })

  it('round-trips a patch and ignores broken storage', () => {
    patchTripDraft({ destinationCity: 'izmir', adults: 3 })
    expect(readTripDraft().destinationCity).toBe('izmir')
    expect(readTripDraft().adults).toBe(3)
    localStorage.setItem(TRIP_DRAFT_KEY, '{')
    expect(readTripDraft()).toBeNull()
  })

  it('remembers that plan steps were seen', () => {
    expect(planStepsSeen()).toBe(false)
    markPlanStepsSeen()
    expect(planStepsSeen()).toBe(true)
  })
})
