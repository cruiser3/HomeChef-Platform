<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  createDish, updateDish, getDish, listDishCategories, createDishCategory,
} from '../api/dish'
import { listCuisines, createCuisine } from '../api/cuisine'
import { uploadFile } from '../api/upload'
import { parseRecipe, analyzeCalories as analyzeDishCalories } from '../api/ai'

// 「未分类」是后端虚拟分类（sort_no=1001），不能手动选择，编辑下拉需排除。
const UNCATEGORIZED_SORT_NO = 1001

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => Boolean(route.params.id))
const loading = ref(false)
const loadError = ref(false)
const submitting = ref(false)

const form = reactive({
  name: '',
  description: '',
  imageUrl: '',
  categoryId: null,
  categoryName: '',
  cuisineId: null,
  cuisineName: '',
  cookingMinutes: '',
  calories: '',
  price: '',
  ingredients: [],
  steps: [],
})

const categories = ref([])
const showCategoryPicker = ref(false)
const showCategoryCreate = ref(false)
const creatingCategory = ref(false)
const newCategoryName = ref('')

const cuisines = ref([])
const showCuisinePicker = ref(false)
const showCuisineCreate = ref(false)
const creatingCuisine = ref(false)
const newCuisineName = ref('')
const aiAnalyzingCalories = ref(false)

const unitOptions = ['适量', '少许', 'g', 'kg', '斤', '两', 'ml', 'L', '个', '根', '只', '条', '块', '片', '张', '颗', '粒', '瓣', '朵', '棵', '把', '束', '勺', '匙', '杯', '碗', '瓶', '罐', '盒', '袋', '包', '头', '段', '滴', '撮', '盘']
const showUnitPicker = ref(false)
const unitPickerIndex = ref(-1)
const customUnit = ref('')

function openUnitPicker(index) {
  unitPickerIndex.value = index
  customUnit.value = ''
  showUnitPicker.value = true
}

function applyUnit(unit) {
  if (unitPickerIndex.value >= 0) {
    form.ingredients[unitPickerIndex.value].unit = unit
  }
  showUnitPicker.value = false
}

function applyCustomUnit() {
  const unit = customUnit.value.trim()
  if (!unit) {
    showToast('请输入单位')
    return
  }
  applyUnit(unit)
}

async function loadCategories() {
  try {
    categories.value = await listDishCategories()
  } catch {
    // 拦截器已提示
  }
}

async function loadCuisines() {
  try {
    const all = await listCuisines()
    cuisines.value = all.filter((c) => c.sortNo !== UNCATEGORIZED_SORT_NO)
  } catch {
    // 拦截器已提示
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const dish = await getDish(route.params.id)
    form.name = dish.name || ''
    form.description = dish.description || ''
    form.imageUrl = dish.imageUrl || ''
    form.categoryId = dish.categoryId ?? null
    form.categoryName = dish.categoryName || ''
    form.cuisineId = dish.cuisineId ?? null
    form.cuisineName = dish.cuisineName || ''
    form.cookingMinutes = dish.cookingMinutes ?? ''
    form.calories = dish.calories ?? ''
    form.price = dish.price ?? ''
    form.ingredients = (dish.ingredients || []).map((i) => ({
      name: i.name, amount: i.amount ?? '', unit: i.unit,
    }))
    form.steps = (dish.steps || []).map((s) => ({
      content: s.content, imageUrl: s.imageUrl || '',
    }))
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function onSelectCategory(category) {
  form.categoryId = category.id
  form.categoryName = category.name
  showCategoryPicker.value = false
}

function onClearCategory() {
  form.categoryId = null
  form.categoryName = ''
  showCategoryPicker.value = false
}

function onSelectCuisine(cuisine) {
  form.cuisineId = cuisine.id
  form.cuisineName = cuisine.name
  showCuisinePicker.value = false
}

function onClearCuisine() {
  form.cuisineId = null
  form.cuisineName = ''
  showCuisinePicker.value = false
}

async function onSubmitNewCuisine() {
  const name = newCuisineName.value.trim()
  if (!name) {
    showToast('请输入菜系名称')
    return
  }
  if (name.length > 50) {
    showToast('菜系名称不超过 50 字')
    return
  }
  if (creatingCuisine.value) return

  creatingCuisine.value = true
  try {
    const cuisine = await createCuisine({ name })
    newCuisineName.value = ''
    showCuisineCreate.value = false
    showToast('菜系已创建')
    await loadCuisines()
    form.cuisineId = cuisine.id
    form.cuisineName = cuisine.name
    showCuisinePicker.value = false
  } catch (error) {
    if (error?.fieldErrors) {
      showToast(Object.values(error.fieldErrors)[0] || '请检查填写内容')
    }
  } finally {
    creatingCuisine.value = false
  }
}

async function onSubmitNewCategory() {
  const name = newCategoryName.value.trim()
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
    const category = await createDishCategory({ name })
    newCategoryName.value = ''
    showCategoryCreate.value = false
    showToast('分类已创建')
    await loadCategories()
    form.categoryId = category.id
    form.categoryName = category.name
    showCategoryPicker.value = false
  } catch (error) {
    if (error?.fieldErrors) {
      showToast(Object.values(error.fieldErrors)[0] || '请检查填写内容')
    }
  } finally {
    creatingCategory.value = false
  }
}

function addIngredient() {
  form.ingredients.push({ name: '', amount: '', unit: '' })
}

function removeIngredient(index) {
  form.ingredients.splice(index, 1)
}

function addStep() {
  form.steps.push({ content: '', imageUrl: '' })
}

function removeStep(index) {
  form.steps.splice(index, 1)
}

const showStepPaste = ref(false)
const stepPasteText = ref('')

function parseRecipeSteps(text) {
  if (!text) return []
  const lines = text.split(/\r?\n/)
  const steps = []
  let current = null
  const headRe = /^\s*(?:\d+|[一二三四五六七八九十百]+)\s*[.、)）:：．]\s*(.*)$/
  for (const raw of lines) {
    const m = raw.match(headRe)
    if (m) {
      current = { text: m[1].trim() }
      steps.push(current)
    } else if (current) {
      const line = raw.trim()
      if (line) current.text = current.text ? current.text + '\n' + line : line
    }
  }
  return steps.map((s) => s.text.trim()).filter((s) => s !== '')
}

