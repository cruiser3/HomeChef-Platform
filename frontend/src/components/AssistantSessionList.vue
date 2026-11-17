<script setup>
import { computed, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'

defineProps({
  sessions: { type: Array, default: () => [] },
  currentSessionId: { type: [String, Number], default: null },
  showSettings: { type: Boolean, default: false },
})
const emit = defineEmits(['select', 'delete', 'settings'])

const auth = useAuthStore()
const avatarUrl = computed(() => auth.user?.avatarUrl || '')
const initial = computed(() => (auth.displayName || '').trim().charAt(0) || '我')

onMounted(() => auth.fetchProfile())

function onDelete(id) {
  if (window.confirm('确定删除该会话及其全部消息吗？')) {
    emit('delete', id)
  }
}
</script>

<template>
  <div class="session-list-wrap">
    <div class="session-list">
      <van-empty v-if="!sessions.length" description="暂无历史会话" image-size="60" />
      <div
        v-for="s in sessions"
        :key="s.id"
        class="session-item"
        :class="{ active: s.id === currentSessionId }"
        @click="emit('select', s.id)"
      >
        <span class="session-title">{{ s.title }}</span>
        <van-icon name="delete-o" @click.stop="onDelete(s.id)" />
      </div>
    </div>

    <div v-if="showSettings" class="settings-bar" @click="emit('settings')">
      <div class="avatar">
        <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" alt="用户头像" />
        <span v-else>{{ initial }}</span>
      </div>
      <span class="user-id">{{ auth.displayName }}</span>
      <van-icon name="setting-o" class="settings-btn" />
    </div>
  </div>
</template>

<style scoped>
.session-list-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.session-list {
  flex: 1;
  overflow-y: auto;
}
.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background-color 140ms ease;
}
.session-item:hover {
  background: var(--surface-soft);
}
.session-item.active {
  background: var(--surface-soft);
  color: var(--primary-dark);
}
.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
.session-item.active .session-title {
  font-weight: 600;
}
.settings-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  cursor: pointer;
  transition: background-color 140ms ease;
}
.settings-bar:hover {
  background: var(--surface-soft);
}
.settings-bar .avatar {
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #6b9e7c, #558767);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.user-id {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--text);
}
.settings-btn {
  font-size: 20px;
  color: var(--text-muted);
}
</style>
