<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  id: { type: String, required: true },
  label: { type: String, required: true },
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  error: { type: String, default: '' },
  autocomplete: { type: String, default: 'off' },
  placeholder: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
})

defineEmits(['update:modelValue'])

const revealed = ref(false)
const inputType = computed(() => (props.type === 'password' && revealed.value ? 'text' : props.type))
</script>

<template>
  <label class="field" :for="id">
    <span>{{ label }}</span>
    <span class="control" :class="{ password: type === 'password' }">
      <input
        :id="id"
        :type="inputType"
        :value="modelValue"
        :autocomplete="autocomplete"
        :placeholder="placeholder"
        :readonly="readonly"
        :aria-readonly="readonly ? 'true' : undefined"
        :aria-invalid="error ? 'true' : 'false'"
        :aria-describedby="error ? `${id}-error` : undefined"
        @input="$emit('update:modelValue', $event.target.value)"
      />
      <button
        v-if="type === 'password'"
        class="reveal"
        type="button"
        :aria-pressed="revealed"
        @click="revealed = !revealed"
      >
        {{ revealed ? 'Gizle' : 'Göster' }}
      </button>
    </span>
    <small v-if="error" :id="`${id}-error`">{{ error }}</small>
  </label>
</template>
