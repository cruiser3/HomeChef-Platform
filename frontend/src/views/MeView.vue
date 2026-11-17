<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import BaseCard from '../components/BaseCard.vue'
import { getMyProfile } from '../api/user'
import { useAuthStore } from '../store/auth'
import { useFamilyStore } from '../store/family'

const router = useRouter()
const authStore = useAuthStore()
const familyStore = useFamilyStore()

const profile = ref(null)

const displayName = computed(() => profile.value?.name || authStore.displayName || '家庭私厨用户')
const avatarUrl = computed(() => profile.value?.avatarUrl || '')
const email = computed(() => profile.value?.email || authStore.user?.email || '')
const avatarInitial = computed(() => (displayName.value || '').trim().charAt(0) || '我')

const family = computed(() => familyStore.currentFamily)
const roleLabel = { ADMIN: '管理员', MEMBER: '成员' }

async function loadProfile() {
  try {
    profile.value = await getMyProfile()
  } catch {
    profile.value = null
  }
}

async function onLogout() {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确定要退出当前账号吗？' })
  } catch {
    return
  }
  authStore.clearSession()
  familyStore.clearFamily()
  router.replace({ name: 'login' })
}

if (!familyStore.loaded) {
  familyStore.loadFamilies().catch(() => {})
}
loadProfile()
</script>

<template>
  <section class="me-page">
    <header class="me-head">
      <p class="eyebrow">HOME CHEF · 我的</p>
      <h1>我的</h1>
      <p class="page-description">管理个人资料与家庭空间。</p>
    </header>

    <BaseCard class="me-profile-card">
      <div class="me-profile">
        <div class="me-avatar">
          <van-image v-if="avatarUrl" :src="avatarUrl" fit="cover" round />
          <span v-else class="me-avatar-fallback">{{ avatarInitial }}</span>
        </div>
        <div class="me-profile-info">
          <p class="me-name">{{ displayName }}</p>
          <p class="me-email">{{ email || '未填写邮箱' }}</p>
        </div>
      </div>
      <div v-if="family" class="me-family-row">
        <span class="me-family-label">当前家庭</span>
        <strong class="me-family-name">{{ family.name }}</strong>
        <span class="me-family-role">{{ roleLabel[family.currentUserRole] || '成员' }}</span>
      </div>
    </BaseCard>

    <BaseCard class="me-menu-card">
      <button class="me-menu-item" type="button" @click="router.push({ name: 'profile-account' })">
        <van-icon name="user-o" class="me-menu-icon" aria-hidden="true" />
        <span class="me-menu-body">
          <span class="me-menu-title">个人资料</span>
          <span class="me-menu-sub">头像、昵称、忌口、过敏、饮食偏好、健康情况</span>
        </span>
        <van-icon name="arrow" class="me-menu-arrow" aria-hidden="true" />
      </button>
      <button class="me-menu-item" type="button" @click="router.push({ name: 'family' })">
        <van-icon name="friends-o" class="me-menu-icon" aria-hidden="true" />
        <span class="me-menu-body">
          <span class="me-menu-title">我的家庭</span>
          <span class="me-menu-sub">家庭名称、成员列表、管理员、邀请码</span>
        </span>
        <van-icon name="arrow" class="me-menu-arrow" aria-hidden="true" />
      </button>
    </BaseCard>

    <div class="me-actions">
      <van-button block plain type="danger" @click="onLogout">退出登录</van-button>
    </div>
  </section>
</template>

<style scoped>
.me-page {
  display: grid;
  gap: 20px;
}

.me-head h1 {
  margin: 4px 0 0;
  font-size: clamp(24px, 4vw, 32px);
}

.me-profile-card {
  padding: clamp(18px, 3vw, 24px);
}

.me-profile {
  display: flex;
  align-items: center;
  gap: 16px;
}

.me-avatar {
  display: grid;
  width: 72px;
  height: 72px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: var(--surface-soft);
}

.me-avatar .van-image {
  width: 100%;
  height: 100%;
}

.me-avatar-fallback {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-dark);
}

.me-profile-info {
  min-width: 0;
}

.me-name {
  margin: 0;
  font-size: 20px;
  font-weight: 650;
  color: var(--text);
}

.me-email {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.me-family-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--surface-soft);
}

.me-family-label {
  color: var(--text-muted);
  font-size: 13px;
}

.me-family-name {
  color: var(--text);
  font-size: 14px;
}

.me-family-role {
  margin-left: auto;
  padding: 3px 10px;
  border-radius: 999px;
  background: #e8f0e8;
  color: var(--primary-dark);
  font-size: 12px;
  font-weight: 600;
}

.me-menu-card {
  padding: 8px 0;
}

.me-menu-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 14px;
  padding: 15px 20px;
  border: none;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 140ms ease;
}

.me-menu-item:hover {
  background: var(--surface-soft);
}

.me-menu-item + .me-menu-item {
  border-top: 1px solid var(--border);
}

.me-menu-icon {
  flex: 0 0 auto;
  color: var(--primary-dark);
  font-size: 22px;
}

.me-menu-body {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 3px;
}

.me-menu-title {
  font-size: 16px;
  font-weight: 550;
  color: var(--text);
}

.me-menu-sub {
  color: var(--text-muted);
  font-size: 12px;
}

.me-menu-arrow {
  flex: 0 0 auto;
  color: var(--text-muted);
  font-size: 16px;
}

.me-actions {
  margin-top: 4px;
}
</style>
