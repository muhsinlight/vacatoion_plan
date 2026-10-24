<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Icon from '../components/Icon.vue'
import OfflineBanner from '../components/OfflineBanner.vue'
import UserMenu from '../components/UserMenu.vue'

const route = useRoute()
const navOpen = ref(false)

const items = [
  { to: '/', label: 'Ana Sayfa', icon: 'home' },
  { to: '/pricing', label: 'Planla', icon: 'tag' },
  { to: '/trips', label: 'Seyahatlerim', icon: 'case' },
  { to: '/profile', label: 'Profil', icon: 'user' },
]

function isCurrent(path) {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}

function closeNav() {
  navOpen.value = false
}

watch(() => route.path, closeNav)
</script>

<template>
  <div class="app">
    <a class="skip-link" href="#main-content">İçeriğe geç</a>
    <header class="topbar">
      <button
        class="icon-button menu-button"
        type="button"
        :aria-label="navOpen ? 'Menüyü kapat' : 'Menüyü aç'"
        :aria-expanded="navOpen"
        aria-controls="app-sidebar"
        @click="navOpen = !navOpen"
      >
        <Icon name="menu" />
      </button>
      <RouterLink class="brand-lockup" to="/" @click="closeNav">
        <span class="mark">VP</span>
        <span>Vacation Plan</span>
      </RouterLink>
      <UserMenu />
    </header>
    <div class="frame">
      <button v-if="navOpen" class="scrim" type="button" aria-label="Menüyü kapat" @click="closeNav" />
      <aside id="app-sidebar" class="sidebar" :class="{ open: navOpen }">
        <p class="side-label">Gezinti</p>
        <nav class="side-nav" aria-label="Ana menü">
          <RouterLink
            v-for="item in items"
            :key="item.to"
            :to="item.to"
            custom
            v-slot="{ href, navigate }"
          >
            <a
              :href="href"
              class="nav-link"
              :class="{ active: isCurrent(item.to) }"
              :aria-current="isCurrent(item.to) ? 'page' : undefined"
              @click="navigate"
            >
              <Icon :name="item.icon" />
              {{ item.label }}
            </a>
          </RouterLink>
        </nav>
      </aside>
      <main id="main-content" class="content" tabindex="-1">
        <OfflineBanner />
        <RouterView />
      </main>
    </div>
  </div>
</template>
