<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TabBar from '../components/TabBar.vue'

const route = useRoute()
const sectionLabel = computed(() => route.meta.label ?? '家庭私厨')
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
      </div>
    </header>

    <main class="main-content" :aria-label="sectionLabel">
      <RouterView />
    </main>

    <TabBar mode="mobile" />
  </div>
</template>
