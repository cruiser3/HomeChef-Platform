<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { useFamilyStore } from '../store/family'
import { listShoppingItems, addShoppingItem, markPurchased, deleteShoppingItem, updateShoppingItem } from '../api/shopping'
import { listOrders } from '../api/order'

const familyStore = useFamilyStore()

const items = ref([])
const orders = ref([])
const loading = ref(true)
const loadError = ref(false)
const submitting = ref(false)
const togglingId = ref('')
const deletingId = ref('')

const showAdd = ref(false)
const showUnitPicker = ref(false)
const showOrderPicker = ref(false)
const showItemPicker = ref(false)

const customUnit = ref('')

const form = reactive({
  name: '',
  amount: '',
  unit: '',
  orderId: null,
  orderItemId: null,
})

const showEdit = ref(false)
const editingId = ref('')
const savingEdit = ref(false)
const editingUnit = ref(false)
const editForm = reactive({ name: '', amount: '', unit: '' })

const unitOptions = [
  '个', '份', '包', '盒', '瓶', '罐', '袋',
  '斤', '公斤', '克', '毫升', '升', '把', '棵', '块', '片',
]

const selectedOrder = computed(() => orders.value.find((o) => o.id === form.orderId) || null)

const orderLabel = computed(() => {
  if (!form.orderId) return ''
  if (form.orderItemId) {
    const orderItem = selectedOrder.value?.items?.find((i) => i.id === form.orderItemId)
    return orderItem ? `订单 #${form.orderId} · ${orderItem.dishName}` : `订单 #${form.orderId}`
  }
  return `订单 #${form.orderId}`
})

const itemLabel = computed(() => {
  const orderItem = selectedOrder.value?.items?.find((i) => i.id === form.orderItemId)
  return orderItem ? orderItem.dishName : ''
})

function formatAmount(value) {
  return String(Number(value ?? 0))
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

function orderLabelForItem(item) {
  if (!item.orderId) return ''
  if (item.orderItemId) {
    const order = orders.value.find((o) => o.id === item.orderId)
    const orderItem = order?.items?.find((i) => i.id === item.orderItemId)
    return orderItem ? `订单 #${item.orderId} · ${orderItem.dishName}` : `订单 #${item.orderId}`
  }
  return `订单 #${item.orderId}`
}

function isValidAmount(value) {
  const str = String(value).trim()
  if (!str || !/^\d+(\.\d{1,2})?$/.test(str)) return false
  const n = Number(str)
  return Number.isFinite(n) && n > 0
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    items.value = await listShoppingItems()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }

  try {
    orders.value = await listOrders()
  } catch {
    orders.value = []
  }
}

function openAdd() {
  resetForm()
  showAdd.value = true
}

function openAddUnit() {
  editingUnit.value = false
  showUnitPicker.value = true
}

function resetForm() {
  form.name = ''
  form.amount = ''
  form.unit = ''
  form.orderId = null
  form.orderItemId = null
}

function pickUnit(unit) {
  if (editingUnit.value) {
    editForm.unit = unit
  } else {
    form.unit = unit
  }
  showUnitPicker.value = false
}

function confirmCustomUnit() {
  const unit = customUnit.value.trim()
  if (unit) {
    if (editingUnit.value) editForm.unit = unit
    else form.unit = unit
  }
  customUnit.value = ''
  showUnitPicker.value = false
}

function openOrderPicker() {
  showOrderPicker.value = true
}

function pickOrder(order) {
  form.orderId = order.id
  form.orderItemId = null
  showOrderPicker.value = false
}

function clearOrder() {
  form.orderId = null
  form.orderItemId = null
  showOrderPicker.value = false
}

function openItemPicker() {
  showItemPicker.value = true
}

function pickItem(orderItem) {
  form.orderItemId = orderItem.id
  showItemPicker.value = false
}

function clearItem() {
  form.orderItemId = null
  showItemPicker.value = false
}

