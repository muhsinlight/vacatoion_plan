<script setup>
import { computed, ref, watch } from 'vue'
import Icon from '../Icon.vue'
import { suggestActivities } from '../../services/pricing'
import { formatMoney } from '../../utils/format'
import { INTEREST_ICONS } from '../../constants/planIcons'
import { MOCK_ACTIVITIES } from '../../mocks/planMocks'

const MARKS = ['museum', 'leaf', 'utensils', 'beach', 'compass', 'bag']

/*
 * Tek bir kart grid'i uc kaynaktan beslenir, siralama su:
 *   1) katalog etkinlikleri (id ile secilir, tutara dogrudan girer)
 *   2) katalog bossa yapay zeka onerileri (ada gore eklenir)
 *   3) ikisi de bossa ornek veri, kartlarda etiketiyle
 * Bos durum blogu yok; veri yoksa iskelet ya da tek satir bilgi metni var.
 */
const props = defineProps({
  cityKey: { type: String, default: '' },
  cityLabel: { type: String, default: '' },
  context: { type: Object, default: () => ({}) },
  catalogActivities: { type: Array, default: () => [] },
  catalogLoading: { type: Boolean, default: false },
  selectedIds: { type: Array, default: () => [] },
  addedNames: { type: Array, default: () => [] },
})

const emit = defineEmits(['toggle-activity', 'add-custom', 'remove-custom'])

const aiItems = ref([])
const aiLoading = ref(false)
const aiError = ref('')
const aiTried = ref(false)

const canAsk = computed(
  () => Boolean(props.cityKey && props.context?.startDate && props.context?.endDate),
)

/* Katalog bos oldugunda yapay zekaya dusulur. */
const needsAi = computed(() => !props.catalogLoading && !props.catalogActivities.length)

const loading = computed(() => props.catalogLoading || aiLoading.value)

const source = computed(() => {
  if (props.catalogActivities.length) {
    return 'catalog'
  }
  if (aiItems.value.length) {
    return 'ai'
  }
  if (aiTried.value && !aiLoading.value) {
    return 'mock'
  }
  return 'none'
})

const cards = computed(() => {
  if (source.value === 'catalog') {
    return props.catalogActivities.map((item) => ({
      key: `catalog-${item.id}`,
      id: item.id,
      name: item.name,
      description: item.description || '',
      minutes: item.estimatedMinutes,
      price: priceText(item),
      mark: markIcon(item),
      mode: 'id',
      added: props.selectedIds.includes(item.id),
      mock: false,
    }))
  }
  if (source.value === 'ai') {
    return aiItems.value.map((item, index) => ({
      key: `ai-${item.name}-${index}`,
      id: null,
      name: item.name,
      description: item.description || '',
      minutes: item.estimatedMinutes,
      price: priceText(item),
      mark: markIcon(item),
      mode: 'name',
      added: props.addedNames.includes(String(item.name || '').trim()),
      mock: false,
    }))
  }
  if (source.value === 'mock') {
    return MOCK_ACTIVITIES.map((item) => ({
      key: item.id,
      id: null,
      name: item.name,
      description: item.description,
      minutes: item.estimatedMinutes,
      price: priceText(item),
      mark: markIcon(item),
      mode: 'name',
      added: props.addedNames.includes(item.name),
      mock: true,
    }))
  }
  return []
})

const sourceNote = computed(() => {
  if (source.value === 'catalog') {
    return props.cityLabel ? `${props.cityLabel} kataloğundan` : 'Katalogdan'
  }
  if (source.value === 'ai') {
    return 'Yapay zeka önerisi'
  }
  if (source.value === 'mock') {
    return 'Örnek veri'
  }
  return ''
})

/*
 * Katalog ve yapay zeka yanitlarinda fiyat alani her zaman gelmiyor;
 * gelmediginde uydurulmaz, kartta sure gosterilir.
 */
