<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useCartStore } from '../store/cart'
import { useFamilyStore } from '../store/family'
import { createOrder } from '../api/order'
import { getMyProfile } from '../api/user'
import { restrictionsToText, allergiesToText } from '../constants/dietary'
import { analyzeCart } from '../api/ai'

const router = useRouter()
const cartStore = useCartStore()
const familyStore = useFamilyStore()

const mealPeriod = ref('')
const remark = ref('')
const remarkHint = ref('')
const submitting = ref(false)
const loadError = ref(false)
const removingId = ref('')
const clearing = ref(false)
const showAiAnalyze = ref(false)
const analyzing = ref(false)
const aiResult = ref(null)
const includeHealth = ref(false)

const mealPeriods = ['早餐', '午餐', '晚餐', '夜宵']

function formatPrice(value) {
  return Number(value ?? 0).toFixed(2)
}

async function loadCart() {
  loadError.value = false
  try {
    await cartStore.loadCart()
  } catch {
    loadError.value = true
  }
}

async function prefillRemark() {
  try {
    const profile = await getMyProfile()
    const parts = [restrictionsToText(profile.dietaryRestrictions), allergiesToText(profile.allergies)].filter(Boolean)
    const text = parts.join('；')
    if (text && !remark.value) {
      remark.value = text
      remarkHint.value = '已根据你的忌口与过敏自动填入备注，可编辑'
    }
  } catch {
    // 预填失败不影响下单，静默忽略
  }
}

async function onQuantityChange(item, quantity) {
  try {
    await cartStore.updateItem(item.id, quantity)
  } catch {
    await loadCart()
  }
}

async function onRemoveItem(item) {
  if (removingId.value) return
  removingId.value = item.id
  try {
    await cartStore.removeItem(item.id)
    showToast('已删除')
  } catch {
    // 拦截器已提示
  } finally {
    removingId.value = ''
  }
}

async function onClear() {
  if (clearing.value) return
  try {
    await showConfirmDialog({ title: '清空购物车', message: '确定要清空购物车吗？' })
  } catch {
    return
  }
  clearing.value = true
  try {
    await cartStore.clear()
    showToast('已清空')
  } catch {
    // 拦截器已提示
  } finally {
    clearing.value = false
  }
}

