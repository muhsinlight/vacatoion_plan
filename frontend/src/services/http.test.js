import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, setUnauthorizedHandler } from './http'

describe('api', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0'
    vi.stubGlobal('fetch', vi.fn())
    setUnauthorizedHandler(() => {})
  })

  it('loads a GET without a csrf header', async () => {
    fetch.mockResolvedValue(jsonResponse({ cities: [] }, 200))
    await expect(api('/api/v1/destinations')).resolves.toEqual({ cities: [] })
    expect(fetch).toHaveBeenCalledTimes(1)
    expect(fetch.mock.calls[0][1].headers['X-CSRF-TOKEN']).toBeUndefined()
    expect(fetch.mock.calls[0][1].credentials).toBe('include')
  })

  it('asks for a csrf cookie before a write', async () => {
    fetch.mockImplementation(async (url) => {
      if (String(url).endsWith('/api/v1/auth/me')) {
        document.cookie = 'XSRF-TOKEN=abc%2B'
        return new Response(null, { status: 204 })
      }
      return jsonResponse({ id: 7 }, 201)
    })

    await expect(api('/api/v1/trips', { method: 'POST', body: { adults: 2 } })).resolves.toEqual({ id: 7 })
    const post = fetch.mock.calls[1]
    expect(post[0]).toBe('/api/v1/trips')
    expect(post[1].headers['X-CSRF-TOKEN']).toBe('abc+')
    expect(post[1].body).toBe(JSON.stringify({ adults: 2 }))
  })

  it('returns null for 204 and reports network failure', async () => {
    document.cookie = 'XSRF-TOKEN=token'
    fetch.mockResolvedValueOnce(new Response(null, { status: 204 }))
    await expect(api('/api/v1/auth/logout', { method: 'POST' })).resolves.toBeNull()

    fetch.mockRejectedValueOnce(new Error('offline'))
    await expect(api('/api/v1/destinations')).rejects.toMatchObject({
      status: 0,
      code: 'NETWORK',
    })
  })

  it('clears the session only for unexpected 401s', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    fetch.mockImplementation((url) => {
      if (String(url).endsWith('/api/v1/auth/refresh')) {
        return jsonResponse({ code: 'AUTH', message: 'Oturum yok' }, 401)
      }
      return jsonResponse({ code: 'AUTH', message: 'Oturum yok' }, 401)
    })

    await expect(api('/api/v1/trips')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()

    document.cookie = 'XSRF-TOKEN=token'
    onUnauthorized.mockClear()
    await expect(api('/api/v1/auth/login', { method: 'POST', body: {} })).rejects.toMatchObject({
      status: 401,
      code: 'AUTH',
      message: 'Oturum yok',
    })
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('retries the original request after a successful refresh', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    document.cookie = 'XSRF-TOKEN=token'
    let tripCalls = 0
    fetch.mockImplementation(async (url) => {
      if (String(url).endsWith('/api/v1/trips')) {
        tripCalls += 1
        if (tripCalls === 1) {
          return jsonResponse({ code: 'AUTH', message: 'Oturum yok' }, 401)
        }
        return jsonResponse({ content: [] }, 200)
      }
      if (String(url).endsWith('/api/v1/auth/refresh')) {
        return jsonResponse({ email: 'a@b.c' }, 200)
      }
      return jsonResponse({}, 404)
    })

    await expect(api('/api/v1/trips')).resolves.toEqual({ content: [] })
    expect(onUnauthorized).not.toHaveBeenCalled()
    expect(fetch.mock.calls.some((call) => String(call[0]).endsWith('/api/v1/auth/refresh'))).toBe(true)
  })
})

function jsonResponse(body, status) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
