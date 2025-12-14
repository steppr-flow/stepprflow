<template>
  <div class="json-viewer">
    <pre v-html="formattedJson"></pre>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: [Object, Array, String, Number, Boolean, null], default: null }
})

const formattedJson = computed(() => {
  if (props.data === null || props.data === undefined) {
    return '<span class="json-null">null</span>'
  }

  try {
    const json = typeof props.data === 'string' ? JSON.parse(props.data) : props.data
    return syntaxHighlight(JSON.stringify(json, null, 2))
  } catch (e) {
    return String(props.data)
  }
})

function syntaxHighlight(json) {
  return json
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(
      /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
      (match) => {
        let cls = 'json-number'
        if (/^"/.test(match)) {
          if (/:$/.test(match)) {
            cls = 'json-key'
            match = match.replace(/"/g, '')
          } else {
            cls = 'json-string'
          }
        } else if (/true|false/.test(match)) {
          cls = 'json-boolean'
        } else if (/null/.test(match)) {
          cls = 'json-null'
        }
        return `<span class="${cls}">${match}</span>`
      }
    )
}
</script>