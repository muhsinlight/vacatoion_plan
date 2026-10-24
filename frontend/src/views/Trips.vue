<script setup>
import { onMounted } from 'vue'
import AlertMessage from '../components/AlertMessage.vue'
import EmptyState from '../components/EmptyState.vue'
import TripCard from '../components/TripCard.vue'
import { useTrips } from '../composables/useTrips'

const {
  trips,
  destinations,
  loading,
  error,
  page,
  totalPages,
  totalElements,
  hasNext,
  hasPrevious,
  load,
  goToPage,
} = useTrips({ pageSize: 10 })

onMounted(() => load(0))
</script>

<template>
  <div class="page">
    <header class="page-head">
      <p class="eyebrow">Seyahatlerim</p>
      <h1>Kayıtlı seyahat bütçeleri</h1>
      <p>Kaydettiğiniz bütçeler burada. Yeni bir güzergâh için Planla’ya geçin.</p>
    </header>

    <section class="panel">
      <AlertMessage :message="error" />
      <div v-if="loading" class="plan-skeleton" aria-busy="true">
        <span class="skeleton-line"></span>
        <span class="skeleton-line short"></span>
        <span class="skeleton-line"></span>
      </div>
      <EmptyState
        v-else-if="!trips.length"
        icon="case"
        title="Kayıtlı seyahat yok"
        body="Planla ekranında bütçeyi hesaplayıp kaydedin. Kayıtlar burada durur."
      >
        <template #action>
          <RouterLink class="button" to="/pricing">Planla</RouterLink>
        </template>
      </EmptyState>
      <template v-else>
        <div class="trip-list">
          <TripCard
            v-for="trip in trips"
            :key="trip.id"
            :trip="trip"
            :destinations="destinations"
            as-link
          />
        </div>
        <nav
          v-if="totalPages > 1"
          class="pagination"
          aria-label="Seyahat sayfaları"
        >
          <button
            type="button"
            class="button ghost"
            :disabled="!hasPrevious || loading"
            @click="goToPage(page - 1)"
          >
            Önceki
          </button>
          <span class="pagination-meta muted">
            Sayfa {{ page + 1 }} / {{ totalPages }}
            <span class="pagination-count">({{ totalElements }} kayıt)</span>
          </span>
          <button
            type="button"
            class="button ghost"
            :disabled="!hasNext || loading"
            @click="goToPage(page + 1)"
          >
            Sonraki
          </button>
        </nav>
      </template>
    </section>
  </div>
</template>
