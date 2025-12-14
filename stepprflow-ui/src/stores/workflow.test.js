import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWorkflowStore } from './workflow'
import { dashboardApi, executionApi, ErrorCodes, ApiError } from '@/services/api'

// Mock the API module
vi.mock('@/services/api', () => ({
  dashboardApi: {
    getOverview: vi.fn()
  },
  executionApi: {
    getExecutions: vi.fn(),
    getExecution: vi.fn(),
    resumeExecution: vi.fn(),
    cancelExecution: vi.fn(),
    updatePayloadField: vi.fn(),
    restorePayload: vi.fn()
  },
  ErrorCodes: {
    INVALID_ARGUMENT: 'INVALID_ARGUMENT',
    INVALID_STATE: 'INVALID_STATE',
    CONCURRENT_MODIFICATION: 'CONCURRENT_MODIFICATION',
    RESOURCE_NOT_FOUND: 'RESOURCE_NOT_FOUND'
  },
  ApiError: class ApiError extends Error {
    constructor(code, message, details = {}) {
      super(message)
      this.code = code
      this.details = details
      this.isApiError = true
    }
    isConflict() { return this.code === 'CONCURRENT_MODIFICATION' }
    isNotFound() { return this.code === 'RESOURCE_NOT_FOUND' }
    isValidation() { return this.code === 'INVALID_ARGUMENT' }
  }
}))

