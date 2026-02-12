<template>
  <div class="json-viewer" v-html="highlighted" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: [Object, Array, String, Number, Boolean], default: null },
  indent: { type: Number, default: 2 }
})

function escapeHtml(str) {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

const highlighted = computed(() => {
  if (props.data === null || props.data === undefined) {
    return '<span class="json-null">null</span>'
  }

  const raw = typeof props.data === 'string' ? props.data : JSON.stringify(props.data, null, props.indent)

  return escapeHtml(raw).replace(
    /("(?:\\.|[^"\\])*")\s*:/g,
    '<span class="json-key">$1</span>:'
  ).replace(
    /:\s*("(?:\\.|[^"\\])*")/g,
    ': <span class="json-string">$1</span>'
  ).replace(
    /:\s*(-?\d+\.?\d*(?:[eE][+-]?\d+)?)/g,
    ': <span class="json-number">$1</span>'
  ).replace(
    /:\s*(true|false)/g,
    ': <span class="json-boolean">$1</span>'
  ).replace(
    /:\s*(null)/g,
    ': <span class="json-null">$1</span>'
  )
})
</script>
