<script setup>
import { computed } from 'vue'

const props = defineProps({
  dish: {
    type: Object,
    required: true,
  },
})

const hasRating = computed(() => props.dish.averageRating != null && props.dish.reviewCount > 0)
</script>

<template>
  <RouterLink
    class="system-dish-card"
    :to="{ name: 'system-dish-detail', params: { id: dish.id } }"
  >
    <div class="system-dish-card-media">
      <van-image :src="dish.coverImageUrl || ''" fit="cover" lazy-load>
        <template #loading>
          <span class="system-dish-card-fallback">{{ dish.name }}</span>
        </template>
        <template #error>
          <span class="system-dish-card-fallback">{{ dish.name }}</span>
        </template>
      </van-image>
    </div>
    <div class="system-dish-card-body">
      <h3 class="system-dish-card-title">{{ dish.name }}</h3>
      <p v-if="dish.cuisineName || dish.categoryName || dish.cookingMinutes != null || hasRating" class="system-dish-card-meta">
        <span v-if="dish.cuisineName" class="system-dish-card-cuisine">{{ dish.cuisineName }}</span>
        <span v-if="dish.categoryName">{{ dish.categoryName }}</span>
        <span v-if="dish.cookingMinutes != null">{{ dish.cookingMinutes }} 分钟</span>
        <span v-if="hasRating" class="system-dish-card-rating">
          <van-rate :model-value="dish.averageRating" readonly :size="12" allow-half />
        </span>
      </p>
      <p
        v-if="dish.sourceType === 'FAMILY' && (dish.familyName || dish.publisherName)"
        class="system-dish-card-source"
      >
        来自 {{ dish.familyName || '未知家庭' }} · {{ dish.publisherName || '匿名发布' }}
      </p>
      <div v-if="dish.calories != null" class="system-dish-card-foot">
        <span class="system-dish-card-tag">{{ dish.calories }} 千卡</span>
      </div>
    </div>
  </RouterLink>
</template>
