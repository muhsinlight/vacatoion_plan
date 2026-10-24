import { isoDate } from './format'

export const TRIP_DRAFT_KEY = 'vp-trip-draft'
export const PLAN_STEPS_KEY = 'vp-plan-steps-seen'

const MODES = new Set(['BUS', 'TRAIN', 'PLANE', 'CAR'])
const STAYS = new Set(['HOTEL', 'PENSION', 'RENTAL', 'CAMP', 'NONE'])
const STYLES = new Set(['ECONOMIC', 'BALANCED', 'COMFORT'])

export function emptyTripDraft() {
  return {
    originCity: '',
    destinationCity: '',
    startDate: isoDate(0),
    endDate: isoDate(2),
    adults: 2,
    transportMode: 'BUS',
    accommodationType: 'HOTEL',
    travelStyle: 'BALANCED',
    interests: [],
    totalBudget: '',
    perPersonBudget: '',
    selectedActivityIds: [],
    specialNeeds: '',
    customActivityNames: [],
  }
}

function asText(value) {
  return typeof value === 'string' ? value : ''
}

function asMoney(value) {
  if (value === '' || value == null) {
    return ''
  }
  const number = Number(value)
  if (!Number.isFinite(number) || number < 0) {
    return ''
  }
  return String(number)
}

export function sanitizeTripDraft(parsed) {
  const base = emptyTripDraft()
  const adults = Number(parsed?.adults)
  return {
    ...base,
    originCity: asText(parsed?.originCity),
    destinationCity: asText(parsed?.destinationCity),
    startDate: asText(parsed?.startDate) || base.startDate,
    endDate: asText(parsed?.endDate) || base.endDate,
    adults: Number.isInteger(adults) ? Math.min(20, Math.max(1, adults)) : base.adults,
    transportMode: MODES.has(parsed?.transportMode) ? parsed.transportMode : base.transportMode,
    accommodationType: STAYS.has(parsed?.accommodationType) ? parsed.accommodationType : base.accommodationType,
    travelStyle: STYLES.has(parsed?.travelStyle) ? parsed.travelStyle : base.travelStyle,
    interests: Array.isArray(parsed?.interests) ? parsed.interests.filter((id) => typeof id === 'string') : [],
    totalBudget: asMoney(parsed?.totalBudget),
    perPersonBudget: asMoney(parsed?.perPersonBudget),
    selectedActivityIds: Array.isArray(parsed?.selectedActivityIds)
      ? parsed.selectedActivityIds.filter((id) => typeof id === 'string' || typeof id === 'number')
      : [],
    specialNeeds: asText(parsed?.specialNeeds),
    customActivityNames: Array.isArray(parsed?.customActivityNames)
      ? parsed.customActivityNames.filter((name) => typeof name === 'string' && name.trim()).slice(0, 8)
      : [],
  }
}

export function readTripDraft() {
  try {
    const raw = localStorage.getItem(TRIP_DRAFT_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return null
    }
    return sanitizeTripDraft(parsed)
  } catch {
    return null
  }
}

export function writeTripDraft(form) {
  localStorage.setItem(TRIP_DRAFT_KEY, JSON.stringify(sanitizeTripDraft(form)))
}

export function patchTripDraft(partial) {
  writeTripDraft({ ...(readTripDraft() || emptyTripDraft()), ...partial })
}

export function planStepsSeen() {
  return localStorage.getItem(PLAN_STEPS_KEY) === '1'
}

export function markPlanStepsSeen() {
  localStorage.setItem(PLAN_STEPS_KEY, '1')
}
