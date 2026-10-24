<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from './Icon.vue'
import { useAuth } from '../stores/auth'
import { displayName, initials } from '../utils/user'

const auth = useAuth()
const router = useRouter()
const open = ref(false)
const root = ref(null)
const leaving = ref(false)

const name = computed(() => displayName(auth.user.value))
const letters = computed(() => initials(auth.user.value))

function toggle() {
  open.value = !open.value
}

function close() {
  open.value = false
}

function onPointerDown(event) {
  if (!root.value?.contains(event.target)) {
    close()
  }
}

function onKey(event) {
  if (event.key === 'Escape') {
    close()
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', onPointerDown)
  document.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown)
  document.removeEventListener('keydown', onKey)
})

async function onLogout() {
  if (leaving.value) {
    return
  }
  leaving.value = true
  close()
  try {
    await auth.logout()
    await router.push('/login')
  } finally {
    leaving.value = false
  }
}
</script>

<template>
  <div ref="root" class="user-menu">
    <button
      type="button"
      class="user-trigger"
      :aria-expanded="open"
      aria-haspopup="menu"
      aria-controls="account-menu"
      @click="toggle"
    >
      <span class="avatar">
        <template v-if="letters">{{ letters }}</template>
        <Icon v-else name="user" />
      </span>
      <span class="user-name">{{ name || 'Ad ekle' }}</span>
      <Icon name="chevron" />
    </button>
    <div v-if="open" id="account-menu" class="menu" role="menu">
      <div v-if="name" class="menu-head">
        <strong>{{ name }}</strong>
      </div>
      <RouterLink class="menu-item" to="/profile" role="menuitem" @click="close">
        <Icon name="user" />
        Profil
      </RouterLink>
      <button class="menu-item danger" type="button" role="menuitem" :disabled="leaving" @click="onLogout">
        <Icon name="logout" />
        {{ leaving ? 'Çıkılıyor…' : 'Çıkış Yap' }}
      </button>
    </div>
  </div>
</template>
