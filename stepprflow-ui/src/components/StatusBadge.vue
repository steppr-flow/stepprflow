<template>
  <span
    class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
    :class="classes"
  >
    <span v-if="dot" class="mr-1.5 h-1.5 w-1.5 rounded-full" :class="dotClass" />
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
  dot: { type: Boolean, default: true }
})

const statusMap = {
  PENDING:       { label: 'Pending',       bg: 'bg-gray-100 text-gray-700',     dot: 'bg-gray-500' },
  IN_PROGRESS:   { label: 'In Progress',   bg: 'bg-blue-100 text-blue-700',     dot: 'bg-blue-500' },
  COMPLETED:     { label: 'Completed',     bg: 'bg-emerald-100 text-emerald-700', dot: 'bg-emerald-500' },
  PASSED:        { label: 'Passed',       bg: 'bg-emerald-100 text-emerald-700', dot: 'bg-emerald-500' },
  FAILED:        { label: 'Failed',        bg: 'bg-red-100 text-red-700',       dot: 'bg-red-500' },
  RETRY_PENDING: { label: 'Retry Pending', bg: 'bg-amber-100 text-amber-700',   dot: 'bg-amber-500' },
  CANCELLED:     { label: 'Cancelled',     bg: 'bg-gray-100 text-gray-500',     dot: 'bg-gray-400' }
}

const config = computed(() => statusMap[props.status] || statusMap.PENDING)
const classes = computed(() => config.value.bg)
const dotClass = computed(() => config.value.dot)
const label = computed(() => config.value.label)
</script>
