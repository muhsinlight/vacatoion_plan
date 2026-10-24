<script setup>
import { computed, ref } from 'vue'
import AlertMessage from '../components/AlertMessage.vue'
import FormField from '../components/FormField.vue'
import Icon from '../components/Icon.vue'
import { useAuth } from '../stores/auth'
import { fieldText } from '../utils/format'
import { displayName, initials } from '../utils/user'

const auth = useAuth()
const user = computed(() => auth.user.value)
const name = computed(() => displayName(user.value))
const letters = computed(() => initials(user.value))

const firstName = ref(user.value?.firstName || '')
const lastName = ref(user.value?.lastName || '')
const fieldErrors = ref({})
const formError = ref('')
const saved = ref('')
const loading = ref(false)

function nameError(value, label) {
  const trimmed = value.trim().replace(/\s+/g, ' ')
  if (!trimmed) {
    return `${label} zorunlu`
  }
  if (trimmed.length < 2 || trimmed.length > 60) {
    return `${label} 2 ile 60 karakter arasında olmalı`
  }
  if (!/^[\p{L}][\p{L} .'-]*$/u.test(trimmed)) {
    return `${label} yalnızca harf içermelidir`
  }
  return ''
}

function validate() {
  const errors = {
    firstName: nameError(firstName.value, 'Ad'),
    lastName: nameError(lastName.value, 'Soyad'),
  }
  fieldErrors.value = Object.fromEntries(Object.entries(errors).filter(([, message]) => message))
  return Object.keys(fieldErrors.value).length === 0
}

async function onSubmit() {
  formError.value = ''
  saved.value = ''
  if (!validate()) {
    return
  }
  loading.value = true
  try {
    await auth.updateProfile({
      firstName: firstName.value.trim(),
      lastName: lastName.value.trim(),
    })
    saved.value = 'Ad ve soyad kaydedildi. Sağ üstte bu ad görünür.'
  } catch (error) {
    fieldErrors.value = {
      firstName: fieldText(error.fields?.firstName),
      lastName: fieldText(error.fields?.lastName),
    }
    formError.value = error.message || 'Profil kaydedilemedi'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page narrow">
    <header class="page-head">
      <p class="eyebrow">Profil</p>
      <h1>{{ name || 'Ad ve soyad' }}</h1>
      <p>Görünen ad sağ üstte ve kayıtlarda bu haliyle durur.</p>
    </header>

    <section class="panel profile-card">
      <form @submit.prevent="onSubmit">
        <div class="profile-identity">
          <span class="avatar large">
            <template v-if="letters">{{ letters }}</template>
            <Icon v-else name="user" />
          </span>
          <div class="split">
            <FormField id="profileFirstName" label="Ad" autocomplete="given-name" v-model="firstName" :error="fieldErrors.firstName" />
            <FormField id="profileLastName" label="Soyad" autocomplete="family-name" v-model="lastName" :error="fieldErrors.lastName" />
          </div>
        </div>
        <FormField
          id="profileEmail"
          label="E-posta"
          type="email"
          autocomplete="email"
          :model-value="user?.email || ''"
          readonly
        />
        <button type="submit" :disabled="loading" :aria-busy="loading">
          {{ loading ? 'Kaydediliyor…' : 'Kaydet' }}
        </button>
        <AlertMessage :message="saved" tone="ok" />
        <AlertMessage :message="formError" />
      </form>
    </section>
  </div>
</template>
