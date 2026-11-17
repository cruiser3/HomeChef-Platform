<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { getDish, changeDishAvailability, deleteDish, publishDishToSquare } from '../api/dish'
import { listSystemDishCategories } from '../api/systemDish'
import { listDishReviews, createDishReview, createDishReply, updateDishReview, deleteDishReview } from '../api/review'
import { uploadFile } from '../api/upload'
import ReviewItem from '../components/ReviewItem.vue'
import { getMyProfile } from '../api/user'
import { useCartStore } from '../store/cart'
import { useFamilyStore } from '../store/family'

const route = useRoute()
const router = useRouter()

const dish = ref(null)
const loading = ref(true)
const loadError = ref(false)
const toggling = ref(false)
const adding = ref(false)
const cartStore = useCartStore()
const familyStore = useFamilyStore()
const myId = ref(null)
const deleting = ref(false)

const publishPopup = ref(false)
const publishCategories = ref([])
const publishCategoryId = ref(null)
const publishing = ref(false)

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

const isAdmin = computed(() => familyStore.currentFamily?.currentUserRole === 'ADMIN')
const canManage = computed(() => dish.value && (dish.value.createdBy === myId.value || isAdmin.value))
const isReplyMode = computed(() => replyTarget.value != null || editingReview.value?.parentId != null)
const reviewTitle = computed(() => {
  if (editingReview.value) return editingReview.value.parentId != null ? '编辑回复' : '编辑评价'
  if (replyTarget.value) return '回复'
  return '评价这道菜'
})

function formatPrice(price) {
  return Number(price ?? 0).toFixed(2)
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

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
    dish.value = await getDish(route.params.id)
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
    reviews.value = await listDishReviews(dish.value.id)
  } catch {
    // 拦截器已提示
  } finally {
    reviewsLoading.value = false
  }
}

function onBack() {
  router.push({ name: 'dishes' })
}

function onEdit() {
  router.push({ name: 'dish-edit', params: { id: dish.value.id } })
}

async function onAddToCart() {
  if (adding.value || !dish.value?.available) return
  adding.value = true
  try {
    await cartStore.addItem(dish.value.id, 1)
    showToast('已加入购物车')
  } catch {
    // 拦截器已提示
  } finally {
    adding.value = false
  }
}

