<template>
  <div class="space-y-4">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">Executions</h1>
        <p class="text-xs text-gray-500">Browse and manage workflow executions</p>
      </div>
      <button
        @click="refreshExecutions"
        :disabled="loading"
        class="btn-sm flex items-center space-x-1.5"
      >
        <svg
          class="w-3.5 h-3.5"
          :class="{ 'animate-spin': loading }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        <span>Refresh</span>
      </button>
    </div>

    <!-- Filters -->
    <div class="bg-white border border-gray-100 rounded-lg p-3">
      <div class="flex flex-wrap items-center gap-3">
        <!-- Search -->
        <div class="flex-1 min-w-[200px]">
          <div class="relative">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search execution ID..."
              class="input-sm pl-8"
              @input="debouncedSearch"
            />
            <svg class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
        </div>

        <!-- Topic Filter -->
        <select v-model="selectedTopic" class="select-sm" @change="applyFilters">
          <option value="">All Topics</option>
          <option v-for="workflow in workflows" :key="workflow.topic" :value="workflow.topic">
            {{ workflow.topic }}
          </option>
        </select>

        <!-- Status Filter -->
        <div class="relative" ref="statusDropdownRef">
          <button
            type="button"
            @click="showStatusDropdown = !showStatusDropdown"
            class="select-sm flex items-center justify-between min-w-[120px]"
          >
            <span v-if="selectedStatuses.length === 0" class="text-gray-400">Status</span>
            <span v-else class="text-gray-600">{{ selectedStatuses.length }} selected</span>
            <svg class="w-3.5 h-3.5 text-gray-400 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <div
            v-if="showStatusDropdown"
            class="absolute z-10 mt-1 w-40 bg-white border border-gray-200 rounded-lg shadow-lg"
          >
            <div class="p-1.5 space-y-0.5">
              <label
                v-for="option in statusOptions"
                :key="option.value"
                class="flex items-center px-2 py-1 rounded hover:bg-gray-50 cursor-pointer text-xs"
              >
                <input
                  type="checkbox"
                  :value="option.value"
                  v-model="selectedStatuses"
                  @change="applyFilters"
                  class="w-3 h-3 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <span class="ml-2" :class="option.color">{{ option.label }}</span>
              </label>
            </div>
            <div v-if="selectedStatuses.length > 0" class="border-t border-gray-100 p-1.5">
              <button @click="clearStatuses" class="w-full text-xs text-gray-500 hover:text-gray-700 py-0.5">
                Clear
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Results Table -->
    <div class="bg-white border border-gray-100 rounded-lg overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-xs">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="px-3 py-2 text-left font-medium text-gray-500">ID</th>
              <th class="px-3 py-2 text-left font-medium text-gray-500">Topic</th>
              <th class="px-3 py-2 text-left font-medium text-gray-500">Status</th>
              <th class="px-3 py-2 text-left font-medium text-gray-500">Progress</th>
              <th class="px-3 py-2 text-left font-medium text-gray-500">Created</th>
              <th class="px-3 py-2 text-left font-medium text-gray-500 w-16"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr
              v-for="execution in executions"
              :key="execution.executionId"
              class="hover:bg-gray-50 cursor-pointer"
              @click="goToExecution(execution.executionId)"
            >
              <td class="px-3 py-2 whitespace-nowrap">
                <code class="text-gray-600">{{ execution.executionId?.substring(0, 8) }}</code>
              </td>
              <td class="px-3 py-2 whitespace-nowrap">
                <span class="text-gray-600">{{ execution.topic }}</span>
              </td>
              <td class="px-3 py-2 whitespace-nowrap">
                <StatusBadge :status="execution.status" />
              </td>
              <td class="px-3 py-2 whitespace-nowrap">
                <div class="flex items-center space-x-2">
                  <div class="h-1 bg-gray-100 rounded-full overflow-hidden w-16">
                    <div
                      class="h-full rounded-full"
                      :class="getProgressColor(execution.status)"
                      :style="{ width: `${getProgress(execution)}%` }"
                    />
                  </div>
                  <span class="text-gray-400">{{ execution.currentStep }}/{{ execution.totalSteps }}</span>
                </div>
              </td>
              <td class="px-3 py-2 whitespace-nowrap text-gray-400">
                {{ formatDate(execution.createdAt) }}
              </td>
              <td class="px-3 py-2 whitespace-nowrap">
                <div class="flex items-center space-x-1" @click.stop>
                  <button
                    v-if="canResume(execution)"
                    @click="resumeExecution(execution)"
                    class="p-1 text-emerald-500 hover:bg-emerald-50 rounded"
                    title="Resume"
                  >
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                    </svg>
                  </button>
                  <button
                    v-if="canCancel(execution)"
                    @click="cancelExecution(execution)"
                    class="p-1 text-red-500 hover:bg-red-50 rounded"
                    title="Cancel"
                  >
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Empty State -->
      <div v-if="!executions.length && !loading" class="p-8 text-center">
        <p class="text-xs text-gray-400">No executions found</p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="p-8 text-center">
        <div class="inline-flex items-center space-x-2 text-xs text-gray-400">
          <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          <span>Loading...</span>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalElements > 0" class="px-3 py-2 border-t border-gray-100 flex items-center justify-between text-xs">
        <div class="flex items-center space-x-3">
          <span class="text-gray-400">
            {{ pagination.page * pagination.size + 1 }}-{{ Math.min((pagination.page + 1) * pagination.size, pagination.totalElements) }} of {{ pagination.totalElements }}
          </span>
          <select
            :value="pagination.size"
            @change="changePageSize($event)"
            class="select-sm text-xs py-1"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </div>
        <div class="flex items-center space-x-0.5">
          <button
            @click="prevPage"
            :disabled="pagination.page === 0"
            class="p-1 rounded hover:bg-gray-100 disabled:opacity-30"
          >
            <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <template v-for="pageNum in visiblePages" :key="pageNum">
            <span v-if="pageNum === '...'" class="px-1 text-gray-300">...</span>
            <button
              v-else
              @click="goToPage(pageNum - 1)"
              class="min-w-[24px] h-6 px-1.5 rounded text-xs"
              :class="pagination.page === pageNum - 1
                ? 'bg-primary-600 text-white'
                : 'text-gray-600 hover:bg-gray-100'"
            >
              {{ pageNum }}
            </button>
          </template>
          <button
            @click="nextPage"
            :disabled="!hasMorePages"
            class="p-1 rounded hover:bg-gray-100 disabled:opacity-30"
          >
            <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </div>
    </div>
    <!-- Cancel Confirmation Modal -->
    <ConfirmModal
      :show="showCancelModal"
      title="Cancel Execution"
      :message="`Are you sure you want to cancel this execution? This action cannot be undone.`"
      type="danger"
      confirm-text="Cancel Execution"
      cancel-text="Keep Running"
      @confirm="confirmCancel"
      @cancel="closeCancelModal"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'
