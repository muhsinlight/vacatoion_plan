import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import { useAuth } from '../stores/auth'

const titles = {
  home: 'Ana Sayfa',
  pricing: 'Planla',
  trips: 'Seyahatlerim',
  tripDetail: 'Seyahat',
  profile: 'Profil',
  login: 'Giriş',
  register: 'Kayıt',
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { guest: true } },
    { path: '/register', name: 'register', component: () => import('../views/Register.vue'), meta: { guest: true } },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'home', component: () => import('../views/Home.vue') },
        { path: 'pricing', name: 'pricing', component: () => import('../views/Pricing.vue') },
        { path: 'trips', name: 'trips', component: () => import('../views/Trips.vue') },
        { path: 'trips/:id', name: 'tripDetail', component: () => import('../views/TripDetail.vue') },
        { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuth()
  await auth.bootstrap()
  const needsAuth = to.matched.some((record) => record.meta.requiresAuth)
  const guestOnly = to.matched.some((record) => record.meta.guest)
  if (needsAuth && !auth.isAuthenticated.value) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (guestOnly && auth.isAuthenticated.value) {
    return { path: '/' }
  }
  return true
})

router.afterEach((to) => {
  const title = titles[to.name] || 'Vacation Plan'
  document.title = title === 'Vacation Plan' ? title : `${title} · Vacation Plan`
})
