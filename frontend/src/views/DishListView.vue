<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import DishCard from '../components/DishCard.vue'
import { listDishes, listDishCategories, createDishCategory, updateDishCategory, deleteDishCategory } from '../api/dish'
import { listCuisines } from '../api/cuisine'
import { useFamilyStore } from '../store/family'

const familyStore = useFamilyStore()

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

const viewMode = ref('dishes')

const memberGroups = computed(() => {
  const groups = new Map()
  dishes.value.forEach((dish) => {
    const name = dish.createdByName || '未知成员'
    if (!groups.has(name)) {
      groups.set(name, { name, dishes: [] })
    }
    groups.get(name).dishes.push(dish)
  })
  return [...groups.values()]
})

const showCategoryPopup = ref(false)
const creatingCategory = ref(false)
const categoryForm = reactive({ name: '' })

async function loadCategories() {
  try {
    categories.value = await listDishCategories()
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
    dishes.value = await listDishes(params)
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

async function onSubmitCategory() {
  const name = categoryForm.name.trim()
  if (!name) {
    showToast('请输入分类名称')
    return
  }
  if (name.length > 50) {
    showToast('分类名称不超过 50 字')
    return
  }
  if (creatingCategory.value) return

  creatingCategory.value = true
  try {
    await createDishCategory({ name })
    showCategoryPopup.value = false
    categoryForm.name = ''
    showToast('分类已创建')
    await loadCategories()
  } catch {
    // 拦截器已提示
  } finally {
    creatingCategory.value = false
  }
}

const editingCategoryId = ref(null)
const editingName = ref('')

function startRename(category) {
  editingCategoryId.value = category.id
  editingName.value = category.name
}

function cancelRename() {
  editingCategoryId.value = null
  editingName.value = ''
}

async function submitRename(category) {
  const name = editingName.value.trim()
  if (!name) {
    showToast('请输入分类名称')
    return
  }
  if (name.length > 50) {
    showToast('分类名称不超过 50 字')
    return
  }
  try {
    await updateDishCategory(category.id, { name })
    showToast('分类已重命名')
    editingCategoryId.value = null
    await loadCategories()
  } catch {
    // 拦截器已提示
  }
}

async function confirmDeleteCategory(category) {
  try {
    await showConfirmDialog({
      title: '删除分类',
      message: `删除「${category.name}」后，该分类下的菜品将变为「未分类」，确定删除吗？`,
      confirmButtonText: '删除',
      confirmButtonColor: '#ee0a24',
    })
  } catch {
    return
  }
  try {
    await deleteDishCategory(category.id)
    showToast('分类已删除')
    if (activeCategoryId.value === category.id) {
      activeCategoryId.value = null
      loadDishes()
    }
    await loadCategories()
  } catch {
    // 拦截器已提示
  }
}

watch(
  () => familyStore.currentFamilyId,
  () => {
    activeCategoryId.value = null
    activeCuisineId.value = null
    activeCaloriesMax.value = null
    keyword.value = ''
    loadCategories()
    loadDishes()
  },
)

loadCategories()
loadCuisines()
loadDishes()
</script>

<template>
  <section class="dish-page">
    <header class="dish-hero">
      <p class="eyebrow">HOME CHEF · 小家</p>
      <div class="dish-hero-row">
        <h1>今天吃什么？</h1>
        <RouterLink class="dish-add-btn" :to="{ name: 'dish-edit' }" aria-label="新建菜品">
          <van-icon name="plus" />
        </RouterLink>
      </div>
      <div class="dish-search-row">
        <van-search
          v-model="keyword"
          placeholder="搜菜品、套餐"
          shape="round"
          @search="onSearch"
          @clear="onSearch"
        />
        <button
          type="button"
          class="dish-filter-btn"
          :class="{ 'is-filtered': hasActiveFilters }"
          aria-label="筛选"
          @click="showFilters = true"
        >
          <van-icon name="filter-o" />
        </button>
      </div>
    </header>

    <div class="dish-view-switch">
      <button
        type="button"
        :class="{ 'is-active': viewMode === 'dishes' }"
        @click="viewMode = 'dishes'"
      >
        菜品库
      </button>
      <button
        type="button"
        :class="{ 'is-active': viewMode === 'members' }"
        @click="viewMode = 'members'"
      >
        成员贡献
      </button>
    </div>

    <template v-if="viewMode === 'dishes'">
      <div class="dish-browse">
        <aside class="dish-sidebar dish-categories">
          <button
            type="button"
            class="dish-category-chip"
            :class="{ 'is-active': activeCategoryId === null }"
            @click="onSelectCategory(null)"
          >
            全部
          </button>
          <button
            v-for="category in categories"
            :key="category.id"
            type="button"
            class="dish-category-chip"
            :class="{ 'is-active': activeCategoryId === category.id }"
            @click="onSelectCategory(category.id)"
          >
            {{ category.name }}
          </button>
          <button type="button" class="dish-category-chip dish-category-add" @click="showCategoryPopup = true">
            <van-icon name="plus" /> 分类
          </button>
        </aside>

        <div class="dish-main">
          <div v-if="loading" class="dish-loading">
            <van-loading>加载中...</van-loading>
          </div>

          <div v-else-if="loadError" class="dish-error">
            <van-empty description="加载失败，请重试" />
            <van-button type="primary" @click="loadDishes">重新加载</van-button>
          </div>

          <van-empty
            v-else-if="!dishes.length"
            description="还没有菜品，点击右上角 + 创建第一道"
          />

          <div v-else class="dish-grid">
            <DishCard v-for="dish in dishes" :key="dish.id" :dish="dish" />
          </div>
        </div>
      </div>

      <van-popup v-model:show="showFilters" position="top">
        <div class="dish-filter-sheet">
          <div class="dish-filter-sheet-head">
            <h3>筛选</h3>
            <button type="button" class="dish-filter-close" aria-label="关闭" @click="showFilters = false">
              <van-icon name="cross" />
            </button>
          </div>

          <div class="dish-filter-group">
            <p class="dish-filter-label">菜系</p>
            <div class="dish-filter-chips">
              <button
                type="button"
                class="dish-category-chip"
                :class="{ 'is-active': activeCuisineId === null }"
                @click="onSelectCuisine(null)"
              >
                全部菜系
              </button>
              <button
                v-for="cuisine in cuisines"
                :key="cuisine.id"
                type="button"
                class="dish-category-chip"
                :class="{ 'is-active': activeCuisineId === cuisine.id }"
                @click="onSelectCuisine(cuisine.id)"
              >
                {{ cuisine.name }}
              </button>
            </div>
          </div>

          <div class="dish-filter-group">
            <p class="dish-filter-label">卡路里</p>
            <div class="dish-filter-chips">
              <button
                v-for="(opt, i) in calorieOptions"
                :key="i"
                type="button"
                class="dish-category-chip"
                :class="{ 'is-active': isCalorieOptionActive(opt) }"
                @click="onSelectCalories(opt)"
              >
                {{ opt.label }}
              </button>
              <button
                type="button"
                class="dish-category-chip"
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
    </template>

    <template v-else>
      <div v-if="loading" class="dish-loading">
        <van-loading>加载中...</van-loading>
      </div>
      <div v-else-if="loadError" class="dish-error">
        <van-empty description="加载失败，请重试" />
        <van-button type="primary" @click="loadDishes">重新加载</van-button>
      </div>
      <van-empty v-else-if="!memberGroups.length" description="还没有成员发布菜品" />
      <div v-else>
        <div v-for="group in memberGroups" :key="group.name" class="member-group">
          <h2>{{ group.name }} <span>{{ group.dishes.length }} 道</span></h2>
          <div class="member-dish-list">
            <RouterLink
              v-for="dish in group.dishes"
              :key="dish.id"
              class="member-dish-item"
              :to="{ name: 'dish-detail', params: { id: dish.id } }"
            >
              {{ dish.name }}
            </RouterLink>
          </div>
        </div>
      </div>
    </template>

    <van-popup v-model:show="showCategoryPopup" round position="bottom">
      <div class="dish-category-sheet">
        <h2>分类管理</h2>
        <div class="dish-category-create">
          <van-field v-model="categoryForm.name" label="新分类" placeholder="例如：热菜" maxlength="50" />
          <van-button
            class="dish-category-submit"
            block
            type="primary"
            :loading="creatingCategory"
            loading-text="创建中..."
            @click="onSubmitCategory"
          >
            创建分类
          </van-button>
        </div>
        <div v-if="categories.length" class="dish-category-manage-list">
          <div v-for="cat in categories" :key="cat.id" class="dish-category-manage-item">
            <template v-if="editingCategoryId === cat.id">
              <van-field v-model="editingName" maxlength="50" placeholder="分类名称" />
              <div class="dish-category-manage-actions">
                <van-button size="small" plain @click="cancelRename">取消</van-button>
                <van-button size="small" type="primary" @click="submitRename(cat)">确定</van-button>
              </div>
            </template>
            <template v-else>
              <span class="dish-category-manage-name">{{ cat.name }}</span>
              <div class="dish-category-manage-actions">
                <van-button size="small" plain @click="startRename(cat)">重命名</van-button>
                <van-button size="small" plain type="danger" @click="confirmDeleteCategory(cat)">删除</van-button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </van-popup>
  </section>
</template>
