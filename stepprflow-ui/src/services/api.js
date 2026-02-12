import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

export class ApiError extends Error {
  constructor(message, status, code, details) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }

  get isNotFound() {
    return this.status === 404
  }

  get isValidation() {
    return this.status === 400 || this.status === 422
  }

  get isServerError() {
    return this.status >= 500
  }

  get isConflict() {
    return this.status === 409
  }
}

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      const { status, data } = error.response
      const message = data?.message || data?.error || error.message
      const code = data?.code || `HTTP_${status}`
      throw new ApiError(message, status, code, data)
    }
    if (error.request) {
      throw new ApiError('Network error — unable to reach the server', 0, 'NETWORK_ERROR')
    }
    throw new ApiError(error.message, 0, 'REQUEST_ERROR')
  }
)

export const dashboardApi = {
  getOverview: () => api.get('/dashboard/overview').then(r => r.data),
  getConfig: () => api.get('/dashboard/config').then(r => r.data),
  getWorkflows: (params) => api.get('/dashboard/workflows', { params }).then(r => r.data)
}

export const executionApi = {
  get: (id) => api.get(`/dashboard/executions/${id}`).then(r => r.data),

  list: (params) => api.get('/dashboard/executions', { params }).then(r => r.data),

  recent: () => api.get('/workflows/recent').then(r => r.data),

  stats: () => api.get('/workflows/stats').then(r => r.data),

  resume: (id) => api.post(`/workflows/${id}/resume`).then(r => r.data),

  cancel: (id) => api.delete(`/workflows/${id}`).then(r => r.data),

  updatePayloadField: (id, fieldPath, newValue, reason) =>
    api.patch(`/workflows/${id}/payload`, { fieldPath, newValue, reason }).then(r => r.data),

  restorePayload: (id) => api.post(`/workflows/${id}/payload/restore`).then(r => r.data)
}

export const metricsApi = {
  getDashboard: () => api.get('/metrics').then(r => r.data),
  getByTopic: (topic) => api.get(`/metrics/${encodeURIComponent(topic)}`).then(r => r.data),
  getSummary: () => api.get('/metrics/summary').then(r => r.data)
}

export const circuitBreakerApi = {
  list: () => api.get('/circuit-breakers').then(r => r.data),
  getConfig: () => api.get('/circuit-breakers/config').then(r => r.data),
  get: (name) => api.get(`/circuit-breakers/${encodeURIComponent(name)}`).then(r => r.data),
  reset: (name) => api.post(`/circuit-breakers/${encodeURIComponent(name)}/reset`).then(r => r.data)
}

export const healthApi = {
  get: () => api.get('/health').then(r => r.data)
}

export const outboxApi = {
  stats: () => api.get('/outbox/stats').then(r => r.data)
}

export default api
