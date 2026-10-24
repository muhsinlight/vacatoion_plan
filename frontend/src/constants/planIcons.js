/*
 * Secenek kimlikleri `constants/tripForm.js` icinde tanimli kalir.
 * Burada yalnizca her kimligin hangi `Icon.vue` glifini kullandigi durur,
 * boylece backend sozlesmesini tasiyan liste tek yerde kalir.
 */

export const TRANSPORT_ICONS = {
  BUS: 'bus',
  TRAIN: 'train',
  PLANE: 'plane',
  CAR: 'car',
}

export const STAY_ICONS = {
  HOTEL: 'bed',
  PENSION: 'house',
  RENTAL: 'key',
  CAMP: 'tent',
  NONE: 'ban',
}

export const INTEREST_ICONS = {
  culture: 'museum',
  nature: 'leaf',
  food: 'utensils',
  beach: 'beach',
  shopping: 'bag',
  adventure: 'compass',
}

export const CATEGORY_LABELS = {
  TRANSPORT: 'Ulaşım',
  LODGING: 'Konaklama',
  FOOD: 'Yemek',
  LOCAL_TRANSPORT: 'Şehir içi',
  ACTIVITY: 'Aktivite',
  OTHER: 'Diğer',
}

/* Kirilim cubugunda her kategorinin rengi. */
export const CATEGORY_COLORS = {
  TRANSPORT: '#0f5132',
  LODGING: '#1f6f4a',
  FOOD: '#42906c',
  LOCAL_TRANSPORT: '#79b495',
  ACTIVITY: '#d97706',
  OTHER: '#f59e0b',
}

/*
 * Satir rozeti iki degere indirgenir: canli ya da resmi fiyattan gelen
 * "Guncel", katalog tahmininden gelen "Tahmini".
 */
const LIVE_SOURCES = new Set(['SCRAPE', 'OFFICIAL_API'])

export function sourceBadge(sourceType) {
  return LIVE_SOURCES.has(sourceType)
    ? { label: 'Güncel', live: true }
    : { label: 'Tahmini', live: false }
}