async function onToggleAvailability() {
  if (toggling.value || !dish.value) return
  toggling.value = true
  try {
    const next = !dish.value.available
    dish.value = await changeDishAvailability(dish.value.id, next)
    showToast(next ? '已上架' : '已下架')
  } catch {
    // 拦截器已提示
  } finally {
    toggling.value = false
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

async function onDelete() {
  if (deleting.value || !dish.value) return
  try {
    await showConfirmDialog({
      title: '删除菜品',
      message: `确定删除「${dish.value.name}」吗？删除后不可恢复。`,
      confirmButtonText: '删除',
      confirmButtonColor: '#ee0a24',
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteDish(dish.value.id)
    showToast('已删除')
    router.push({ name: 'dishes' })
  } catch {
    // 拦截器已提示（含被历史订单引用 409）
  } finally {
    deleting.value = false
  }
}

async function openPublish() {
  publishCategoryId.value = null
  publishPopup.value = true
  if (!publishCategories.value.length) {
    try {
      publishCategories.value = await listSystemDishCategories()
    } catch {
      // 拦截器已提示
    }
  }
}

async function submitPublish() {
  if (publishing.value || !dish.value) return
  if (!publishCategoryId.value) {
    showToast('请选择广场分类')
    return
  }
  publishing.value = true
  try {
    await publishDishToSquare(dish.value.id, publishCategoryId.value)
    showToast('已发布到广场')
    publishPopup.value = false
  } catch {
    // 拦截器已提示
  } finally {
    publishing.value = false
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
      await updateDishReview(dish.value.id, editingReview.value.id, payload)
    } else if (replyTarget.value) {
      await createDishReply(dish.value.id, replyTarget.value.id, payload)
    } else {
      await createDishReview(dish.value.id, payload)
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
    await deleteDishReview(dish.value.id, review.id)
    showToast('已删除')
    await loadReviews()
  } catch {
    // 拦截器已提示
  }
}

loadDetail()
loadMyId()
</script>

<template>
  <section class="dish-detail">
    <van-loading v-if="loading" class="dish-loading">加载中...</van-loading>

    <div v-else-if="loadError" class="dish-error">
      <van-empty description="菜品加载失败，请稍后重试" />
      <van-button type="primary" plain @click="loadDetail">重新加载</van-button>
    </div>

    <template v-else-if="dish">
      <button class="dish-back" type="button" @click="onBack">
        <van-icon name="arrow-left" /> 返回小家
      </button>

      <div class="dish-detail-media">
        <van-image v-if="dish.imageUrl" :src="dish.imageUrl" fit="cover">
          <template #loading>
            <span class="dish-card-fallback">{{ dish.name?.charAt(0) || '菜' }}</span>
          </template>
          <template #error>
            <span class="dish-card-fallback">{{ dish.name?.charAt(0) || '菜' }}</span>
          </template>
        </van-image>
        <span v-else class="dish-card-fallback">{{ dish.name?.charAt(0) || '菜' }}</span>
        <span v-if="!dish.available" class="dish-card-badge">已下架</span>
      </div>

      <header class="dish-detail-head">
        <h1>{{ dish.name }}</h1>
        <p class="dish-detail-meta">
          <span v-if="dish.categoryName">{{ dish.categoryName }}</span>
          <span v-if="dish.cookingMinutes != null">{{ dish.cookingMinutes }} 分钟</span>
          <span class="dish-detail-price">¥{{ formatPrice(dish.price) }}</span>
        </p>
        <p v-if="dish.description" class="dish-detail-desc">{{ dish.description }}</p>
        <p v-if="dish.createdByName" class="dish-detail-creator">由 {{ dish.createdByName }} 创建</p>
      </header>

      <div class="dish-detail-actions">
        <van-button
          block
          type="primary"
          :loading="adding"
          :disabled="!dish.available"
          @click="onAddToCart"
        >
          加入购物车
        </van-button>
        <van-button v-if="canManage" block @click="onEdit">编辑菜品</van-button>
        <van-button block plain type="primary" @click="openPublish">发布到广场</van-button>
        <van-button v-if="canManage" block :loading="toggling" @click="onToggleAvailability">
          {{ dish.available ? '下架菜品' : '重新上架' }}
        </van-button>
        <van-button
          v-if="canManage"
          block
          type="danger"
          :loading="deleting"
          @click="onDelete"
        >
          删除菜品
        </van-button>
      </div>

      <section v-if="dish.ingredients?.length" class="dish-detail-section">
        <h2>食材</h2>
        <ul class="dish-ingredient-list">
          <li v-for="(ingredient, index) in dish.ingredients" :key="index">
            <span class="dish-ingredient-name">{{ ingredient.name }}</span>
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

    <van-empty v-else description="菜品不存在或已被移除" />

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

    <van-popup v-model:show="publishPopup" round position="bottom">
      <div class="plaza-copy-sheet">
        <h2>发布到广场</h2>
        <p class="plaza-copy-dish">{{ dish?.name }}</p>
        <p class="plaza-copy-hint">请选择发布到广场的分类，再次发布会覆盖更新之前的广场副本。</p>
        <van-cell-group class="plaza-cuisine-list">
          <van-cell
            v-for="category in publishCategories"
            :key="category.id"
            :title="category.name"
            :class="{ 'is-active': publishCategoryId === category.id }"
            @click="publishCategoryId = category.id"
          />
        </van-cell-group>
        <van-button
          class="plaza-copy-submit"
          block
          type="primary"
          :loading="publishing"
          :disabled="!publishCategoryId"
          @click="submitPublish"
        >
          发布
        </van-button>
      </div>
    </van-popup>
  </section>
</template>
