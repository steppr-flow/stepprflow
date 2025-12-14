<template>
  <div class="payload-node">
    <!-- Key and value row -->
    <div
      class="flex items-start py-1 hover:bg-gray-50 rounded px-2 -mx-2"
      :style="{ paddingLeft: `${depth * 16 + 8}px` }"
    >
      <!-- Expand/collapse for objects and arrays -->
      <button
        v-if="isExpandable"
        @click="expanded = !expanded"
        class="mr-1 text-gray-400 hover:text-gray-600 focus:outline-none flex-shrink-0 w-4"
      >
        <svg v-if="expanded" class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
        </svg>
        <svg v-else class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
        </svg>
      </button>
      <span v-else class="w-4 mr-1 flex-shrink-0"></span>

      <!-- Key -->
      <span class="text-primary-700 font-medium flex-shrink-0">{{ fieldKey }}</span>
      <span class="text-gray-400 mx-1">:</span>

      <!-- Value -->
      <div class="flex-1 min-w-0">
        <!-- Editable primitive value -->
        <template v-if="!isExpandable">
          <div v-if="editable && isEditing" class="flex items-center space-x-2">
            <input
              ref="inputRef"
              v-model="editValue"
              @keyup.enter="saveEdit"
              @keyup.escape="cancelEdit"
              @blur="saveEdit"
              class="flex-1 px-2 py-0.5 text-sm border border-primary-300 rounded focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              :class="inputClass"
            />
          </div>
          <div v-else class="flex items-center group">
            <span :class="valueClass">{{ displayValue }}</span>
            <button
              v-if="editable"
              @click="startEdit"
              class="ml-2 opacity-0 group-hover:opacity-100 text-gray-400 hover:text-primary-600 transition-opacity"
              title="Edit value"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
              </svg>
            </button>
          </div>
        </template>

        <!-- Object/Array indicator -->
        <template v-else>
          <span class="text-gray-500">
            {{ isArray ? `Array(${value.length})` : `Object(${Object.keys(value).length})` }}
          </span>
        </template>
      </div>
    </div>

    <!-- Children -->
    <div v-if="isExpandable && expanded">
      <template v-if="isArray">
        <PayloadNode
          v-for="(item, index) in value"
          :key="index"
          :fieldKey="`[${index}]`"
          :value="item"
          :path="`${path}[${index}]`"
          :editable="editable"
          :depth="depth + 1"
          @update="(p, v, o) => $emit('update', p, v, o)"
        />
      </template>
      <template v-else>
        <PayloadNode
          v-for="childKey in orderedChildKeys"
          :key="childKey"
          :fieldKey="childKey"
          :value="value[childKey]"
          :path="`${path}.${childKey}`"
          :editable="editable"
          :depth="depth + 1"
          @update="(p, v, o) => $emit('update', p, v, o)"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  fieldKey: { type: [String, Number], required: true },
  value: { type: [Object, Array, String, Number, Boolean, null], default: null },
  path: { type: String, required: true },
  editable: { type: Boolean, default: false },
  depth: { type: Number, default: 0 }
})

const emit = defineEmits(['update'])

const expanded = ref(true)
const isEditing = ref(false)
const editValue = ref('')
const inputRef = ref(null)

const isArray = computed(() => Array.isArray(props.value))
const isObject = computed(() => props.value !== null && typeof props.value === 'object' && !isArray.value)
const isExpandable = computed(() => isArray.value || isObject.value)

// Use Object.keys() to preserve key order for nested objects
const orderedChildKeys = computed(() => isObject.value ? Object.keys(props.value) : [])

const valueType = computed(() => {
  if (props.value === null) return 'null'
  if (typeof props.value === 'boolean') return 'boolean'
  if (typeof props.value === 'number') return 'number'
  if (typeof props.value === 'string') return 'string'
  return 'object'
})

const displayValue = computed(() => {
  if (props.value === null) return 'null'
  if (typeof props.value === 'string') return `"${props.value}"`
  return String(props.value)
})

const valueClass = computed(() => {
  const classes = {
    null: 'text-gray-400 italic',
    boolean: 'text-purple-600',
    number: 'text-blue-600',
    string: 'text-green-700'
  }
  return classes[valueType.value] || 'text-gray-700'
})

const inputClass = computed(() => {
  const classes = {
    boolean: 'text-purple-600',
    number: 'text-blue-600',
    string: 'text-green-700'
  }
  return classes[valueType.value] || 'text-gray-700'
})

function startEdit() {
  // Remove quotes for string display in input
  if (typeof props.value === 'string') {
    editValue.value = props.value
  } else {
    editValue.value = props.value === null ? '' : String(props.value)
  }
  isEditing.value = true
  nextTick(() => {
    inputRef.value?.focus()
    inputRef.value?.select()
  })
}

function saveEdit() {
  if (!isEditing.value) return

  let newValue = editValue.value
  const oldValue = props.value

  // Try to preserve the original type
  if (typeof props.value === 'number') {
    const num = Number(newValue)
    if (!isNaN(num)) {
      newValue = num
    }
  } else if (typeof props.value === 'boolean') {
    newValue = newValue.toLowerCase() === 'true'
  } else if (props.value === null && newValue === '') {
    newValue = null
  }

  // Only emit if value changed
  if (newValue !== props.value) {
    emit('update', props.path, newValue, oldValue)
  }

  isEditing.value = false
}

function cancelEdit() {
  isEditing.value = false
}
</script>