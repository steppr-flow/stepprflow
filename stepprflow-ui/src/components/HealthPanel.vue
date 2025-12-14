<template>
  <div class="bg-white border border-gray-100 rounded-lg">
    <div class="px-4 py-3 border-b border-gray-50 flex items-center justify-between">
      <div>
        <h2 class="text-sm font-medium text-gray-800">System Health</h2>
        <p class="text-[10px] text-gray-400">Component status</p>
      </div>
      <span
        class="px-2 py-0.5 text-[10px] font-medium rounded"
        :class="overallStatusClass"
      >
        {{ health.status }}
      </span>
    </div>

    <div v-if="!hasComponents" class="p-6 text-center text-xs text-gray-400">
      No health data available
    </div>

    <div v-else class="divide-y divide-gray-50">
      <!-- Broker Health -->
      <div v-if="health.components?.broker" class="px-4 py-3 flex items-center justify-between">
        <div class="flex items-center space-x-2">
          <div class="w-5 h-5 rounded-full flex items-center justify-center" :class="getStatusBgClass(health.components.broker.status)">
            <svg v-if="health.components.broker.status === 'UP'" class="w-3 h-3 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <svg v-else class="w-3 h-3 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <div>
            <div class="text-xs font-medium text-gray-700">Message Broker</div>
            <div class="flex items-center space-x-1.5 mt-0.5">
              <span class="px-1 py-0.5 text-[9px] font-medium rounded" :class="getStatusBadgeClass(health.components.broker.status)">
                {{ health.components.broker.status }}
              </span>
              <span class="text-[10px] text-gray-400">{{ health.components.broker.details?.type || 'unknown' }}</span>
            </div>
          </div>
        </div>
        <div class="text-center">
          <div class="text-xs font-medium" :class="health.components.broker.details?.available ? 'text-emerald-600' : 'text-red-600'">
            {{ health.components.broker.details?.available ? 'Connected' : 'Disconnected' }}
          </div>
        </div>
      </div>

      <!-- Circuit Breakers Health -->
      <div v-if="health.components?.circuitBreakers" class="px-4 py-3 flex items-center justify-between">
        <div class="flex items-center space-x-2">
          <div class="w-5 h-5 rounded-full flex items-center justify-center" :class="getStatusBgClass(health.components.circuitBreakers.status)">
            <svg v-if="health.components.circuitBreakers.status === 'UP'" class="w-3 h-3 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <svg v-else class="w-3 h-3 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <div>
            <div class="text-xs font-medium text-gray-700">Circuit Breakers</div>
            <div class="flex items-center space-x-1.5 mt-0.5">
              <span class="px-1 py-0.5 text-[9px] font-medium rounded" :class="getStatusBadgeClass(health.components.circuitBreakers.status)">
                {{ health.components.circuitBreakers.status }}
              </span>
              <span class="text-[10px] text-gray-400">{{ circuitBreakerCount }} registered</span>
            </div>
          </div>
        </div>
        <div class="flex flex-wrap gap-1 justify-end max-w-[150px]">
          <span
            v-for="(state, name) in circuitBreakerStates"
            :key="name"
            class="px-1 py-0.5 text-[8px] font-medium rounded"
            :class="getCircuitBreakerStateClass(state)"
          >
            {{ formatCbName(name) }}: {{ state }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  health: {
    type: Object,
    default: () => ({ status: 'UP', components: {} })
  }
})

const hasComponents = computed(() => {
  return props.health.components && Object.keys(props.health.components).length > 0
})

const overallStatusClass = computed(() => {
  if (props.health.status === 'UP') return 'bg-emerald-50 text-emerald-600'
  if (props.health.status === 'DEGRADED') return 'bg-amber-50 text-amber-600'
  return 'bg-red-50 text-red-600'
})

const circuitBreakerCount = computed(() => {
  const details = props.health.components?.circuitBreakers?.details || {}
  return Object.keys(details).length
})

const circuitBreakerStates = computed(() => {
  const details = props.health.components?.circuitBreakers?.details || {}
  // Filter out non-state entries
  const states = {}
  for (const [key, value] of Object.entries(details)) {
    if (typeof value === 'string' && ['CLOSED', 'OPEN', 'HALF_OPEN', 'FORCED_OPEN', 'DISABLED'].includes(value)) {
      states[key] = value
    }
  }
  return states
})

function getStatusBgClass(status) {
  return status === 'UP' ? 'bg-emerald-100' : 'bg-red-100'
}

function getStatusBadgeClass(status) {
  return status === 'UP' ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'
}

function getCircuitBreakerStateClass(state) {
  if (state === 'CLOSED') return 'bg-emerald-50 text-emerald-600'
  if (state === 'OPEN' || state === 'FORCED_OPEN') return 'bg-red-50 text-red-600'
  return 'bg-amber-50 text-amber-600'
}

function formatCbName(name) {
  // Remove "broker-" prefix for display
  return name.replace('broker-', '')
}
</script>