async function onSubmit() {
  const name = form.name.trim()
  if (!name) {
    showToast('请输入食材名称')
    return
  }
  if (!isValidAmount(form.amount)) {
    showToast('请输入正确的数量（大于 0，最多 2 位小数）')
    return
  }
  const unit = form.unit.trim()
  if (!unit) {
    showToast('请选择或输入单位')
    return
  }
  if (submitting.value) return

  submitting.value = true
  try {
    const payload = { name, amount: Number(form.amount), unit }
    if (form.orderId) payload.orderId = form.orderId
    if (form.orderItemId) payload.orderItemId = form.orderItemId

    await addShoppingItem(payload)
    showToast('已添加')
    showAdd.value = false
    resetForm()
    await load()
  } catch (error) {
    if (error?.fieldErrors) {
      const first = Object.values(error.fieldErrors)[0]
      showToast(first || '请检查填写内容')
    }
  } finally {
    submitting.value = false
  }
}

async function onTogglePurchased(item) {
  if (togglingId.value) return
  togglingId.value = item.id
  try {
    await markPurchased(item.id, !item.purchased)
    showToast(item.purchased ? '已标记为待采购' : '已标记为已采购')
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    togglingId.value = ''
  }
}

async function onDelete(item) {
  try {
    await showConfirmDialog({
      title: '删除采购项',
      message: `确定删除「${item.name}」吗？`,
      confirmButtonText: '删除',
      confirmButtonColor: '#ee0a24',
    })
  } catch {
    return
  }
  deletingId.value = item.id
  try {
    await deleteShoppingItem(item.id)
    showToast('已删除')
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    deletingId.value = ''
  }
}

function openEdit(item) {
  editingId.value = item.id
  editForm.name = item.name
  editForm.amount = formatAmount(item.amount)
  editForm.unit = item.unit
  showEdit.value = true
}

function openEditUnit() {
  editingUnit.value = true
  showUnitPicker.value = true
}

async function onSaveEdit() {
  const name = editForm.name.trim()
  if (!name) {
    showToast('请输入食材名称')
    return
  }
  if (!isValidAmount(editForm.amount)) {
    showToast('请输入正确的数量（大于 0，最多 2 位小数）')
    return
  }
  const unit = editForm.unit.trim()
  if (!unit) {
    showToast('请选择或输入单位')
    return
  }
  if (savingEdit.value) return

  savingEdit.value = true
  try {
    await updateShoppingItem(editingId.value, {
      name,
      amount: Number(editForm.amount),
      unit,
    })
    showToast('已保存')
    showEdit.value = false
    await load()
  } catch (error) {
    if (error?.fieldErrors) {
      const first = Object.values(error.fieldErrors)[0]
      showToast(first || '请检查填写内容')
    }
  } finally {
    savingEdit.value = false
  }
}

watch(
  () => familyStore.currentFamilyId,
  () => load(),
)

load()
</script>

