<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAssistantStore } from '../store/assistant'
import ChatMessageList from '../components/ChatMessageList.vue'
import ChatInputBar from '../components/ChatInputBar.vue'
import AssistantSessionList from '../components/AssistantSessionList.vue'
import AssistantSettings from '../components/AssistantSettings.vue'

const store = useAssistantStore()
const router = useRouter()
const sidebarOpen = ref(false)
const showSettings = ref(false)

const currentTitle = computed(() => {
  const s = store.sessions.find((x) => x.id === store.currentSessionId)
  return s ? s.title : '小铲'
})

onMounted(() => store.loadSessions().catch(() => {}))

function send(text) { store.send(text) }
function onNewSession() {
  store.newSession()
  sidebarOpen.value = false
}
async function onSwitchSession(id) {
  await store.switchSession(id)
  sidebarOpen.value = false
}
function onDeleteSession(id) { store.removeSession(id) }
function openSettings() {
  sidebarOpen.value = false
  showSettings.value = true
}
function closeSettings() {
  showSettings.value = false
}
function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>

<template>
  <div class="assistant-view">
    <aside class="sidebar">
      <div class="sidebar-header">
        <span class="sidebar-title">🍳 小铲</span>
        <van-icon name="add-o" @click="onNewSession" />
      </div>
      <AssistantSessionList
        :sessions="store.sessions"
        :current-session-id="store.currentSessionId"
        :show-settings="true"
        @select="onSwitchSession"
        @delete="onDeleteSession"
        @settings="openSettings"
      />
    </aside>

    <main class="chat-main">
      <template v-if="!showSettings">
        <header class="chat-header">
          <van-icon name="arrow-left" class="header-back" @click="goBack" />
          <span class="header-title">{{ currentTitle }}</span>
          <div class="header-actions mobile-only">
            <van-icon name="wap-nav" @click="sidebarOpen = true" />
            <van-icon name="add-o" @click="onNewSession" />
          </div>
        </header>

        <ChatMessageList :messages="store.messages" :streaming="store.streaming" />

        <ChatInputBar :streaming="store.streaming" @send="send" />
      </template>
      <AssistantSettings v-else @back="closeSettings" />
    </main>

    <van-popup v-model:show="sidebarOpen" position="left" :style="{ width: '78%', height: '100%' }">
      <div class="drawer">
        <div class="drawer-header">历史会话</div>
        <AssistantSessionList
          :sessions="store.sessions"
          :current-session-id="store.currentSessionId"
          :show-settings="true"
          @select="onSwitchSession"
          @delete="onDeleteSession"
          @settings="openSettings"
        />
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.assistant-view {
  display: flex;
  height: 100vh;
  height: 100dvh;
  background: var(--bg);
}
.sidebar {
  width: 280px;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
  background: var(--card);
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border);
}
.sidebar-title {
  font-size: 16px;
  font-weight: 650;
}
.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
}
.header-back {
  font-size: 20px;
  cursor: pointer;
}
.header-title {
  flex: 1;
  font-size: 16px;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.mobile-only {
  display: none;
}
.drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.drawer-header {
  padding: 14px 16px;
  font-size: 16px;
  font-weight: 650;
  border-bottom: 1px solid var(--border);
}
@media (max-width: 767px) {
  .sidebar { display: none; }
  .mobile-only { display: flex; }
}
</style>
