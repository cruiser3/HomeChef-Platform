<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import {
  getSystemDish,
  copySystemDishToFamily,
  setSystemDishCuisine,
  setSystemDishAvailability,
  deleteSystemDish,
  listSystemDishReviews,
  createSystemDishReview,
  createSystemDishReply,
  updateSystemDishReview,
  deleteSystemDishReview,
} from '../api/systemDish'
import { listCuisines } from '../api/cuisine'
import { getMyProfile } from '../api/user'
import { uploadFile } from '../api/upload'
import { useAuthStore } from '../store/auth'
import { useFamilyStore } from '../store/family'
import ReviewItem from '../components/ReviewItem.vue'

const route = useRoute()
const router = useRouter()

const authStore = useAuthStore()
const familyStore = useFamilyStore()

const dish = ref(null)
const loading = ref(true)
const loadError = ref(false)

const copyPopup = ref(false)
const price = ref('')
const overrides = ref([])
const copying = ref(false)

const cuisines = ref([])
const cuisinePopup = ref(false)
const settingCuisine = ref(false)

const myId = ref(null)
const managing = ref(false)

const reviews = ref([])
const reviewsLoading = ref(false)
const reviewPopup = ref(false)
const editingReview = ref(null)
const replyTarget = ref(null)
const reviewRating = ref(0)
const reviewContent = ref('')
const reviewImageUrl = ref('')
const reviewAnonymous = ref(false)
const reviewSubmitting = ref(false)
const reviewImageInput = ref(null)
const uploadingReviewImage = ref(false)

const isSourceFamilyAdmin = computed(() =>
  familyStore.families.some((f) => f.id === dish.value?.sourceFamilyId && f.currentUserRole === 'ADMIN'),
)

const canManage = computed(
  () =>
    dish.value?.sourceType === 'FAMILY' &&
    (dish.value.publishedBy === myId.value ||
      authStore.user?.role === 'ADMIN' ||
      isSourceFamilyAdmin.value),
)

const needsAmount = computed(() =>
  (dish.value?.ingredients || []).filter(
    (item) => item.amount == null || item.unit == null || !item.unit.trim(),
  ),
)

const isReplyMode = computed(() => replyTarget.value != null || editingReview.value?.parentId != null)
const reviewTitle = computed(() => {
  if (editingReview.value) return editingReview.value.parentId != null ? '编辑回复' : '编辑评价'
  if (replyTarget.value) return '回复'
  return '评价这道菜'
})

function formatAmount(ingredient) {
  if (ingredient.amount != null && ingredient.unit && ingredient.unit.trim()) {
    return `${ingredient.amount} ${ingredient.unit}`
  }
  if (ingredient.amountText && ingredient.amountText.trim()) {
    return ingredient.amountText
  }
  if (ingredient.amount != null) return String(ingredient.amount)
  if (ingredient.unit) return ingredient.unit
  return ''
}

