<script setup>
defineProps({
  dish: {
    type: Object,
    required: true,
  },
})
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
      <p v-if="dish.categoryName || dish.cookingMinutes != null" class="system-dish-card-meta">
        <span v-if="dish.categoryName">{{ dish.categoryName }}</span>
        <span v-if="dish.cookingMinutes != null">{{ dish.cookingMinutes }} 分钟</span>
      </p>
      <div v-if="dish.calories != null" class="system-dish-card-foot">
        <span class="system-dish-card-tag">{{ dish.calories }} 千卡</span>
      </div>
    </div>
  </RouterLink>
</template>
