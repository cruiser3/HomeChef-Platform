<script setup>
import { computed, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { useFamilyStore } from '../store/family'
import { listOrders, deleteOrder } from '../api/order'
import StatusTag from '../components/StatusTag.vue'

const familyStore = useFamilyStore()

const orders = ref([])
const loading = ref(true)
const loadError = ref(false)
const deletingId = ref(null)

const filter = ref('active')

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

const isActive = (order) => order.status === 'CREATED' || order.status === 'PREPARING'

const activeOrders = computed(() => orders.value.filter(isActive))
const historyOrders = computed(() => orders.value.filter((order) => !isActive(order)))

const filteredOrders = computed(() =>
  filter.value === 'active' ? activeOrders.value : historyOrders.value,
)

const activeCount = computed(() => activeOrders.value.length)
const historyCount = computed(() => historyOrders.value.length)

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

function dishSummary(order) {
  return order.items.map((item) => item.dishName).join('、')
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    orders.value = await listOrders()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const isDeletable = (order) =>
  order.status === 'COMPLETED' || order.status === 'CANCELLED'

async function onDelete(order) {
  if (deletingId.value) return
  try {
    await showConfirmDialog({
      title: '删除订单',
      message: `确定删除订单 #${order.id} 吗？删除后不可恢复`,
      confirmButtonText: '删除',
      confirmButtonColor: '#ee0a24',
    })
  } catch {
    return
  }
  deletingId.value = order.id
  try {
    await deleteOrder(order.id)
    showToast('已删除')
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    deletingId.value = null
  }
}

watch(
  () => familyStore.currentFamilyId,
  () => load(),
)

load()
</script>

<template>
  <section class="order-page">
    <header class="order-head">
      <p class="eyebrow">HOME CHEF · 订单</p>
      <h1>订单与备菜</h1>
    </header>

    <van-loading v-if="loading" class="dish-loading">加载中...</van-loading>

    <div v-else-if="loadError" class="dish-error">
      <van-empty description="加载失败，请重试" />
      <van-button type="primary" @click="load">重新加载</van-button>
    </div>

    <template v-else>
      <div class="dish-view-switch order-filter">
        <button type="button" :class="{ 'is-active': filter === 'active' }" @click="filter = 'active'">
          进行中{{ activeCount ? ` ${activeCount}` : '' }}
        </button>
        <button type="button" :class="{ 'is-active': filter === 'history' }" @click="filter = 'history'">
          历史{{ historyCount ? ` ${historyCount}` : '' }}
        </button>
      </div>

      <van-empty v-if="!orders.length" description="还没有订单，去购物车下一单吧" />

      <van-empty
        v-else-if="!filteredOrders.length"
        :description="filter === 'active' ? '没有进行中的订单' : '没有历史订单'"
      />

      <div v-else class="order-list">
        <RouterLink
          v-for="order in filteredOrders"
          :key="order.id"
          class="order-card"
          :to="{ name: 'order-detail', params: { id: order.id } }"
        >
          <div class="order-card-head">
            <span class="order-card-title">
              订单 #{{ order.id }}
              <StatusTag :label="statusLabel[order.status] || order.status" :tone="statusTone[order.status] || 'neutral'" />
            </span>
            <span v-if="order.mealServed && order.status !== 'CANCELLED'" class="status-tag status-tag--success">已开饭</span>
          </div>

          <p class="order-card-meta">
            <span v-if="order.userName">下单人：{{ order.userName }}</span>
            <span v-if="order.mealPeriod">{{ order.mealPeriod }}</span>
            <span v-if="formatTime(order.createdAt)">{{ formatTime(order.createdAt) }}</span>
          </p>

          <p class="order-card-items">{{ dishSummary(order) }}</p>

          <div class="order-card-foot">
            <span>共 {{ order.itemCount }} 项</span>
            <span class="order-card-total">合计 ¥{{ formatPrice(order.totalAmount) }}</span>
            <button
              v-if="isDeletable(order)"
              type="button"
              class="order-card-delete"
              :disabled="deletingId === order.id"
              @click.prevent.stop="onDelete(order)"
            >
              {{ deletingId === order.id ? '删除中…' : '删除' }}
            </button>
          </div>
        </RouterLink>
      </div>
    </template>
  </section>
</template>
