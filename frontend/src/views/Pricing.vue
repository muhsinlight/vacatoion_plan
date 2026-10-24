<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AlertMessage from '../components/AlertMessage.vue'
import EmptyState from '../components/EmptyState.vue'
import PlanTripEvidence from '../components/plan/PlanTripEvidence.vue'
import SearchBar from '../components/plan/SearchBar.vue'
import OptionCardGroup from '../components/plan/OptionCardGroup.vue'
import ComfortSelector from '../components/plan/ComfortSelector.vue'
import InterestPicker from '../components/plan/InterestPicker.vue'
import AdvancedOptions from '../components/plan/AdvancedOptions.vue'
import ActivitySuggestions from '../components/plan/ActivitySuggestions.vue'
import BudgetSummary from '../components/plan/BudgetSummary.vue'
import { useOnline } from '../composables/useOnline'
import { usePrices } from '../composables/usePrices'
import { INTEREST_OPTIONS, STAY_OPTIONS, STYLE_OPTIONS, TRANSPORT_OPTIONS } from '../constants/tripForm'
import { INTEREST_ICONS, STAY_ICONS, TRANSPORT_ICONS } from '../constants/planIcons'
import { createTrip, listActivities } from '../services/pricing'
import { emptyTripDraft, readTripDraft, writeTripDraft } from '../utils/tripDraft'
import { isoDate } from '../utils/format'
import { resolveCityName } from '../utils/tripDisplay'
import { addCustomActivityName, removeCustomActivityName, toggleActivityId, toggleInterest } from '../utils/customActivities'
import { buildTripPayload, groqContextFromForm } from '../utils/tripPayload'

const route = useRoute()
const router = useRouter()

const {
  destinations,
  quote,
  loadingDestinations,
  loadingQuote,
  destinationError,
  quoteError,
  loadDestinations,
  loadQuote: fetchQuote,
} = usePrices()

const form = reactive({
  originCity: '',
  destinationCity: '',
  startDate: '',
  endDate: '',
  adults: 2,
  transportMode: 'BUS',
  accommodationType: 'HOTEL',
  travelStyle: 'BALANCED',
  interests: [],
  totalBudget: '',
  perPersonBudget: '',
  selectedActivityIds: [],
  specialNeeds: '',
  customActivityNames: [],
})

const catalogActivities = ref([])
const activitiesLoading = ref(false)
const activitiesError = ref('')
const hydrated = ref(false)
const hasAsked = ref(false)
const saving = ref(false)
const saveError = ref('')
const deltaText = ref('')
const baselineTotal = ref(null)

const nights = computed(() => {
  const start = Date.parse(form.startDate)
  const end = Date.parse(form.endDate)
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) {
    return undefined
  }
  return Math.round((end - start) / 86400000)
})

const budget = computed(() => quote.value?.budget || null)
const groqContext = computed(() => groqContextFromForm(form))
const online = useOnline()

const canQuote = computed(
  () =>
    Boolean(form.originCity) &&
    Boolean(form.destinationCity) &&
    form.originCity !== form.destinationCity &&
    Boolean(form.startDate) &&
    Boolean(form.endDate) &&
    form.endDate >= form.startDate,
)

const quoteChecks = computed(() => ({
  origin: Boolean(form.originCity),
  destination: Boolean(form.destinationCity) && form.destinationCity !== form.originCity,
  dates: Boolean(form.startDate && form.endDate && form.endDate >= form.startDate),
}))

function cityName(key) {
  return resolveCityName(destinations.value, key)
}

function payload() {
  return buildTripPayload(form)
}

function snapshotTotal(preview) {
  const total = preview?.budget?.total
  if (total?.min == null || total?.max == null) {
    return null
  }
  return {
    min: Number(total.min),
    max: Number(total.max),
    currency: total.currency || 'TRY',
  }
}

function describeTotalDelta(previous, next) {
  if (!previous || next?.min == null || next?.max == null) {
    return ''
  }
  const currency = next.currency || previous.currency || 'TRY'
  const format = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 0 })
  const amount = (diff) => `${format.format(Math.abs(diff))} ${currency}`
  const minDiff = Number(next.min) - Number(previous.min)
  if (minDiff) {
    return minDiff > 0 ? `Alt sınır ${amount(minDiff)} arttı` : `Alt sınır ${amount(minDiff)} indi`
  }
  const maxDiff = Number(next.max) - Number(previous.max)
  if (!maxDiff) {
    return ''
  }
  return maxDiff > 0 ? `Üst sınır ${amount(maxDiff)} arttı` : `Üst sınır ${amount(maxDiff)} indi`
}

