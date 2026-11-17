<script setup>
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import SystemDishCard from '../components/SystemDishCard.vue'
import { listSystemDishCategories, listSystemDishes } from '../api/systemDish'
import { listCuisines } from '../api/cuisine'

// 显式声明 name，供 <KeepAlive :include> 匹配缓存。
defineOptions({ name: 'SystemDishListView' })

const dishes = ref([])
const categories = ref([])
const cuisines = ref([])
const keyword = ref('')
const activeCategoryId = ref(null)
const activeCuisineId = ref(null)
const activeCaloriesMin = ref(null)
const activeCaloriesMax = ref(null)
const loading = ref(false)
const loadError = ref(false)

// 卡路里区间预设（左闭右开 [min, max)）：150 以下 / 150-300 / ... / 1000 以上。
const calorieOptions = [
  { min: null, max: null, label: '卡路里不限' },
  { min: null, max: 150, label: '150 以下' },
  { min: 150, max: 300, label: '150-300' },
  { min: 300, max: 500, label: '300-500' },
  { min: 500, max: 800, label: '500-800' },
  { min: 800, max: 1000, label: '800-1000' },
  { min: 1000, max: null, label: '1000 以上' },
]

const showCalorieCustom = ref(false)
const calorieCustomMin = ref('')
const calorieCustomMax = ref('')

const showFilters = ref(false)
const hasActiveFilters = computed(
  () => activeCuisineId.value != null || activeCaloriesMin.value != null || activeCaloriesMax.value != null,
)

function isCalorieOptionActive(opt) {
  return activeCaloriesMin.value === opt.min && activeCaloriesMax.value === opt.max
}

const isCustomCalorieActive = computed(() => {
  if (activeCaloriesMin.value == null && activeCaloriesMax.value == null) return false
  return !calorieOptions.some((opt) => activeCaloriesMin.value === opt.min && activeCaloriesMax.value === opt.max)
})

const customCalorieLabel = computed(() => {
  const min = activeCaloriesMin.value
  const max = activeCaloriesMax.value
  if (min != null && max != null) return `自定义 ${min}-${max}`
  if (min != null) return `≥ ${min}`
  if (max != null) return `≤ ${max}`
  return '自定义'
})

async function loadCategories() {
  try {
    categories.value = await listSystemDishCategories()
  } catch {
    // 拦截器已提示
  }
}

async function loadCuisines() {
  try {
    cuisines.value = await listCuisines()
  } catch {
    // 拦截器已提示
  }
}

