<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  excludeKey: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const open = ref(false)
const root = ref(null)
const query = ref('')
const highlight = ref(0)

const selectedName = computed(() => {
  return props.options.find((city) => city.cityKey === props.modelValue)?.displayName || 'İl seçin'
})

const filtered = computed(() => {
  const q = query.value.trim().toLocaleLowerCase('tr')
  if (!q) {
    return props.options
  }
  return props.options.filter((city) => {
    const name = (city.displayName || '').toLocaleLowerCase('tr')
    const key = (city.cityKey || '').toLocaleLowerCase('tr')
    return name.includes(q) || key.includes(q)
  })
})

watch(filtered, () => {
  highlight.value = 0
})

function openList() {
  open.value = true
  query.value = ''
  highlight.value = Math.max(
    0,
    filtered.value.findIndex((city) => city.cityKey === props.modelValue),
  )
}

function closeList() {
  open.value = false
  query.value = ''
}

function toggle() {
  if (open.value) {
    closeList()
  } else {
    openList()
  }
}

function isDimmed(city) {
  return Boolean(props.excludeKey) && city.cityKey === props.excludeKey
}

function choose(cityKey) {
  if (props.excludeKey && cityKey === props.excludeKey) {
    return
  }
  emit('update:modelValue', cityKey)
  closeList()
}

function chooseHighlighted() {
  const city = filtered.value[highlight.value]
  if (city) {
    choose(city.cityKey)
  }
}

function onPointerDown(event) {
  if (!root.value?.contains(event.target)) {
    closeList()
  }
}

function onKeydown(event) {
  if (!open.value) {
    if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      openList()
    }
    return
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    closeList()
    return
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    highlight.value = Math.min(highlight.value + 1, filtered.value.length - 1)
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    highlight.value = Math.max(highlight.value - 1, 0)
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    chooseHighlighted()
  }
}

onMounted(() => document.addEventListener('pointerdown', onPointerDown))
onBeforeUnmount(() => document.removeEventListener('pointerdown', onPointerDown))
</script>

<template>
  <div ref="root" class="city-picker" @keydown="onKeydown">
    <button
      class="city-trigger"
      type="button"
      aria-haspopup="listbox"
      :aria-expanded="open"
      @click="toggle"
    >
      <span>{{ selectedName }}</span>
      <span aria-hidden="true">▾</span>
    </button>
    <div v-if="open" class="city-dropdown" role="presentation">
      <input
        v-model="query"
        class="city-search"
        type="search"
        placeholder="İl ara…"
        autocomplete="off"
        aria-label="İl ara"
        @keydown.stop
      />
      <ul v-if="filtered.length" class="city-list" role="listbox">
        <li v-for="(city, index) in filtered" :key="city.cityKey">
          <button
            type="button"
            role="option"
            :aria-selected="city.cityKey === modelValue"
            :aria-disabled="isDimmed(city) || undefined"
            :disabled="isDimmed(city)"
            :class="{ highlighted: index === highlight, dimmed: isDimmed(city) }"
            @mouseenter="highlight = index"
            @click="choose(city.cityKey)"
          >
            {{ city.displayName }}
          </button>
        </li>
      </ul>
      <p v-else class="city-empty muted">Sonuç yok</p>
    </div>
  </div>
</template>
