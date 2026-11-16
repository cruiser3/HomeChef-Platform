<script setup>
import { computed, ref } from 'vue'
import { showToast } from 'vant'
import { useCartStore } from '../store/cart'

const props = defineProps({
  dish: {
    type: Object,
    required: true,
  },
})

const cartStore = useCartStore()
const adding = ref(false)

const hasRating = computed(() => props.dish.averageRating != null && props.dish.reviewCount > 0)

function formatPrice(price) {
  return Number(price ?? 0).toFixed(2)
}

async function onAdd() {
  if (adding.value || !props.dish.available) return
  adding.value = true
  try {
    await cartStore.addItem(props.dish.id, 1)
    showToast('已加入购物车')
  } catch {
    // 拦截器已提示
  } finally {
    adding.value = false
  }
}
</script>

<template>
  <RouterLink
    class="dish-card"
    :class="{ 'is-unavailable': !dish.available }"
    :to="{ name: 'dish-detail', params: { id: dish.id } }"
  >
    <div class="dish-card-media">
      <van-image :src="dish.imageUrl || ''" fit="cover" lazy-load>
        <template #loading>
          <span class="dish-card-fallback">{{ dish.name }}</span>
        </template>
        <template #error>
          <span class="dish-card-fallback">{{ dish.name }}</span>
        </template>
      </van-image>
      <span v-if="!dish.available" class="dish-card-badge">已下架</span>
      <button
        class="dish-card-add"
        type="button"
        :disabled="!dish.available || adding"
        aria-label="加入购物车"
        @click.stop.prevent="onAdd"
      >
        <van-icon name="plus" />
      </button>
    </div>
    <div class="dish-card-body">
      <h3 class="dish-card-title">{{ dish.name }}</h3>
      <p class="dish-card-meta">
        <span v-if="dish.categoryName">{{ dish.categoryName }}</span>
        <span v-if="dish.cookingMinutes != null">{{ dish.cookingMinutes }} 分钟</span>
        <span v-if="hasRating" class="dish-card-rating">
          <van-rate :model-value="dish.averageRating" readonly :size="12" allow-half />
        </span>
      </p>
      <div class="dish-card-foot">
        <span class="dish-card-price">¥{{ formatPrice(dish.price) }}</span>
        <span v-if="dish.createdByName" class="dish-card-creator">{{ dish.createdByName }}</span>
      </div>
    </div>
  </RouterLink>
</template>
