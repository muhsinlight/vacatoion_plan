<script setup>
import { computed, ref } from 'vue'
import AlertMessage from './AlertMessage.vue'
import Icon from './Icon.vue'
import { suggestActivities } from '../services/pricing'

const props = defineProps({
  cityKey: { type: String, default: '' },
  cityLabel: { type: String, default: '' },
  context: { type: Object, default: () => ({}) },
  addedNames: { type: Array, default: () => [] },
})

const emit = defineEmits(['add-custom'])

const loading = ref(false)
const error = ref('')
const items = ref([])

const canRequest = computed(
  () => Boolean(props.cityKey && props.context?.startDate && props.context?.endDate),
)

const heading = computed(() => {
  if (props.cityLabel) {
    return `Yapay zeka önerileri (${props.cityLabel})`
  }
  return 'Yapay zeka etkinlik önerileri'
})

async function load() {
  if (!canRequest.value) {
    return
  }
  loading.value = true
  error.value = ''
  items.value = []
  try {
    const response = await suggestActivities(props.cityKey, {
      startDate: props.context.startDate,
      endDate: props.context.endDate,
      adults: props.context.adults ?? 2,
      travelStyle: props.context.travelStyle ?? 'BALANCED',
    })
    items.value = response.activities ?? []
    if (!items.value.length) {
      error.value = 'Bu seçim için öneri üretilemedi.'
    }
  } catch (err) {
    error.value = err.message || 'Öneri alınamadı'
  } finally {
    loading.value = false
  }
}

function isAdded(name) {
  const trimmed = name?.trim()
  return Boolean(trimmed && props.addedNames.includes(trimmed))
}

function addCustom(name) {
  const trimmed = name?.trim()
  if (trimmed && !isAdded(trimmed)) {
    emit('add-custom', trimmed)
  }
}
</script>

<template>
  <section class="panel groq-panel">
    <div class="panel-head">
      <div>
        <h2>{{ heading }}</h2>
        <p class="muted">Kısa etkinlik fikirleri. Beğendiğini programa ekleyebilirsin.</p>
      </div>
      <button type="button" class="ghost" :disabled="!canRequest || loading" @click="load">
        <Icon name="search" />
        {{ loading ? 'İsteniyor…' : 'Öneri getir' }}
      </button>
    </div>
    <AlertMessage :message="error" />
    <p v-if="!canRequest" class="muted">Öneri için varış ve tarih gerekir.</p>
    <ul v-else-if="items.length" class="groq-list">
      <li v-for="(item, index) in items" :key="`${item.name}-${index}`" class="groq-card">
        <div>
          <strong>{{ item.name }}</strong>
          <p v-if="item.description" class="muted">{{ item.description }}</p>
        </div>
        <button v-if="!isAdded(item.name)" type="button" class="ghost" @click="addCustom(item.name)">Ekle</button>
        <span v-else class="added-mark">Eklendi</span>
      </li>
    </ul>
  </section>
</template>
