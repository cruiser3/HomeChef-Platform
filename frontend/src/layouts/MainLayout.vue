<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import TabBar from '../components/TabBar.vue'
import { useAuthStore } from '../store/auth'
import { useFamilyStore } from '../store/family'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const familyStore = useFamilyStore()

const showFamilyPopup = ref(false)

// 广场列表从详情返回时保持滚动位置与筛选状态，仅缓存列表页。
const cachedViews = ['SystemDishListView']

const sectionLabel = computed(() => route.meta.label ?? '家庭私厨')
const currentFamilyName = computed(() => familyStore.currentFamily?.name || '选择家庭')

function roleLabel(role) {
  return role === 'ADMIN' ? '管理员' : '成员'
}

function onSwitchFamily(family) {
  familyStore.selectFamily(family.id)
  showFamilyPopup.value = false
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
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-inner">
        <RouterLink class="brand" :to="{ name: 'dishes' }" aria-label="家庭私厨首页">
          <span class="brand-mark" aria-hidden="true">家</span>
          <span class="brand-copy">
            <strong>家庭私厨</strong>
            <span>一起安排每一餐</span>
          </span>
        </RouterLink>

        <TabBar mode="desktop" />

        <div class="header-actions">
          <button
            class="family-switcher"
            type="button"
            :aria-label="`当前家庭：${currentFamilyName}`"
            @click="showFamilyPopup = true"
          >
            <span class="family-switcher-label">{{ currentFamilyName }}</span>
            <span class="family-switcher-caret" aria-hidden="true">⌄</span>
          </button>
          <button class="logout-btn" type="button" @click="onLogout">退出</button>
        </div>
      </div>
    </header>

    <main class="main-content" :aria-label="sectionLabel">
      <KeepAlive :include="cachedViews">
        <RouterView />
      </KeepAlive>
    </main>

    <TabBar mode="mobile" />

    <van-popup v-model:show="showFamilyPopup" round position="bottom">
      <div class="family-sheet">
        <h2>切换家庭</h2>
        <van-cell-group class="family-list">
          <van-cell
            v-for="family in familyStore.families"
            :key="family.id"
            class="family-cell"
            :title="family.name"
            :label="`${roleLabel(family.currentUserRole)} · ${family.memberCount} 位成员`"
            :class="{ 'is-active': family.id === familyStore.currentFamilyId }"
            @click="onSwitchFamily(family)"
          />
        </van-cell-group>
        <van-button block type="primary" @click="router.push({ name: 'family-select' })">
          创建 / 管理家庭
        </van-button>
      </div>
    </van-popup>
  </div>
</template>
