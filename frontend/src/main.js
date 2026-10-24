import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { setUnauthorizedHandler } from './services/http'
import { clearLocalSession } from './stores/auth'
import './assets/main.css'
import './assets/tailwind.css'

setUnauthorizedHandler(() => {
  clearLocalSession()
  const current = router.currentRoute.value
  if (current.matched.some((record) => record.meta.requiresAuth)) {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
})

createApp(App).use(router).mount('#app')
