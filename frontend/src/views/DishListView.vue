<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import DishCard from '../components/DishCard.vue'
import { listDishes, listDishCategories, createDishCategory, updateDishCategory, deleteDishCategory } from '../api/dish'
import { useFamilyStore } from '../store/family'

const familyStore = useFamilyStore()

const dishes = ref([])
const categories = ref([])
const keyword = ref('')
const activeCategoryId = ref(null)
const loading = ref(false)
const loadError = ref(false)

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

async function loadDishes() {
  loading.value = true
  loadError.value = false
  const params = {}
  if (activeCategoryId.value != null) params.categoryId = activeCategoryId.value
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
    keyword.value = ''
    loadCategories()
    loadDishes()
  },
)

loadCategories()
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
      <van-search
        v-model="keyword"
        placeholder="搜菜品、套餐"
        shape="round"
        @search="onSearch"
        @clear="onSearch"
      />
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
      <div class="dish-categories">
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
      </div>

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
