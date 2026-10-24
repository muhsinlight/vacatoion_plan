<script setup>
import Icon from '../Icon.vue'
import CityPicker from '../CityPicker.vue'
import { isoDate } from '../../utils/format'

const today = isoDate(0)

/*
 * Guzergah bilgisi yalnizca burada durur. Sonucun ustunde tekrar
 * ozetlenmez; kullanici secimini bu cubuktan okur ve buradan degistirir.
 */
const props = defineProps({
  originCity: { type: String, default: '' },
  destinationCity: { type: String, default: '' },
  startDate: { type: String, default: '' },
  endDate: { type: String, default: '' },
  adults: { type: Number, default: 2 },
  destinations: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  ready: { type: Boolean, default: false },
  live: { type: Boolean, default: false },
})

const emit = defineEmits([
  'update:originCity',
  'update:destinationCity',
  'update:startDate',
  'update:endDate',
  'update:adults',
  'submit',
])

function changeAdults(delta) {
  const next = Math.min(20, Math.max(1, Number(props.adults) + delta))
  if (next !== props.adults) {
    emit('update:adults', next)
  }
}
</script>

<template>
  <form
    class="vp-plan-search-form"
    @submit.prevent="emit('submit')"
  >
    <div class="plan-search-row">
      <label class="plan-search-field">
        <span class="plan-search-label">
          <Icon name="pin" class="size-4 text-brand-700" /> Nereden
        </span>
        <CityPicker
          class="plan-city"
          :model-value="originCity"
          :options="destinations"
          :exclude-key="destinationCity"
          @update:model-value="emit('update:originCity', $event)"
        />
      </label>

      <div class="plan-route-arrow-wrap" aria-hidden="true">
        <svg
          class="plan-route-arrow"
          viewBox="0 0 24 24"
          width="20"
          height="20"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
        >
          <path d="M4 12h15M14 7l5 5-5 5" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </div>

      <label class="plan-search-field">
        <span class="plan-search-label">
          <Icon name="pin" class="size-4 text-brand-700" /> Nereye
        </span>
        <CityPicker
          class="plan-city"
          :model-value="destinationCity"
          :options="destinations"
          :exclude-key="originCity"
          @update:model-value="emit('update:destinationCity', $event)"
        />
      </label>

      <div class="plan-search-dates">
        <label class="plan-search-field">
          <span class="plan-search-label">
            <Icon name="calendar" class="size-4 text-brand-700" /> Gidiş
          </span>
          <input
            class="vp-control plan-date"
            type="date"
            required
            :min="today"
            :value="startDate"
            @input="emit('update:startDate', $event.target.value)"
          />
        </label>
        <label class="plan-search-field">
          <span class="plan-search-label">
            <Icon name="calendar" class="size-4 text-brand-700" /> Dönüş
          </span>
          <input
            class="vp-control plan-date"
            type="date"
            required
            :min="startDate"
            :value="endDate"
            @input="emit('update:endDate', $event.target.value)"
          />
        </label>
      </div>

      <div class="plan-search-adults">
        <span id="vp-adults-label" class="plan-search-label">
          <Icon name="users" class="size-4 text-brand-700" /> Yetişkin
        </span>
        <div class="vp-stepper" role="group" aria-labelledby="vp-adults-label">
          <button
            type="button"
            class="vp-stepper-btn"
            aria-label="Yetişkin sayısını azalt"
            :disabled="adults <= 1"
            @click="changeAdults(-1)"
          >
            −
          </button>
          <output
            class="w-8 text-center text-sm font-semibold text-ink-900 tabular-nums"
            aria-live="polite"
          >
            {{ adults }}
          </output>
          <button
            type="button"
            class="vp-stepper-btn"
            aria-label="Yetişkin sayısını artır"
            :disabled="adults >= 20"
            @click="changeAdults(1)"
          >
            +
          </button>
        </div>
      </div>

      <button
        type="submit"
        class="vp-plan-submit"
        :class="{ 'is-busy': loading }"
        :disabled="loading || !ready"
        :aria-busy="loading"
      >
        <span v-if="loading" class="vp-btn-spin" aria-hidden="true"></span>
        <Icon v-else :name="live ? 'refresh' : 'search'" class="size-4" />
        {{ loading ? 'Hesaplanıyor…' : live ? 'Güncelle' : 'Hesapla' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.plan-route-arrow-wrap {
  display: none;
  flex-shrink: 0;
  color: var(--color-ink-300);
}

@media (min-width: 720px) {
  .plan-route-arrow-wrap {
    display: flex;
  }
}

.plan-route-arrow {
  display: block;
  width: 20px;
  height: 20px;
  max-width: 20px;
  max-height: 20px;
}

/*
 * `CityPicker` kendi isaretlemesini basiyor; hangi elemani kullandigindan
 * bagimsiz olsun diye kontrol gorunumu derin seciciyle veriliyor.
 */
.plan-city {
  width: 100%;
}

.plan-city :deep(.city-trigger:focus-visible),
.vp-control:focus-visible,
.vp-stepper-btn:focus-visible {
  outline: 2px solid var(--color-brand-700);
  outline-offset: 2px;
}
</style>