import StatusBadge from '@/components/StatusBadge.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { format } from 'date-fns'

const router = useRouter()
const store = useWorkflowStore()

const searchQuery = ref('')
const selectedTopic = ref('')
const selectedStatuses = ref([])
const showStatusDropdown = ref(false)
const statusDropdownRef = ref(null)
const showCancelModal = ref(false)
const executionToCancel = ref(null)

const statusOptions = [
  { value: 'PENDING', label: 'Pending', color: 'bg-amber-100 text-amber-700' },
  { value: 'IN_PROGRESS', label: 'In Progress', color: 'bg-sky-100 text-sky-700' },
  { value: 'COMPLETED', label: 'Completed', color: 'bg-emerald-100 text-emerald-700' },
  { value: 'FAILED', label: 'Failed', color: 'bg-red-100 text-red-700' },
  { value: 'RETRY_PENDING', label: 'Retry Pending', color: 'bg-orange-100 text-orange-700' },
  { value: 'CANCELLED', label: 'Cancelled', color: 'bg-gray-100 text-gray-700' }
]

const executions = computed(() => store.executions)
const workflows = computed(() => store.workflows)
const loading = computed(() => store.loading)
const pagination = computed(() => store.pagination)
const hasMorePages = computed(() => store.hasMorePages)

