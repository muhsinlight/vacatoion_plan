const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])
const REFRESH_PATH = '/api/v1/auth/refresh'

let onUnauthorized = () => {}
let refreshInFlight = null

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

export class ApiError extends Error {
  constructor(status, code, message, fields) {
    super(message)
    this.status = status
    this.code = code
    this.fields = fields || {}
  }
}

function baseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL
  if (!configured) {
    return ''
  }
  return String(configured).replace(/\/$/, '')
}

function readCookie(name) {
  const prefix = `${name}=`
  for (const part of document.cookie.split(';')) {
    const trimmed = part.trim()
    if (trimmed.startsWith(prefix)) {
      return decodeURIComponent(trimmed.slice(prefix.length))
    }
  }
  return ''
}

async function ensureCsrf() {
  if (readCookie('XSRF-TOKEN')) {
    return
  }
  await fetch(`${baseUrl()}/api/v1/auth/me`, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
}

async function readBody(response) {
  const text = await response.text()
  if (!text) {
    return null
  }
  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}

function isAuthEntry(path) {
  return path === '/api/v1/auth/login' || path === '/api/v1/auth/register'
}

function shouldAttemptRefresh(path, retried) {
  return !retried && path !== REFRESH_PATH && !isAuthEntry(path)
}

async function send(path, { method = 'GET', body, signal } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (!SAFE_METHODS.has(method)) {
    try {
      await ensureCsrf()
    } catch {
      throw new ApiError(0, 'NETWORK', 'Sunucuya ulaşılamadı. API çalışıyor mu?')
    }
    const token = readCookie('XSRF-TOKEN')
    if (token) {
      headers['X-CSRF-TOKEN'] = token
    }
  }

  let response
  try {
    response = await fetch(`${baseUrl()}${path}`, {
      method,
      headers,
      credentials: 'include',
      signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw error
    }
    throw new ApiError(0, 'NETWORK', 'Sunucuya ulaşılamadı. API çalışıyor mu?')
  }

  if (response.status === 204) {
    return null
  }

  const payload = await readBody(response)
  if (!response.ok) {
    throw new ApiError(
      response.status,
      payload?.code || 'HTTP_ERROR',
      payload?.message || 'İstek tamamlanamadı',
      payload?.fields,
    )
  }
  return payload
}

function refreshSession() {
  if (!refreshInFlight) {
    refreshInFlight = send(REFRESH_PATH, { method: 'POST' }).finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

export async function api(path, options = {}, retried = false) {
  try {
    return await send(path, options)
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) {
      throw error
    }
    if (shouldAttemptRefresh(path, retried)) {
      try {
        await refreshSession()
        return await api(path, options, true)
      } catch (refreshError) {
        if (refreshError instanceof ApiError && refreshError.status === 401) {
          onUnauthorized()
        }
        throw error
      }
    }
    if (path === REFRESH_PATH || (!isAuthEntry(path) && retried)) {
      onUnauthorized()
    }
    throw error
  }
}
