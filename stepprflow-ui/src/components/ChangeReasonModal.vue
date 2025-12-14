<template>
  <Teleport to="body">
    <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
      <!-- Backdrop -->
      <div class="fixed inset-0 bg-gray-900/30" @click="cancel" />

      <!-- Modal -->
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="relative bg-white rounded-lg shadow-lg w-full max-w-sm">
          <!-- Header -->
          <div class="px-4 py-3 border-b border-gray-100">
            <h3 class="text-sm font-medium text-gray-800">Payload Change</h3>
          </div>

          <!-- Content -->
          <div class="px-4 py-3">
            <!-- Change Summary -->
            <div class="mb-3 p-2.5 bg-gray-50 border border-gray-100 rounded">
              <div class="text-[10px] text-gray-400 mb-1">Field</div>
              <code class="text-xs font-medium text-primary-600">{{ fieldPath }}</code>

              <div class="mt-2 flex items-center space-x-2 text-xs">
                <div>
                  <div class="text-[10px] text-gray-400 mb-0.5">Old</div>
                  <span class="px-1.5 py-0.5 bg-red-50 text-red-600 rounded text-[10px]">{{ formatValue(oldValue) }}</span>
                </div>
                <svg class="w-3 h-3 text-gray-300 mt-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
                <div>
                  <div class="text-[10px] text-gray-400 mb-0.5">New</div>
                  <span class="px-1.5 py-0.5 bg-emerald-50 text-emerald-600 rounded text-[10px]">{{ formatValue(newValue) }}</span>
                </div>
              </div>
            </div>

            <!-- Reason Input -->
            <div>
              <label for="reason" class="block text-[11px] font-medium text-gray-500 mb-1">
                Reason <span class="text-gray-300">(optional)</span>
              </label>
              <textarea
                id="reason"
                ref="reasonInput"
                v-model="reason"
                rows="2"
                class="w-full px-2.5 py-1.5 text-xs border border-gray-200 rounded text-gray-700 placeholder-gray-300 focus:outline-none focus:ring-1 focus:ring-primary-500 resize-none"
                placeholder="Explain why this change is needed..."
                @keydown.enter.ctrl="confirm"
                @keydown.escape="cancel"
              />
            </div>
          </div>

          <!-- Footer -->
          <div class="px-4 py-3 border-t border-gray-100 flex justify-end space-x-2">
            <button
              @click="cancel"
              class="px-3 py-1.5 text-xs font-medium text-gray-600 bg-white border border-gray-200 rounded hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              @click="confirm"
              class="px-3 py-1.5 text-xs font-medium text-white bg-primary-500 rounded hover:bg-primary-600"
            >
              Apply
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  fieldPath: { type: String, default: '' },
  oldValue: { type: [String, Number, Boolean, Object, Array], default: null },
  newValue: { type: [String, Number, Boolean, Object, Array], default: null }
})

const emit = defineEmits(['confirm', 'cancel'])

const reason = ref('')
const reasonInput = ref(null)

watch(() => props.show, (newVal) => {
  if (newVal) {
    reason.value = ''
    nextTick(() => {
      reasonInput.value?.focus()
    })
  }
})

function formatValue(value) {
  if (value === null) return 'null'
  if (typeof value === 'string') return `"${value}"`
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function confirm() {
  emit('confirm', reason.value)
}

function cancel() {
  emit('cancel')
}
</script>