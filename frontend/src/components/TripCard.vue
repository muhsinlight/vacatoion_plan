<script setup>
import { formatMoney } from '../utils/format'
import { prettyTripDate, resolveCityName } from '../utils/tripDisplay'

defineProps({
  trip: { type: Object, required: true },
  destinations: { type: Array, default: () => [] },
  asLink: { type: Boolean, default: false },
})
</script>

<template>
  <RouterLink
    v-if="asLink"
    class="trip-card link-card"
    :to="{ name: 'tripDetail', params: { id: trip.id } }"
  >
    <span class="muted">#{{ trip.id }}</span>
    <h3>
      {{ resolveCityName(destinations, trip.originCity) }}
      →
      {{ resolveCityName(destinations, trip.destinationCity) }}
    </h3>
    <p class="muted">{{ prettyTripDate(trip.startDate) }} – {{ prettyTripDate(trip.endDate) }}</p>
    <p class="price">
      {{ formatMoney(trip.budget?.total?.min, trip.budget?.total?.max, trip.budget?.total?.currency) }}
    </p>
  </RouterLink>
  <article v-else class="trip-card">
    <span class="muted">#{{ trip.id }}</span>
    <h3>
      {{ resolveCityName(destinations, trip.originCity) }}
      →
      {{ resolveCityName(destinations, trip.destinationCity) }}
    </h3>
    <p class="muted">{{ prettyTripDate(trip.startDate) }} – {{ prettyTripDate(trip.endDate) }}</p>
    <p class="price">
      {{ formatMoney(trip.budget?.total?.min, trip.budget?.total?.max, trip.budget?.total?.currency) }}
    </p>
  </article>
</template>
