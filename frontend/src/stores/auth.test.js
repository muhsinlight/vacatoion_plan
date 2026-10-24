import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../services/auth.js', () => ({
  me: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  updateProfile: vi.fn(),
  logout: vi.fn(),
}))

async function loadStore() {
  vi.resetModules()
  const api = await import('../services/auth.js')
  const store = await import('./auth.js')
  return { api, ...store }
}

describe('auth store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('bootstraps once and keeps the account', async () => {
    const { api, useAuth } = await loadStore()
    api.me.mockResolvedValue({ email: 'ada@ornek.com' })
    const auth = useAuth()
    await auth.bootstrap()
    await auth.bootstrap()
    expect(api.me).toHaveBeenCalledOnce()
    expect(auth.isAuthenticated.value).toBe(true)
    expect(auth.user.value.email).toBe('ada@ornek.com')
  })

  it('treats a failed session check as signed out', async () => {
    const { api, useAuth } = await loadStore()
    api.me.mockRejectedValue(new Error('401'))
    const auth = useAuth()
    await auth.bootstrap()
    expect(auth.user.value).toBeNull()
    expect(auth.ready.value).toBe(true)
  })

  it('clears the user even when logout fails', async () => {
    const { api, useAuth } = await loadStore()
    api.login.mockResolvedValue({ firstName: 'Ada' })
    api.logout.mockRejectedValue(new Error('ağ'))
    const auth = useAuth()
    await auth.login('ada@ornek.com', 'secret')
    await expect(auth.logout()).rejects.toThrow('ağ')
    expect(auth.user.value).toBeNull()
  })
})
