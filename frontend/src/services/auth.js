import { api } from './http'

export function login(email, password) {
  return api('/api/v1/auth/login', { method: 'POST', body: { email, password } })
}

export function register({ firstName, lastName, email, password }) {
  return api('/api/v1/auth/register', {
    method: 'POST',
    body: { firstName, lastName, email, password },
  })
}

export function logout() {
  return api('/api/v1/auth/logout', { method: 'POST' })
}

export function me() {
  return api('/api/v1/auth/me')
}

export function refresh() {
  return api('/api/v1/auth/refresh', { method: 'POST' })
}

export function updateProfile({ firstName, lastName }) {
  return api('/api/v1/auth/profile', { method: 'PUT', body: { firstName, lastName } })
}
