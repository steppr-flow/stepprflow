import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * Error codes from the backend GlobalExceptionHandler.
 */
export const ErrorCodes = {
  INVALID_ARGUMENT: 'INVALID_ARGUMENT',
  INVALID_STATE: 'INVALID_STATE',
  CONCURRENT_MODIFICATION: 'CONCURRENT_MODIFICATION',
  RESOURCE_NOT_FOUND: 'RESOURCE_NOT_FOUND',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  INTERNAL_ERROR: 'INTERNAL_ERROR'
}

/**
 * User-friendly error messages for each error code.
 */
const errorMessages = {
  [ErrorCodes.INVALID_ARGUMENT]: 'Invalid input provided',
  [ErrorCodes.INVALID_STATE]: 'This action is not allowed in the current state',
  [ErrorCodes.CONCURRENT_MODIFICATION]: 'This item was modified by another user. Please refresh and try again.',
  [ErrorCodes.RESOURCE_NOT_FOUND]: 'The requested item was not found',
  [ErrorCodes.VALIDATION_ERROR]: 'Please check your input and try again',
  [ErrorCodes.INTERNAL_ERROR]: 'An unexpected error occurred. Please try again later.'
}

/**
 * Custom API error class with parsed backend response.
 */
export class ApiError extends Error {
  constructor(code, message, details = {}) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.details = details
    this.isApiError = true
  }

  /**
   * Check if error is a specific type.
   */
  is(code) {
    return this.code === code
  }

  /**
   * Check if this is a conflict error (concurrent modification or invalid state).
   */
  isConflict() {
    return this.code === ErrorCodes.CONCURRENT_MODIFICATION ||
           this.code === ErrorCodes.INVALID_STATE
  }

  /**
   * Check if this is a not found error.
   */
  isNotFound() {
    return this.code === ErrorCodes.RESOURCE_NOT_FOUND
  }

  /**
   * Check if this is a validation error.
   */
  isValidation() {
    return this.code === ErrorCodes.INVALID_ARGUMENT ||
           this.code === ErrorCodes.VALIDATION_ERROR
  }
}

/**
 * Parse error response from backend.
 */
function parseErrorResponse(error) {
  const response = error.response

  if (!response) {
    // Network error or timeout
    if (error.code === 'ECONNABORTED') {
      return new ApiError('TIMEOUT', 'Request timed out. Please check your connection and try again.')
    }
    return new ApiError('NETWORK_ERROR', 'Unable to connect to server. Please check your connection.')
  }

  const data = response.data

  if (data && data.code) {
    // Structured error response from our GlobalExceptionHandler
    const userMessage = data.message || errorMessages[data.code] || 'An error occurred'
    return new ApiError(data.code, userMessage, {
      timestamp: data.timestamp,
      executionId: data.executionId,
      resourceType: data.resourceType,
      resourceId: data.resourceId,
      fieldErrors: data.fieldErrors
    })
  }

  // Fallback for non-structured errors
  const status = response.status
  if (status === 401) {
    return new ApiError('UNAUTHORIZED', 'Please log in to continue')
  }
  if (status === 403) {
    return new ApiError('FORBIDDEN', 'You do not have permission to perform this action')
  }
  if (status === 404) {
    return new ApiError(ErrorCodes.RESOURCE_NOT_FOUND, 'The requested resource was not found')
  }
  if (status >= 500) {
    return new ApiError(ErrorCodes.INTERNAL_ERROR, 'Server error. Please try again later.')
  }

  return new ApiError('UNKNOWN_ERROR', data?.message || error.message || 'An error occurred')
}

// Response interceptor for error handling
api.interceptors.response.use(
  response => response,
  error => {
    const apiError = parseErrorResponse(error)
    console.error(`API Error [${apiError.code}]:`, apiError.message, apiError.details)
    return Promise.reject(apiError)
  }
)

// Dashboard endpoints (server module - UI config and overview)
export const dashboardApi = {
  getOverview: () => api.get('/dashboard/overview'),
  getConfig: () => api.get('/dashboard/config'),
  getWorkflows: (params = {}) => api.get('/dashboard/workflows', { params })
}

// Metrics endpoints (monitor module)
export const metricsApi = {
  getDashboard: () => api.get('/metrics'),
  getWorkflowMetrics: (topic) => api.get(`/metrics/${topic}`),
  getSummary: () => api.get('/metrics/summary')
}

// Circuit breaker endpoints (monitor module)
export const circuitBreakerApi = {
  getAll: () => api.get('/circuit-breakers'),
  getConfig: () => api.get('/circuit-breakers/config'),
  get: (name) => api.get(`/circuit-breakers/${name}`),
  reset: (name) => api.post(`/circuit-breakers/${name}/reset`)
}

// Health endpoints (monitor module)
export const healthApi = {
  getHealth: () => api.get('/health')
}

// Outbox endpoints (monitor module)
export const outboxApi = {
  getStats: () => api.get('/outbox/stats')
}

// Workflow execution endpoints (monitor module - /api/workflows)
export const executionApi = {
  // List executions with pagination and filters
  getExecutions: (params = {}) => api.get('/workflows', { params }),

  // Get single execution by ID
  getExecution: (id) => api.get(`/workflows/${id}`),

  // Get recent executions
  getRecentExecutions: () => api.get('/workflows/recent'),

  // Get statistics
  getStats: () => api.get('/workflows/stats'),

  // Resume a failed/paused execution
  resumeExecution: (id, fromStep) => api.post(`/workflows/${id}/resume`, null, {
    params: fromStep ? { fromStep } : {}
  }),

  // Cancel a running execution (DELETE method)
  cancelExecution: (id) => api.delete(`/workflows/${id}`),

  // Update a payload field
  updatePayloadField: (id, fieldPath, newValue, reason) => api.patch(`/workflows/${id}/payload`, {
    fieldPath,
    newValue,
    changedBy: 'UI User',
    reason
  }),

  // Restore payload to original state
  restorePayload: (id) => api.post(`/workflows/${id}/payload/restore`)
}

export default api
