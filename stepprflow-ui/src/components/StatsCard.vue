<template>
  <div class="bg-white border border-gray-100 rounded-lg px-3 py-2.5">
    <div class="text-[11px] text-gray-400 mb-0.5">{{ label }}</div>
    <div class="text-xl font-semibold" :class="colorClass">{{ animatedValue }}</div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  label: { type: String, required: true },
  subtitle: { type: String, default: '' },
  variant: { type: String, default: 'default' },
  iconPath: { type: String, default: 'M13 10V3L4 14h7v7l9-11h-7z' },
  showProgress: { type: Boolean, default: false },
  progress: { type: Number, default: 0 }
})

const animatedValue = ref(0)

const variants = {
  default: { color: 'text-gray-700' },
  primary: { color: 'text-primary-600' },
  success: { color: 'text-emerald-600' },
  warning: { color: 'text-amber-600' },
  danger: { color: 'text-red-600' },
  info: { color: 'text-sky-600' }
}

const colorClass = computed(() => variants[props.variant]?.color || variants.default.color)

// Animate number on mount and value change
const animateValue = (target) => {
  const start = animatedValue.value
  const duration = 500
  const startTime = performance.now()

  const animate = (currentTime) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easeOutQuad = 1 - (1 - progress) * (1 - progress)
    animatedValue.value = Math.floor(start + (target - start) * easeOutQuad)

    if (progress < 1) {
      requestAnimationFrame(animate)
    }
  }

  requestAnimationFrame(animate)
}

onMounted(() => animateValue(props.value))
watch(() => props.value, animateValue)
</script>