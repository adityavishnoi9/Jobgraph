// Vite sends /api requests to Spring Boot, so the browser can use one origin.
const API_BASE = '/api'
let csrf = null

async function errorFromResponse(response, fallback) {
  const body = await response.text().catch(() => '')
  let message = body
  if (response.headers.get('content-type')?.includes('application/json')) {
    try {
      const parsed = JSON.parse(body)
      message = parsed.detail || parsed.message || parsed.error || ''
    } catch { message = '' }
  }
  const error = new Error((message || `${fallback} (${response.status})`).slice(0, 300))
  error.status = response.status
  return error
}

async function csrfToken() {
  // Spring requires this token before a request can change server data.
  if (!csrf) {
    const response = await fetch(`${API_BASE}/auth/csrf`, { credentials: 'same-origin' })
    if (!response.ok) throw await errorFromResponse(response, 'Could not start secure session')
    csrf = await response.json()
  }
  return csrf
}

async function request(path, options = {}) {
  const method = options.method || 'GET'
  const headers = new Headers(options.headers || {})
  // Read requests need no CSRF header; writes do.
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())) {
    const token = await csrfToken()
    headers.set(token.headerName, token.token)
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, method, headers, credentials: 'same-origin' })
  if (!response.ok) throw await errorFromResponse(response, 'Request failed')
  if (response.status === 204) return null
  return response.json()
}

export async function signup(email, password) {
  const account = await request('/auth/signup', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })
  // Login creates a new session, which needs a new CSRF token next time.
  csrf = null
  return account
}

export async function login(email, password) {
  const account = await request('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })
  csrf = null
  return account
}

export async function logout() {
  await request('/auth/logout', { method: 'POST' })
  csrf = null
}

export const getMe = () => request('/auth/me')

export async function getProfile() {
  // A new account has no profile yet; the workspace then shows the upload form.
  try { return await request('/profile') }
  catch (error) { if (error.status === 404) return null; throw error }
}

export function uploadResume(resumeFile, additionalInfo) {
  // Multipart sends the file and the optional notes to the parsing endpoint.
  const formData = new FormData()
  formData.append('resume', resumeFile)
  formData.append('additionalInfo', additionalInfo)
  return request('/profile/upload', { method: 'POST', body: formData })
}

export function saveProfile(profile) {
  return request('/profile', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(profile) })
}

export const getGmailStatus = () => request('/gmail/status')
export const disconnectGmail = () => request('/gmail/connection', { method: 'DELETE' })
export const fetchEmails = () => request('/emails/fetch', { method: 'POST' })
export const getEmailStack = () => request('/emails/stack')
export const startGmailConnection = (email) => request('/gmail/connect', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email }) })
