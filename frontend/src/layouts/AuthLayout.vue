<script setup>
defineProps({
  title: { type: String, required: true },
  lede: { type: String, default: '' },
  wide: { type: Boolean, default: false },
})
</script>

<template>
  <main class="auth-shell">
    <section class="auth-card" :class="{ wide }">
      <RouterLink class="auth-brand" to="/login">
        <span class="mark">VP</span>
        <span>
          <strong>Vacation Plan</strong>
        </span>
      </RouterLink>
      <h1>{{ title }}</h1>
      <p v-if="lede" class="lede">{{ lede }}</p>
      <slot />
    </section>
  </main>
</template>
