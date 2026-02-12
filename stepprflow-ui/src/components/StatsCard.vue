<template>
  <div class="card-compact flex items-center gap-4">
    <div
      class="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg"
      :class="variantClasses.bg"
    >
      <svg class="h-6 w-6" :class="variantClasses.icon" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
        <path stroke-linecap="round" stroke-linejoin="round" :d="icon" />
      </svg>
    </div>
    <div class="min-w-0">
      <p class="truncate text-sm text-gray-500">{{ label }}</p>
      <p class="text-2xl font-semibold" :class="variantClasses.text">{{ displayValue }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: Number, default: 0 },
  icon: { type: String, required: true },
  variant: { type: String, default: 'default' }
})

const displayValue = ref(0)

const variants = {
  default: { bg: 'bg-gray-100', icon: 'text-gray-600', text: 'text-gray-900' },
  primary: { bg: 'bg-primary-100', icon: 'text-primary-600', text: 'text-primary-700' },
  success: { bg: 'bg-emerald-100', icon: 'text-emerald-600', text: 'text-emerald-700' },
  danger: { bg: 'bg-red-100', icon: 'text-red-600', text: 'text-red-700' },
  warning: { bg: 'bg-amber-100', icon: 'text-amber-600', text: 'text-amber-700' },
  info: { bg: 'bg-sky-100', icon: 'text-sky-600', text: 'text-sky-700' }
}

const variantClasses = variants[props.variant] || variants.default

function animateValue(from, to) {
  if (from === to) return
  const duration = 400
  const start = performance.now()

  function tick(now) {
    const elapsed = now - start
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    displayValue.value = Math.round(from + (to - from) * eased)
    if (progress < 1) requestAnimationFrame(tick)
  }

  requestAnimationFrame(tick)
}

watch(() => props.value, (newVal, oldVal) => {
  animateValue(oldVal ?? 0, newVal ?? 0)
})

onMounted(() => {
  animateValue(0, props.value ?? 0)
})
</script>
