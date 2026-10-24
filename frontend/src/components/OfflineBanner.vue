<script setup>
import { useOnline } from '../composables/useOnline'

const online = useOnline()
</script>

<template>
  <p v-if="!online" class="banner offline" role="status">
    Çevrimdışısınız. Bağlantı gelince bütçe ve öneriler yenilenebilir.
  </p>
</template>