async function loadDetail() {
  loading.value = true
  loadError.value = false
  try {
    dish.value = await getSystemDish(route.params.id)
    await loadReviews()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  if (!dish.value) return
  reviewsLoading.value = true
  try {
    reviews.value = await listSystemDishReviews(dish.value.id)
  } catch {
    // 拦截器已提示
  } finally {
    reviewsLoading.value = false
  }
}

function openReview(review = null) {
  editingReview.value = review
  replyTarget.value = null
  reviewRating.value = review && review.parentId == null ? review.rating : 0
  reviewContent.value = review ? review.content || '' : ''
  reviewImageUrl.value = review ? review.imageUrl || '' : ''
  reviewAnonymous.value = review ? review.anonymous : false
  reviewPopup.value = true
}

function openReply(target) {
  editingReview.value = null
  replyTarget.value = target
  reviewRating.value = 0
  reviewContent.value = ''
  reviewImageUrl.value = ''
  reviewAnonymous.value = false
  reviewPopup.value = true
}

function closeReview() {
  reviewPopup.value = false
  editingReview.value = null
  replyTarget.value = null
  reviewRating.value = 0
  reviewContent.value = ''
  reviewImageUrl.value = ''
  reviewAnonymous.value = false
}

function pickReviewImage() {
  reviewImageInput.value?.click()
}

async function onReviewImageChange(event) {
  const file = event.target.files?.[0]
  if (!file || uploadingReviewImage.value) return
  uploadingReviewImage.value = true
  try {
    const { url } = await uploadFile(file)
    reviewImageUrl.value = url
    showToast('图片已上传')
  } catch {
    // 拦截器已提示
  } finally {
    uploadingReviewImage.value = false
    event.target.value = ''
  }
}

async function submitReview() {
  if (reviewSubmitting.value) return
  const isReply = isReplyMode.value
  if (!isReply && !reviewRating.value) {
    showToast('请先打分')
    return
  }
  if (isReply && !reviewContent.value.trim() && !reviewImageUrl.value.trim()) {
    showToast('请输入回复内容或上传图片')
    return
  }
  reviewSubmitting.value = true
  try {
    const payload = {
      rating: isReply ? null : reviewRating.value,
      content: reviewContent.value.trim() || null,
      imageUrl: reviewImageUrl.value.trim() || null,
      anonymous: reviewAnonymous.value,
    }
    if (editingReview.value) {
      await updateSystemDishReview(dish.value.id, editingReview.value.id, payload)
    } else if (replyTarget.value) {
      await createSystemDishReply(dish.value.id, replyTarget.value.id, payload)
    } else {
      await createSystemDishReview(dish.value.id, payload)
    }
    showToast(editingReview.value ? '已更新' : (replyTarget.value ? '已回复' : '已评价'))
    closeReview()
    await loadReviews()
  } catch {
    // 拦截器已提示
  } finally {
    reviewSubmitting.value = false
  }
}

async function onDeleteReview(review) {
  try {
    await showConfirmDialog({ title: '删除评价', message: '确定删除这条评价吗？' })
  } catch {
    return
  }
  try {
    await deleteSystemDishReview(dish.value.id, review.id)
    showToast('已删除')
    await loadReviews()
  } catch {
    // 拦截器已提示
  }
}

function onBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push({ name: 'plaza' })
  }
}

function openCopy() {
  price.value = ''
  overrides.value = needsAmount.value.map((item) => ({
    sortNo: item.sortNo,
    name: item.name,
    amount: '',
    unit: '',
  }))
  copyPopup.value = true
}

async function submitCopy() {
  const priceNum = Number(price.value)
  if (price.value === '' || Number.isNaN(priceNum) || priceNum < 0) {
    showToast('请输入有效的价格')
    return
  }
  const ingredientOverrides = overrides.value
    .filter((o) => o.amount !== '' && o.unit.trim() !== '')
    .map((o) => ({ sortNo: o.sortNo, amount: Number(o.amount), unit: o.unit.trim() }))
  copying.value = true
  try {
    const copied = await copySystemDishToFamily(dish.value.id, {
      price: priceNum,
      ingredientOverrides: ingredientOverrides.length ? ingredientOverrides : null,
    })
    showToast('已复制到我家')
    copyPopup.value = false
    router.push({ name: 'dish-detail', params: { id: copied.id } })
  } catch {
    // 拦截器已提示
  } finally {
    copying.value = false
  }
}

async function loadCuisines() {
  try {
    cuisines.value = await listCuisines()
  } catch {
    // 拦截器已提示
  }
}

function openCuisine() {
  cuisinePopup.value = true
}

async function onSelectCuisine(cuisine) {
  if (settingCuisine.value) return
  settingCuisine.value = true
  try {
    dish.value = await setSystemDishCuisine(dish.value.id, cuisine.id)
    showToast('菜系已更新')
    cuisinePopup.value = false
  } catch {
    // 拦截器已提示
  } finally {
    settingCuisine.value = false
  }
}

async function onClearCuisine() {
  if (settingCuisine.value) return
  settingCuisine.value = true
  try {
    dish.value = await setSystemDishCuisine(dish.value.id, null)
    showToast('菜系已清除')
    cuisinePopup.value = false
  } catch {
    // 拦截器已提示
  } finally {
    settingCuisine.value = false
  }
}

async function loadMyId() {
  try {
    const profile = await getMyProfile()
    myId.value = profile.id
  } catch {
    myId.value = null
  }
}

async function loadFamilies() {
  if (familyStore.loaded) return
  try {
    await familyStore.loadFamilies()
  } catch {
    // 忽略：管理权限由后端兜底
  }
}

