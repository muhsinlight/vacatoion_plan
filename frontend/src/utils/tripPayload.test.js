import { describe, expect, it } from 'vitest'
import { prettyTripDate, resolveCityName } from './tripDisplay'
import { buildTripPayload, travelerSummaryFromForm } from './tripPayload'

describe('trip display', () => {
  it('formats iso dates and falls back to the city key', () => {
    expect(prettyTripDate('2026-10-21')).toBe('21.10.2026')
    expect(prettyTripDate('')).toBe('')
    const cities = [{ cityKey: 'izmir', displayName: 'İzmir' }]
    expect(resolveCityName(cities, 'izmir')).toBe('İzmir')
    expect(resolveCityName(cities, 'antalya')).toBe('antalya')
    expect(resolveCityName(cities, '')).toBe('')
  })
})

describe('buildTripPayload', () => {
  const form = {
    originCity: 'ankara',
    destinationCity: 'izmir',
    startDate: '2026-07-01',
    endDate: '2026-07-04',
    adults: '2',
    transportMode: 'BUS',
    accommodationType: 'HOTEL',
    travelStyle: 'BALANCED',
    selectedActivityIds: ['a1'],
    interests: [],
    totalBudget: '',
    perPersonBudget: '4000',
    specialNeeds: '  ',
    customActivityNames: [],
  }

  it('omits empty optional fields', () => {
    expect(buildTripPayload(form)).toEqual({
      originCity: 'ankara',
      destinationCity: 'izmir',
      startDate: '2026-07-01',
      endDate: '2026-07-04',
      adults: 2,
      transportMode: 'BUS',
      accommodationType: 'HOTEL',
      travelStyle: 'BALANCED',
      selectedActivityIds: ['a1'],
      includeContingency: false,
      perPersonBudget: 4000,
    })
  })

  it('summarizes travelers', () => {
    expect(travelerSummaryFromForm(form)).toBe('2 yetişkin')
  })
})