async function onSubmitOrder() {
  if (cartStore.isEmpty) {
    showToast('购物车是空的')
    return
  }
  if (submitting.value) return

  submitting.value = true
  try {
    const order = await createOrder({
      mealPeriod: mealPeriod.value || null,
      remark: remark.value.trim() || null,
    })
    showToast('下单成功')
    mealPeriod.value = ''
    remark.value = ''
    await loadCart()
    router.push({ name: 'order-detail', params: { id: order.id } })
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function runAiAnalyze() {
  if (analyzing.value) return
  analyzing.value = true
  try {
    aiResult.value = await analyzeCart({ includeHealth: includeHealth.value })
  } catch {
    aiResult.value = null
  } finally {
    analyzing.value = false
  }
}

function openAiAnalyze() {
  if (cartStore.isEmpty) {
    showToast('购物车是空的')
    return
  }
  showAiAnalyze.value = true
  runAiAnalyze()
}

function onHealthToggle() {
  runAiAnalyze()
}

watch(
  () => familyStore.currentFamilyId,
  () => {
    mealPeriod.value = ''
    remark.value = ''
    remarkHint.value = ''
    loadCart()
    prefillRemark()
  },
)

loadCart()
prefillRemark()
</script>

<template>
  <section class="cart-page">
    <header class="cart-head">
      <p class="eyebrow">HOME CHEF · 购物车</p>
      <h1>我的购物车</h1>
    </header>

    <van-loading v-if="cartStore.loading" class="dish-loading">加载中...</van-loading>

    <div v-else-if="loadError" class="cart-error">
      <van-empty description="购物车加载失败，请稍后重试" />
      <van-button type="primary" plain @click="loadCart">重新加载</van-button>
    </div>

    <div v-else-if="cartStore.isEmpty" class="cart-empty">
      <van-empty description="购物车是空的，去小家加购菜品吧" />
      <RouterLink class="primary-link" :to="{ name: 'dishes' }">去逛逛菜品</RouterLink>
    </div>

    <template v-else>
      <div class="cart-list">
        <div
          v-for="item in cartStore.items"
          :key="item.id"
          class="cart-item"
          :class="{ 'is-unavailable': !item.dishAvailable }"
        >
          <van-image :src="item.imageUrl || ''" fit="cover" class="cart-item-image">
            <template #error>
              <span class="dish-card-fallback">{{ item.dishName?.charAt(0) || '菜' }}</span>
            </template>
          </van-image>

          <div class="cart-item-info">
            <h3>{{ item.dishName }}</h3>
            <p class="cart-item-price">¥{{ formatPrice(item.unitPrice) }} / 份</p>
            <p v-if="!item.dishAvailable" class="cart-item-unavailable">已下架，不可下单</p>
          </div>

          <div class="cart-item-side">
            <van-stepper
              :model-value="item.quantity"
              min="1"
              max="99"
              @change="(val) => onQuantityChange(item, val)"
            />
            <p class="cart-item-subtotal">¥{{ formatPrice(item.subtotal) }}</p>
            <button class="cart-item-remove" type="button" :disabled="removingId === item.id" @click="onRemoveItem(item)">{{ removingId === item.id ? '删除中...' : '删除' }}</button>
          </div>
        </div>
      </div>

      <div class="cart-summary">
        <span>共 {{ cartStore.itemCount }} 项</span>
        <span class="cart-total">合计 ¥{{ formatPrice(cartStore.totalAmount) }}</span>
        <van-button size="small" type="danger" plain :loading="clearing" @click="onClear">清空</van-button>
      </div>

      <div class="cart-ai">
        <van-button block plain type="primary" icon="bulb-o" @click="openAiAnalyze">AI 整餐分析</van-button>
      </div>

      <div class="cart-submit">
        <h2>提交订单</h2>
        <div class="meal-period-group">
          <span class="meal-period-label">餐次</span>
          <div class="meal-period-options">
            <button
              v-for="period in mealPeriods"
              :key="period"
              type="button"
              class="dish-category-chip"
              :class="{ 'is-active': mealPeriod === period }"
              @click="mealPeriod = mealPeriod === period ? '' : period"
            >
              {{ period }}
            </button>
          </div>
        </div>
        <p v-if="remarkHint" class="cart-remark-hint">{{ remarkHint }}</p>
        <van-field
          v-model="remark"
          type="textarea"
          rows="2"
          placeholder="给大厨的备注（可选）"
          maxlength="500"
        />
        <van-button
          class="cart-submit-btn"
          block
          type="primary"
          :loading="submitting"
          loading-text="提交中..."
          @click="onSubmitOrder"
        >
          提交订单 · ¥{{ formatPrice(cartStore.totalAmount) }}
        </van-button>
      </div>
    </template>

    <van-popup v-model:show="showAiAnalyze" round position="bottom">
      <div class="dish-category-sheet ai-analyze-sheet">
        <h2>整餐营养分析</h2>
        <p class="dish-edit-hint">根据当前购物车菜品估算热量与营养，并给出搭配建议。</p>

        <div class="ai-analyze-toggle">
          <van-checkbox v-model="includeHealth" @change="onHealthToggle">结合我的健康档案</van-checkbox>
          <p class="ai-analyze-disclaimer">建议仅供饮食参考，不构成医疗建议。</p>
        </div>

        <van-loading v-if="analyzing" class="ai-analyze-loading">AI 分析中（首次约十几秒）...</van-loading>

        <div v-else-if="aiResult" class="ai-analyze-result">
          <div v-if="aiResult.summary" class="ai-analyze-block">
            <h3>整体评价</h3>
            <p>{{ aiResult.summary }}</p>
          </div>
          <div v-if="aiResult.nutrition" class="ai-analyze-block">
            <h3>营养概况</h3>
            <p>{{ aiResult.nutrition }}</p>
          </div>
          <div v-if="aiResult.suggestions && aiResult.suggestions.length" class="ai-analyze-block">
            <h3>搭配建议</h3>
            <ul class="ai-analyze-list">
              <li v-for="(s, i) in aiResult.suggestions" :key="i">{{ s }}</li>
            </ul>
          </div>
          <div v-if="aiResult.cautions && aiResult.cautions.length" class="ai-analyze-block">
            <h3>注意事项</h3>
            <ul class="ai-analyze-list">
              <li v-for="(c, i) in aiResult.cautions" :key="i">{{ c }}</li>
            </ul>
          </div>
        </div>

        <div v-else class="ai-analyze-failed">
          <p>分析失败，可能是 AI 服务不可用或购物车为空。</p>
          <van-button size="small" plain type="primary" @click="runAiAnalyze">重试</van-button>
        </div>
      </div>
    </van-popup>
  </section>
</template>
