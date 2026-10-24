<script setup>
import Icon from './Icon.vue'

defineProps({
  icon: { type: String, default: 'case' },
  title: { type: String, required: true },
  body: { type: String, default: '' },
})
</script>

<template>
  <div class="empty-state">
    <span class="empty-state-icon" aria-hidden="true">
      <Icon :name="icon" />
    </span>
    <div class="empty-state-copy">
      <strong>{{ title }}</strong>
      <p v-if="body">{{ body }}</p>
    </div>
    <div v-if="$slots.action" class="empty-state-action">
      <slot name="action" />
    </div>
  </div>
</template>
