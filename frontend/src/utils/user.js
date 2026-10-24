export function displayName(user) {
  if (!user) {
    return ''
  }
  return [user.firstName, user.lastName].filter(Boolean).join(' ').trim()
}

export function initials(user) {
  const source = displayName(user)
  if (!source) {
    return ''
  }
  const parts = source.split(/\s+/).filter(Boolean)
  const letters = parts.length >= 2
    ? `${parts[0].charAt(0)}${parts[1].charAt(0)}`
    : source.slice(0, 2)
  return letters.toLocaleUpperCase('tr-TR')
}
