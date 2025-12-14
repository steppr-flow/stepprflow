<template>
  <span
    class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium"
    :class="statusClasses"
  >
    <span v-if="showDot" class="w-1 h-1 rounded-full mr-1" :class="dotClass" />
    {{ displayText }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
  showDot: { type: Boolean, default: true },
  size: { type: String, default: 'md' }
})

const statusConfig = {
  PENDING: {
    bg: 'bg-amber-50',
    text: 'text-amber-600',
    dot: 'bg-amber-400',
    label: 'Pending'
  },
  IN_PROGRESS: {
    bg: 'bg-sky-50',
    text: 'text-sky-600',
    dot: 'bg-sky-400',
    label: 'Running'
  },
  COMPLETED: {
    bg: 'bg-emerald-50',
    text: 'text-emerald-600',
    dot: 'bg-emerald-400',
    label: 'Done'
  },
  FAILED: {
    bg: 'bg-red-50',
    text: 'text-red-600',
    dot: 'bg-red-400',
    label: 'Failed'
  },
  RETRY_PENDING: {
    bg: 'bg-orange-50',
    text: 'text-orange-600',
    dot: 'bg-orange-400',
    label: 'Retry'
  },
  CANCELLED: {
    bg: 'bg-gray-100',
    text: 'text-gray-500',
    dot: 'bg-gray-400',
    label: 'Cancelled'
  },
  PASSED: {
    bg: 'bg-emerald-50',
    text: 'text-emerald-600',
    dot: 'bg-emerald-400',
    label: 'Passed'
  }
}

const config = computed(() => statusConfig[props.status] || statusConfig.PENDING)
const statusClasses = computed(() => `${config.value.bg} ${config.value.text}`)
const dotClass = computed(() => config.value.dot)
const displayText = computed(() => config.value.label)
</script>