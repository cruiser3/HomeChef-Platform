<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { showToast } from 'vant'
import { getAiConfig, saveAiApiConfig, saveAiPersona, resetAiConfig } from '../api/ai'
import { PREFERENCES, RESTRICTIONS, ALLERGIES, CONDITIONS } from '../constants/dietary'

const emit = defineEmits(['back'])

const personaSections = [
  { key: 'dietaryPreferences', title: '饮食偏好', options: PREFERENCES, max: 5, custom: false, hint: '最多 5 项' },
  { key: 'dietaryRestrictions', title: '忌口', options: RESTRICTIONS, max: 5, custom: true, hint: '最多 5 项，可自定义' },
  { key: 'allergies', title: '过敏', options: ALLERGIES, max: 8, custom: true, hint: '最多 8 项，可自定义' },
  { key: 'medicalConditions', title: '健康情况', options: CONDITIONS, max: 8, custom: true, hint: '最多 8 项，可自定义' },
]

const persona = reactive({
  dietaryPreferences: [],
  dietaryRestrictions: [],
  allergies: [],
  medicalConditions: [],
})
const customInput = reactive({ dietaryRestrictions: '', allergies: '', medicalConditions: '' })

function sectionOf(key) {
  return personaSections.find((s) => s.key === key)
}
function toggle(key, value) {
  const arr = persona[key]
  const i = arr.indexOf(value)
  if (i >= 0) {
    arr.splice(i, 1)
  } else if (arr.length >= sectionOf(key).max) {
    showToast(`最多 ${sectionOf(key).max} 项`)
  } else {
    arr.push(value)
  }
}
function isSelected(key, value) {
  return persona[key].includes(value)
}
function customValues(key) {
  const known = sectionOf(key).options.map((o) => o.value)
  return persona[key].filter((v) => !known.includes(v))
}
function addCustom(key) {
  const val = customInput[key].trim()
  if (!val) return
  const arr = persona[key]
  if (arr.includes(val)) {
    customInput[key] = ''
    return
  }
  if (arr.length >= sectionOf(key).max) {
    showToast(`最多 ${sectionOf(key).max} 项`)
    return
  }
  arr.push(val)
  customInput[key] = ''
}

