<template>
  <div class="card space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-gray-900">Outbox</h3>
      <span
        v-if="outbox"
        class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
        :class="healthBadge"
      >
        <span class="h-1.5 w-1.5 rounded-full" :class="healthDot" />
        {{ outbox.health }}
      </span>
    </div>

    <div v-if="!outbox" class="text-sm text-gray-400">Loading...</div>

    <template v-else>
      <!-- Counters -->
      <div class="grid grid-cols-3 gap-3">
        <div class="text-center">
          <p class="text-lg font-semibold text-amber-600">{{ outbox.pending }}</p>
          <p class="text-xs text-gray-500">Pending</p>
        </div>
        <div class="text-center">
          <p class="text-lg font-semibold text-emerald-600">{{ outbox.sent }}</p>
          <p class="text-xs text-gray-500">Sent</p>
        </div>
        <div class="text-center">
          <p class="text-lg font-semibold text-red-600">{{ outbox.failed }}</p>
          <p class="text-xs text-gray-500">Failed</p>
        </div>
      </div>

      <!-- Distribution bar -->
      <div v-if="outbox.total > 0" class="space-y-1">
        <div class="flex h-2 overflow-hidden rounded-full bg-gray-100">
          <div class="bg-emerald-500 transition-all" :style="{ width: sentPct + '%' }" />
          <div class="bg-amber-500 transition-all" :style="{ width: pendingPct + '%' }" />
          <div class="bg-red-500 transition-all" :style="{ width: failedPct + '%' }" />
        </div>
        <div class="flex justify-between text-xs text-gray-400">
          <span>{{ sendRate }}% delivered</span>
          <span>{{ outbox.total }} total</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  outbox: { type: Object, default: null }
})

const sentPct = computed(() => props.outbox?.total ? (props.outbox.sent / props.outbox.total * 100) : 0)
const pendingPct = computed(() => props.outbox?.total ? (props.outbox.pending / props.outbox.total * 100) : 0)
const failedPct = computed(() => props.outbox?.total ? (props.outbox.failed / props.outbox.total * 100) : 0)
const sendRate = computed(() => props.outbox?.sendRate != null ? props.outbox.sendRate.toFixed(1) : '0.0')

const healthBadge = computed(() => {
  if (!props.outbox) return ''
  if (props.outbox.health === 'UP') return 'bg-emerald-100 text-emerald-700'
  if (props.outbox.health === 'WARNING') return 'bg-amber-100 text-amber-700'
  return 'bg-red-100 text-red-700'
})

const healthDot = computed(() => {
  if (!props.outbox) return ''
  if (props.outbox.health === 'UP') return 'bg-emerald-500'
  if (props.outbox.health === 'WARNING') return 'bg-amber-500'
  return 'bg-red-500'
})
</script>