// Compute visible page numbers with current page always visible
const visiblePages = computed(() => {
  const total = pagination.value.totalPages
  const current = pagination.value.page + 1 // Convert 0-indexed to 1-indexed

  if (total <= 9) {
    // Show all pages if 9 or fewer
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const pages = new Set()

  // Always show first 3 pages
  for (let i = 1; i <= 3; i++) {
    pages.add(i)
  }

  // Always show last 3 pages
  for (let i = total - 2; i <= total; i++) {
    pages.add(i)
  }

  // Show current page and 1 page on each side
  for (let i = current - 1; i <= current + 1; i++) {
    if (i > 0 && i <= total) {
      pages.add(i)
    }
  }

  // Convert to sorted array and add ellipsis where needed
  const sortedPages = [...pages].sort((a, b) => a - b)
  const result = []

  for (let i = 0; i < sortedPages.length; i++) {
    if (i > 0 && sortedPages[i] - sortedPages[i - 1] > 1) {
      result.push('...')
    }
    result.push(sortedPages[i])
  }

  return result
})

let searchTimeout

onMounted(() => {
  store.fetchDashboard()
  store.fetchExecutions(true)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  clearTimeout(searchTimeout)
  document.removeEventListener('click', handleClickOutside)
})

function refreshExecutions() {
  store.fetchExecutions(true)
}

function applyFilters() {
  store.setFilters({
    topic: selectedTopic.value,
    statuses: selectedStatuses.value,
    search: searchQuery.value
  })
  store.fetchExecutions(true)
}

function getStatusOption(value) {
  return statusOptions.find(opt => opt.value === value)
}

function removeStatus(status) {
  selectedStatuses.value = selectedStatuses.value.filter(s => s !== status)
  applyFilters()
}

function clearStatuses() {
  selectedStatuses.value = []
  showStatusDropdown.value = false
  applyFilters()
}

function handleClickOutside(event) {
  if (statusDropdownRef.value && !statusDropdownRef.value.contains(event.target)) {
    showStatusDropdown.value = false
  }
}

function debouncedSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(applyFilters, 300)
}

function goToExecution(id) {
  router.push(`/executions/${id}`)
}

function getProgress(execution) {
  if (!execution.totalSteps) return 0
  return Math.round((execution.currentStep / execution.totalSteps) * 100)
}

function getProgressColor(status) {
  const colors = {
    COMPLETED: 'bg-emerald-500',
    FAILED: 'bg-red-500',
    IN_PROGRESS: 'bg-sky-500',
    PENDING: 'bg-amber-500',
    RETRY_PENDING: 'bg-orange-500',
    CANCELLED: 'bg-gray-500'
  }
  return colors[status] || 'bg-gray-500'
}

function canResume(execution) {
  return execution.status === 'FAILED'
}

function canCancel(execution) {
  return ['PENDING', 'IN_PROGRESS', 'RETRY_PENDING', 'FAILED'].includes(execution.status)
}

async function resumeExecution(execution) {
  await store.resumeExecution(execution.executionId)
  store.fetchExecutions()
}

function cancelExecution(execution) {
  executionToCancel.value = execution
  showCancelModal.value = true
}

async function confirmCancel() {
  if (executionToCancel.value) {
    await store.cancelExecution(executionToCancel.value.executionId)
    store.fetchExecutions()
  }
  closeCancelModal()
}

function closeCancelModal() {
  showCancelModal.value = false
  executionToCancel.value = null
}

function prevPage() {
  store.prevPage()
}

function nextPage() {
  store.nextPage()
}

function changePageSize(event) {
  store.setPageSize(Number(event.target.value))
}

function goToPage(page) {
  store.setPage(page)
}

function formatDate(date) {
  if (!date) return '-'
  return format(new Date(date), 'MMM d, yyyy HH:mm:ss')
}
</script>