import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMetricsStore } from './metrics'
import { metricsApi, circuitBreakerApi } from '@/services/api'

// Mock the API module
vi.mock('@/services/api', () => ({
  metricsApi: {
    getDashboard: vi.fn(),
    getWorkflowMetrics: vi.fn()
  },
  circuitBreakerApi: {
    getAll: vi.fn(),
    getConfig: vi.fn(),
    reset: vi.fn()
  }
}))

describe('MetricsStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useMetricsStore()
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have zero metrics', () => {
      expect(store.dashboard.totalStarted).toBe(0)
      expect(store.dashboard.totalCompleted).toBe(0)
      expect(store.dashboard.totalFailed).toBe(0)
    })

    it('should not be loading', () => {
      expect(store.loading).toBe(false)
    })

    it('should have no error', () => {
      expect(store.error).toBeNull()
    })

    it('should have empty circuit breakers', () => {
      expect(store.circuitBreakers).toEqual([])
    })
  })

  describe('computed properties', () => {
    it('totalProcessed should sum completed, failed and cancelled', () => {
      store.dashboard.totalCompleted = 100
      store.dashboard.totalFailed = 10
      store.dashboard.totalCancelled = 5

      expect(store.totalProcessed).toBe(115)
    })

    describe('healthStatus', () => {
      it('should be healthy when success rate >= 95', () => {
        store.dashboard.globalSuccessRate = 98

        expect(store.healthStatus).toBe('healthy')
      })

      it('should be warning when success rate >= 80 and < 95', () => {
        store.dashboard.globalSuccessRate = 85

        expect(store.healthStatus).toBe('warning')
      })

      it('should be critical when success rate < 80', () => {
        store.dashboard.globalSuccessRate = 70

        expect(store.healthStatus).toBe('critical')
      })
    })

    describe('workflowsSorted', () => {
      it('should sort workflows by started count descending', () => {
        store.dashboard.workflowMetrics = [
          { topic: 'order', started: 50 },
          { topic: 'payment', started: 100 },
          { topic: 'shipping', started: 25 }
        ]

        const sorted = store.workflowsSorted

        expect(sorted[0].topic).toBe('payment')
        expect(sorted[1].topic).toBe('order')
        expect(sorted[2].topic).toBe('shipping')
      })

      it('should handle empty array', () => {
        store.dashboard.workflowMetrics = []

        expect(store.workflowsSorted).toEqual([])
      })

      it('should handle null/undefined metrics', () => {
        store.dashboard.workflowMetrics = null

        expect(store.workflowsSorted).toEqual([])
      })
    })
  })

  describe('fetchDashboard', () => {
    it('should fetch and update dashboard metrics', async () => {
      const mockData = {
        totalStarted: 1000,
        totalCompleted: 950,
        totalFailed: 30,
        totalCancelled: 10,
        totalActive: 10,
        globalSuccessRate: 95,
        workflowMetrics: [{ topic: 'order-workflow', started: 500 }]
      }
      metricsApi.getDashboard.mockResolvedValue({ data: mockData })

      await store.fetchDashboard()

      expect(store.dashboard.totalStarted).toBe(1000)
      expect(store.dashboard.totalCompleted).toBe(950)
      expect(store.dashboard.globalSuccessRate).toBe(95)
      expect(store.dashboard.workflowMetrics).toHaveLength(1)
    })

    it('should set lastUpdated after fetch', async () => {
      metricsApi.getDashboard.mockResolvedValue({ data: {} })

      await store.fetchDashboard()

      expect(store.lastUpdated).toBeInstanceOf(Date)
    })

    it('should handle API errors', async () => {
      metricsApi.getDashboard.mockRejectedValue(new Error('Network error'))

      await store.fetchDashboard()

      expect(store.error).toBe('Network error')
    })

    it('should set loading state during fetch', async () => {
      metricsApi.getDashboard.mockImplementation(() => new Promise(resolve => {
        expect(store.loading).toBe(true)
        resolve({ data: {} })
      }))

      await store.fetchDashboard()

      expect(store.loading).toBe(false)
    })

    it('should handle null response data', async () => {
      metricsApi.getDashboard.mockResolvedValue({ data: null })

      await store.fetchDashboard()

      expect(store.dashboard.totalStarted).toBe(0)
      expect(store.dashboard.workflowMetrics).toEqual([])
    })
  })

  describe('fetchWorkflowMetrics', () => {
    it('should fetch metrics for specific workflow', async () => {
      const mockData = { topic: 'order-workflow', started: 100, completed: 95 }
      metricsApi.getWorkflowMetrics.mockResolvedValue({ data: mockData })

      const result = await store.fetchWorkflowMetrics('order-workflow')

      expect(metricsApi.getWorkflowMetrics).toHaveBeenCalledWith('order-workflow')
      expect(result).toEqual(mockData)
    })

    it('should return null on error', async () => {
      metricsApi.getWorkflowMetrics.mockRejectedValue(new Error('Not found'))

      const result = await store.fetchWorkflowMetrics('unknown')

      expect(result).toBeNull()
      expect(store.error).toBe('Not found')
    })
  })

  describe('circuit breakers', () => {
    describe('fetchCircuitBreakers', () => {
      it('should fetch all circuit breakers', async () => {
        const mockData = [
          { name: 'cb-order', state: 'CLOSED' },
          { name: 'cb-payment', state: 'OPEN' }
        ]
        circuitBreakerApi.getAll.mockResolvedValue({ data: mockData })

        await store.fetchCircuitBreakers()

        expect(store.circuitBreakers).toHaveLength(2)
        expect(store.circuitBreakers[0].name).toBe('cb-order')
      })

      it('should handle error gracefully', async () => {
        circuitBreakerApi.getAll.mockRejectedValue(new Error('Error'))

        await store.fetchCircuitBreakers()

        expect(store.circuitBreakers).toEqual([])
      })
    })

    describe('fetchCircuitBreakerConfig', () => {
      it('should fetch config', async () => {
        const mockConfig = { failureRateThreshold: 50, waitDurationInOpenState: 60000 }
        circuitBreakerApi.getConfig.mockResolvedValue({ data: mockConfig })

        await store.fetchCircuitBreakerConfig()

        expect(store.circuitBreakerConfig).toEqual(mockConfig)
      })

      it('should set null on error', async () => {
        circuitBreakerApi.getConfig.mockRejectedValue(new Error('Error'))

        await store.fetchCircuitBreakerConfig()

        expect(store.circuitBreakerConfig).toBeNull()
      })
    })

    describe('resetCircuitBreaker', () => {
      it('should reset and refresh circuit breakers', async () => {
        circuitBreakerApi.reset.mockResolvedValue({})
        circuitBreakerApi.getAll.mockResolvedValue({ data: [] })

        const result = await store.resetCircuitBreaker('cb-order')

        expect(circuitBreakerApi.reset).toHaveBeenCalledWith('cb-order')
        expect(circuitBreakerApi.getAll).toHaveBeenCalled()
        expect(result).toBe(true)
      })

      it('should return false on error', async () => {
        circuitBreakerApi.reset.mockRejectedValue(new Error('Error'))

        const result = await store.resetCircuitBreaker('cb-order')

        expect(result).toBe(false)
      })
    })
  })

  describe('clearError', () => {
    it('should clear error', () => {
      store.error = 'Some error'

      store.clearError()

      expect(store.error).toBeNull()
    })
  })
})
