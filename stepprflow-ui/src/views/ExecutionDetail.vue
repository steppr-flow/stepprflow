<template>
  <div class="space-y-4">
    <!-- Back Button -->
    <button @click="goBack" class="flex items-center space-x-1.5 text-xs text-gray-500 hover:text-gray-700">
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
      </svg>
      <span>Back</span>
    </button>

    <!-- Loading State -->
    <div v-if="loading && !execution" class="flex items-center justify-center py-12">
      <div class="text-center">
        <svg class="animate-spin h-6 w-6 text-primary-500 mx-auto" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
        </svg>
        <p class="mt-2 text-xs text-gray-400">Loading...</p>
      </div>
    </div>

    <!-- Not Found State -->
    <div v-else-if="!execution" class="flex items-center justify-center py-12">
      <div class="text-center">
        <p class="text-sm text-gray-500">Execution not found</p>
      </div>
    </div>

    <!-- Execution Details -->
    <template v-else>
      <!-- Header -->
      <div class="bg-white border border-gray-100 rounded-lg p-4">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-3">
            <div>
              <div class="flex items-center space-x-2">
                <code class="text-sm font-medium text-gray-700">{{ execution.executionId?.substring(0, 16) }}</code>
                <StatusBadge :status="execution.status" />
              </div>
              <div class="mt-1 text-xs text-gray-400">{{ execution.topic }}</div>
            </div>
          </div>
          <div class="flex items-center space-x-2">
            <button
              v-if="canResume"
              @click="resumeExecution"
              class="btn-sm-success"
            >
              Resume
            </button>
            <button
              v-if="canCancel"
              @click="cancelExecution"
              class="btn-sm-danger"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>

      <!-- Progress & Info -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-3">
        <!-- Progress -->
        <div class="bg-white border border-gray-100 rounded-lg p-3">
          <div class="text-[11px] text-gray-400 mb-2">Progress</div>
          <div class="flex items-center space-x-3">
            <div class="relative w-12 h-12">
              <svg class="w-12 h-12 transform -rotate-90">
                <circle cx="24" cy="24" r="20" stroke-width="4" fill="none" class="stroke-gray-100" />
                <circle
                  cx="24" cy="24" r="20" stroke-width="4" fill="none"
                  :class="progressStrokeColor"
                  :stroke-dasharray="`${progress * 1.26} 126`"
                  stroke-linecap="round"
                />
              </svg>
              <div class="absolute inset-0 flex items-center justify-center">
                <span class="text-xs font-semibold text-gray-700">{{ progress }}%</span>
              </div>
            </div>
            <div>
              <div class="text-lg font-semibold text-gray-700">{{ execution.currentStep }}/{{ execution.totalSteps }}</div>
              <div class="text-[10px] text-gray-400">Steps</div>
            </div>
          </div>
        </div>

        <!-- Timestamps -->
        <div class="bg-white border border-gray-100 rounded-lg p-3">
          <div class="text-[11px] text-gray-400 mb-2">Timeline</div>
          <div class="space-y-1.5 text-xs">
            <div class="flex justify-between">
              <span class="text-gray-400">Created</span>
              <span class="text-gray-600">{{ formatDate(execution.createdAt) }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">Updated</span>
              <span class="text-gray-600">{{ formatDate(execution.updatedAt) }}</span>
            </div>
            <div v-if="execution.completedAt" class="flex justify-between">
              <span class="text-gray-400">Completed</span>
              <span class="text-gray-600">{{ formatDate(execution.completedAt) }}</span>
            </div>
          </div>
        </div>

        <!-- Retry Info -->
        <div class="bg-white border border-gray-100 rounded-lg p-3">
          <div class="text-[11px] text-gray-400 mb-2">Retry Info</div>
          <div v-if="execution.retryInfo" class="space-y-1.5 text-xs">
            <div class="flex justify-between">
              <span class="text-gray-400">Attempt</span>
              <span class="text-gray-600">{{ execution.retryInfo.attempt }}/{{ execution.retryInfo.maxAttempts }}</span>
            </div>
            <div v-if="execution.retryInfo.nextRetryAt" class="flex justify-between">
              <span class="text-gray-400">Next</span>
              <span class="text-gray-600">{{ formatDate(execution.retryInfo.nextRetryAt) }}</span>
            </div>
          </div>
          <div v-else class="text-xs text-gray-400">No retries</div>
        </div>
      </div>

      <!-- Error Info -->
      <div v-if="execution.errorInfo" class="card p-6 border-red-200">
        <h3 class="text-sm font-medium text-red-600 mb-4 flex items-center space-x-2">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <span>Error Details</span>
        </h3>
        <div class="space-y-3">
          <div class="flex items-start space-x-4">
            <div class="flex-1">
              <div class="text-gray-500 text-sm">Message</div>
              <p class="text-red-600 mt-1">{{ execution.errorInfo.message }}</p>
            </div>
            <div>
              <div class="text-gray-500 text-sm">Failed Step</div>
              <p class="text-gray-700 mt-1">{{ execution.errorInfo.stepLabel }} ({{ execution.errorInfo.stepId }})</p>
            </div>
          </div>
          <div>
            <div class="text-gray-500 text-sm mb-2">Stack Trace</div>
            <pre class="text-xs text-gray-600 bg-gray-50 border border-gray-200 rounded-lg p-4 overflow-auto max-h-48">{{ execution.errorInfo.stackTrace }}</pre>
          </div>
        </div>
      </div>

      <!-- Payload -->
      <div class="card p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-medium text-gray-600">Payload</h3>
          <div v-if="canEditPayload" class="text-xs text-gray-500">
            Click on a value to edit it
          </div>
        </div>

        <!-- Warning when editable -->
        <div v-if="canEditPayload" class="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-lg flex items-start space-x-2">
          <svg class="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <p class="text-sm text-amber-700">
            Warning: Changes may lead to data inconsistency.
          </p>
        </div>

        <!-- Payload Editor (editable when failed) -->
        <PayloadEditor
          v-if="execution.payload && typeof execution.payload === 'object'"
          :payload="execution.payload"
          :editable="canEditPayload"
          @update="handlePayloadFieldUpdate"
        />
        <JsonViewer v-else :data="execution.payload" />
      </div>

      <!-- Pending Payload Changes -->
      <div v-if="execution.payloadHistory?.length" class="card p-6 border-amber-200">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-medium text-amber-600 flex items-center space-x-2">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Pending Changes (will be applied on Resume)</span>
          </h3>
          <button
            @click="restorePayload"
            class="text-sm px-3 py-1 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg flex items-center space-x-1 transition-colors"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>Restore</span>
          </button>
        </div>
        <div class="space-y-2">
          <div
            v-for="(change, index) in execution.payloadHistory"
            :key="index"
            class="flex items-center space-x-2 text-sm p-2 bg-amber-50 rounded"
          >
            <code class="font-medium text-primary-700">{{ change.fieldPath }}</code>
            <span class="text-gray-400">:</span>
            <span class="px-2 py-0.5 bg-red-100 text-red-700 rounded line-through">{{ formatChangeValue(change.oldValue) }}</span>
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
            </svg>
            <span class="px-2 py-0.5 bg-green-100 text-green-700 rounded">{{ formatChangeValue(change.newValue) }}</span>
          </div>
        </div>
      </div>

      <!-- Execution Attempts History -->
      <div v-if="execution.executionAttempts?.length" class="card p-6">
        <h3 class="text-sm font-medium text-gray-600 mb-4">Execution Attempts</h3>
        <div class="space-y-4">
          <div
            v-for="(attempt, index) in [...execution.executionAttempts].reverse()"
            :key="index"
            class="border rounded-lg overflow-hidden"
            :class="getAttemptBorderClass(attempt.result)"
          >
            <!-- Attempt Header -->
            <div class="p-4 flex items-center justify-between" :class="getAttemptHeaderClass(attempt.result)">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold"
                  :class="getAttemptCircleClass(attempt.result)"
                >
                  {{ attempt.attemptNumber }}
                </div>
                <div>
                  <div class="font-medium text-gray-900">
                    Attempt #{{ attempt.attemptNumber }}
                    <span v-if="attempt.resumedBy" class="text-gray-500 font-normal">
                      (resumed by {{ attempt.resumedBy }})
                    </span>
                  </div>
                  <div class="text-xs text-gray-500">
                    Started: {{ formatDate(attempt.startedAt) }}
                    <span v-if="attempt.endedAt"> - Ended: {{ formatDate(attempt.endedAt) }}</span>
                  </div>
                </div>
              </div>
              <StatusBadge v-if="attempt.result" :status="attempt.result" />
              <span v-else class="px-2 py-1 text-xs bg-sky-100 text-sky-700 rounded-full">Running...</span>
            </div>

            <!-- Attempt Details -->
            <div class="p-4 bg-white border-t border-gray-100">
              <div class="flex items-center space-x-4 text-sm text-gray-600 mb-3">
                <span>Steps: {{ attempt.startStep }} → {{ attempt.endStep || '...' }}</span>
              </div>

              <!-- Error Message if failed -->
              <div v-if="attempt.errorMessage" class="mb-3 p-3 bg-red-50 border border-red-100 rounded text-sm text-red-700">
                {{ attempt.errorMessage }}
              </div>

              <!-- Payload Changes for this attempt -->
              <div v-if="attempt.payloadChanges?.length" class="mt-3">
                <div class="text-xs font-medium text-gray-500 mb-2">Payload changes applied before this attempt:</div>
                <div class="space-y-1">
                  <div
                    v-for="(change, changeIndex) in attempt.payloadChanges"
                    :key="changeIndex"
                    class="flex items-center space-x-2 text-xs p-2 bg-gray-50 rounded"
                  >
                    <code class="font-medium text-primary-700">{{ change.fieldPath }}</code>
                    <span class="px-1.5 py-0.5 bg-red-100 text-red-600 rounded line-through">{{ formatChangeValue(change.oldValue) }}</span>
                    <svg class="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                    </svg>
                    <span class="px-1.5 py-0.5 bg-green-100 text-green-600 rounded">{{ formatChangeValue(change.newValue) }}</span>
                    <span v-if="change.reason" class="text-gray-400 italic ml-2">({{ change.reason }})</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Step History -->
      <div v-if="execution.stepHistory?.length" class="card p-6">
        <h3 class="text-sm font-medium text-gray-600 mb-4">Step History</h3>
        <div class="space-y-2">
          <div
            v-for="(step, index) in execution.stepHistory"
            :key="index"
            class="flex items-center space-x-4 p-3 bg-gray-50 border border-gray-100 rounded-lg"
          >
            <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium"
              :class="getStepCircleClass(step.status)"
            >
              {{ step.stepId }}
            </div>
            <div class="flex-1">
              <div class="font-medium text-gray-700">{{ getStepLabel(step) }}</div>
              <div v-if="getStepDescription(step)" class="text-xs text-gray-500">{{ getStepDescription(step) }}</div>
              <div class="text-xs text-gray-400">{{ formatDate(step.startedAt) }}</div>
            </div>
            <StatusBadge :status="step.status" :show-dot="false" />
          </div>
        </div>
      </div>
    </template>

    <!-- Change Reason Modal -->
    <ChangeReasonModal
      :show="showChangeModal"
      :field-path="pendingChange.fieldPath"
      :old-value="pendingChange.oldValue"
      :new-value="pendingChange.newValue"
      @confirm="confirmPayloadChange"
      @cancel="cancelPayloadChange"
    />

    <!-- Restore Confirmation Modal -->
    <ConfirmModal
      :show="showRestoreModal"
      title="Restore Payload"
      message="Are you sure you want to restore the payload to its original state? All pending changes will be reverted."
      type="warning"
      confirm-text="Restore"
      cancel-text="Cancel"
      @confirm="confirmRestore"
      @cancel="cancelRestore"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'
import StatusBadge from '@/components/StatusBadge.vue'
import JsonViewer from '@/components/JsonViewer.vue'
import PayloadEditor from '@/components/PayloadEditor.vue'
import ChangeReasonModal from '@/components/ChangeReasonModal.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { dashboardApi } from '@/services/api'
import { format } from 'date-fns'

const props = defineProps({
  id: { type: String, required: true }
})

const router = useRouter()
const route = useRoute()
const store = useWorkflowStore()

// Workflow definitions for step labels
const workflowDefinitions = ref([])

// Modal state
const showChangeModal = ref(false)
const showRestoreModal = ref(false)
const pendingChange = ref({ fieldPath: '', oldValue: null, newValue: null })

const execution = computed(() => store.currentExecution)
const loading = computed(() => store.loading)

const progress = computed(() => {
  if (!execution.value?.totalSteps) return 0
  return Math.round((execution.value.currentStep / execution.value.totalSteps) * 100)
})

const progressStrokeColor = computed(() => {
  const status = execution.value?.status
  const colors = {
    COMPLETED: 'stroke-emerald-500',
    FAILED: 'stroke-red-500',
    IN_PROGRESS: 'stroke-sky-500',
    PENDING: 'stroke-amber-500',
    RETRY_PENDING: 'stroke-orange-500',
    CANCELLED: 'stroke-gray-500'
  }
  return colors[status] || 'stroke-gray-500'
})

const canResume = computed(() => {
  return execution.value?.status === 'FAILED'
})

const canCancel = computed(() => {
  return ['PENDING', 'IN_PROGRESS'].includes(execution.value?.status)
})

const canEditPayload = computed(() => {
  return execution.value?.status === 'FAILED'
})

let interval

onMounted(async () => {
  store.fetchExecution(props.id)

  // Fetch workflow definitions for step labels
  try {
    const { data } = await dashboardApi.getWorkflows()
    workflowDefinitions.value = data
  } catch (e) {
    console.error('Failed to fetch workflow definitions:', e)
  }

  // Auto-refresh if in progress
  interval = setInterval(() => {
    if (['PENDING', 'IN_PROGRESS', 'RETRY_PENDING'].includes(execution.value?.status)) {
      store.fetchExecution(props.id)
    }
  }, 3000)
})

onUnmounted(() => {
  clearInterval(interval)
})

function goBack() {
  router.push('/executions')
}

async function resumeExecution() {
  await store.resumeExecution(props.id)
  // Refresh immediately after resume
  await store.fetchExecution(props.id)
}

async function cancelExecution() {
  if (confirm('Are you sure you want to cancel this execution?')) {
    await store.cancelExecution(props.id)
  }
}

function restorePayload() {
  showRestoreModal.value = true
}

async function confirmRestore() {
  showRestoreModal.value = false
  await store.restorePayload(props.id)
}

function cancelRestore() {
  showRestoreModal.value = false
}

function formatDate(date) {
  if (!date) return '-'
  return format(new Date(date), 'MMM d, yyyy HH:mm:ss')
}

function getStepCircleClass(status) {
  const classes = {
    COMPLETED: 'bg-emerald-100 text-emerald-700 border-2 border-emerald-200',
    PASSED: 'bg-emerald-100 text-emerald-700 border-2 border-emerald-200',
    FAILED: 'bg-red-100 text-red-700 border-2 border-red-200',
    IN_PROGRESS: 'bg-sky-100 text-sky-700 border-2 border-sky-200',
    PENDING: 'bg-amber-100 text-amber-700 border-2 border-amber-200',
    RETRY_PENDING: 'bg-orange-100 text-orange-700 border-2 border-orange-200',
    CANCELLED: 'bg-gray-100 text-gray-600 border-2 border-gray-200'
  }
  return classes[status] || 'bg-gray-100 text-gray-600 border-2 border-gray-200'
}

// Get step info from workflow definitions
function getStepInfo(stepId) {
  if (!execution.value?.topic || !workflowDefinitions.value.length) {
    return null
  }
  const workflow = workflowDefinitions.value.find(w => w.topic === execution.value.topic)
  if (!workflow?.steps) {
    return null
  }
  return workflow.steps.find(s => s.id === stepId)
}

function getStepLabel(step) {
  // First try from step history (persisted label)
  if (step.stepLabel) {
    return step.stepLabel
  }
  // Fallback to workflow definition
  const stepInfo = getStepInfo(step.stepId)
  return stepInfo?.label || 'Step ' + step.stepId
}

function getStepDescription(step) {
  const stepInfo = getStepInfo(step.stepId)
  return stepInfo?.description || null
}

// Payload field update handler - opens modal
function handlePayloadFieldUpdate(fieldPath, newValue, oldValue) {
  pendingChange.value = { fieldPath, newValue, oldValue }
  showChangeModal.value = true
}

// Confirm change from modal
async function confirmPayloadChange(reason) {
  showChangeModal.value = false
  await store.updatePayloadField(
    props.id,
    pendingChange.value.fieldPath,
    pendingChange.value.newValue,
    reason
  )
}

// Cancel change from modal
function cancelPayloadChange() {
  showChangeModal.value = false
  pendingChange.value = { fieldPath: '', oldValue: null, newValue: null }
}

// Format change value for display
function formatChangeValue(value) {
  if (value === null) return 'null'
  if (typeof value === 'string') return `"${value}"`
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

// Attempt styling functions
function getAttemptBorderClass(result) {
  if (!result) return 'border-sky-200'
  const classes = {
    COMPLETED: 'border-emerald-200',
    FAILED: 'border-red-200',
    IN_PROGRESS: 'border-sky-200',
    CANCELLED: 'border-gray-200'
  }
  return classes[result] || 'border-gray-200'
}

function getAttemptHeaderClass(result) {
  if (!result) return 'bg-sky-50'
  const classes = {
    COMPLETED: 'bg-emerald-50',
    FAILED: 'bg-red-50',
    IN_PROGRESS: 'bg-sky-50',
    CANCELLED: 'bg-gray-50'
  }
  return classes[result] || 'bg-gray-50'
}

function getAttemptCircleClass(result) {
  if (!result) return 'bg-sky-100 text-sky-700'
  const classes = {
    COMPLETED: 'bg-emerald-100 text-emerald-700',
    FAILED: 'bg-red-100 text-red-700',
    IN_PROGRESS: 'bg-sky-100 text-sky-700',
    CANCELLED: 'bg-gray-100 text-gray-600'
  }
  return classes[result] || 'bg-gray-100 text-gray-600'
}
</script>