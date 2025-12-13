<template>
  <div class="bg-white border border-gray-100 rounded-lg">
    <div class="px-4 py-3 border-b border-gray-50 flex items-center justify-between">
      <div>
        <h2 class="text-sm font-medium text-gray-800">Outbox Queue</h2>
        <p class="text-[10px] text-gray-400">Message delivery status</p>
      </div>
      <span
        class="px-2 py-0.5 text-[10px] font-medium rounded"
        :class="healthStatusClass"
      >
        {{ outbox.health }}
      </span>
    </div>

    <div class="p-4">
      <!-- Stats Grid -->
      <div class="grid grid-cols-4 gap-3 mb-4">
        <div class="text-center">
          <div class="text-lg font-semibold text-amber-600">{{ outbox.pending }}</div>
          <div class="text-[10px] text-gray-400">Pending</div>
        </div>
        <div class="text-center">
          <div class="text-lg font-semibold text-emerald-600">{{ outbox.sent }}</div>
          <div class="text-[10px] text-gray-400">Sent</div>
        </div>
        <div class="text-center">
          <div class="text-lg font-semibold text-red-600">{{ outbox.failed }}</div>
          <div class="text-[10px] text-gray-400">Failed</div>
        </div>
        <div class="text-center">
          <div class="text-lg font-semibold text-gray-600">{{ outbox.total }}</div>
          <div class="text-[10px] text-gray-400">Total</div>
        </div>
      </div>

      <!-- Send Rate Progress Bar -->
      <div class="mb-3">
        <div class="flex items-center justify-between mb-1">
          <span class="text-[11px] text-gray-400">Send Rate</span>
          <span class="text-sm font-semibold" :class="sendRateClass">{{ sendRateDisplay }}%</span>
        </div>
        <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
          <div
            class="h-full rounded-full transition-all duration-300"
            :class="sendRateBarClass"
            :style="{ width: `${outbox.sendRate}%` }"
          />
        </div>
      </div>

      <!-- Visual Distribution Bar -->
      <div class="mb-2">
        <div class="text-[10px] text-gray-400 mb-1">Distribution</div>
        <div class="h-2 bg-gray-100 rounded-full overflow-hidden flex">
          <div
            v-if="outbox.sent > 0"
            class="h-full bg-emerald-400 transition-all duration-300"
            :style="{ width: `${getPercentage(outbox.sent)}%` }"
            :title="`Sent: ${outbox.sent}`"
          />
          <div
            v-if="outbox.pending > 0"
            class="h-full bg-amber-400 transition-all duration-300"
            :style="{ width: `${getPercentage(outbox.pending)}%` }"
            :title="`Pending: ${outbox.pending}`"
          />
          <div
            v-if="outbox.failed > 0"
            class="h-full bg-red-400 transition-all duration-300"
            :style="{ width: `${getPercentage(outbox.failed)}%` }"
            :title="`Failed: ${outbox.failed}`"
          />
        </div>
      </div>

      <!-- Legend -->
      <div class="flex items-center justify-center space-x-4 text-[9px]">
        <div class="flex items-center space-x-1">
          <div class="w-2 h-2 rounded-full bg-emerald-400"></div>
          <span class="text-gray-500">Sent</span>
        </div>
        <div class="flex items-center space-x-1">
          <div class="w-2 h-2 rounded-full bg-amber-400"></div>
          <span class="text-gray-500">Pending</span>
        </div>
        <div class="flex items-center space-x-1">
          <div class="w-2 h-2 rounded-full bg-red-400"></div>
          <span class="text-gray-500">Failed</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  outbox: {
    type: Object,
    default: () => ({
      pending: 0,
      sent: 0,
      failed: 0,
      total: 0,
      sendRate: 0,
      health: 'UP'
    })
  }
})

const healthStatusClass = computed(() => {
  if (props.outbox.health === 'UP') return 'bg-emerald-50 text-emerald-600'
  if (props.outbox.health === 'WARNING') return 'bg-amber-50 text-amber-600'
  return 'bg-red-50 text-red-600'
})

const sendRateDisplay = computed(() => {
  return (props.outbox.sendRate ?? 0).toFixed(1)
})

const sendRateClass = computed(() => {
  const rate = props.outbox.sendRate ?? 0
  if (rate >= 95) return 'text-emerald-600'
  if (rate >= 80) return 'text-amber-600'
  return 'text-red-600'
})

const sendRateBarClass = computed(() => {
  const rate = props.outbox.sendRate ?? 0
  if (rate >= 95) return 'bg-emerald-500'
  if (rate >= 80) return 'bg-amber-500'
  return 'bg-red-500'
})

function getPercentage(value) {
  if (!props.outbox.total || props.outbox.total === 0) return 0
  return (value / props.outbox.total) * 100
}
</script>