const parsedSteps = computed(() => parseRecipeSteps(stepPasteText.value))

function openStepPaste() {
  stepPasteText.value = ''
  showStepPaste.value = true
}

function applyParsedSteps() {
  const steps = parsedSteps.value
  if (!steps.length) {
    showToast('未识别到步骤，请粘贴带编号的文本')
    return
  }
  steps.forEach((content) => form.steps.push({ content, imageUrl: '' }))
  showToast(`已添加 ${steps.length} 步`)
  stepPasteText.value = ''
  showStepPaste.value = false
}

const showAiParse = ref(false)
const aiParseText = ref('')
const aiParsing = ref(false)

function openAiParse() {
  aiParseText.value = ''
  showAiParse.value = true
}

async function applyAiParse() {
  const text = aiParseText.value.trim()
  if (!text) {
    showToast('请粘贴菜谱内容')
    return
  }
  if (aiParsing.value) return
  aiParsing.value = true
  try {
    const result = await parseRecipe(text)
    const addedIngredients = (result.ingredients || [])
      .filter((i) => i.name && String(i.name).trim())
      .map((i) => ({ name: i.name.trim(), amount: i.amount ?? '', unit: (i.unit || '').trim() }))
    const addedSteps = (result.steps || []).map((content) => ({ content, imageUrl: '' }))

    if (!addedIngredients.length && !addedSteps.length && !result.name) {
      showToast('AI 未识别到内容，请换一段菜谱')
      return
    }
    if (!form.name.trim() && result.name) {
      form.name = result.name
    }
    form.ingredients.push(...addedIngredients)
    form.steps.push(...addedSteps)
    showToast(`已识别 ${addedIngredients.length} 项食材、${addedSteps.length} 步`)
    aiParseText.value = ''
    showAiParse.value = false
  } catch {
    // 拦截器已提示
  } finally {
    aiParsing.value = false
  }
}

const stepImageInputs = []
const uploadingStepIndex = ref(-1)

function pickStepImage(index) {
  stepImageInputs[index]?.click()
}

async function onStepImageChange(event, index) {
  const file = event.target.files?.[0]
  if (!file || uploadingStepIndex.value >= 0) return
  uploadingStepIndex.value = index
  try {
    const { url } = await uploadFile(file)
    form.steps[index].imageUrl = url
    showToast('图片已上传')
  } catch {
    // 拦截器已提示
  } finally {
    uploadingStepIndex.value = -1
    event.target.value = ''
  }
}

function onCancel() {
  router.back()
}

const dishImageInput = ref(null)
const uploadingImage = ref(false)

function pickDishImage() {
  dishImageInput.value?.click()
}

