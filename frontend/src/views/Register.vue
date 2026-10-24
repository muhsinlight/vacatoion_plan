<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AlertMessage from '../components/AlertMessage.vue'
import FormField from '../components/FormField.vue'
import AuthLayout from '../layouts/AuthLayout.vue'
import { useAuth } from '../stores/auth'
import { fieldText } from '../utils/format'

const auth = useAuth()
const router = useRouter()

const firstName = ref('')
const lastName = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const fieldErrors = ref({})
const formError = ref('')
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
  const errors = {}
  errors.firstName = nameError(firstName.value, 'Ad')
  errors.lastName = nameError(lastName.value, 'Soyad')
  if (!email.value.trim()) {
    errors.email = 'E-posta zorunlu'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    errors.email = 'Geçerli bir e-posta girin'
  }
  if (!password.value) {
    errors.password = 'Şifre zorunlu'
  } else if (password.value.length < 8 || password.value.length > 72) {
    errors.password = 'Şifre 8 ile 72 karakter arasında olmalı'
  }
  if (!confirmPassword.value) {
    errors.confirmPassword = 'Şifre tekrarı zorunlu'
  } else if (password.value !== confirmPassword.value) {
    errors.confirmPassword = 'Şifreler eşleşmiyor'
  }
  fieldErrors.value = Object.fromEntries(Object.entries(errors).filter(([, message]) => message))
  return Object.keys(fieldErrors.value).length === 0
}

async function onSubmit() {
  formError.value = ''
  if (!validate()) {
    return
  }
  loading.value = true
  try {
    await auth.register({
      firstName: firstName.value.trim(),
      lastName: lastName.value.trim(),
      email: email.value.trim(),
      password: password.value,
    })
    sessionStorage.setItem('vp-notice', 'Hesabınız oluşturuldu. Ana sayfadasınız.')
    await router.push('/')
  } catch (error) {
    fieldErrors.value = {
      firstName: fieldText(error.fields?.firstName),
      lastName: fieldText(error.fields?.lastName),
      email: fieldText(error.fields?.email),
      password: fieldText(error.fields?.password),
    }
    formError.value = error.message || 'Kayıt tamamlanamadı'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout wide title="Hesap oluştur" lede="Birkaç bilgi yeter. Ardından güzergâh seçip bütçeyi görün.">
    <form @submit.prevent="onSubmit">
      <div class="split">
        <FormField
          id="firstName"
          label="Ad"
          autocomplete="given-name"
          v-model="firstName"
          :error="fieldErrors.firstName"
        />
        <FormField
          id="lastName"
          label="Soyad"
          autocomplete="family-name"
          v-model="lastName"
          :error="fieldErrors.lastName"
        />
      </div>
      <FormField
        id="email"
        label="E-posta adresi"
        type="email"
        autocomplete="email"
        v-model="email"
        :error="fieldErrors.email"
      />
      <FormField
        id="password"
        label="Şifre"
        type="password"
        autocomplete="new-password"
        v-model="password"
        :error="fieldErrors.password"
      />
      <FormField
        id="confirmPassword"
        label="Şifre tekrar"
        type="password"
        autocomplete="new-password"
        v-model="confirmPassword"
        :error="fieldErrors.confirmPassword"
      />
      <button type="submit" :disabled="loading" :aria-busy="loading">
        {{ loading ? 'Kaydediliyor…' : 'Hesap oluştur' }}
      </button>
      <AlertMessage :message="formError" />
    </form>
    <p class="switch">Zaten hesabınız var mı? <RouterLink to="/login">Giriş yap</RouterLink></p>
  </AuthLayout>
</template>
