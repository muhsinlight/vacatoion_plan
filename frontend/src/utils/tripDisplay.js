export function prettyTripDate(value) {
  if (!value?.includes('-')) {
    return value || ''
  }
  const [year, month, day] = value.split('-')
  return `${day}.${month}.${year}`
}

export function resolveCityName(destinations, cityKey) {
  if (!cityKey) {
    return ''
  }
  return destinations.find((city) => city.cityKey === cityKey)?.displayName || cityKey
}