async function onDishImageChange(event) {
  const file = event.target.files?.[0]
  if (!file || uploadingImage.value) return
  uploadingImage.value = true
  try {
    const { url } = await uploadFile(file)
    form.imageUrl = url
    showToast('图片已上传')
  } catch {
    // 拦截器已提示
  } finally {
    uploadingImage.value = false
    event.target.value = ''
  }
}

function buildCalorieIngredients() {
  return form.ingredients
    .filter((i) => i.name.trim())
    .map((i) => {
      const item = { name: i.name.trim() }
      const amount = String(i.amount).trim()
      if (amount !== '' && !Number.isNaN(Number(amount))) {
        item.amount = Number(amount)
      }
      if (i.unit && i.unit.trim()) {
        item.unit = i.unit.trim()
      }
      return item
    })
}

async function onAnalyzeCalories() {
  if (!form.name.trim()) {
    showToast('请先填写菜名')
    return
  }
  if (aiAnalyzingCalories.value) return
  aiAnalyzingCalories.value = true
  try {
    const result = await analyzeDishCalories({
      name: form.name.trim(),
      ingredients: buildCalorieIngredients(),
    })
    form.calories = result.calories ?? ''
    showToast(result.note || `已估算约 ${result.calories} 千卡`)
  } catch {
    // 拦截器已提示
  } finally {
    aiAnalyzingCalories.value = false
  }
}

async function onSubmit() {
  if (!form.name.trim()) {
    showToast('请输入菜名')
    return
  }
  if (form.price === '' || Number.isNaN(Number(form.price)) || Number(form.price) < 0) {
    showToast('请输入正确的价格')
    return
  }

  const ingredients = form.ingredients.filter(
    (i) => i.name.trim() || String(i.amount).trim() || i.unit.trim(),
  )
  for (const item of ingredients) {
    if (!item.name.trim() || String(item.amount).trim() === '' || !item.unit.trim()) {
      showToast('请完善食材的名称、用量和单位')
      return
    }
  }

  const steps = form.steps.filter((s) => s.content.trim())

  const payload = {
    name: form.name.trim(),
    description: form.description.trim() || null,
    imageUrl: form.imageUrl.trim() || null,
    categoryId: form.categoryId,
    cuisineId: form.cuisineId,
    cookingMinutes: form.cookingMinutes === '' ? null : Number(form.cookingMinutes),
    calories: form.calories === '' ? null : Number(form.calories),
    price: Number(form.price),
    ingredients: ingredients.map((i) => ({
      name: i.name.trim(), amount: Number(i.amount), unit: i.unit.trim(),
    })),
    steps: steps.map((s, idx) => ({
      stepNo: idx + 1, content: s.content.trim(), imageUrl: s.imageUrl?.trim() || null,
    })),
  }

  if (submitting.value) return
  submitting.value = true
  try {
    const saved = isEdit.value
      ? await updateDish(route.params.id, payload)
      : await createDish(payload)
    showToast(isEdit.value ? '已保存' : '菜品已创建')
    router.replace({ name: 'dish-detail', params: { id: saved.id } })
  } catch (error) {
    if (error?.fieldErrors) {
      showToast(Object.values(error.fieldErrors)[0] || '请检查填写内容')
    }
  } finally {
    submitting.value = false
  }
}

loadCategories()
loadCuisines()
if (isEdit.value) {
  loadDetail()
}
</script>

