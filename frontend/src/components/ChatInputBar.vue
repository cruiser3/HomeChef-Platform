<script setup>
import { ref } from 'vue'

const props = defineProps({
  streaming: { type: Boolean, default: false },
  placeholder: { type: String, default: '问问小铲吃什么…' },
})
const emit = defineEmits(['send'])

const input = ref('')

function submit() {
  const text = input.value.trim()
  if (!text || props.streaming) return
  input.value = ''
  emit('send', text)
}
</script>

<template>
  <div class="input-bar">
    <van-field
      v-model="input"
      :placeholder="placeholder"
      :disabled="streaming"
      @keyup.enter="submit"
    />
    <van-button
      type="primary"
      :loading="streaming"
      :disabled="!input.trim() || streaming"
      @click="submit"
    >发送</van-button>
  </div>
</template>

<style scoped>
.input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--border);
  background: var(--card);
}
.input-bar :deep(.van-field) {
  flex: 1;
  padding: 6px 16px;
  border-radius: 22px;
  background: var(--surface-soft);
}
.input-bar :deep(.van-button) {
  border-radius: 22px;
}
</style>
