<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AlertMessage from '../components/AlertMessage.vue'
import CityPicker from '../components/CityPicker.vue'
import EmptyState from '../components/EmptyState.vue'
import Icon from '../components/Icon.vue'
import TripCard from '../components/TripCard.vue'
import { useTrips } from '../composables/useTrips'
import { useAuth } from '../stores/auth'
import { patchTripDraft, readTripDraft } from '../utils/tripDraft'

const router = useRouter()
const auth = useAuth()
const { trips, destinations, loading, error, catalogError, load } = useTrips({ pageSize: 3 })
const notice = ref('')
const originCity = ref('')
const destinationCity = ref('')

const greeting = computed(() => auth.user.value?.firstName || '')
const canStart = computed(
  () => Boolean(originCity.value && destinationCity.value && originCity.value !== destinationCity.value),
)

function startPlan() {
  if (!canStart.value) {
    return
  }
  patchTripDraft({
    originCity: originCity.value,
    destinationCity: destinationCity.value,
  })
  router.push({ name: 'pricing', query: { run: '1' } })
}

onMounted(async () => {
  notice.value = sessionStorage.getItem('vp-notice') || ''
  sessionStorage.removeItem('vp-notice')
  await load()
  const draft = readTripDraft()
  const keys = new Set(destinations.value.map((city) => city.cityKey))
  if (draft && keys.has(draft.originCity)) {
    originCity.value = draft.originCity
  }
  if (draft && keys.has(draft.destinationCity) && draft.destinationCity !== originCity.value) {
    destinationCity.value = draft.destinationCity
  }
})
</script>

<template>
  <div class="page">
    <AlertMessage :message="notice" tone="ok" />

    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">Ana sayfa</p>
        <h1 v-if="greeting">Merhaba, {{ greeting }}</h1>
        <h1 v-else>Merhaba</h1>
        <p>Nereden nereye gideceğinizi seçin. Bütçe Planla ekranında açılır. Kayıtlı seyahatler aşağıda durur.</p>
        <AlertMessage :message="catalogError" />
        <div v-if="loading" class="plan-skeleton hero-skeleton" aria-busy="true">
          <span class="skeleton-line"></span>
          <span class="skeleton-line"></span>
        </div>
        <EmptyState
          v-else-if="!destinations.length"
          icon="pin"
          title="İl listesi yok"
          body="Katalog gelmeden yolculuk başlatılamaz. Bağlantıyı kontrol edip sayfayı yenileyin."
        />
        <form v-else class="hero-plan" @submit.prevent="startPlan">
          <label class="field">
            <span class="field-label"><Icon name="pin" /> Nereden</span>
            <CityPicker v-model="originCity" :options="destinations" :exclude-key="destinationCity" />
          </label>
          <label class="field">
            <span class="field-label"><Icon name="pin" /> Nereye</span>
            <CityPicker v-model="destinationCity" :options="destinations" :exclude-key="originCity" />
          </label>
          <button type="submit" :disabled="!canStart">Yolculuğu gör</button>
        </form>
      </div>
      <div class="hero-art" aria-hidden="true">
        <span class="sun" />
        <span class="sea" />
        <span class="palm" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>Son seyahatler</h2>
          <p class="muted">Kayıtlı bütçeler burada özetlenir.</p>
        </div>
        <RouterLink class="text-link" to="/trips">Tümünü gör</RouterLink>
      </div>
      <AlertMessage :message="error" />
      <div v-if="loading" class="plan-skeleton" aria-busy="true">
        <span class="skeleton-line"></span>
        <span class="skeleton-line short"></span>
        <span class="skeleton-line"></span>
      </div>
      <EmptyState
        v-else-if="!trips.length"
        icon="case"
        title="Henüz kayıtlı seyahat yok"
        body="Yukarıdan iki il seçip bütçeyi görün. Beğenirseniz kaydedin."
      >
        <template #action>
          <RouterLink class="button ghost" to="/pricing">Planla</RouterLink>
        </template>
      </EmptyState>
      <div v-else class="trip-list">
        <TripCard
          v-for="trip in trips"
          :key="trip.id"
          :trip="trip"
          :destinations="destinations"
          as-link
        />
      </div>
    </section>
  </div>
</template>