<template>
  <section class="dish-edit">
    <header class="dish-edit-head">
      <button class="dish-back" type="button" @click="onCancel">
        <van-icon name="arrow-left" /> 返回
      </button>
      <h1>{{ isEdit ? '编辑菜品' : '创建菜品' }}</h1>
    </header>

    <van-loading v-if="loading" class="dish-loading">加载中...</van-loading>

    <div v-else-if="loadError" class="dish-error">
      <van-empty description="菜品信息加载失败，请稍后重试" />
      <van-button type="primary" plain @click="loadDetail">重新加载</van-button>
    </div>

    <van-form v-else class="dish-edit-form" @submit="onSubmit">
      <van-field
        v-model="form.name"
        name="name"
        label="菜名"
        placeholder="例如：番茄炒蛋"
        maxlength="100"
        required
      />
      <van-field
        :model-value="form.categoryName"
        name="category"
        label="分类"
        placeholder="选择分类（可选）"
        readonly
        is-link
        @click="showCategoryPicker = true"
      />
      <van-field
        :model-value="form.cuisineName"
        name="cuisine"
        label="菜系"
        placeholder="选择菜系（可选）"
        readonly
        is-link
        @click="showCuisinePicker = true"
      />
      <div class="dish-image-field">
        <van-field
          v-model="form.imageUrl"
          name="imageUrl"
          label="图片"
          placeholder="图片 URL（可选）"
          maxlength="500"
        />
        <div class="dish-image-toolbar">
          <van-button
            size="small"
            plain
            type="primary"
            icon="photograph"
            :loading="uploadingImage"
            loading-text="上传中..."
            @click="pickDishImage"
          >
            上传图片
          </van-button>
          <van-image v-if="form.imageUrl" :src="form.imageUrl" fit="cover" class="dish-image-preview" />
        </div>
        <input
          ref="dishImageInput"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          hidden
          @change="onDishImageChange"
        />
      </div>
      <van-field
        v-model="form.cookingMinutes"
        name="cookingMinutes"
        type="digit"
        label="耗时"
        placeholder="分钟（可选）"
      />
      <div class="dish-calorie-field">
        <van-field
          v-model="form.calories"
          name="calories"
          type="digit"
          label="卡路里"
          placeholder="千卡（可选）"
        />
        <div class="dish-calorie-toolbar">
          <van-button
            size="small"
            plain
            type="primary"
            icon="bulb-o"
            :loading="aiAnalyzingCalories"
            loading-text="分析中..."
            @click="onAnalyzeCalories"
          >
            AI 分析卡路里
          </van-button>
        </div>
      </div>
      <van-field
        v-model="form.price"
        name="price"
        type="number"
        label="价格"
        placeholder="¥ 0.00"
        required
      />
      <van-field
        v-model="form.description"
        name="description"
        type="textarea"
        rows="2"
        label="简介"
        placeholder="一句话介绍这道菜（可选）"
        maxlength="500"
      />

      <div class="dish-edit-sub">
        <div class="dish-edit-sub-head">
          <h2>食材</h2>
          <van-button size="small" icon="plus" type="primary" plain @click="addIngredient">添加</van-button>
        </div>
        <div v-for="(ingredient, index) in form.ingredients" :key="index" class="dish-ingredient-row">
          <van-field v-model="ingredient.name" placeholder="食材名" maxlength="100" />
          <van-field v-model="ingredient.amount" type="number" placeholder="用量" />
          <van-field
            :model-value="ingredient.unit"
            placeholder="单位"
            readonly
            is-link
            @click="openUnitPicker(index)"
          />
          <button class="dish-row-remove" type="button" aria-label="删除食材" @click="removeIngredient(index)">
            <van-icon name="cross" />
          </button>
        </div>
        <p v-if="!form.ingredients.length" class="dish-edit-hint">还没有食材，点上方「添加」</p>
      </div>

      <div class="dish-edit-sub">
        <div class="dish-edit-sub-head">
          <h2>做法步骤</h2>
          <div class="dish-edit-sub-actions">
            <van-button size="small" icon="bulb-o" plain @click="openAiParse">AI 智能识别</van-button>
            <van-button size="small" icon="description" plain @click="openStepPaste">粘贴分步</van-button>
            <van-button size="small" icon="plus" type="primary" plain @click="addStep">添加</van-button>
          </div>
        </div>
        <div v-for="(step, index) in form.steps" :key="index" class="dish-step-row">
          <span class="dish-step-index">{{ index + 1 }}</span>
          <div class="dish-step-body">
            <van-field
              v-model="step.content"
              type="textarea"
              rows="2"
              placeholder="这一步怎么做"
              maxlength="500"
            />
            <div class="dish-image-field">
              <van-field
                v-model="step.imageUrl"
                placeholder="步骤图片 URL（可选）"
                maxlength="500"
              />
              <div class="dish-image-toolbar">
                <van-button
                  size="small"
                  plain
                  type="primary"
                  icon="photograph"
                  :loading="uploadingStepIndex === index"
                  loading-text="上传中..."
                  @click="pickStepImage(index)"
                >
                  上传图片
                </van-button>
                <van-image v-if="step.imageUrl" :src="step.imageUrl" fit="cover" class="dish-image-preview" />
              </div>
              <input
                :ref="(el) => (stepImageInputs[index] = el)"
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                hidden
                @change="onStepImageChange($event, index)"
              />
            </div>
          </div>
          <button class="dish-row-remove" type="button" aria-label="删除步骤" @click="removeStep(index)">
            <van-icon name="cross" />
          </button>
        </div>
        <p v-if="!form.steps.length" class="dish-edit-hint">还没有步骤，点上方「添加」（步骤号会自动连续编号）</p>
      </div>

      <div class="dish-edit-actions">
        <van-button block type="primary" native-type="submit" :loading="submitting" loading-text="保存中...">
          {{ isEdit ? '保存菜品' : '创建菜品' }}
        </van-button>
      </div>
    </van-form>

    <van-popup v-model:show="showCategoryPicker" round position="bottom">
      <div class="dish-category-sheet">
        <h2>选择分类</h2>
        <van-cell-group class="family-list">
          <van-cell title="不设置分类" @click="onClearCategory" />
          <van-cell
            v-for="category in categories"
            :key="category.id"
            :title="category.name"
            @click="onSelectCategory(category)"
          />
        </van-cell-group>
        <van-button block type="primary" plain @click="showCategoryCreate = true">新建分类</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showCategoryCreate" round position="bottom">
      <div class="dish-category-sheet">
        <h2>新建分类</h2>
        <van-field v-model="newCategoryName" label="分类名称" placeholder="例如：热菜" maxlength="50" />
        <van-button
          class="dish-category-submit"
          block
          type="primary"
          :loading="creatingCategory"
          loading-text="创建中..."
          @click="onSubmitNewCategory"
        >
          创建分类
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showCuisinePicker" round position="bottom">
      <div class="dish-category-sheet">
        <h2>选择菜系</h2>
        <van-cell-group class="family-list">
          <van-cell title="不设置菜系" @click="onClearCuisine" />
          <van-cell
            v-for="cuisine in cuisines"
            :key="cuisine.id"
            :title="cuisine.name"
            @click="onSelectCuisine(cuisine)"
          />
        </van-cell-group>
        <van-button block type="primary" plain @click="showCuisineCreate = true">新建菜系</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showCuisineCreate" round position="bottom">
      <div class="dish-category-sheet">
        <h2>新建菜系</h2>
        <van-field v-model="newCuisineName" label="菜系名称" placeholder="例如：家常菜" maxlength="50" />
        <van-button
          class="dish-category-submit"
          block
          type="primary"
          :loading="creatingCuisine"
          loading-text="创建中..."
          @click="onSubmitNewCuisine"
        >
          创建菜系
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showUnitPicker" round position="bottom">
      <div class="dish-category-sheet">
        <h2>选择单位</h2>
        <div class="unit-grid">
          <button
            v-for="unit in unitOptions"
            :key="unit"
            type="button"
            class="unit-chip"
            @click="applyUnit(unit)"
          >
            {{ unit }}
          </button>
        </div>
        <van-field v-model="customUnit" label="自定义" placeholder="其他单位" maxlength="20" />
        <van-button class="dish-category-submit" block type="primary" @click="applyCustomUnit">
          使用自定义单位
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showStepPaste" round position="bottom">
      <div class="dish-category-sheet">
        <h2>粘贴做法，自动分步</h2>
        <p class="dish-edit-hint">支持「1.」「1、」「1)」「一、」等编号，粘贴后自动识别每一步。</p>
        <van-field
          v-model="stepPasteText"
          type="textarea"
          rows="8"
          placeholder="把复制的做法整段粘贴到这里"
          class="step-paste-textarea"
        />
        <div v-if="parsedSteps.length" class="step-paste-preview">
          <p class="step-paste-count">已识别 {{ parsedSteps.length }} 步</p>
          <ol class="step-paste-list">
            <li v-for="(s, i) in parsedSteps" :key="i">{{ s }}</li>
          </ol>
        </div>
        <van-button
          class="dish-category-submit"
          block
          type="primary"
          :disabled="!parsedSteps.length"
          @click="applyParsedSteps"
        >
          填充 {{ parsedSteps.length }} 步
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showAiParse" round position="bottom">
      <div class="dish-category-sheet">
        <h2>AI 智能识别菜谱</h2>
        <p class="dish-edit-hint">粘贴整段菜谱，自动识别菜名、食材和步骤（需本机 Ollama 已启动，首次识别约需十几秒）。</p>
        <van-field
          v-model="aiParseText"
          type="textarea"
          rows="8"
          placeholder="把整段菜谱粘贴到这里，例如：番茄炒蛋：番茄2个，鸡蛋2个，盐少许。步骤：1.番茄切块；2.…"
          class="step-paste-textarea"
        />
        <van-button
          class="dish-category-submit"
          block
          type="primary"
          :loading="aiParsing"
          loading-text="AI 识别中..."
          :disabled="!aiParseText.trim() || aiParsing"
          @click="applyAiParse"
        >
          开始识别
        </van-button>
      </div>
    </van-popup>
  </section>
</template>
