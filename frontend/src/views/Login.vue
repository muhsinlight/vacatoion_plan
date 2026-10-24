<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AlertMessage from '../components/AlertMessage.vue'
import FormField from '../components/FormField.vue'
import AuthLayout from '../layouts/AuthLayout.vue'
import { useAuth } from '../stores/auth'
import { fieldText } from '../utils/format'

const auth = useAuth()
const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const fieldErrors = ref({})
const formError = ref('')
const loading = ref(false)

function validate() {
  const errors = {}
  if (!email.value.trim()) {
    errors.email = 'E-posta zorunlu'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    errors.email = 'Geçerli bir e-posta girin'
  }
  if (!password.value) {
    errors.password = 'Şifre zorunlu'
  }
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

function nextPath() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  return redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
}

async function onSubmit() {
  formError.value = ''
  if (!validate()) {
    return
  }
  loading.value = true
  try {
    await auth.login(email.value.trim(), password.value)
    await router.push(nextPath())
  } catch (error) {
    fieldErrors.value = {
      email: fieldText(error.fields?.email),
      password: fieldText(error.fields?.password),
    }
    formError.value = error.message || 'Giriş yapılamadı'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout title="Giriş yap" lede="Bütçenizi görmek ve kaydetmek için hesabınıza girin.">
    <form @submit.prevent="onSubmit">
      <FormField
        id="email"
        label="E-posta"
        type="email"
        autocomplete="username"
        v-model="email"
        :error="fieldErrors.email"
      />
      <FormField
        id="password"
        label="Şifre"
        type="password"
        autocomplete="current-password"
        v-model="password"
        :error="fieldErrors.password"
      />
      <button type="submit" :disabled="loading" :aria-busy="loading">
        {{ loading ? 'Giriş yapılıyor…' : 'Giriş yap' }}
      </button>
      <AlertMessage :message="formError" />
    </form>
    <p class="switch">Hesabınız yok mu? <RouterLink to="/register">Kayıt ol</RouterLink></p>
  </AuthLayout>
</template>
