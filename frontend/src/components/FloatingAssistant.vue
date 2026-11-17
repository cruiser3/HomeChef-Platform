<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAssistantStore } from '../store/assistant'
import ChatMessageList from './ChatMessageList.vue'
import ChatInputBar from './ChatInputBar.vue'
import AssistantSessionList from './AssistantSessionList.vue'

const store = useAssistantStore()
const router = useRouter()
const BALL_SIZE = 52
const ball = ref({ x: 0, y: 0 })
let startX = 0, startY = 0, originX = 0, originY = 0, dragging = false

function initPosition() {
  ball.value.x = window.innerWidth - BALL_SIZE - 16
  ball.value.y = window.innerHeight - BALL_SIZE - 140
}
const clamp = (v, min, max) => Math.min(Math.max(v, min), max)

function start(cx, cy) {
  dragging = false
  startX = cx; startY = cy
  originX = ball.value.x; originY = ball.value.y
}
function move(cx, cy) {
  const dx = cx - startX, dy = cy - startY
  if (!dragging && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) dragging = true
  if (!dragging) return
  ball.value.x = clamp(originX + dx, 0, window.innerWidth - BALL_SIZE)
  ball.value.y = clamp(originY + dy, 0, window.innerHeight - BALL_SIZE)
}
function end() {
  if (!dragging) return
  const center = ball.value.x + BALL_SIZE / 2
  ball.value.x = center < window.innerWidth / 2 ? 0 : window.innerWidth - BALL_SIZE
}
function onTouchStart(e) { start(e.touches[0].clientX, e.touches[0].clientY) }
function onTouchMove(e) { move(e.touches[0].clientX, e.touches[0].clientY) }
function onTouchEnd() { end() }
function onMouseDown(e) {
  e.preventDefault()
  start(e.clientX, e.clientY)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
}
function onMouseMove(e) { move(e.clientX, e.clientY) }
function onMouseUp() {
  end()
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
}
function onBallClick() {
  if (!dragging) store.openPanel()
}

const historyOpen = ref(false)
const currentTitle = computed(() => {
  const s = store.sessions.find((x) => x.id === store.currentSessionId)
  return s ? s.title : '小铲'
})

function send(text) { store.send(text) }
function onNewSession() {
  store.newSession()
  historyOpen.value = false
}
async function onSwitchSession(id) {
  await store.switchSession(id)
  historyOpen.value = false
}
function onDeleteSession(id) { store.removeSession(id) }
function openFull() {
  store.closePanel()
  router.push('/assistant')
}

initPosition()
</script>

<template>
  <div
    v-show="!store.open"
    class="assistant-ball"
    :style="{ transform: `translate(${ball.x}px, ${ball.y}px)` }"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
    @mousedown="onMouseDown"
    @click="onBallClick"
  >
    <span class="ball-icon">🍳</span>
  </div>

  <van-popup v-model:show="store.open" position="bottom" round>
    <div class="assistant-panel">
      <div class="panel-header">
        <van-icon name="wap-nav" class="header-nav" @click="historyOpen = true" />
        <span class="header-title">{{ currentTitle }}</span>
        <div class="header-actions">
          <van-icon name="enlarge" @click="openFull" />
          <van-icon name="add-o" @click="onNewSession" />
          <van-icon name="cross" @click="store.closePanel()" />
        </div>
      </div>

      <ChatMessageList :messages="store.messages" :streaming="store.streaming" />

      <ChatInputBar :streaming="store.streaming" @send="send" />
    </div>
  </van-popup>

  <van-popup v-model:show="historyOpen" position="left" :style="{ width: '78%', height: '100%' }">
    <div class="history-panel">
      <div class="history-header">历史会话</div>
      <AssistantSessionList
        :sessions="store.sessions"
        :current-session-id="store.currentSessionId"
        @select="onSwitchSession"
        @delete="onDeleteSession"
      />
    </div>
  </van-popup>
</template>

<style scoped>
.assistant-ball {
  position: fixed; left: 0; top: 0; width: 52px; height: 52px;
  border-radius: 50%; background: linear-gradient(135deg, #6b9e7c, #558767);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 14px rgba(85, 135, 103, 0.4);
  z-index: 1500; cursor: pointer; user-select: none; touch-action: none;
}
.ball-icon { font-size: 26px; line-height: 1; }
.assistant-panel { display: flex; flex-direction: column; height: 70vh; }
.panel-header {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 16px; border-bottom: 1px solid var(--border);
  background: var(--card);
}
.header-title {
  flex: 1; font-size: 16px; font-weight: 650;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.header-actions { display: flex; align-items: center; gap: 16px; }
.history-panel { height: 100%; display: flex; flex-direction: column; }
.history-header { padding: 14px 16px; font-size: 16px; font-weight: 650; border-bottom: 1px solid var(--border); }
</style>
