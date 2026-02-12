<template>
  <div class="font-mono text-sm">
    <!-- Object -->
    <template v-if="isObject">
      <div
        class="flex items-center gap-1 cursor-pointer select-none hover:bg-gray-100 rounded px-1 -mx-1"
        @click="expanded = !expanded"
      >
        <svg
          class="h-3.5 w-3.5 text-gray-400 transition-transform"
          :class="{ 'rotate-90': expanded }"
          fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
        </svg>
        <span class="text-gray-500">{{ isArray ? '[' : '{' }}</span>
        <span v-if="!expanded" class="text-gray-400 text-xs ml-1">
          {{ isArray ? `${Object.keys(value).length} items` : `${Object.keys(value).length} keys` }}
          {{ isArray ? ']' : '}' }}
        </span>
      </div>
      <div v-if="expanded" class="ml-4 border-l border-gray-200 pl-3 space-y-0.5">
        <div v-for="(val, key) in value" :key="key" class="flex items-start gap-1">
          <span class="text-purple-600 shrink-0">{{ isArray ? `[${key}]` : key }}:</span>
          <PayloadNode
            :value="val"
            :path="childPath(key)"
            :edit-mode="editMode"
            @update="(p, v) => $emit('update', p, v)"
          />
        </div>
      </div>
      <div v-if="expanded" class="text-gray-500">{{ isArray ? ']' : '}' }}</div>
    </template>

    <!-- Primitive with inline edit -->
    <template v-else>
      <span v-if="!editing" :class="valueClass" class="group inline-flex items-center gap-1">
        {{ displayValue }}
        <button
          v-if="editMode"
          class="hidden group-hover:inline-flex h-4 w-4 items-center justify-center rounded text-gray-400 hover:text-primary-600 hover:bg-primary-50"
          @click.stop="startEdit"
        >
          <svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.832 19.82a4.5 4.5 0 01-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 011.13-1.897L16.863 4.487z" />
          </svg>
        </button>
      </span>
      <span v-else class="inline-flex items-center gap-1">
        <input
          ref="editInput"
          v-model="editValue"
          class="input-sm max-w-[200px]"
          @keydown.enter="commitEdit"
          @keydown.escape="editing = false"
        />
        <button class="btn-primary btn-sm !px-1.5 !py-0.5 text-xs" @click="commitEdit">OK</button>
        <button class="btn-secondary btn-sm !px-1.5 !py-0.5 text-xs" @click="editing = false">X</button>
      </span>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  value: { type: [Object, Array, String, Number, Boolean, null], default: null },
  path: { type: String, default: '' },
  editMode: { type: Boolean, default: false }
})

const emit = defineEmits(['update'])

const expanded = ref(true)
const editing = ref(false)
const editValue = ref('')
const editInput = ref(null)

const isObject = computed(() => props.value !== null && typeof props.value === 'object')
const isArray = computed(() => Array.isArray(props.value))

const valueClass = computed(() => {
  if (props.value === null || props.value === undefined) return 'text-gray-400 italic'
  if (typeof props.value === 'string') return 'text-emerald-600'
  if (typeof props.value === 'number') return 'text-amber-600'
  if (typeof props.value === 'boolean') return 'text-sky-600'
  return 'text-gray-700'
})

const displayValue = computed(() => {
  if (props.value === null || props.value === undefined) return 'null'
  if (typeof props.value === 'string') return `"${props.value}"`
  return String(props.value)
})

function childPath(key) {
  return props.path ? `${props.path}.${key}` : String(key)
}

function startEdit() {
  editValue.value = props.value === null ? '' : String(props.value)
  editing.value = true
  nextTick(() => editInput.value?.focus())
}

function commitEdit() {
  let parsed = editValue.value
  if (typeof props.value === 'number') {
    const num = Number(parsed)
    if (!isNaN(num)) parsed = num
  } else if (typeof props.value === 'boolean') {
    parsed = parsed === 'true'
  }
  emit('update', props.path, parsed)
  editing.value = false
}
</script>