describe('WorkflowStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useWorkflowStore()
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have empty stats', () => {
      expect(store.stats.total).toBe(0)
      expect(store.stats.pending).toBe(0)
      expect(store.stats.completed).toBe(0)
    })

    it('should have empty executions', () => {
      expect(store.executions).toEqual([])
      expect(store.recentExecutions).toEqual([])
    })

    it('should not be loading', () => {
      expect(store.loading).toBe(false)
    })

    it('should have no error', () => {
      expect(store.error).toBeNull()
      expect(store.hasError).toBe(false)
    })

    it('should have default pagination', () => {
      expect(store.pagination.page).toBe(0)
      expect(store.pagination.size).toBe(20)
    })
  })

  describe('fetchDashboard', () => {
    it('should fetch and update stats', async () => {
      const mockData = {
        stats: { pending: 5, inProgress: 3, completed: 100, failed: 2, total: 110 },
        recentExecutions: [{ executionId: 'exec-1' }],
        workflows: [{ topic: 'order-workflow' }]
      }
      dashboardApi.getOverview.mockResolvedValue({ data: mockData })

      await store.fetchDashboard()

      expect(store.stats.pending).toBe(5)
      expect(store.stats.completed).toBe(100)
      expect(store.stats.total).toBe(110)
      expect(store.recentExecutions).toHaveLength(1)
      expect(store.workflows).toHaveLength(1)
    })

    it('should set loading state during fetch', async () => {
      dashboardApi.getOverview.mockImplementation(() => new Promise(resolve => {
        expect(store.loading).toBe(true)
        resolve({ data: { stats: {} } })
      }))

      await store.fetchDashboard()

      expect(store.loading).toBe(false)
    })

    it('should handle API errors', async () => {
      const apiError = new (vi.mocked(ApiError))('INTERNAL_ERROR', 'Server error')
      apiError.isApiError = true
      dashboardApi.getOverview.mockRejectedValue(apiError)

      await store.fetchDashboard()

      expect(store.hasError).toBe(true)
      expect(store.errorCode).toBe('INTERNAL_ERROR')
    })
  })

  describe('fetchExecutions', () => {
    it('should fetch paginated executions', async () => {
      const mockData = {
        content: [{ executionId: 'exec-1' }, { executionId: 'exec-2' }],
        totalElements: 50,
        totalPages: 3
      }
      executionApi.getExecutions.mockResolvedValue({ data: mockData })

      await store.fetchExecutions()

      expect(store.executions).toHaveLength(2)
      expect(store.pagination.totalElements).toBe(50)
      expect(store.pagination.totalPages).toBe(3)
    })

    it('should reset page when resetPage is true', async () => {
      store.pagination.page = 5
      executionApi.getExecutions.mockResolvedValue({ data: { content: [] } })

      await store.fetchExecutions(true)

      expect(store.pagination.page).toBe(0)
    })

    it('should include filters in API call', async () => {
      store.setFilters({ topic: 'order-workflow', statuses: ['FAILED', 'COMPLETED'] })
      executionApi.getExecutions.mockResolvedValue({ data: { content: [] } })

      await store.fetchExecutions()

      expect(executionApi.getExecutions).toHaveBeenCalledWith(
        expect.objectContaining({
          topic: 'order-workflow',
          statuses: 'FAILED,COMPLETED'
        })
      )
    })
  })

  describe('fetchExecution', () => {
    it('should fetch single execution by id', async () => {
      const mockExecution = { executionId: 'exec-123', status: 'COMPLETED' }
      executionApi.getExecution.mockResolvedValue({ data: mockExecution })

      const result = await store.fetchExecution('exec-123')

      expect(result).toEqual(mockExecution)
      expect(store.currentExecution).toEqual(mockExecution)
    })

    it('should return null on error', async () => {
      executionApi.getExecution.mockRejectedValue(new Error('Not found'))

      const result = await store.fetchExecution('unknown')

      expect(result).toBeNull()
      expect(store.hasError).toBe(true)
    })
  })

  describe('resumeExecution', () => {
    it('should resume execution and refresh', async () => {
      const mockExecution = { executionId: 'exec-123', status: 'IN_PROGRESS' }
      executionApi.resumeExecution.mockResolvedValue({})
      executionApi.getExecution.mockResolvedValue({ data: mockExecution })

      const result = await store.resumeExecution('exec-123')

      expect(result.success).toBe(true)
      expect(executionApi.resumeExecution).toHaveBeenCalledWith('exec-123', null)
    })

    it('should resume from specific step', async () => {
      executionApi.resumeExecution.mockResolvedValue({})
      executionApi.getExecution.mockResolvedValue({ data: {} })

      await store.resumeExecution('exec-123', 2)

      expect(executionApi.resumeExecution).toHaveBeenCalledWith('exec-123', 2)
    })

    it('should return needsRefresh on concurrent modification', async () => {
      const apiError = { code: 'CONCURRENT_MODIFICATION', message: 'Modified', isApiError: true }
      executionApi.resumeExecution.mockRejectedValue(apiError)

      const result = await store.resumeExecution('exec-123')

      expect(result.success).toBe(false)
      expect(result.needsRefresh).toBe(true)
    })
  })

  describe('cancelExecution', () => {
    it('should cancel execution and refresh', async () => {
      const mockExecution = { executionId: 'exec-123', status: 'CANCELLED' }
      executionApi.cancelExecution.mockResolvedValue({})
      executionApi.getExecution.mockResolvedValue({ data: mockExecution })

      const result = await store.cancelExecution('exec-123')

      expect(result.success).toBe(true)
      expect(executionApi.cancelExecution).toHaveBeenCalledWith('exec-123')
    })
  })

  describe('updatePayloadField', () => {
    it('should update payload field', async () => {
      const mockExecution = { executionId: 'exec-123', payload: { field: 'newValue' } }
      executionApi.updatePayloadField.mockResolvedValue({ data: mockExecution })

      const result = await store.updatePayloadField('exec-123', 'field', 'newValue', 'Fix typo')

      expect(result.success).toBe(true)
      expect(store.currentExecution).toEqual(mockExecution)
    })
  })

  describe('restorePayload', () => {
    it('should restore original payload', async () => {
      const mockExecution = { executionId: 'exec-123', payload: { original: true } }
      executionApi.restorePayload.mockResolvedValue({ data: mockExecution })

      const result = await store.restorePayload('exec-123')

      expect(result.success).toBe(true)
      expect(store.currentExecution).toEqual(mockExecution)
    })
  })

  describe('pagination', () => {
    beforeEach(() => {
      executionApi.getExecutions.mockResolvedValue({
        data: { content: [], totalElements: 100, totalPages: 5 }
      })
    })

    it('should go to next page', async () => {
      store.pagination.totalPages = 5
      store.pagination.page = 0

      store.nextPage()

      expect(store.pagination.page).toBe(1)
    })

    it('should not go beyond last page', () => {
      store.pagination.totalPages = 5
      store.pagination.page = 4

      store.nextPage()

      expect(store.pagination.page).toBe(4)
    })

    it('should go to previous page', async () => {
      store.pagination.page = 2

      store.prevPage()

      expect(store.pagination.page).toBe(1)
    })

    it('should not go below first page', () => {
      store.pagination.page = 0

      store.prevPage()

      expect(store.pagination.page).toBe(0)
    })

    it('should change page size and reset to first page', () => {
      store.pagination.page = 3

      store.setPageSize(50)

      expect(store.pagination.size).toBe(50)
      expect(store.pagination.page).toBe(0)
    })

    it('hasMorePages should be true when not on last page', () => {
      store.pagination.page = 2
      store.pagination.totalPages = 5

      expect(store.hasMorePages).toBe(true)
    })

    it('hasMorePages should be false on last page', () => {
      store.pagination.page = 4
      store.pagination.totalPages = 5

      expect(store.hasMorePages).toBe(false)
    })
  })

  describe('filters', () => {
    it('should update filters', () => {
      store.setFilters({ topic: 'payment-workflow' })

      expect(store.filters.topic).toBe('payment-workflow')
    })

    it('should merge filters', () => {
      store.setFilters({ topic: 'order-workflow' })
      store.setFilters({ statuses: ['FAILED'] })

      expect(store.filters.topic).toBe('order-workflow')
      expect(store.filters.statuses).toEqual(['FAILED'])
    })
  })

  describe('error handling', () => {
    it('should clear error', () => {
      store.error = { code: 'ERROR', message: 'Test' }

      store.clearError()

      expect(store.error).toBeNull()
      expect(store.hasError).toBe(false)
    })

    it('should clear error before new fetch', async () => {
      store.error = { code: 'OLD_ERROR', message: 'Old' }
      dashboardApi.getOverview.mockResolvedValue({ data: {} })

      await store.fetchDashboard()

      expect(store.error).toBeNull()
    })
  })
})
