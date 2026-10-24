<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ActivityGroqSuggestions from '../components/ActivityGroqSuggestions.vue'
import AlertMessage from '../components/AlertMessage.vue'
import Icon from '../components/Icon.vue'
import TripPreviewSections from '../components/TripPreviewSections.vue'
import {
  deleteTrip,
  getTrip,
  getTripRequest,
  listDestinations,
  previewSavedTrip,
  refreshTripBudget,
  updateTrip,
} from '../services/pricing'
import { STAY_LABEL, STYLE_LABEL, TRANSPORT_LABEL } from '../constants/tripForm'
import { addCustomActivityName } from '../utils/customActivities'
import { prettyTripDate, resolveCityName } from '../utils/tripDisplay'

const route = useRoute()
const router = useRouter()
const tripId = computed(() => Number(route.params.id))

const destinations = ref([])
const summary = ref(null)
const tripRequest = ref(null)
const preview = ref(null)
const loading = ref(true)
const error = ref('')
const busy = ref(false)
const groqNotice = ref('')

const transportLabel = TRANSPORT_LABEL
const stayLabel = STAY_LABEL
const styleLabel = STYLE_LABEL

const groqContext = computed(() => {
  if (!tripRequest.value) {
    return {}
  }
  return {
    startDate: tripRequest.value.startDate,
    endDate: tripRequest.value.endDate,
    adults: tripRequest.value.adults,
    travelStyle: tripRequest.value.travelStyle,
  }
})

const destinationKey = computed(() => tripRequest.value?.destinationCity || summary.value?.destinationCity || '')

const customActivities = computed(() => tripRequest.value?.customActivityNames ?? [])

function cityName(key) {
  return resolveCityName(destinations.value, key)
}

function travelerLine() {
  if (!tripRequest.value) {
    return ''
  }
  return `${tripRequest.value.adults} yetişkin`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    destinations.value = await listDestinations()
    summary.value = await getTrip(tripId.value)
    tripRequest.value = await getTripRequest(tripId.value)
    preview.value = await previewSavedTrip(tripId.value)
  } catch (err) {
    error.value = err.message || 'Seyahat yüklenemedi'
  } finally {
    loading.value = false
  }
}

async function refreshBudget() {
  busy.value = true
  error.value = ''
  try {
    summary.value = await refreshTripBudget(tripId.value)
    preview.value = await previewSavedTrip(tripId.value)
    tripRequest.value = await getTripRequest(tripId.value)
  } catch (err) {
    error.value = err.message || 'Bütçe yenilenemedi'
  } finally {
    busy.value = false
  }
}

async function addCustomActivity(name) {
  if (!tripRequest.value) {
    return
  }
  const previousNames = tripRequest.value.customActivityNames || []
  const names = addCustomActivityName(previousNames, name)
  if (names.length === previousNames.length) {
    groqNotice.value = 'Bu etkinlik zaten programda.'
    return
  }
  const previousPreview = preview.value
  const previousRequest = tripRequest.value
  tripRequest.value = { ...tripRequest.value, customActivityNames: names }
  groqNotice.value = ''
  error.value = ''
  busy.value = true
  try {
    summary.value = await updateTrip(tripId.value, { ...tripRequest.value, customActivityNames: names })
    preview.value = await previewSavedTrip(tripId.value)
    tripRequest.value = await getTripRequest(tripId.value)
    groqNotice.value = `"${name}" programa alındı, bütçe yenilendi.`
  } catch (err) {
    tripRequest.value = previousRequest
    preview.value = previousPreview
    error.value = err.message || 'Etkinlik eklenemedi'
  } finally {
    busy.value = false
  }
}

async function remove() {
  if (!window.confirm('Bu seyahat silinsin mi?')) {
    return
  }
  busy.value = true
  try {
    await deleteTrip(tripId.value)
    await router.push({ name: 'trips' })
  } catch (err) {
    error.value = err.message || 'Silinemedi'
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-head">
      <p class="eyebrow">Seyahat #{{ tripId }}</p>
      <h1 v-if="summary">
        {{ cityName(summary.originCity) }} → {{ cityName(summary.destinationCity) }}
      </h1>
      <p v-if="summary" class="muted">
        {{ prettyTripDate(summary.startDate) }} – {{ prettyTripDate(summary.endDate) }}
      </p>
    </header>

    <AlertMessage :message="error" />
    <AlertMessage :message="groqNotice" tone="ok" />

    <p v-if="loading" class="loading">Yükleniyor…</p>
    <p v-else-if="busy" class="loading">Program ve bütçe hesaplanıyor…</p>

    <template v-if="!loading && summary">
      <section v-if="tripRequest" class="panel meta-panel">
        <h2>Seyahat özeti</h2>
        <ul class="meta-list">
          <li><strong>Yolcular:</strong> {{ travelerLine() }}</li>
          <li v-if="tripRequest.transportMode">
            <strong>Ulaşım:</strong> {{ transportLabel[tripRequest.transportMode] || tripRequest.transportMode }}
          </li>
          <li v-if="tripRequest.accommodationType">
            <strong>Konaklama:</strong> {{ stayLabel[tripRequest.accommodationType] || tripRequest.accommodationType }}
          </li>
          <li v-if="tripRequest.travelStyle">
            <strong>Stil:</strong> {{ styleLabel[tripRequest.travelStyle] || tripRequest.travelStyle }}
          </li>
          <li v-if="tripRequest.interests?.length">
            <strong>İlgi alanları:</strong> {{ tripRequest.interests.join(', ') }}
          </li>
          <li v-if="tripRequest.specialNeeds">
            <strong>Özel ihtiyaçlar:</strong> {{ tripRequest.specialNeeds }}
          </li>
        </ul>
        <div v-if="customActivities.length" class="chip-row">
          <span v-for="name in customActivities" :key="name" class="chip selected">{{ name }}</span>
        </div>
      </section>

      <TripPreviewSections :preview="preview" />

      <ActivityGroqSuggestions
        :city-key="destinationKey"
        :city-label="cityName(destinationKey)"
        :context="groqContext"
        :added-names="customActivities"
        @add-custom="addCustomActivity"
      />

      <div class="actions row">
        <button type="button" :disabled="busy" @click="refreshBudget">
          <Icon name="refresh" />
          Bütçeyi güncelle
        </button>
        <button class="ghost danger" type="button" :disabled="busy" @click="remove">Sil</button>
        <RouterLink class="ghost" :to="{ name: 'trips' }">Listeye dön</RouterLink>
      </div>
    </template>
  </div>
</template>