function markIcon(item) {
  const key = String(item.category || item.interest || '').toLowerCase()
  if (INTEREST_ICONS[key]) {
    return INTEREST_ICONS[key]
  }
  const text = String(item.name || '')
  let index = 0
  for (let i = 0; i < text.length; i += 1) {
    index = (index + text.charCodeAt(i)) % MARKS.length
  }
  return MARKS[index]
}

function priceText(item) {
  const fee = item.entryFeePerPerson
  if (fee?.min != null && fee?.max != null) {
    return formatMoney(fee.min, fee.max, fee.currency || 'TRY')
  }
  const min = item.priceMin ?? item.minPrice ?? null
  const max = item.priceMax ?? item.maxPrice ?? null
  if (min == null || max == null) {
    return ''
  }
  return formatMoney(min, max, item.currency || 'TRY')
}

async function loadAi() {
  if (!canAsk.value) {
    return
  }
  aiLoading.value = true
  aiError.value = ''
  try {
    const response = await suggestActivities(props.cityKey, {
      startDate: props.context.startDate,
      endDate: props.context.endDate,
      adults: props.context.adults ?? 2,
      travelStyle: props.context.travelStyle ?? 'BALANCED',
    })
    aiItems.value = response.activities ?? []
  } catch (error) {
    aiItems.value = []
    aiError.value = error.message || 'Öneriler alınamadı'
  } finally {
    aiLoading.value = false
    aiTried.value = true
  }
}

function onCard(card) {
  if (card.mode === 'id') {
    emit('toggle-activity', card.id)
    return
  }
  if (card.added) {
    emit('remove-custom', card.name)
    return
  }
  emit('add-custom', card.name)
}

watch(
  () => [props.cityKey, props.context?.startDate, props.context?.endDate, needsAi.value],
  () => {
    aiTried.value = false
    aiItems.value = []
    aiError.value = ''
    if (needsAi.value && canAsk.value) {
      loadAi()
    }
  },
  { immediate: true },
)
</script>

<template>
  <section class="vp-act-section min-w-0">
    <div class="vp-act-head">
      <div>
        <h2>Etkinlik önerileri</h2>
        <p>Rotana eklemek istediklerini seç. Seçilenler bütçeye yansır.</p>
      </div>
      <span v-if="sourceNote" class="vp-act-source">{{ sourceNote }}</span>
      <span
        v-if="source === 'mock'"
        class="rounded-full bg-amber-accent-soft px-2 py-0.5 text-[11px] font-semibold text-amber-accent"
      >
        Örnek veri
      </span>
    </div>

    <!-- Yuklenirken: iskelet kartlar, bos durum blogu yerine. -->
    <div v-if="loading" class="vp-activity-grid grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3" aria-busy="true">
      <div
        v-for="n in 6"
        :key="n"
        class="rounded-[16px] border border-ink-200 bg-white p-4"
      >
        <div class="vp-skeleton h-4 w-2/3 rounded"></div>
        <div class="vp-skeleton mt-2.5 h-3 w-full rounded"></div>
        <div class="vp-skeleton mt-1.5 h-3 w-4/5 rounded"></div>
        <div class="vp-skeleton mt-4 h-8 w-24 rounded-[10px]"></div>
      </div>
    </div>

    <div v-else-if="cards.length" class="vp-activity-grid grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      <article
        v-for="card in cards"
        :key="card.key"
        class="vp-act-card"
        :class="card.added ? 'is-added' : 'is-idle'"
      >
        <button
          type="button"
          class="vp-act-hit"
          :aria-pressed="card.added"
          :aria-label="card.added ? `${card.name} eklendi` : `${card.name} ekle`"
          @click="onCard(card)"
        >
          <span class="vp-act-title-row">
            <span class="vp-act-mark" aria-hidden="true">
              <Icon :name="card.mark" class="size-4" />
            </span>
            <h3>{{ card.name }}</h3>
          </span>
          <p class="vp-act-desc">{{ card.description }}</p>
          <div class="vp-act-meta flex flex-wrap items-center gap-x-3 gap-y-1">
            <span v-if="card.price" class="font-semibold tabular-nums">{{ card.price }}</span>
            <span v-else-if="source === 'catalog'" class="text-ink-500">Giriş ücreti katalogda yok</span>
            <span v-else-if="source === 'ai'" class="text-ink-500">Tutar bütçede (Diğer) satırına yansır</span>
            <span v-if="card.minutes" class="flex items-center gap-1 text-ink-500">
              <Icon name="clock" class="size-3.5" /> {{ card.minutes }} dk
            </span>
          </div>
          <span class="vp-act-cta" :class="card.added ? 'is-on' : 'is-off'">
            <Icon :name="card.added ? 'check' : 'plus'" class="size-3.5" />
            {{ card.added ? 'Eklendi' : 'Ekle' }}
          </span>
        </button>
      </article>
    </div>

    <div v-else-if="aiError" class="rounded-[16px] border border-ink-200 bg-white p-4">
      <p class="text-sm text-ink-700">{{ aiError }}</p>
      <button type="button" class="vp-retry mt-3" @click="loadAi">
        <Icon name="refresh" class="size-4" />
        Önerileri yeniden dene
      </button>
    </div>
    <p v-else class="rounded-[16px] border border-dashed border-ink-200 bg-white px-4 py-5 text-sm text-ink-600">
      Varış ili ve tarih seçilince öneriler burada listelenir.
    </p>
  </section>
