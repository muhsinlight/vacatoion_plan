<script setup>
import { computed } from 'vue'
import Icon from './Icon.vue'
import { formatMoney } from '../utils/format'
import { prettyTripDate } from '../utils/tripDisplay'
import { safeExternalUrl } from '../utils/safeUrl'

const props = defineProps({
  preview: { type: Object, default: null },
  // Planla ekrani toplami kendi butce kartinda basiyor, burada tekrar etmesin.
  showTotals: { type: Boolean, default: true },
})

const tierLabel = { BUDGET: 'Ekonomik', MID: 'Orta', PREMIUM: 'Lüks' }
const itemKind = { MEAL: 'Yemek', ACTIVITY: 'Durak', TRANSPORT: 'Geçiş' }

const sourceLabel = {
  OFFICIAL_API: 'Resmi fiyat',
  SCRAPE: 'Canlı fiyat',
  ESTIMATE: 'Katalog tahmini',
  MANUAL: 'El ile',
  IMPUTED: 'Türetilmiş',
}

const budget = computed(() => props.preview?.budget || null)
const lines = computed(() => budget.value?.lines || [])
const tableLines = computed(() => lines.value.filter((line) => line.category !== 'LOCAL_TRANSPORT'))
const localLine = computed(() => lines.value.find((line) => line.category === 'LOCAL_TRANSPORT') || null)
const lodging = computed(() => props.preview?.lodging || null)
const liveOffers = computed(() => lodging.value?.offers || [])
const foodPlaces = computed(() => (props.preview?.foodPlaces || []).filter((place) => place?.name))
const busLegs = computed(() => props.preview?.busLegs || [])
const itinerary = computed(() => props.preview?.itinerary || [])
const assumptions = computed(() => props.preview?.assumptions || [])
const suggestions = computed(() => props.preview?.budgetSuggestions || [])

function externalHref(value) {
  return safeExternalUrl(value)
}

function inBudget(offer) {
  const offers = liveOffers.value
  const tier = lodging.value?.selectedTier
  if (offers.some((item) => item.tier === tier)) {
    return offer.tier === tier
  }
  return offer === offers[Math.floor(offers.length / 2)]
}

function lineSource(line) {
  if (line.sourceType && sourceLabel[line.sourceType]) {
    return sourceLabel[line.sourceType]
  }
  if (line.category === 'TRANSPORT' && busLegs.value.length) {
    return 'Canlı fiyat'
  }
  if (line.category === 'LODGING' && liveOffers.value.length) {
    return 'Canlı fiyat'
  }
  if (line.category === 'FOOD' && foodPlaces.value.length) {
    return 'Resmi fiyat'
  }
  return 'Katalog tahmini'
}

function evidenceFallback(line) {
  if (line.category === 'FOOD') return 'Canlı restoran listesi yok; tutar katalog tahmini.'
  if (line.category === 'LODGING') return 'Katalog tahmini, ilan yok.'
  if (line.category === 'TRANSPORT') {
    const notes = assumptions.value.filter((item) => item.field === 'transportMode' && item.message)
    if (notes.length) return notes[notes.length - 1].message
    return 'Katalog tahmini, sefer linki yok.'
  }
  if (line.category === 'OTHER') return "Alt toplamın %10'u."
  return 'Katalog tahmini.'
}

function localDriving(line) {
  return String(line?.label || '').includes('araçla')
}

function localMeta(line) {
  const match = String(line?.label || '').match(/\(([^)]+)\)/)
  return match ? match[1] : ''
}

function localLead(line) {
  if (localDriving(line)) {
    return 'Park ve kısa taksi. Araçla gidildiği için tutar kişi sayısıyla çarpılmaz.'
  }
  if (String(line?.label || '').includes('kişi başı')) {
    return 'Toplu taşıma ve kısa taksi. Günlük bant, gün sayısı ve kişi ile çarpılır.'
  }
  return 'Toplu taşıma ve kısa taksi. Bu kayıt eski günlük bantla duruyor.'
}

function localAside(line) {
  if (!localMeta(line)) {
    return 'Bütçeyi yenileyince tutar kişi sayısına ve stile göre yeniden yazılır.'
  }
  const style = 'Ekonomik stil bandı biraz indirir, konfor biraz çıkarır. Aralık her ilde aynı katalog bandıdır.'
  if (localDriving(line)) {
    return `${style} Şehirler arası yakıt ayrı satırdadır.`
  }
  return `${style} Şehirler arası bilet ayrı satırdadır.`
}

function reviewText(count) {
  if (count == null || count === '') {
    return ''
  }
  return `${new Intl.NumberFormat('tr-TR').format(count)} yorum`
}
</script>

