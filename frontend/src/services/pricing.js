import { api } from './http'

export function listDestinations() {
  return api('/api/v1/destinations')
}

export function listActivities(cityKey) {
  return api(`/api/v1/destinations/${encodeURIComponent(cityKey)}/activities`)
}

export function suggestActivities(cityKey, body) {
  return api(`/api/v1/destinations/${encodeURIComponent(cityKey)}/activity-suggestions`, {
    method: 'POST',
    body: body ?? {},
  })
}

export function getTripRequest(id) {
  return api(`/api/v1/trips/${id}/request`)
}

export function previewTrip(body, { signal } = {}) {
  return api('/api/v1/trips/preview', { method: 'POST', body, signal })
}

export function createTrip(body) {
  return api('/api/v1/trips', { method: 'POST', body })
}

export function listTrips({ page = 0, size = 20 } = {}) {
  const q = new URLSearchParams({ page: String(page), size: String(size) })
  return api(`/api/v1/trips?${q}`)
}

export function getTrip(id) {
  return api(`/api/v1/trips/${id}`)
}

export function previewSavedTrip(id) {
  return api(`/api/v1/trips/${id}/preview`)
}

export function updateTrip(id, body) {
  return api(`/api/v1/trips/${id}`, { method: 'PUT', body })
}

export function refreshTripBudget(id) {
  return api(`/api/v1/trips/${id}/refresh-budget`, { method: 'POST' })
}

export function deleteTrip(id) {
  return api(`/api/v1/trips/${id}`, { method: 'DELETE' })
}
