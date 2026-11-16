<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useFamilyStore } from '../store/family'
import {
  getOrder,
  cancelOrder,
  startPreparing,
  markMealServed,
  updateCookStatus,
  assignResponsible,
} from '../api/order'
import { listFamilyMembers } from '../api/family'
import { addShoppingItem } from '../api/shopping'
import { getMyProfile } from '../api/user'
import StatusTag from '../components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const familyStore = useFamilyStore()

const order = ref(null)
const members = ref([])
const loading = ref(true)
const loadError = ref(false)
const actionLoading = ref('')
const myId = ref(null)

const statusLabel = {
  CREATED: '已下单',
  PREPARING: '备菜中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
const statusTone = {
  CREATED: 'neutral',
  PREPARING: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'danger',
}

const cookStatusLabel = {
  PENDING: '待备菜',
  COOKING: '烹饪中',
  READY: '已就绪',
  BLOCKED: '缺食材',
  SERVED: '已上桌',
}
const cookStatusTone = {
  PENDING: 'neutral',
  COOKING: 'warm',
  READY: 'primary',
  BLOCKED: 'danger',
  SERVED: 'success',
}

const cookNext = {
  PENDING: ['COOKING', 'BLOCKED'],
  COOKING: ['READY', 'BLOCKED'],
  READY: ['SERVED', 'BLOCKED'],
  BLOCKED: ['PENDING', 'COOKING', 'READY'],
  SERVED: [],
}
const cookTargetLabel = {
  COOKING: '开始烹饪',
  READY: '做好了',
  SERVED: '上桌',
  BLOCKED: '缺食材',
  PENDING: '恢复待备',
}

const myUserId = computed(() => myId.value)

const canCancel = computed(() => {
  if (!order.value) return false
  return (
    order.value.status === 'CREATED' &&
    order.value.userId === myUserId.value &&
    order.value.items.every((item) => item.cookStatus === 'PENDING')
  )
})

const showStartPreparing = computed(() => order.value?.status === 'CREATED')
const showMealServed = computed(
  () => order.value && !order.value.mealServed && order.value.status !== 'CANCELLED',
)

const blockPopup = ref(false)
const blockSubmitting = ref(false)
const blockItemId = ref(null)
const blockReason = ref('')
const blockIngredient = ref('')
const blockIngredientAmount = ref('')
const blockIngredientUnit = ref('')

const responsiblePopup = ref(false)
const responsibleItemId = ref(null)

function formatPrice(value) {
  return Number(value ?? 0).toFixed(2)
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

function roleLabel(role) {
  return role === 'ADMIN' ? '管理员' : '成员'
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const [orderData, memberData] = await Promise.all([
      getOrder(route.params.id),
      listFamilyMembers(familyStore.currentFamilyId),
    ])
    order.value = orderData
    members.value = memberData
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
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

function onBack() {
  router.push({ name: 'orders' })
}

async function onStartPreparing() {
  if (actionLoading.value) return
  actionLoading.value = 'start'
  try {
    order.value = await startPreparing(order.value.id)
    showToast('已开始备菜')
  } catch {
    // 拦截器已提示
  } finally {
    actionLoading.value = ''
  }
}

async function onMealServed() {
  if (actionLoading.value) return
  actionLoading.value = 'served'
  try {
    order.value = await markMealServed(order.value.id)
    showToast('已开饭')
  } catch {
    // 拦截器已提示
  } finally {
    actionLoading.value = ''
  }
}

async function onCancel() {
  try {
    await showConfirmDialog({ title: '取消订单', message: '确定要取消这笔订单吗？' })
  } catch {
    return
  }
  if (actionLoading.value) return
  actionLoading.value = 'cancel'
  try {
    order.value = await cancelOrder(order.value.id)
    showToast('已取消')
  } catch {
    // 拦截器已提示
  } finally {
    actionLoading.value = ''
  }
}

function onCook(target, item) {
  if (target === 'BLOCKED') {
    blockItemId.value = item.id
    blockReason.value = item.blockedReason || ''
    blockIngredient.value = ''
    blockIngredientAmount.value = ''
    blockIngredientUnit.value = ''
    blockPopup.value = true
    return
  }
  doUpdateCookStatus(item.id, target)
}

async function doUpdateCookStatus(itemId, status, blockedReason = null) {
  if (actionLoading.value) return
  actionLoading.value = `cook-${itemId}`
  try {
    const payload = { status }
    if (blockedReason) payload.blockedReason = blockedReason
    order.value = await updateCookStatus(itemId, payload)
    showToast('已更新')
  } catch {
    // 拦截器已提示
  } finally {
    actionLoading.value = ''
  }
}

async function confirmBlock() {
  if (blockSubmitting.value) return
  const reason = blockReason.value.trim()
  if (!reason) {
    showToast('请填写缺食材原因')
    return
  }

  const ingredientName = blockIngredient.value.trim()
  let amount = null
  if (ingredientName) {
    const rawAmount = blockIngredientAmount.value.trim()
    if (rawAmount) {
      amount = Number(rawAmount)
      if (!Number.isFinite(amount) || amount <= 0) {
        showToast('食材数量需大于 0')
        return
      }
    }
  }
  const unit = blockIngredientUnit.value.trim() || '份'

  blockSubmitting.value = true
  blockPopup.value = false
  const itemId = blockItemId.value
  try {
    await doUpdateCookStatus(itemId, 'BLOCKED', reason)

    if (ingredientName) {
      try {
        await addShoppingItem({
          name: ingredientName,
          amount: amount || 1,
          unit,
          orderId: order.value.id,
          orderItemId: itemId,
        })
        showToast('已记入采购清单')
      } catch {
        // 采购清单添加失败不影响缺食材标记；拦截器已提示
      }
    }
  } finally {
    blockSubmitting.value = false
    blockIngredient.value = ''
    blockIngredientAmount.value = ''
    blockIngredientUnit.value = ''
  }
}

function openResponsible(item) {
  responsibleItemId.value = item.id
  responsiblePopup.value = true
}

async function onAssignResponsible(member) {
  responsiblePopup.value = false
  if (actionLoading.value) return
  actionLoading.value = `resp-${responsibleItemId.value}`
  try {
    order.value = await assignResponsible(responsibleItemId.value, {
      responsibleUserId: member.userId,
    })
    showToast('已指定负责人')
  } catch {
    // 拦截器已提示
  } finally {
    actionLoading.value = ''
  }
}

load()
loadMyId()
</script>

<template>
  <section class="order-detail">
    <van-loading v-if="loading" class="dish-loading">加载中...</van-loading>

    <div v-else-if="loadError" class="dish-error">
      <van-empty description="订单加载失败，请稍后重试" />
      <van-button type="primary" plain @click="load">重新加载</van-button>
    </div>

    <template v-else-if="order">
      <button class="dish-back" type="button" @click="onBack">
        <van-icon name="arrow-left" /> 返回订单
      </button>

      <header class="order-detail-head">
        <div class="order-detail-title">
          <h1>订单 #{{ order.id }}</h1>
          <StatusTag :label="statusLabel[order.status] || order.status" :tone="statusTone[order.status] || 'neutral'" />
        </div>
        <p class="order-detail-meta">
          <span v-if="order.mealPeriod">{{ order.mealPeriod }}</span>
          <span v-if="order.userName">下单人：{{ order.userName }}</span>
          <span v-if="formatTime(order.createdAt)">{{ formatTime(order.createdAt) }}</span>
          <span v-if="order.mealServed && order.status !== 'CANCELLED'">已开饭</span>
          <span v-if="order.status === 'CANCELLED'">已取消</span>
        </p>
        <p v-if="order.remark" class="order-detail-remark">备注：{{ order.remark }}</p>
      </header>

      <div class="order-actions">
        <van-button
          v-if="showStartPreparing"
          type="primary"
          :loading="actionLoading === 'start'"
          @click="onStartPreparing"
        >
          开始备菜
        </van-button>
        <van-button
          v-if="showMealServed"
          type="success"
          :loading="actionLoading === 'served'"
          @click="onMealServed"
        >
          开饭
        </van-button>
        <van-button
          v-if="canCancel"
          type="danger"
          plain
          :loading="actionLoading === 'cancel'"
          @click="onCancel"
        >
          取消订单
        </van-button>
      </div>

      <section class="order-detail-section">
        <h2>菜品明细</h2>
        <ul class="order-item-list">
          <li v-for="item in order.items" :key="item.id" class="order-item">
            <div class="order-item-main">
              <span class="order-item-name">{{ item.dishName }}</span>
              <StatusTag
                :label="cookStatusLabel[item.cookStatus] || item.cookStatus"
                :tone="cookStatusTone[item.cookStatus] || 'neutral'"
              />
            </div>
            <div class="order-item-sub">
              <span>¥{{ formatPrice(item.unitPrice) }} × {{ item.quantity }}</span>
              <span class="order-item-subtotal">¥{{ formatPrice(item.subtotal) }}</span>
            </div>

            <p v-if="item.blockedReason" class="order-item-blocked-reason">
              缺食材：{{ item.blockedReason }}
            </p>

            <div class="order-item-cook-row">
              <div class="order-item-responsible-line">
                <span>负责人：{{ item.responsibleUserName || '未指定' }}</span>
                <button
                  v-if="order.status !== 'CANCELLED'"
                  class="order-item-responsible-btn"
                  type="button"
                  @click="openResponsible(item)"
                >
                  指定
                </button>
              </div>

              <div v-if="order.status !== 'CANCELLED' && cookNext[item.cookStatus]?.length" class="cook-controls">
                <button
                  v-for="target in cookNext[item.cookStatus]"
                  :key="target"
                  class="cook-action-btn"
                  :class="{ 'is-blocked': target === 'BLOCKED' }"
                  type="button"
                  :disabled="Boolean(actionLoading)"
                  @click="onCook(target, item)"
                >
                  {{ cookTargetLabel[target] }}
                </button>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <div class="order-detail-total">
        <span>共 {{ order.itemCount }} 项</span>
        <span class="cart-total">合计 ¥{{ formatPrice(order.totalAmount) }}</span>
      </div>
    </template>

    <van-empty v-else description="订单不存在" />

    <van-popup v-model:show="blockPopup" round position="bottom">
      <div class="block-sheet">
        <h2>为什么缺食材？</h2>
        <van-field
          v-model="blockReason"
          type="textarea"
          rows="2"
          placeholder="说明缺少的食材或原因"
          maxlength="200"
        />
        <p class="block-ingredient-hint">顺手记到采购清单（选填）</p>
        <van-field v-model="blockIngredient" label="缺的食材" placeholder="如：鸡蛋" maxlength="100" />
        <div class="block-ingredient-row">
          <van-field v-model="blockIngredientAmount" label="数量" type="number" placeholder="默认 1" />
          <van-field v-model="blockIngredientUnit" label="单位" placeholder="默认 份" maxlength="20" />
        </div>
        <van-button class="block-submit" block type="primary" :loading="blockSubmitting" @click="confirmBlock">
          标记为缺食材
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="responsiblePopup" round position="bottom">
      <div class="responsible-sheet">
        <h2>选择负责人</h2>
        <van-cell-group class="responsible-list">
          <van-cell
            v-for="member in members"
            :key="member.userId"
            :title="member.name"
            :label="roleLabel(member.role)"
            @click="onAssignResponsible(member)"
          />
        </van-cell-group>
      </div>
    </van-popup>
  </section>
</template>
