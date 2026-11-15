<script setup>
import { useRoute } from 'vue-router'
import BaseCard from '../components/BaseCard.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'

const route = useRoute()
</script>

<template>
  <section class="module-page">
    <header class="page-heading">
      <p class="eyebrow">HOME CHEF · {{ route.meta.label }}</p>
      <h1>{{ route.meta.label }}</h1>
      <p class="page-description">{{ route.meta.description }}</p>
    </header>

    <BaseCard class="module-card">
      <div class="module-card-heading">
        <div>
          <p class="section-kicker">当前阶段</p>
          <h2>基础框架已就绪</h2>
        </div>
        <StatusTag label="后续建设" tone="warm" />
      </div>
      <EmptyState
        title="这里将成为家庭协作空间"
        description="此页面暂不连接业务接口，相关功能会在后续阶段按计划实现。"
      />
    </BaseCard>
  </section>
</template>
