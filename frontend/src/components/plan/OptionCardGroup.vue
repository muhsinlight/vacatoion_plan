<script setup>
import Icon from '../Icon.vue'

/*
 * Tek secimli ikonlu kart grubu. Altta gercek radio input'lari durur,
 * boylece ok tuslariyla gezinme ve ekran okuyucu davranisi tarayicidan gelir.
 */
defineProps({
  title: { type: String, required: true },
  name: { type: String, required: true },
  options: { type: Array, required: true },
  icons: { type: Object, default: () => ({}) },
  modelValue: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <fieldset class="min-w-0">
    <legend>{{ title }}</legend>
    <div class="vp-option-grid">
      <label
        v-for="option in options"
        :key="option.id"
        class="vp-option"
        :class="modelValue === option.id ? 'vp-option--on' : 'vp-option--off'"
      >
        <input
          class="vp-sr"
          type="radio"
          :name="name"
          :value="option.id"
          :checked="modelValue === option.id"
          @change="emit('update:modelValue', option.id)"
        />
        <Icon :name="icons[option.id] || 'tag'" class="size-6" />
        <span class="text-sm font-semibold">{{ option.label }}</span>
      </label>
    </div>
  </fieldset>
</template>

<style scoped>
.vp-option:has(.vp-sr:focus-visible) {
  outline: 2px solid var(--color-brand-700);
  outline-offset: 2px;
}
</style>