// 模型来源预设：OLLAMA 用系统默认；其余都是 OpenAI 兼容接口，仅 Base URL 与默认模型不同。
const API_SOURCES = [
  { key: 'OLLAMA', label: '系统 Ollama', baseUrl: '', model: '' },
  { key: 'DEEPSEEK', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com', model: 'deepseek-chat' },
  { key: 'KIMI', label: 'Kimi', baseUrl: 'https://api.moonshot.cn/v1', model: 'moonshot-v1-8k' },
  { key: 'OPENAI', label: 'OpenAI', baseUrl: 'https://api.openai.com/v1', model: 'gpt-4o-mini' },
  { key: 'CUSTOM', label: '自定义', baseUrl: '', model: '' },
]

const apiForm = reactive({
  source: 'OLLAMA',
  baseUrl: '',
  apiKey: '',
  model: '',
})
const apiKeyMasked = ref('')
const hasApiKey = ref(false)
const isCustomSource = computed(() => apiForm.source !== 'OLLAMA')

function selectSource(key) {
  apiForm.source = key
  const preset = API_SOURCES.find((s) => s.key === key)
  apiForm.baseUrl = preset ? preset.baseUrl : ''
  apiForm.model = preset ? preset.model : ''
}

// 加载时根据后端存的 provider + baseUrl 反推当前「来源」。
function resolveSource(data) {
  if (!data.provider || data.provider === 'OLLAMA') return 'OLLAMA'
  const url = (data.baseUrl || '').toLowerCase()
  if (url.includes('deepseek')) return 'DEEPSEEK'
  if (url.includes('moonshot')) return 'KIMI'
  if (url.includes('openai.com')) return 'OPENAI'
  return 'CUSTOM'
}

onMounted(load)

async function load() {
  try {
    const data = await getAiConfig()
    apiForm.source = resolveSource(data)
    apiForm.baseUrl = data.baseUrl || ''
    apiForm.model = data.model || ''
    hasApiKey.value = data.hasApiKey
    apiKeyMasked.value = data.apiKeyMasked || ''
    const p = data.persona || {}
    persona.dietaryPreferences = [...(p.dietaryPreferences || [])]
    persona.dietaryRestrictions = [...(p.dietaryRestrictions || [])]
    persona.allergies = [...(p.allergies || [])]
    persona.medicalConditions = [...(p.medicalConditions || [])]
  } catch (e) {
    showToast(e.message || '加载配置失败')
  }
}

const savingPersona = ref(false)
async function onSavePersona() {
  savingPersona.value = true
  try {
    await saveAiPersona({
      dietaryPreferences: persona.dietaryPreferences,
      dietaryRestrictions: persona.dietaryRestrictions,
      allergies: persona.allergies,
      medicalConditions: persona.medicalConditions,
    })
    showToast('角色设定已保存')
  } catch (e) {
    showToast(e.message || '保存失败')
  } finally {
    savingPersona.value = false
  }
}

const savingApi = ref(false)
async function onSaveApi() {
  savingApi.value = true
  try {
    const isOllama = apiForm.source === 'OLLAMA'
    const data = await saveAiApiConfig({
      provider: isOllama ? 'OLLAMA' : 'OPENAI_COMPATIBLE',
      baseUrl: isOllama ? '' : apiForm.baseUrl,
      apiKey: apiForm.apiKey,
      model: isOllama ? '' : apiForm.model,
    })
    apiKeyMasked.value = data.apiKeyMasked || ''
    hasApiKey.value = data.hasApiKey
    apiForm.apiKey = ''
    showToast('API 配置已保存')
  } catch (e) {
    showToast(e.message || '保存失败')
  } finally {
    savingApi.value = false
  }
}

async function onReset() {
  if (!window.confirm('确定重置为系统默认（Ollama）配置吗？')) return
  try {
    await resetAiConfig()
    await load()
    apiForm.apiKey = ''
    showToast('已重置')
  } catch (e) {
    showToast(e.message || '重置失败')
  }
}
</script>

<template>
  <div class="assistant-settings">
    <header class="settings-header">
      <van-icon name="arrow-left" class="back" @click="emit('back')" />
      <span class="title">小铲设置</span>
    </header>

    <div class="settings-body">
      <section class="card">
        <h3 class="card-title">角色设定</h3>
        <p class="card-sub">仅小铲对话生效，不影响个人账户资料；未保存前沿用账户健康档案</p>

        <div v-for="sec in personaSections" :key="sec.key" class="field">
          <div class="field-head">
            <span class="field-title">{{ sec.title }}</span>
            <span class="field-hint">{{ sec.hint }}</span>
          </div>
          <div class="chips">
            <span
              v-for="opt in sec.options"
              :key="opt.value"
              class="chip"
              :class="{ active: isSelected(sec.key, opt.value) }"
              @click="toggle(sec.key, opt.value)"
            >{{ opt.label }}</span>
            <span
              v-for="(v, i) in customValues(sec.key)"
              :key="'c' + i"
              class="chip active"
              @click="toggle(sec.key, v)"
            >{{ v }} ×</span>
          </div>
          <div v-if="sec.custom" class="custom-row">
            <input
              v-model="customInput[sec.key]"
              class="custom-input"
              :placeholder="'自定义' + sec.title"
              @keyup.enter="addCustom(sec.key)"
            />
            <button class="add-btn" @click="addCustom(sec.key)">添加</button>
          </div>
        </div>

        <button class="save-btn" :disabled="savingPersona" @click="onSavePersona">
          {{ savingPersona ? '保存中…' : '保存角色设定' }}
        </button>
      </section>

      <section class="card">
        <h3 class="card-title">对话 API</h3>
        <p class="card-sub">选择模型来源，可接入自己申请的 DeepSeek / Kimi / OpenAI 等 OpenAI 兼容模型</p>

        <div class="field">
          <div class="field-title">模型来源</div>
          <div class="source-grid">
            <button
              v-for="s in API_SOURCES"
              :key="s.key"
              class="source-item"
              :class="{ active: apiForm.source === s.key }"
              @click="selectSource(s.key)"
            >{{ s.label }}</button>
          </div>
        </div>

        <template v-if="isCustomSource">
          <div class="field">
            <div class="field-title">接口地址（Base URL）</div>
            <input v-model="apiForm.baseUrl" class="text-input" placeholder="https://api.deepseek.com" />
          </div>
          <div class="field">
            <div class="field-title">模型名称</div>
            <input v-model="apiForm.model" class="text-input" placeholder="deepseek-chat" />
          </div>
          <div class="field">
            <div class="field-title">API 密钥</div>
            <input
              v-model="apiForm.apiKey"
              class="text-input"
              type="password"
              :placeholder="hasApiKey ? `已保存：${apiKeyMasked}，留空不修改` : 'sk-...'"
            />
          </div>
        </template>

        <button class="save-btn" :disabled="savingApi" @click="onSaveApi">
          {{ savingApi ? '保存中…' : '保存 API 配置' }}
        </button>
        <button class="reset-btn" @click="onReset">重置为系统默认</button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.assistant-settings {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}
.settings-header {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border);
  background: var(--card);
}
.back {
  font-size: 20px;
  cursor: pointer;
}
.title {
  font-size: 16px;
  font-weight: 650;
}
.settings-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
}
.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  box-shadow: var(--shadow-soft);
}
.card-title {
  font-size: 16px;
  font-weight: 650;
  margin-bottom: 4px;
}
.card-sub {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 12px;
}
.field {
  margin-bottom: 14px;
}
.field-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}
.field-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}
.field-hint {
  font-size: 12px;
  color: var(--text-muted);
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--surface-soft);
  border: 1px solid var(--border);
  font-size: 13px;
  color: var(--text);
  cursor: pointer;
  transition: all 140ms ease;
}
.chip.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.custom-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.custom-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 13px;
  background: var(--bg);
}
.add-btn {
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  background: var(--primary);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.text-input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  background: var(--bg);
}
.save-btn {
  width: 100%;
  margin-top: 4px;
  padding: 11px;
  border: none;
  border-radius: 10px;
  background: var(--primary);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.save-btn:disabled {
  opacity: 0.6;
}
.source-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.source-item {
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface-soft);
  color: var(--text);
  font-size: 13px;
  cursor: pointer;
  transition: all 140ms ease;
}
.source-item.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.reset-btn {
  width: 100%;
  margin-top: 10px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: transparent;
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
}
</style>