async function loadQuote() {
  quoteError.value = ''
  saveError.value = ''
  if (!form.originCity || !form.destinationCity) {
    quoteError.value = 'Kalkış ve varış seçin'
    return
  }
  if (form.originCity === form.destinationCity) {
    quoteError.value = 'Kalkış ve varış farklı olmalı'
    return
  }
  if (!form.startDate || !form.endDate) {
    quoteError.value = 'Gidiş ve dönüş tarihi seçin'
    return
  }
  if (form.endDate < form.startDate) {
    quoteError.value = 'Dönüş tarihi gidişten önce olamaz'
    return
  }
  hasAsked.value = true
  deltaText.value = ''
  baselineTotal.value = snapshotTotal(quote.value)
  await fetchQuote(payload())
}

function chooseOption(field, value) {
  if (form[field] === value) {
    return
  }
  form[field] = value
}

function toggleInterestChip(id) {
  form.interests = toggleInterest(form.interests, id)
}

function toggleActivity(id) {
  form.selectedActivityIds = toggleActivityId(form.selectedActivityIds, id)
}

function addCustomActivity(name) {
  const next = addCustomActivityName(form.customActivityNames, name)
  if (next === form.customActivityNames) {
    return
  }
  form.customActivityNames = next
}

function removeCustomActivity(name) {
  form.customActivityNames = removeCustomActivityName(form.customActivityNames, name)
}

async function loadActivitiesForDestination(cityKey, { keepSelection = false } = {}) {
  if (!cityKey) {
    catalogActivities.value = []
    activitiesError.value = ''
    if (!keepSelection) {
      form.selectedActivityIds = []
    }
    return
  }
  activitiesLoading.value = true
  activitiesError.value = ''
  try {
    catalogActivities.value = await listActivities(cityKey)
    const defaults = catalogActivities.value.filter((item) => item.defaultSelected).map((item) => item.id)
    if (keepSelection) {
      const allowed = new Set(catalogActivities.value.map((item) => item.id))
      const kept = form.selectedActivityIds.filter((id) => allowed.has(id))
      form.selectedActivityIds = kept.length ? kept : [...defaults]
    } else {
      form.selectedActivityIds = defaults.length ? [...defaults] : []
    }
  } catch (err) {
    catalogActivities.value = []
    form.selectedActivityIds = []
    activitiesError.value = err.message || 'Etkinlik listesi alınamadı'
  } finally {
    activitiesLoading.value = false
  }
}

async function saveTrip() {
  saveError.value = ''
  if (!quote.value?.budget) {
    saveError.value = 'Kaydetmek için önce bütçeyi hesaplayın'
    return
  }
  saving.value = true
  try {
    const saved = await createTrip(payload())
    await router.push({ name: 'tripDetail', params: { id: saved.id } })
  } catch (err) {
    saveError.value = err.message || 'Kaydedilemedi'
  } finally {
    saving.value = false
  }
}

watch(quote, (next) => {
  deltaText.value = describeTotalDelta(baselineTotal.value, next?.budget?.total)
})

watch(
  () => form.originCity,
  (origin) => {
    if (origin && origin === form.destinationCity) {
      form.destinationCity = ''
    }
  },
)

watch(
  () => form.destinationCity,
  (cityKey, previous) => {
    if (!hydrated.value || cityKey === previous) {
      return
    }
    loadActivitiesForDestination(cityKey)
  },
)

watch(
  form,
  () => {
    if (!hydrated.value) {
      return
    }
    writeTripDraft(form)
  },
  { deep: true },
)

function showBudgetDetail() {
  const target = document.getElementById('vp-trip-evidence')
  if (!target) {
    return
  }
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  target.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' })
}

onMounted(async () => {
  const draft = readTripDraft()
  Object.assign(form, emptyTripDraft(), draft || {})
  const today = isoDate(0)
  if (form.startDate && form.startDate < today) {
    const spanMs = Date.parse(form.endDate) - Date.parse(form.startDate)
    const nights = Number.isFinite(spanMs) ? Math.max(1, Math.round(spanMs / 86400000)) : 2
    form.startDate = today
    form.endDate = isoDate(nights)
  }
  await loadDestinations()
  const keys = new Set(destinations.value.map((city) => city.cityKey))
  if (!keys.has(form.originCity)) {
    form.originCity = ''
  }
  if (!keys.has(form.destinationCity) || form.destinationCity === form.originCity) {
    form.destinationCity = ''
  }
  await loadActivitiesForDestination(form.destinationCity, { keepSelection: true })
  hydrated.value = true
  writeTripDraft(form)
  const shouldRun = route.query.run === '1'
  if (shouldRun) {
    await router.replace({ name: 'pricing', query: {} })
    if (canQuote.value) {
      await loadQuote()
    }
  }
})

</script>

