<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getSystemDish, copySystemDishToFamily } from '../api/systemDish'

const route = useRoute()
const router = useRouter()

const dish = ref(null)
const loading = ref(true)
const loadError = ref(false)

const copyPopup = ref(false)
const price = ref('')
const overrides = ref([])
const copying = ref(false)

const needsAmount = computed(() =>
  (dish.value?.ingredients || []).filter(
    (item) => item.amount == null || item.unit == null || !item.unit.trim(),
  ),
)

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
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function onBack() {
  router.push({ name: 'plaza' })
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

loadDetail()
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
          <span v-if="dish.cookingMinutes != null">{{ dish.cookingMinutes }} 分钟</span>
          <span v-if="dish.calories != null">{{ dish.calories }} 千卡</span>
        </p>
        <p v-if="dish.description" class="dish-detail-desc">{{ dish.description }}</p>
      </header>

      <div class="dish-detail-actions">
        <van-button block type="primary" @click="openCopy">复制到我家</van-button>
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
  </section>
</template>
