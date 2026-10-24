import { computed, ref } from 'vue'
import { ApiError } from '../services/http'
import * as authApi from '../services/auth'

const user = ref(null)
const ready = ref(false)
let bootstrapTask = null

export function clearLocalSession() {
  user.value = null
}

export function useAuth() {
  async function bootstrap() {
    if (ready.value) {
      return user.value
    }
    if (!bootstrapTask) {
      bootstrapTask = authApi.me()
        .then((account) => {
          user.value = account
        })
        .catch((err) => {
          if (err instanceof ApiError && err.status === 401) {
            user.value = null
          }
        })
        .finally(() => {
          ready.value = true
        })
    }
    await bootstrapTask
    return user.value
  }

  async function login(email, password) {
    user.value = await authApi.login(email, password)
    ready.value = true
  }

  async function register(profile) {
    user.value = await authApi.register(profile)
    ready.value = true
  }

  async function updateProfile(profile) {
    user.value = await authApi.updateProfile(profile)
    ready.value = true
    return user.value
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      user.value = null
      ready.value = true
    }
  }

  return {
    user,
    ready,
    isAuthenticated: computed(() => user.value != null),
    bootstrap,
    login,
    register,
    updateProfile,
    logout,
  }
}