async function onToggleAvailability() {
  if (managing.value || !dish.value) return
  managing.value = true
  try {
    dish.value = await setSystemDishAvailability(dish.value.id, !dish.value.available)
    showToast(dish.value.available ? '已上架' : '已下架')
    if (!dish.value.available) {
      router.push({ name: 'plaza' })
    }
  } catch {
    // 拦截器已提示
  } finally {
    managing.value = false
  }
}

async function onWithdraw() {
  if (managing.value || !dish.value) return
  try {
    await showConfirmDialog({
      title: '撤回发布',
      message: `确定撤回「${dish.value.name}」吗？将从广场移除该菜品。`,
      confirmButtonText: '撤回',
      confirmButtonColor: '#ee0a24',
    })
  } catch {
    return
  }
  managing.value = true
  try {
    await deleteSystemDish(dish.value.id)
    showToast('已撤回')
    router.push({ name: 'plaza' })
  } catch {
    // 拦截器已提示
  } finally {
    managing.value = false
  }
}

loadDetail()
loadCuisines()
loadMyId()
loadFamilies()
</script>

<template>
  <section class="plaza-detail">
    <van-loading v-if="loading" class="dish-loading">加载中...</van-loading>

    <div v-else-if="loadError" class="dish-error">
      <van-empty description="菜谱加载失败，请稍后重试" />
      <van-button type="primary" plain @click="loadDetail">重新加载</van-button>
    </div>

    <template v-else-if="dish">
      <button class="dish-back" type="button" @click="onBack">
        <van-icon name="arrow-left" /> 返回广场
      </button>

      <div class="dish-detail-media">
        <van-image v-if="dish.coverImageUrl" :src="dish.coverImageUrl" fit="cover">
          <template #loading>
            <span class="dish-card-fallback">{{ dish.name }}</span>
          </template>
          <template #error>
            <span class="dish-card-fallback">{{ dish.name }}</span>
          </template>
        </van-image>
        <span v-else class="dish-card-fallback">{{ dish.name }}</span>
      </div>

      <header class="dish-detail-head">
        <h1>{{ dish.name }}</h1>
        <p class="dish-detail-meta">
          <span v-if="dish.categoryName">{{ dish.categoryName }}</span>
          <span v-if="dish.cuisineName">{{ dish.cuisineName }}</span>
          <span v-if="dish.cookingMinutes != null">{{ dish.cookingMinutes }} 分钟</span>
          <span v-if="dish.calories != null">{{ dish.calories }} 千卡</span>
        </p>
        <p v-if="dish.description" class="dish-detail-desc">{{ dish.description }}</p>
        <p
          v-if="dish.sourceType === 'FAMILY' && (dish.familyName || dish.publisherName)"
          class="dish-detail-creator"
        >
          来自 {{ dish.familyName || '未知家庭' }} · 发布人 {{ dish.publisherName || '匿名' }}
        </p>
      </header>

      <div class="dish-detail-actions">
        <van-button block type="primary" @click="openCopy">复制到我家</van-button>
        <van-button block plain type="primary" @click="openCuisine">设置菜系</van-button>
        <van-button v-if="canManage" block :loading="managing" @click="onToggleAvailability">
          下架
        </van-button>
        <van-button v-if="canManage" block type="danger" :loading="managing" @click="onWithdraw">
          撤回发布
        </van-button>
      </div>

      <section v-if="dish.ingredients?.length" class="dish-detail-section">
        <h2>食材</h2>
        <ul class="dish-ingredient-list">
          <li v-for="(ingredient, index) in dish.ingredients" :key="index">
            <span class="dish-ingredient-name">
              {{ ingredient.name }}
              <span v-if="ingredient.optional" class="plaza-optional-tag">可选</span>
            </span>
            <span class="dish-ingredient-amount">{{ formatAmount(ingredient) }}</span>
          </li>
        </ul>
      </section>

      <section v-if="dish.steps?.length" class="dish-detail-section">
        <h2>做法步骤</h2>
        <ol class="dish-step-list">
          <li v-for="step in dish.steps" :key="step.stepNo">
            <span class="dish-step-no">{{ step.stepNo }}</span>
            <div class="dish-step-content">
              <p>{{ step.content }}</p>
              <van-image
                v-if="step.imageUrl"
                :src="step.imageUrl"
                fit="cover"
                class="dish-step-image"
              />
            </div>
          </li>
        </ol>
      </section>

      <section v-if="dish.license || dish.sourceUrl" class="dish-detail-section">
        <h2>来源</h2>
        <p class="plaza-source">
          <span v-if="dish.license">{{ dish.license }}</span>
          <a v-if="dish.sourceUrl" :href="dish.sourceUrl" target="_blank" rel="noopener">查看原始菜谱</a>
        </p>
      </section>

      <section class="dish-detail-section">
        <div class="dish-review-head">
          <h2>评价</h2>
          <button class="review-add-btn" type="button" @click="openReview()">写评价</button>
        </div>

        <div v-if="reviewsLoading" class="dish-loading">加载中...</div>
        <p v-else-if="!reviews.length" class="dish-review-empty">还没有评价，来写第一条吧</p>
        <div v-else class="dish-review-list">
          <ReviewItem
            v-for="review in reviews"
            :key="review.id"
            :review="review"
            @reply="openReply"
            @edit="openReview"
            @delete="onDeleteReview"
          />
        </div>
      </section>
    </template>

    <van-empty v-else description="菜谱不存在或已下架" />

    <van-popup v-model:show="copyPopup" round position="bottom">
      <div class="plaza-copy-sheet">
        <h2>复制到我家</h2>
        <p class="plaza-copy-dish">{{ dish?.name }}</p>

        <van-field
          v-model="price"
          type="number"
          label="价格"
          placeholder="请输入价格，如 25.00"
        />

        <div v-if="overrides.length" class="plaza-copy-overrides">
          <h3>补充食材用量（选填）</h3>
          <p class="plaza-copy-hint">以下食材缺少精确用量，可补充数量与单位；不填则保留原文（如「适量」）。</p>
          <div v-for="item in overrides" :key="item.sortNo" class="plaza-copy-override-row">
            <span class="plaza-copy-override-name">{{ item.name }}</span>
            <van-field v-model="item.amount" type="number" placeholder="数量" />
            <van-field v-model="item.unit" placeholder="单位" maxlength="20" />
          </div>
        </div>

        <van-button class="plaza-copy-submit" block type="primary" :loading="copying" @click="submitCopy">
          复制
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="cuisinePopup" round position="bottom">
      <div class="plaza-copy-sheet">
        <h2>设置菜系</h2>
        <p class="plaza-copy-dish">{{ dish?.name }}</p>
        <van-cell-group class="plaza-cuisine-list">
          <van-cell
            title="不设置菜系"
            :class="{ 'is-active': dish?.cuisineId == null }"
            @click="onClearCuisine"
          />
          <van-cell
            v-for="cuisine in cuisines"
            :key="cuisine.id"
            :title="cuisine.name"
            :class="{ 'is-active': dish?.cuisineId === cuisine.id }"
            @click="onSelectCuisine(cuisine)"
          />
        </van-cell-group>
      </div>
    </van-popup>

    <van-popup v-model:show="reviewPopup" round position="bottom">
      <div class="review-sheet">
        <h2>{{ reviewTitle }}</h2>
        <p class="review-sheet-dish">{{ replyTarget ? `回复 ${replyTarget.userName}` : dish?.name }}</p>
        <div v-if="!isReplyMode" class="review-rate-row">
          <van-rate v-model="reviewRating" :count="5" size="24" />
          <span class="review-rate-label">{{ reviewRating || '请打分' }}</span>
        </div>
        <van-field
          v-model="reviewContent"
          type="textarea"
          rows="3"
          placeholder="说点什么（选填）"
          maxlength="500"
        />
        <div class="review-image-field">
          <div class="review-image-toolbar">
            <van-button
              size="small"
              plain
              type="primary"
              icon="photograph"
              :loading="uploadingReviewImage"
              loading-text="上传中..."
              @click="pickReviewImage"
            >
              上传图片
            </van-button>
            <van-image v-if="reviewImageUrl" :src="reviewImageUrl" fit="cover" class="dish-image-preview" />
          </div>
          <input
            ref="reviewImageInput"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            hidden
            @change="onReviewImageChange"
          />
        </div>
        <van-cell title="匿名评价" center>
          <template #right-icon>
            <van-switch v-model="reviewAnonymous" size="20" />
          </template>
        </van-cell>
        <van-button class="review-submit" block type="primary" :loading="reviewSubmitting" @click="submitReview">
          {{ editingReview ? '保存' : (replyTarget ? '提交回复' : '提交评价') }}
        </van-button>
      </div>
    </van-popup>
  </section>
</template>
