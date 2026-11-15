<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  tone: {
    type: String,
    default: 'neutral',
    validator: (value) => ['neutral', 'primary', 'warm'].includes(value),
  },
})
</script>

<template>
  <span class="status-tag" :class="`status-tag--${tone}`">{{ label }}</span>
</template>