<template>
  <section class="shopping-page">
    <header class="shopping-head">
      <div>
        <p class="eyebrow">HOME CHEF · 采购清单</p>
        <h1>缺什么，记下来</h1>
      </div>
      <van-button type="primary" icon="plus" @click="openAdd">新增食材</van-button>
    </header>

    <van-loading v-if="loading" class="dish-loading">加载中...</van-loading>

    <div v-else-if="loadError" class="dish-error">
      <van-empty description="加载失败，请重试" />
      <van-button type="primary" @click="load">重新加载</van-button>
    </div>

    <van-empty v-else-if="!items.length" description="清单是空的，发现缺食材就记一笔" />

    <div v-else class="shopping-list">
      <div v-for="item in items" :key="item.id" class="shopping-item" :class="{ 'is-purchased': item.purchased }">
        <div class="shopping-item-main">
          <span class="shopping-item-name">{{ item.name }}</span>
          <span class="shopping-item-amount">{{ formatAmount(item.amount) }} {{ item.unit }}</span>
        </div>
        <div class="shopping-item-meta">
          <span v-if="orderLabelForItem(item)" class="shopping-item-order">关联 {{ orderLabelForItem(item) }}</span>
          <span v-if="formatTime(item.createdAt)">{{ formatTime(item.createdAt) }}</span>
        </div>
        <div class="shopping-item-foot">
          <van-tag :type="item.purchased ? 'success' : 'primary'" plain>
            {{ item.purchased ? '已采购' : '待采购' }}
          </van-tag>
          <div class="shopping-item-actions">
            <van-button
              size="mini"
              :type="item.purchased ? 'default' : 'success'"
              :loading="togglingId === item.id"
              @click="onTogglePurchased(item)"
            >
              {{ item.purchased ? '撤销' : '标记已采购' }}
            </van-button>
            <van-button size="mini" @click="openEdit(item)">编辑</van-button>
            <van-button
              size="mini"
              type="danger"
              plain
              :loading="deletingId === item.id"
              @click="onDelete(item)"
            >
              删除
            </van-button>
          </div>
        </div>
      </div>
    </div>

    <van-popup v-model:show="showAdd" round position="bottom">
      <div class="add-sheet">
        <h2>新增采购食材</h2>
        <van-field v-model="form.name" label="名称" placeholder="如：鸡蛋" maxlength="100" />
        <van-field v-model="form.amount" label="数量" type="number" placeholder="如：1 或 0.5" />
        <van-field
          :model-value="form.unit"
          label="单位"
          placeholder="选择或输入单位"
          readonly
          is-link
          @click="openAddUnit"
        />
        <van-field
          :model-value="orderLabel"
          label="关联订单"
          placeholder="可不关联"
          readonly
          is-link
          @click="openOrderPicker"
        />
        <van-field
          v-if="form.orderId"
          :model-value="itemLabel"
          label="关联菜品"
          placeholder="可不关联"
          readonly
          is-link
          @click="openItemPicker"
        />

        <div class="add-actions">
          <van-button block type="primary" :loading="submitting" @click="onSubmit">添加</van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showEdit" round position="bottom">
      <div class="add-sheet">
        <h2>编辑采购食材</h2>
        <van-field v-model="editForm.name" label="名称" placeholder="如：鸡蛋" maxlength="100" />
        <van-field v-model="editForm.amount" label="数量" type="number" placeholder="如：1 或 0.5" />
        <van-field
          :model-value="editForm.unit"
          label="单位"
          placeholder="选择或输入单位"
          readonly
          is-link
          @click="openEditUnit"
        />
        <div class="add-actions">
          <van-button block type="primary" :loading="savingEdit" @click="onSaveEdit">保存</van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showUnitPicker" round position="bottom">
      <div class="unit-sheet">
        <h2>选择单位</h2>
        <van-field v-model="customUnit" placeholder="自定义单位（如：小把）" maxlength="20" />
        <div class="shop-unit-grid">
          <button
            v-for="unit in unitOptions"
            :key="unit"
            class="shop-unit-chip"
            type="button"
            @click="pickUnit(unit)"
          >
            {{ unit }}
          </button>
        </div>
        <van-button block type="primary" @click="confirmCustomUnit">使用自定义单位</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showOrderPicker" round position="bottom">
      <div class="order-sheet">
        <h2>选择关联订单</h2>
        <van-cell-group>
          <van-cell title="不关联订单" @click="clearOrder" />
          <van-cell
            v-for="order in orders"
            :key="order.id"
            :title="`订单 #${order.id}`"
            :label="`${order.userName || ''} · ${order.itemCount} 项`"
            @click="pickOrder(order)"
          />
        </van-cell-group>
        <van-empty v-if="!orders.length" description="暂无订单" />
      </div>
    </van-popup>

    <van-popup v-model:show="showItemPicker" round position="bottom">
      <div class="order-sheet">
        <h2>选择关联菜品</h2>
        <van-cell-group>
          <van-cell title="不关联菜品" @click="clearItem" />
          <van-cell
            v-for="orderItem in selectedOrder?.items || []"
            :key="orderItem.id"
            :title="orderItem.dishName"
            :label="`¥${formatAmount(orderItem.unitPrice)} × ${orderItem.quantity}`"
            @click="pickItem(orderItem)"
          />
        </van-cell-group>
      </div>
    </van-popup>
  </section>
</template>
