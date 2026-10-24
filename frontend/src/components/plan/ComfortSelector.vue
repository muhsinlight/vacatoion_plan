<script setup>
/*
 * Uc kademeli konfor secici. Radio input'lari gorsel olarak gizli,
 * yani sekmeyle gruba girilip ok tuslariyla kademe degistirilebilir.
 */
defineProps({
  title: { type: String, default: 'Konfor seviyesi' },
  options: { type: Array, required: true },
  modelValue: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <fieldset class="min-w-0">
    <legend>{{ title }}</legend>
    <div class="vp-comfort-track">
      <label
        v-for="option in options"
        :key="option.id"
        class="vp-seg"
        :class="modelValue === option.id ? 'vp-seg--on' : 'vp-seg--off'"
      >
        <input
          class="vp-sr"
          type="radio"
          name="vp-comfort"
          :value="option.id"
          :checked="modelValue === option.id"
          @change="emit('update:modelValue', option.id)"
        />
        {{ option.label }}
      </label>
    </div>
  </fieldset>
</template>

<style scoped>
.vp-seg:has(.vp-sr:focus-visible) {
  outline: 2px solid var(--color-brand-700);
  outline-offset: 2px;
}
</style>
