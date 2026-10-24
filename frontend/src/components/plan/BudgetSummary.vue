<script setup>
import { computed } from 'vue'
import Icon from '../Icon.vue'
import AnimatedAmount from './AnimatedAmount.vue'
import { formatMoney } from '../../utils/format'
import { CATEGORY_COLORS, CATEGORY_LABELS, sourceBadge } from '../../constants/planIcons'

/*
 * Butce kartinda ayri bir "guncelle" adimi yok: `budget` prop'u
 * degistiginde kart kendini yeniler.
 */
const props = defineProps({
  budget: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  isMock: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  deltaText: { type: String, default: '' },
  ready: { type: Boolean, default: false },
  online: { type: Boolean, default: true },
  adults: { type: Number, default: 0 },
  nights: { type: Number, default: undefined },
  checks: {
    type: Object,
    default: () => ({ origin: false, destination: false, dates: false }),
  },
})

defineEmits(['save', 'calculate', 'details'])

const total = computed(() => props.budget?.total || null)

const tripMeta = computed(() => {
  const parts = []
  const people = Number(props.adults)
  if (Number.isFinite(people) && people > 0) {
    parts.push(`${people} kişi`)
  }
  const nightCount = Number(props.nights)
  if (Number.isFinite(nightCount) && nightCount > 0) {
    parts.push(`${nightCount} gece`)
  } else if (nightCount === 0) {
    parts.push('Günübirlik')
  }
  return parts.join(' · ')
})

const rows = computed(() =>
  (props.budget?.lines || []).map((line) => {
    const badge = sourceBadge(line.sourceType)
    const mid = (Number(line.min) + Number(line.max)) / 2
    return {
      key: line.label,
      label: CATEGORY_LABELS[line.category] || line.label,
      detail: line.label,
      amount: formatMoney(line.min, line.max, line.currency),
      color: CATEGORY_COLORS[line.category] || '#9ca3af',
      weight: Number.isFinite(mid) ? Math.max(mid, 0) : 0,
      badge,
    }
  }),
)

const weightTotal = computed(() => rows.value.reduce((sum, row) => sum + row.weight, 0))

function share(row) {
  if (!weightTotal.value) {
    return 0
  }
  return (row.weight / weightTotal.value) * 100
}
</script>

