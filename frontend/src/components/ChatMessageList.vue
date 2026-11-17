<script setup>
import { ref, watch, nextTick, computed, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  streaming: { type: Boolean, default: false },
})

const auth = useAuthStore()
const listRef = ref(null)

const userAvatar = computed(() => auth.user?.avatarUrl || '')
const userInitial = computed(() => (auth.displayName || '').trim().charAt(0) || '我')

onMounted(() => auth.fetchProfile())

// 新增消息与流式逐字输出都会触发滚动到底。
watch(
  () => props.messages.map((m) => m.content).join(''),
  () => nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  }),
)
</script>

<template>
  <div ref="listRef" class="chat-messages">
    <van-empty
      v-if="!messages.length"
      description="你好，我是小铲，聊聊今天吃什么吧"
      image-size="80"
    />
    <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
      <div v-if="msg.role === 'assistant'" class="avatar">🍳</div>
      <div class="bubble">
        <template v-if="msg.content">{{ msg.content }}</template>
        <span v-else-if="streaming && i === messages.length - 1" class="thinking">正在思考…</span>
        <span v-if="streaming && i === messages.length - 1 && msg.content" class="cursor" />
      </div>
      <div v-if="msg.role === 'user'" class="avatar user-avatar">
        <img v-if="userAvatar" :src="userAvatar" class="avatar-img" alt="用户头像" />
        <span v-else>{{ userInitial }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg);
}
.msg {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
.msg.user {
  justify-content: flex-end;
}
.avatar {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--surface-soft);
  font-size: 16px;
  line-height: 1;
  overflow: hidden;
}
.user-avatar {
  background: linear-gradient(135deg, #6b9e7c, #558767);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}
.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg.user .bubble {
  background: var(--primary-dark);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg.assistant .bubble {
  background: var(--card);
  color: var(--text);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-soft);
}
.thinking {
  color: var(--text-muted);
}
.cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--primary);
  animation: blink 1s step-end infinite;
}
@keyframes blink {
  50% { opacity: 0; }
}
</style>
