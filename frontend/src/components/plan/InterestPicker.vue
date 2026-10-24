<script setup>
import Icon from '../Icon.vue'

/* Coklu secim; her kart bir checkbox tasir. */
const props = defineProps({
  title: { type: String, default: 'İlgi alanları' },
  options: { type: Array, required: true },
  icons: { type: Object, default: () => ({}) },
  modelValue: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue'])

function isOn(id) {
  return props.modelValue.includes(id)
}
</script>

<template>
  <fieldset class="min-w-0">
    <legend>{{ title }}</legend>
    <p class="vp-interest-hint">Seçimler bütçe hesabına ve kayıtlı seyahate yansır.</p>
    <div class="vp-chip-grid">
      <label
        v-for="option in options"
        :key="option.id"
        class="vp-chipcard"
        :class="isOn(option.id) ? 'vp-chipcard--on' : 'vp-chipcard--off'"
      >
        <input
          class="vp-sr"
          type="checkbox"
          :checked="isOn(option.id)"
          @change="emit('update:modelValue', option.id)"
        />
        <Icon :name="icons[option.id] || 'tag'" class="size-5 shrink-0" />
        <span class="text-sm font-semibold">{{ option.label }}</span>
        <Icon v-if="isOn(option.id)" name="check" class="ml-auto size-4 shrink-0" />
      </label>
    </div>
  </fieldset>
</template>

<style scoped>
.vp-chipcard:has(.vp-sr:focus-visible) {
  outline: 2px solid var(--color-brand-700);
  outline-offset: 2px;
}
</style>