<template>
  <section
    class="vp-budget-summary"
    aria-labelledby="vp-budget-title"
    :aria-busy="loading ? 'true' : 'false'"
  >
    <div class="flex items-center justify-between gap-2">
      <h2 id="vp-budget-title" class="text-sm font-semibold text-ink-900">Tahmini bütçe</h2>
      <span
        v-if="isMock"
        class="rounded-full bg-amber-accent-soft px-2 py-0.5 text-[11px] font-semibold text-amber-accent"
      >
        Örnek veri
      </span>
      <span
        v-else-if="loading && budget"
        class="text-xs font-medium text-ink-600"
      >
        Güncelleniyor…
      </span>
    </div>

    <div v-if="loading && !budget" class="mt-4" aria-busy="true">
      <div class="vp-skeleton h-10 w-4/5 rounded"></div>
      <div class="vp-skeleton mt-3 h-3 w-2/3 rounded"></div>
      <div class="vp-skeleton mt-5 h-3 w-full rounded-full"></div>
      <div v-for="n in 4" :key="n" class="vp-skeleton mt-3 h-3.5 w-full rounded"></div>
    </div>

    <div v-else-if="!online && !budget" class="vp-budget-empty">
      <p class="vp-budget-empty-copy">Çevrimdışısınız. Bağlantı gelince bütçe burada görünür.</p>
    </div>

    <div v-else-if="!budget && !error" class="vp-budget-empty">
      <ul class="vp-sr">
        <li>{{ checks.origin ? 'Kalkış seçildi' : 'Kalkış eksik' }}</li>
        <li>{{ checks.destination ? 'Varış seçildi' : 'Varış eksik' }}</li>
        <li>{{ checks.dates ? 'Tarihler seçildi' : 'Tarihler eksik' }}</li>
      </ul>
      <p class="vp-budget-empty-copy">
        Rotanı ve tercihlerini seç, tahmini bütçeni hesaplayalım.
      </p>
      <button
        v-if="ready"
        type="button"
        class="vp-budget-save mt-4"
        @click="$emit('calculate')"
      >
        <Icon name="search" class="size-4" />
        Bütçeyi hesapla
      </button>
    </div>

    <div v-else-if="!budget && error" class="vp-budget-empty" role="alert">
      <p class="vp-budget-empty-copy">{{ error }}</p>
      <button type="button" class="vp-retry mt-3" @click="$emit('calculate')">
        <Icon name="refresh" class="size-4" />
        Yeniden dene
      </button>
    </div>

    <template v-else>
      <p
        v-if="error"
        class="mt-3 rounded-[10px] border border-amber-accent/30 bg-amber-accent-soft px-3 py-2 text-xs leading-relaxed text-ink-700"
        role="alert"
      >
        {{ error }}
      </p>
      <!-- `:key` degisince kisa vurgu oynar, yani tutar yenilendigi fark edilir. -->
      <p class="vp-budget-kicker">Tahmini toplam</p>
      <p
        :key="`${total?.min}-${total?.max}`"
        class="vp-budget-total vp-highlight"
        aria-live="polite"
      >
        <AnimatedAmount :min="total?.min" :max="total?.max" :currency="total?.currency" />
      </p>
      <p v-if="tripMeta" class="vp-budget-meta">{{ tripMeta }}</p>
      <p v-if="deltaText" class="mt-1 text-xs font-medium text-amber-accent">{{ deltaText }}</p>

      <dl class="vp-budget-kpis">
        <div class="vp-budget-kpi">
          <dt>Kişi başı</dt>
          <dd>
            {{ formatMoney(budget.perPerson?.min, budget.perPerson?.max, budget.perPerson?.currency) }}
          </dd>
        </div>
        <div class="vp-budget-kpi">
          <dt>Günlük</dt>
          <dd>
            {{ formatMoney(budget.dailyAverage?.min, budget.dailyAverage?.max, budget.dailyAverage?.currency) }}
          </dd>
        </div>
      </dl>

      <template v-if="rows.length">
        <div
          class="vp-budget-bar"
          role="img"
          aria-label="Kategori kırılımı"
        >
          <span
            v-for="row in rows"
            :key="row.key"
            :style="{ width: `${share(row)}%`, backgroundColor: row.color }"
          />
        </div>

        <ul class="vp-budget-lines">
          <li v-for="row in rows" :key="row.key" class="vp-budget-line">
            <span
              class="vp-budget-dot"
              :style="{ backgroundColor: row.color }"
              aria-hidden="true"
            />
            <span class="vp-budget-line-main">
              <span class="vp-budget-line-name">
                {{ row.label }}
                <span
                  class="vp-budget-badge"
                  :class="row.badge.live ? 'is-live' : 'is-est'"
                  :title="row.badge.live
                    ? 'Güncel fiyat bandından alındı'
                    : 'Katalog tahmininden hesaplandı'"
                >
                  {{ row.badge.label }}
                </span>
              </span>
              <span class="vp-budget-line-amount">{{ row.amount }}</span>
            </span>
          </li>
        </ul>
      </template>

      <button type="button" class="vp-budget-more" @click="$emit('details')">
        Detaylı bütçeyi göster
      </button>

      <button
        type="button"
        class="vp-budget-save mt-3 flex w-full items-center justify-center gap-1.5 disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="saving || loading"
        @click="$emit('save')"
      >
        <Icon name="case" class="size-4" />
        {{ saving ? 'Kaydediliyor…' : 'Seyahati kaydet' }}
      </button>
    </template>
  </section>
</template>
