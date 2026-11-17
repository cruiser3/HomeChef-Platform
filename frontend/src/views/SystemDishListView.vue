<script setup>
import { ref } from 'vue'
import SystemDishCard from '../components/SystemDishCard.vue'
import { listSystemDishCategories, listSystemDishes } from '../api/systemDish'

const dishes = ref([])
const categories = ref([])
const keyword = ref('')
const activeCategoryId = ref(null)
const loading = ref(false)
const loadError = ref(false)

async function loadCategories() {
  try {
    categories.value = await listSystemDishCategories()
  } catch {
    // 拦截器已提示
  }
}

async function loadDishes() {
  loading.value = true
  loadError.value = false
  const params = {}
  if (activeCategoryId.value != null) params.categoryId = activeCategoryId.value
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  try {
    dishes.value = await listSystemDishes(params)
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function onSearch() {
  loadDishes()
}

function onSelectCategory(categoryId) {
  activeCategoryId.value = categoryId
  loadDishes()
}

loadCategories()
loadDishes()
</script>

<template>
  <section class="plaza-page">
    <header class="plaza-hero">
      <p class="eyebrow">HOME CHEF · 广场</p>
      <h1>美食广场</h1>
      <p class="page-description">浏览系统菜谱库，把喜欢的菜复制到我家。</p>
      <van-search
        v-model="keyword"
        placeholder="搜菜名、食材"
        shape="round"
        @search="onSearch"
        @clear="onSearch"
      />
    </header>

    <div class="plaza-categories">
      <button
        type="button"
        class="plaza-category-chip"
        :class="{ 'is-active': activeCategoryId === null }"
        @click="onSelectCategory(null)"
      >
        全部
      </button>
      <button
        v-for="category in categories"
        :key="category.id"
        type="button"
        class="plaza-category-chip"
        :class="{ 'is-active': activeCategoryId === category.id }"
        @click="onSelectCategory(category.id)"
      >
        {{ category.name }}
      </button>
    </div>

    <div v-if="loading" class="plaza-loading">
      <van-loading>加载中...</van-loading>
    </div>

    <div v-else-if="loadError" class="plaza-error">
      <van-empty description="加载失败，请重试" />
      <van-button type="primary" @click="loadDishes">重新加载</van-button>
    </div>

    <van-empty v-else-if="!dishes.length" description="没有找到匹配的菜谱" />

    <div v-else class="plaza-grid">
      <SystemDishCard v-for="dish in dishes" :key="dish.id" :dish="dish" />
    </div>
  </section>
</template>