</template>

<style scoped>
.vp-act-card {
  display: flex;
  height: 100%;
  padding: 0;
  border: 1px solid var(--color-ink-200, #e5e7eb);
  border-radius: 12px;
  background: #fff;
  transition:
    border-color 120ms ease,
    transform 120ms ease;
}

.vp-act-card.is-added {
  border-color: var(--color-brand-300, #79b495);
  background: var(--color-brand-50, #eef6f1);
}

.vp-act-card:hover {
  border-color: var(--color-brand-300, #79b495);
  transform: translateY(-2px);
}

.vp-act-hit {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: stretch;
  width: 100%;
  min-height: 100%;
  margin: 0;
  padding: 1rem 1.05rem 0.95rem;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: inherit;
  font: inherit;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
}

.vp-act-hit:focus-visible {
  outline: 2px solid var(--color-brand-700, #0f5132);
  outline-offset: 2px;
}

.vp-act-title-row {
  display: flex;
  align-items: flex-start;
  gap: 0.7rem;
}

.vp-act-mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 10px;
  background: var(--color-brand-50, #eef6f1);
  color: var(--color-brand-700, #0f5132);
}

.vp-act-card.is-added .vp-act-mark {
  background: #fff;
}

.vp-act-card h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-ink-900, #111827);
  line-height: 1.35;
}

.vp-act-card .vp-act-desc {
  display: -webkit-box;
  margin-top: 0.7rem;
  min-height: 2.9em;
  overflow: hidden;
  font-size: 0.875rem;
  line-height: 1.45;
  color: var(--color-ink-600, #4b5563);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.vp-act-card .vp-act-meta {
  margin-top: 0.85rem;
  font-size: 0.875rem;
  color: var(--color-ink-700, #374151);
}

.vp-act-cta {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  align-self: flex-start;
  margin-top: auto;
  padding-top: 1rem;
  font-size: 0.875rem;
  font-weight: 700;
}

.vp-act-cta.is-off,
.vp-act-cta.is-on {
  min-height: 2.5rem;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
}

.vp-act-cta.is-off {
  border: 1px solid var(--color-brand-700, #0f5132);
  color: var(--color-brand-700, #0f5132);
}

.vp-act-cta.is-on {
  border: 1px solid var(--color-brand-300, #79b495);
  background: #fff;
  color: var(--color-brand-800, #0b3f27);
}

@media (prefers-reduced-motion: reduce) {
  .vp-act-card:hover {
    transform: none;
  }
}
</style>
