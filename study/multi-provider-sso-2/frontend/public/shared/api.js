/**
 * Shared API client for frontend interactions.
 * Used by: Architectures 1, 2, 3, 4.
 *
 * Always uses credentials: 'same-origin' to include session cookies.
 * Returns { status, data } without throwing exceptions on HTTP error codes (e.g. 401).
 */

async function request(url, options = {}) {
  try {
    const res = await fetch(url, {
      ...options,
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json',
        ...(options.headers || {}),
      },
    })
    const isJson = (res.headers.get('content-type') || '').includes('application/json')
    const data = isJson ? await res.json().catch(() => null) : await res.text().catch(() => null)
    return { status: res.status, data }
  } catch (err) {
    return { status: 0, data: { error: err.message } }
  }
}

export async function getGateway(arch) {
  return request(`/api/${arch}/gateway`)
}

export async function getMe(arch) {
  return request(`/api/${arch}/me`)
}

export async function getHello(arch) {
  return request(`/api/${arch}/hello`)
}

export async function logout(arch) {
  return request(`/api/${arch}/logout`, { method: 'POST' })
}

export async function localLogin(email, password) {
  return request('/api/architecture-4/local/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
}

export async function localSignup(email, password, displayName) {
  return request('/api/architecture-4/local/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, displayName }),
  })
}

export async function localForgotPassword(email) {
  return request('/api/architecture-4/local/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  })
}

export async function localResetPassword(token, newPassword) {
  return request('/api/architecture-4/local/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword }),
  })
}
