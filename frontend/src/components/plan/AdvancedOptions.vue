<script setup>
import Icon from '../Icon.vue'

/*
 * `details` kullaniliyor: ac/kapa durumu, klavye ve ekran okuyucu
 * davranisi tarayicidan gelir, ayrica aria yonetimi gerekmez.
 */
defineProps({
  totalBudget: { type: [String, Number], default: '' },
  perPersonBudget: { type: [String, Number], default: '' },
  specialNeeds: { type: String, default: '' },
})

const emit = defineEmits([
  'update:totalBudget',
  'update:perPersonBudget',
  'update:specialNeeds',
])
</script>

<template>
  <details class="vp-advanced vp-acc">
    <summary class="vp-advanced-summary">
      <span class="vp-advanced-lead">
        <Icon name="wallet" class="size-4 text-brand-700" />
        <span class="vp-advanced-copy">
          <span class="vp-advanced-name">Gelişmiş seçenekler</span>
          <span class="vp-advanced-sub">Maksimum bütçe ve özel ihtiyaçlar</span>
        </span>
      </span>
      <Icon name="chevron" class="vp-acc-arrow size-4 text-ink-500" />
    </summary>

    <div class="vp-advanced-body">
      <p class="vp-advanced-note">
        Bu alanlar isteğe bağlıdır. Boş bırakılırsa hesap mevcut tercihlerinle yapılır.
      </p>
      <div class="vp-advanced-grid">
        <label class="flex flex-col gap-1.5">
          <span class="vp-field-label">Maksimum bütçe (TRY)</span>
          <input
            class="vp-input tabular-nums"
            type="number"
            min="0"
            step="100"
            inputmode="numeric"
            placeholder="İsteğe bağlı"
            :value="totalBudget"
            @input="emit('update:totalBudget', $event.target.value)"
          />
        </label>
        <label class="flex flex-col gap-1.5">
          <span class="vp-field-label">Kişi başı tavan (TRY)</span>
          <input
            class="vp-input tabular-nums"
            type="number"
            min="0"
            step="50"
            inputmode="numeric"
            placeholder="İsteğe bağlı"
            :value="perPersonBudget"
            @input="emit('update:perPersonBudget', $event.target.value)"
          />
        </label>
      </div>
      <label class="flex flex-col gap-1.5">
        <span class="vp-field-label">Özel ihtiyaçlar</span>
        <textarea
          class="vp-input resize-y py-2.5 leading-relaxed"
          rows="3"
          placeholder="Erişim, diyet, bebek arabası vb."
          :value="specialNeeds"
          @input="emit('update:specialNeeds', $event.target.value)"
        />
      </label>
    </div>
  </details>
</template>

<style scoped>
.vp-acc summary::-webkit-details-marker {
  display: none;
}

.vp-acc[open] .vp-acc-arrow {
  transform: rotate(180deg);
}

.vp-acc-arrow {
  transition: transform 160ms ease;
}

@media (prefers-reduced-motion: reduce) {
  .vp-acc-arrow {
    transition: none;
  }
}
</style>
