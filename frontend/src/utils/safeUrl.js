export function safeExternalUrl(value) {
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }
  try {
    const url = new URL(trimmed)
    if (url.protocol === 'https:') {
      return url.toString()
    }
  } catch {
    return null
  }
  return null
}
