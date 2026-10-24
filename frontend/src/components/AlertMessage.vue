<script setup>
import { computed } from 'vue'

const props = defineProps({
  message: { type: String, default: '' },
  tone: { type: String, default: 'error' },
})

const toneClass = computed(() => {
  if (props.tone === 'ok') {
    return 'ok'
  }
  if (props.tone === 'warn') {
    return 'warn'
  }
  if (props.tone === 'info') {
    return 'info'
  }
  return ''
})
</script>

<template>
  <p
    v-if="message"
    class="banner"
    :class="toneClass"
    :role="tone === 'ok' || tone === 'info' ? 'status' : 'alert'"
  >
    {{ message }}
  </p>
</template>
