const FIELD_TEXT = {
  'must not be blank': 'Bu alan zorunlu',
  'must not be null': 'Bu alan zorunlu',
  'must be a well-formed email address': 'Geçerli bir e-posta girin',
  'size must be between 8 and 72': 'Şifre 8 ile 72 karakter arasında olmalı',
}

export function fieldText(message) {
  if (!message) {
    return ''
  }
  return FIELD_TEXT[message] || message
}

export function formatMoney(min, max, currency = 'TRY') {
  if (min == null || max == null) {
    return '—'
  }
  const format = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 0 })
  const same = Number(min) === Number(max)
  const amount = same ? format.format(min) : `${format.format(min)} – ${format.format(max)}`
  return `${amount} ${currency}`
}

export function isoDate(offsetDays) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