async function loadDishes() {
  loading.value = true
  loadError.value = false
  const params = {}
  if (activeCategoryId.value != null) params.categoryId = activeCategoryId.value
  if (activeCuisineId.value != null) params.cuisineId = activeCuisineId.value
  if (activeCaloriesMin.value != null) params.caloriesMin = activeCaloriesMin.value
  if (activeCaloriesMax.value != null) params.caloriesMax = activeCaloriesMax.value
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

function onSelectCuisine(cuisineId) {
  activeCuisineId.value = cuisineId
  loadDishes()
}

function onSelectCalories(opt) {
  activeCaloriesMin.value = opt.min
  activeCaloriesMax.value = opt.max
  loadDishes()
}

function openCalorieCustom() {
  calorieCustomMin.value = activeCaloriesMin.value != null ? String(activeCaloriesMin.value) : ''
  calorieCustomMax.value = activeCaloriesMax.value != null ? String(activeCaloriesMax.value) : ''
  showFilters.value = false
  showCalorieCustom.value = true
}

function applyCalorieCustom() {
  const min = calorieCustomMin.value.trim() === '' ? null : Number(calorieCustomMin.value)
  const max = calorieCustomMax.value.trim() === '' ? null : Number(calorieCustomMax.value)
  if ((min != null && !Number.isInteger(min)) || (max != null && !Number.isInteger(max))) {
    showToast('请输入整数千卡')
    return
  }
  if ((min != null && min < 0) || (max != null && max < 0)) {
    showToast('卡路里不能为负数')
    return
  }
  if (min != null && max != null && min > max) {
    showToast('最低值不能大于最高值')
    return
  }
  activeCaloriesMin.value = min
  activeCaloriesMax.value = max
  showCalorieCustom.value = false
  loadDishes()
}

// 滚动位置保持：列表↔详情往返时停留在原浏览位置。
// 滚动容器是 window（body 滚动）；用 scroll 事件实时记录，避免 onDeactivated 时
// DOM 已被 KeepAlive 移出、window.scrollY 被钳制归零导致记录失效。
let savedScrollTop = 0
function onWindowScroll() {
  savedScrollTop = window.scrollY || document.documentElement.scrollTop || 0
}
function startScrollTracking() {
  window.addEventListener('scroll', onWindowScroll, { passive: true })
}
function stopScrollTracking() {
  window.removeEventListener('scroll', onWindowScroll)
}
onMounted(startScrollTracking)
onBeforeUnmount(stopScrollTracking)
// KeepAlive 缓存：进入详情页时组件不销毁，须在 onDeactivated 停止记录，避免详情页导航滚顶
// 把 savedScrollTop 覆盖成 0；返回列表时恢复监听并回滚到离开位置。
onDeactivated(stopScrollTracking)
onActivated(() => {
  startScrollTracking()
  // 用宏任务延迟，确保在路由 scrollBehavior（微任务）之后执行，覆盖其默认滚顶。
  setTimeout(() => window.scrollTo(0, savedScrollTop), 0)
})

loadCategories()
loadCuisines()
loadDishes()
</script>

<template>
  <section class="plaza-page">
    <header class="plaza-hero">
      <p class="eyebrow">HOME CHEF · 广场</p>
      <h1>美食广场</h1>
      <p class="page-description">浏览系统菜谱库，把喜欢的菜复制到我家。</p>
      <div class="plaza-search-row">
        <van-search
          v-model="keyword"
          placeholder="搜菜名、食材"
          shape="round"
          @search="onSearch"
          @clear="onSearch"
        />
        <button
          type="button"
          class="plaza-filter-btn"
          :class="{ 'is-filtered': hasActiveFilters }"
          aria-label="筛选"
          @click="showFilters = true"
        >
          <van-icon name="filter-o" />
        </button>
      </div>
    </header>

    <div class="plaza-browse">
      <aside class="plaza-sidebar plaza-categories">
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
      </aside>

      <div class="plaza-main">
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
      </div>
    </div>

    <van-popup v-model:show="showFilters" position="top">
      <div class="plaza-filter-sheet">
        <div class="plaza-filter-sheet-head">
          <h3>筛选</h3>
          <button type="button" class="plaza-filter-close" aria-label="关闭" @click="showFilters = false">
            <van-icon name="cross" />
          </button>
        </div>

        <div class="plaza-filter-group">
          <p class="plaza-filter-label">菜系</p>
          <div class="plaza-filter-chips">
            <button
              type="button"
              class="plaza-category-chip"
              :class="{ 'is-active': activeCuisineId === null }"
              @click="onSelectCuisine(null)"
            >
              全部菜系
            </button>
            <button
              v-for="cuisine in cuisines"
              :key="cuisine.id"
              type="button"
              class="plaza-category-chip"
              :class="{ 'is-active': activeCuisineId === cuisine.id }"
              @click="onSelectCuisine(cuisine.id)"
            >
              {{ cuisine.name }}
            </button>
          </div>
        </div>

        <div class="plaza-filter-group">
          <p class="plaza-filter-label">卡路里</p>
          <div class="plaza-filter-chips">
            <button
              v-for="(opt, i) in calorieOptions"
              :key="i"
              type="button"
              class="plaza-category-chip"
              :class="{ 'is-active': isCalorieOptionActive(opt) }"
              @click="onSelectCalories(opt)"
            >
              {{ opt.label }}
            </button>
            <button
              type="button"
              class="plaza-category-chip"
              :class="{ 'is-active': isCustomCalorieActive }"
              @click="openCalorieCustom"
            >
              {{ customCalorieLabel }}
            </button>
          </div>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showCalorieCustom" round position="bottom">
      <div class="calorie-custom-panel">
        <h3>自定义卡路里范围</h3>
        <van-field v-model="calorieCustomMin" type="digit" label="最低（千卡）" placeholder="留空表示不限" />
        <van-field v-model="calorieCustomMax" type="digit" label="最高（千卡）" placeholder="留空表示不限" />
        <div class="calorie-custom-actions">
          <van-button block plain @click="showCalorieCustom = false">取消</van-button>
          <van-button block type="primary" @click="applyCalorieCustom">确定</van-button>
        </div>
      </div>
    </van-popup>
  </section>
</template>