<template>
  <template v-if="preview && budget">
    <slot name="before-tour" />
    <p v-if="preview.priceNote" class="muted">{{ preview.priceNote }}</p>
    <section v-if="showTotals" class="stats">
      <article class="stat hero">
        <span>Toplam</span>
        <strong>{{ formatMoney(budget.total?.min, budget.total?.max, budget.total?.currency) }}</strong>
      </article>
      <article class="stat">
        <span>Kişi başı</span>
        <strong>{{ formatMoney(budget.perPerson?.min, budget.perPerson?.max, budget.perPerson?.currency) }}</strong>
      </article>
      <article class="stat">
        <span>Günlük</span>
        <strong>{{ formatMoney(budget.dailyAverage?.min, budget.dailyAverage?.max, budget.dailyAverage?.currency) }}</strong>
      </article>
    </section>

    <section v-if="localLine" class="panel local-fare">
      <div class="local-fare-top">
        <div>
          <h2>Şehir içi ulaşım</h2>
          <p v-if="localMeta(localLine)" class="local-fare-meta">{{ localMeta(localLine) }}</p>
          <p>{{ localLead(localLine) }}</p>
        </div>
        <div class="local-fare-amount">
          <strong>{{ formatMoney(localLine.min, localLine.max, localLine.currency) }}</strong>
          <span class="chip chip--accent">Katalog tahmini</span>
        </div>
      </div>
      <p class="local-fare-aside">{{ localAside(localLine) }}</p>
    </section>

    <section v-if="!tableLines.length && !localLine" class="panel empty">Bu seçim için fiyat satırı yok.</section>
    <section v-else-if="tableLines.length" class="panel table-wrap">
      <h2>Kalemler</h2>
      <table>
        <thead>
          <tr>
            <th>Kalem</th>
            <th>Veri</th>
            <th class="amount">Aralık</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="line in tableLines" :key="line.label">
            <td>
              <span class="line-label">
                {{ line.label }}
                <span class="chip chip--accent">{{ lineSource(line) }}</span>
              </span>
            </td>
            <td class="evidence">
              <ul v-if="line.category === 'TRANSPORT' && busLegs.length" class="evidence-list">
                <li v-for="leg in busLegs" :key="leg.direction" class="evidence-item">
                  <strong>{{ leg.direction }}</strong>
                  <span v-if="leg.partner">{{ leg.partner }}</span>
                  <span>{{ formatMoney(leg.minAmount, leg.minAmount, leg.currency) }}</span>
                  <a v-if="externalHref(leg.url)" :href="externalHref(leg.url)" target="_blank" rel="noopener">En ucuz bileti aç</a>
                  <span v-else>kaynak linki yok</span>
                </li>
              </ul>
              <ul v-else-if="line.category === 'LODGING' && liveOffers.length" class="evidence-list">
                <li
                  v-for="offer in liveOffers"
                  :key="`${offer.tier}-${offer.name}`"
                  class="evidence-item"
                  :class="{ picked: inBudget(offer) }"
                >
                  <strong>{{ offer.name }}</strong>
                  <span>{{ tierLabel[offer.tier] || offer.tier }}</span>
                  <span v-if="inBudget(offer)">bütçedeki ilan</span>
                  <span>{{ formatMoney(offer.pricePerNight, offer.pricePerNight) }} / gece</span>
                  <a v-if="externalHref(offer.url)" :href="externalHref(offer.url)" target="_blank" rel="noopener">İlanı aç</a>
                  <span v-else>kaynak linki yok</span>
                </li>
              </ul>
              <ul v-else-if="line.category === 'FOOD' && foodPlaces.length" class="evidence-list">
                <li v-for="(place, index) in foodPlaces" :key="`${place.name}-${index}`" class="evidence-item">
                  <strong>{{ place.name }}</strong>
                  <span v-if="place.michelin" class="chip chip--accent">Michelin</span>
                  <span v-if="place.rating != null">{{ place.rating }}</span>
                  <span v-if="reviewText(place.reviewCount)">{{ reviewText(place.reviewCount) }}</span>
                  <span v-if="place.mealMin != null && place.mealMax != null">{{ formatMoney(place.mealMin, place.mealMax) }}</span>
                  <a v-if="externalHref(place.url)" :href="externalHref(place.url)" target="_blank" rel="noopener">Kaynağı aç</a>
                </li>
              </ul>
              <p v-else class="evidence-empty">{{ evidenceFallback(line) }}</p>
            </td>
            <td class="amount">{{ formatMoney(line.min, line.max, line.currency) }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="assumptions.length" class="panel">
      <h2>Varsayımlar</h2>
      <ul class="plain-list">
        <li v-for="(item, index) in assumptions" :key="index">
          {{ item.message }}
        </li>
      </ul>
    </section>

    <section v-if="suggestions.length" class="panel">
      <h2>Bütçe önerileri</h2>
      <ul class="plain-list">
        <li v-for="(item, index) in suggestions" :key="index">{{ item.message }}</li>
      </ul>
    </section>

    <section v-if="itinerary.length" class="panel tour">
      <h2>Gezi turu</h2>
      <p class="muted">Bütün günler burada. Yemek, varış ve seçilen duraklar sırayla.</p>
      <article v-for="day in itinerary" :key="day.dayNumber" class="day-block">
        <h3>Gün {{ day.dayNumber }} · {{ prettyTripDate(day.date) }}</h3>
        <ol>
          <li v-for="(item, index) in day.items" :key="index" class="tour-stop">
            <span class="tour-kind">{{ itemKind[item.type] || 'Durak' }}</span>
            <strong>{{ item.title }}</strong>
            <span v-if="item.estimatedMinutes" class="tour-mins">{{ item.estimatedMinutes }} dk</span>
            <p v-if="item.note" class="muted">{{ item.note }}</p>
          </li>
        </ol>
      </article>
    </section>
  </template>
  <section v-else class="panel empty-block">
    <Icon name="tag" />
    <div>
      <strong>Bütçe önizlemesi yok.</strong>
    </div>
  </section>
</template>