<template>
  <div class="vp-plan mx-auto w-full max-w-[1120px] px-0 pb-10 font-sans">
    <header class="vp-plan-head">
      <h1>Planla</h1>
      <p class="vp-plan-lead">
        Nereden, nereye, tarihler ve kişi sayısı. Hesapla tahmini bütçeyi çıkarır.
      </p>
    </header>

    <p class="vp-sr" aria-live="polite">
      {{ loadingQuote ? 'Bütçe hesaplanıyor' : saveError || quoteError }}
    </p>

    <AlertMessage :message="destinationError" />

    <div v-if="loadingDestinations" class="rounded-[16px] border border-ink-200 bg-white p-4" aria-busy="true">
      <div class="vp-skeleton h-11 w-full rounded-[12px]"></div>
      <div class="mt-3 flex gap-2">
        <div class="vp-skeleton h-11 w-1/3 rounded-[12px]"></div>
        <div class="vp-skeleton h-11 w-1/3 rounded-[12px]"></div>
        <div class="vp-skeleton h-11 w-24 rounded-[12px]"></div>
      </div>
      <p class="mt-3 text-sm text-ink-600">İller yükleniyor…</p>
    </div>

    <EmptyState
      v-else-if="!destinations.length"
      icon="pin"
      title="İl listesi yok"
      body="Katalog gelmeden bütçe hesaplanamaz. Bağlantıyı kontrol edip sayfayı yenileyin."
    />

    <template v-else>
      <SearchBar
        v-model:origin-city="form.originCity"
        v-model:destination-city="form.destinationCity"
        v-model:start-date="form.startDate"
        v-model:end-date="form.endDate"
        v-model:adults="form.adults"
        :destinations="destinations"
        :loading="loadingQuote"
        :ready="canQuote"
        :live="hasAsked"
        @submit="loadQuote"
      />

      <div class="vp-plan-hero">
        <aside class="vp-budget-sticky" aria-label="Tahmini bütçe">
          <AlertMessage :message="saveError" />
          <BudgetSummary
            class="vp-budget-panel vp-budget-summary"
            :budget="budget"
            :adults="form.adults"
            :nights="nights"
            :loading="loadingQuote"
            :error="quoteError"
            :saving="saving"
            :delta-text="deltaText"
            :ready="canQuote"
            :online="online"
            :checks="quoteChecks"
            @save="saveTrip"
            @calculate="loadQuote"
            @details="showBudgetDetail"
          />
        </aside>

        <div class="vp-plan-primary">
          <div class="vp-plan-card">
            <div class="vp-block-head">
              <h2>Seyahat tercihleri</h2>
              <p>Ulaşım, konaklama ve konfor bütçenin ana kalemlerini belirler.</p>
            </div>
            <OptionCardGroup
              title="Ulaşım"
              name="vp-transport"
              :options="TRANSPORT_OPTIONS"
              :icons="TRANSPORT_ICONS"
              :model-value="form.transportMode"
              @update:model-value="chooseOption('transportMode', $event)"
            />
            <OptionCardGroup
              title="Konaklama"
              name="vp-stay"
              :options="STAY_OPTIONS"
              :icons="STAY_ICONS"
              :model-value="form.accommodationType"
              @update:model-value="chooseOption('accommodationType', $event)"
            />
            <ComfortSelector
              :options="STYLE_OPTIONS"
              :model-value="form.travelStyle"
              @update:model-value="chooseOption('travelStyle', $event)"
            />
            <InterestPicker
              :options="INTEREST_OPTIONS"
              :icons="INTEREST_ICONS"
              :model-value="form.interests"
              @update:model-value="toggleInterestChip"
            />
          </div>

          <AdvancedOptions
            v-model:total-budget="form.totalBudget"
            v-model:per-person-budget="form.perPersonBudget"
            v-model:special-needs="form.specialNeeds"
          />
        </div>
      </div>

      <div class="vp-plan-below">
          <!-- Eklenen ozel etkinlikler; yalnizca varsa yer kaplar. -->
          <div v-if="form.customActivityNames.length" class="flex flex-wrap items-center gap-2">
            <span class="text-xs font-semibold text-ink-700">Eklenenler:</span>
            <span
              v-for="name in form.customActivityNames"
              :key="name"
              class="flex items-center gap-1.5 rounded-full bg-brand-700 py-1 pl-3 pr-1.5 text-xs font-semibold text-white"
            >
              {{ name }}
              <button
                type="button"
                class="grid size-5 place-items-center rounded-full bg-white/15 transition hover:bg-white/30 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-white"
                :aria-label="`${name} etkinliğini kaldır`"
                @click="removeCustomActivity(name)"
              >
                ×
              </button>
            </span>
          </div>

          <AlertMessage :message="activitiesError" />

          <ActivitySuggestions
            :city-key="form.destinationCity"
            :city-label="cityName(form.destinationCity)"
            :context="groqContext"
            :catalog-activities="catalogActivities"
            :catalog-loading="activitiesLoading"
            :selected-ids="form.selectedActivityIds"
            :added-names="form.customActivityNames"
            @toggle-activity="toggleActivity"
            @add-custom="addCustomActivity"
            @remove-custom="removeCustomActivity"
          />

          <!-- Kaynak linkleri, varsayimlar ve gun gun tur asagida kalir. -->
          <div v-if="quote?.budget" id="vp-trip-evidence" class="vp-evidence-anchor">
            <PlanTripEvidence :preview="quote" />
          </div>
      </div>
    </template>
  </div>
</template>
