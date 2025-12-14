<template>
  <Teleport to="body">
    <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
      <!-- Backdrop -->
      <div class="fixed inset-0 bg-gray-900/30" @click="cancel" />

      <!-- Modal -->
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="relative bg-white rounded-lg shadow-lg w-full max-w-sm">
          <!-- Header -->
          <div class="px-4 py-3 border-b border-gray-100 flex items-center space-x-2">
            <div class="w-6 h-6 rounded-full flex items-center justify-center" :class="iconBgClass">
              <svg v-if="type === 'warning'" class="w-3.5 h-3.5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <svg v-else-if="type === 'danger'" class="w-3.5 h-3.5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
              <svg v-else class="w-3.5 h-3.5 text-sky-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 class="text-sm font-medium text-gray-800">{{ title }}</h3>
          </div>

          <!-- Content -->
          <div class="px-4 py-3">
            <p class="text-xs text-gray-500">{{ message }}</p>
          </div>

          <!-- Footer -->
          <div class="px-4 py-3 border-t border-gray-100 flex justify-end space-x-2">
            <button
              ref="cancelButton"
              @click="cancel"
              class="px-3 py-1.5 text-xs font-medium text-gray-600 bg-white border border-gray-200 rounded hover:bg-gray-50"
            >
              {{ cancelText }}
            </button>
            <button
              @click="confirm"
              class="px-3 py-1.5 text-xs font-medium text-white rounded"
              :class="confirmButtonClass"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: 'Confirm' },
  message: { type: String, default: 'Are you sure?' },
  type: { type: String, default: 'warning', validator: (v) => ['info', 'warning', 'danger'].includes(v) },
  confirmText: { type: String, default: 'Confirm' },
  cancelText: { type: String, default: 'Cancel' }
})

const emit = defineEmits(['confirm', 'cancel'])

const cancelButton = ref(null)

watch(() => props.show, (newVal) => {
  if (newVal) {
    nextTick(() => {
      cancelButton.value?.focus()
    })
  }
})

const iconBgClass = computed(() => {
  const classes = {
    info: 'bg-sky-100',
    warning: 'bg-amber-100',
    danger: 'bg-red-100'
  }
  return classes[props.type] || 'bg-amber-100'
})

const confirmButtonClass = computed(() => {
  const classes = {
    info: 'bg-sky-500 hover:bg-sky-600',
    warning: 'bg-amber-500 hover:bg-amber-600',
    danger: 'bg-red-500 hover:bg-red-600'
  }
  return classes[props.type] || 'bg-amber-500 hover:bg-amber-600'
})

function confirm() {
  emit('confirm')
}

function cancel() {
  emit('cancel')
}
</script>