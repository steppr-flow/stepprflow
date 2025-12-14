<template>
  <div class="space-y-4">
    <!-- Header -->
    <div>
      <h1 class="text-lg font-semibold text-gray-900">Workflows</h1>
      <p class="text-xs text-gray-500">Registered workflow definitions</p>
    </div>

    <!-- Filters -->
    <div class="bg-white border border-gray-100 rounded-lg p-3">
      <div class="flex flex-wrap items-center gap-3">
        <!-- Search Topic -->
        <div class="flex-1 min-w-[150px]">
          <input
            v-model="filters.topic"
            type="text"
            placeholder="Search topic..."
            class="w-full px-3 py-1.5 text-xs text-gray-700 border border-gray-200 rounded-md focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>

        <!-- Service Name Select -->
        <div class="min-w-[140px]">
          <select
            v-model="filters.serviceName"
            class="w-full px-3 py-1.5 text-xs text-gray-700 border border-gray-200 rounded-md focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500 bg-white"
          >
            <option value="">All services</option>
            <option v-for="service in serviceNames" :key="service" :value="service">
              {{ service }}
            </option>
          </select>
        </div>

        <!-- Status Select -->
        <div class="min-w-[120px]">
          <select
            v-model="filters.status"
            class="w-full px-3 py-1.5 text-xs text-gray-700 border border-gray-200 rounded-md focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500 bg-white"
          >
            <option value="">All status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>

        <!-- Clear Filters -->
        <button
          v-if="hasActiveFilters"
          @click="clearFilters"
          class="px-2 py-1.5 text-xs text-gray-500 hover:text-gray-700 transition-colors"
        >
          Clear
        </button>
      </div>
    </div>

    <!-- Workflows Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
      <div
        v-for="workflow in workflows"
        :key="`${workflow.topic}:${workflow.serviceName}`"
        class="bg-white border border-gray-100 rounded-lg overflow-hidden"
      >
        <!-- Header -->
        <div class="px-4 py-3 border-b border-gray-50">
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <div class="w-6 h-6 rounded bg-primary-100 flex items-center justify-center">
                <svg class="w-3.5 h-3.5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
                </svg>
              </div>
              <div>
                <h3 class="text-sm font-medium text-gray-800">{{ workflow.topic }}</h3>
                <div class="flex items-center space-x-2">
                  <span class="text-[10px] px-1.5 py-0.5 rounded bg-blue-50 text-blue-600">{{ workflow.serviceName || 'unknown' }}</span>
                  <p class="text-[11px] text-gray-400 truncate max-w-[150px]">{{ workflow.description || 'No description' }}</p>
                </div>
              </div>
            </div>
            <div class="flex items-center space-x-1">
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="workflow.status === 'ACTIVE' ? 'bg-emerald-400' : 'bg-gray-300'"
              />
              <span class="text-[10px]" :class="workflow.status === 'ACTIVE' ? 'text-emerald-500' : 'text-gray-400'">
                {{ workflow.status === 'ACTIVE' ? 'Active' : 'Inactive' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Steps Button -->
        <div class="px-4 py-3">
          <button
            @click="openStepsModal(workflow)"
            class="flex items-center space-x-2 text-xs text-primary-600 hover:text-primary-700 transition-colors"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
            </svg>
            <span>{{ getSteps(workflow).length }} steps</span>
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>

        <!-- Footer -->
        <div class="px-4 py-2 bg-gray-50/50 border-t border-gray-50 flex items-center justify-between">
          <div class="flex items-center space-x-3 text-[10px] text-gray-400">
            <span>{{ workflow.partitions || 1 }} part.</span>
            <span>{{ workflow.replication || 1 }} repl.</span>
          </div>
          <router-link
            :to="`/executions?topic=${workflow.topic}`"
            class="text-[10px] text-primary-500 hover:text-primary-600"
          >
            View Executions →
          </router-link>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!workflows.length && !loading" class="bg-white border border-gray-100 rounded-lg p-8 text-center">
      <p class="text-xs text-gray-400">No workflows registered</p>
    </div>

    <!-- Steps Modal -->
    <Teleport to="body">
      <div
        v-if="showStepsModal"
        class="fixed inset-0 z-50 flex items-center justify-center"
        @click.self="closeStepsModal"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/50" @click="closeStepsModal" />

        <!-- Modal -->
        <div class="relative bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[80vh] overflow-hidden">
          <!-- Modal Header -->
          <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
            <div>
              <h3 class="text-sm font-semibold text-gray-900">{{ selectedWorkflow?.topic }}</h3>
              <p class="text-xs text-gray-500">{{ getSteps(selectedWorkflow).length }} steps</p>
            </div>
            <button
              @click="closeStepsModal"
              class="w-8 h-8 rounded-full hover:bg-gray-100 flex items-center justify-center transition-colors"
            >
              <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Modal Body -->
          <div class="px-4 py-3 overflow-y-auto max-h-[60vh]">
            <div class="space-y-2">
              <div
                v-for="(step, index) in getSteps(selectedWorkflow)"
                :key="step.id || index"
                class="flex items-start space-x-3 p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors"
              >
                <div class="w-6 h-6 rounded-full bg-primary-100 border border-primary-200 flex items-center justify-center text-xs font-medium text-primary-600 flex-shrink-0">
                  {{ step.id }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center space-x-2">
                    <span class="text-sm font-medium text-gray-800">{{ step.label }}</span>
                    <span
                      v-if="step.skippable"
                      class="px-1.5 py-0.5 text-[10px] rounded bg-amber-100 text-amber-600"
                    >
                      skippable
                    </span>
                  </div>
                  <p v-if="step.description" class="text-xs text-gray-500 mt-0.5">{{ step.description }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useWorkflowStore } from '@/stores/workflow'
import { dashboardApi } from '@/services/api'

const store = useWorkflowStore()
const workflows = ref([])
const allWorkflows = ref([]) // For service names dropdown
const isLoading = ref(true)
const error = ref(null)
const showStepsModal = ref(false)
const selectedWorkflow = ref(null)

// Filters
const filters = ref({
  topic: '',
  serviceName: '',
  status: ''
})

// Debounce timer for topic search
let debounceTimer = null

const loading = computed(() => isLoading.value || store.loading)

const openStepsModal = (workflow) => {
  selectedWorkflow.value = workflow
  showStepsModal.value = true
}

const closeStepsModal = () => {
  showStepsModal.value = false
  selectedWorkflow.value = null
}

// Get all unique service names for the dropdown (from initial unfiltered load)
const serviceNames = computed(() => {
  if (!Array.isArray(allWorkflows.value)) return []
  const names = [...new Set(allWorkflows.value.map(w => w?.serviceName).filter(Boolean))]
  return names.sort()
})

const hasActiveFilters = computed(() => {
  return filters.value.topic || filters.value.serviceName || filters.value.status
})

const clearFilters = () => {
  filters.value = { topic: '', serviceName: '', status: '' }
}

// Fetch workflows with filters
const fetchWorkflows = async () => {
  try {
    isLoading.value = true
    error.value = null

    const params = {}
    if (filters.value.topic) params.topic = filters.value.topic
    if (filters.value.serviceName) params.serviceName = filters.value.serviceName
    if (filters.value.status) params.status = filters.value.status

    const { data } = await dashboardApi.getWorkflows(params)
    if (Array.isArray(data)) {
      workflows.value = data
    }
  } catch (e) {
    console.error('Failed to fetch workflow details:', e)
    error.value = e.message
  } finally {
    isLoading.value = false
  }
}

// Watch filters and fetch with debounce for topic, immediate for others
watch(() => filters.value.topic, () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchWorkflows, 300)
})

watch(() => filters.value.serviceName, fetchWorkflows)
watch(() => filters.value.status, fetchWorkflows)

const getSteps = (workflow) => {
  return Array.isArray(workflow?.steps) ? workflow.steps : []
}

onMounted(async () => {
  store.fetchDashboard()
  // First load: get all workflows for the service names dropdown
  try {
    const { data } = await dashboardApi.getWorkflows()
    if (Array.isArray(data)) {
      allWorkflows.value = data
      workflows.value = data
    }
  } catch (e) {
    console.error('Failed to fetch workflow details:', e)
    error.value = e.message
  } finally {
    isLoading.value = false
  }
})
</script>