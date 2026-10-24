export const INTEREST_OPTIONS = [
  { id: 'culture', label: 'Kültür' },
  { id: 'nature', label: 'Doğa' },
  { id: 'food', label: 'Yemek' },
  { id: 'beach', label: 'Plaj' },
  { id: 'shopping', label: 'Alışveriş' },
  { id: 'adventure', label: 'Macera' },
]

export const TRANSPORT_OPTIONS = [
  { id: 'BUS', label: 'Otobüs' },
  { id: 'TRAIN', label: 'Tren' },
  { id: 'PLANE', label: 'Uçak' },
  { id: 'CAR', label: 'Araba' },
]

export const TRANSPORT_LABEL = Object.fromEntries(TRANSPORT_OPTIONS.map((option) => [option.id, option.label]))

export const STAY_OPTIONS = [
  { id: 'HOTEL', label: 'Otel' },
  { id: 'PENSION', label: 'Pansiyon' },
  { id: 'RENTAL', label: 'Kiralık' },
  { id: 'CAMP', label: 'Kamp' },
  { id: 'NONE', label: 'Konaklama yok' },
]

export const STAY_LABEL = Object.fromEntries(STAY_OPTIONS.map((option) => [option.id, option.label]))

export const STYLE_OPTIONS = [
  { id: 'ECONOMIC', label: 'Ekonomik' },
  { id: 'BALANCED', label: 'Dengeli' },
  { id: 'COMFORT', label: 'Konfor' },
]

export const STYLE_LABEL = Object.fromEntries(STYLE_OPTIONS.map((option) => [option.id, option.label]))
