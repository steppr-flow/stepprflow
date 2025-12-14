<template>
  <div class="payload-editor">
    <!-- Tree view of payload with editable values -->
    <div class="space-y-1">
      <PayloadNode
        v-for="key in orderedKeys"
        :key="key"
        :fieldKey="key"
        :value="payload[key]"
        :path="key"
        :editable="editable"
        :depth="0"
        @update="handleUpdate"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PayloadNode from './PayloadNode.vue'

const props = defineProps({
  payload: { type: Object, required: true },
  editable: { type: Boolean, default: false }
})

const emit = defineEmits(['update'])

// Use Object.keys() to preserve the order from the backend
const orderedKeys = computed(() => Object.keys(props.payload))

function handleUpdate(path, newValue, oldValue) {
  emit('update', path, newValue, oldValue)
}
</script>

<style scoped>
.payload-editor {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.875rem;
}
</style>