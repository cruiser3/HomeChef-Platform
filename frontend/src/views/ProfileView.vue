<script setup>
import { computed, reactive, ref } from 'vue'
import { showToast } from 'vant'
import BaseCard from '../components/BaseCard.vue'
import { getMyProfile, updateMyProfile } from '../api/user'
import { uploadFile } from '../api/upload'
import { useAuthStore } from '../store/auth'
import { PREFERENCES, RESTRICTIONS, ALLERGIES, CONDITIONS } from '../constants/dietary'

const authStore = useAuthStore()

const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)
const form = reactive({ name: '', avatarUrl: '', dietaryPreferences: [], dietaryRestrictions: [], allergies: [], medicalConditions: [] })

function togglePreference(value) {
  const index = form.dietaryPreferences.indexOf(value)
  if (index >= 0) {
    form.dietaryPreferences.splice(index, 1)
    return
  }
  if (form.dietaryPreferences.length >= 5) {
    showToast('最多选择 5 个偏好')
    return
  }
  form.dietaryPreferences.push(value)
}

function toggleRestriction(value) {
  const index = form.dietaryRestrictions.indexOf(value)
  if (index >= 0) {
    form.dietaryRestrictions.splice(index, 1)
    return
  }
  if (form.dietaryRestrictions.length >= 5) {
    showToast('最多选择 5 个忌口')
    return
  }
  form.dietaryRestrictions.push(value)
}

function toggleAllergy(value) {
  const index = form.allergies.indexOf(value)
  if (index >= 0) {
    form.allergies.splice(index, 1)
    return
  }
  if (form.allergies.length >= 8) {
    showToast('最多选择 8 个过敏')
    return
  }
  form.allergies.push(value)
}

function toggleCondition(value) {
  const index = form.medicalConditions.indexOf(value)
  if (index >= 0) {
    form.medicalConditions.splice(index, 1)
    return
  }
  if (form.medicalConditions.length >= 8) {
    showToast('最多选择 8 个健康情况')
    return
  }
  form.medicalConditions.push(value)
}

const customRestrictions = computed(() =>
  form.dietaryRestrictions.filter((v) => !RESTRICTIONS.some((r) => r.value === v)),
)
const customAllergies = computed(() =>
  form.allergies.filter((v) => !ALLERGIES.some((a) => a.value === v)),
)
const customConditions = computed(() =>
  form.medicalConditions.filter((v) => !CONDITIONS.some((c) => c.value === v)),
)

const customRestrictionInput = ref('')
const customAllergyInput = ref('')
const customConditionInput = ref('')

function addCustomRestriction() {
  const value = customRestrictionInput.value.trim()
  if (!value) {
    showToast('请输入自定义忌口')
    return
  }
  if (value.length > 32) {
    showToast('忌口不超过 32 字')
    return
  }
  if (form.dietaryRestrictions.includes(value)) {
    showToast('该忌口已添加')
    return
  }
  if (form.dietaryRestrictions.length >= 5) {
    showToast('最多选择 5 个忌口')
    return
  }
  form.dietaryRestrictions.push(value)
  customRestrictionInput.value = ''
}

function addCustomAllergy() {
  const value = customAllergyInput.value.trim()
  if (!value) {
    showToast('请输入自定义过敏')
    return
  }
  if (value.length > 32) {
    showToast('过敏不超过 32 字')
    return
  }
  if (form.allergies.includes(value)) {
    showToast('该过敏已添加')
    return
  }
  if (form.allergies.length >= 8) {
    showToast('最多选择 8 个过敏')
    return
  }
  form.allergies.push(value)
  customAllergyInput.value = ''
}

function addCustomCondition() {
  const value = customConditionInput.value.trim()
  if (!value) {
    showToast('请输入自定义情况')
    return
  }
  if (value.length > 32) {
    showToast('健康情况不超过 32 字')
    return
  }
  if (form.medicalConditions.includes(value)) {
    showToast('该情况已添加')
    return
  }
  if (form.medicalConditions.length >= 8) {
    showToast('最多选择 8 个健康情况')
    return
  }
  form.medicalConditions.push(value)
  customConditionInput.value = ''
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const data = await getMyProfile()
    form.name = data.name || ''
    form.avatarUrl = data.avatarUrl || ''
    form.dietaryPreferences = data.dietaryPreferences || []
    form.dietaryRestrictions = data.dietaryRestrictions || []
    form.allergies = data.allergies || []
    form.medicalConditions = data.medicalConditions || []
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const avatarInput = ref(null)
const uploadingAvatar = ref(false)

function pickAvatar() {
  avatarInput.value?.click()
}

async function onAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file || uploadingAvatar.value) return
  uploadingAvatar.value = true
  try {
    const { url } = await uploadFile(file)
    form.avatarUrl = url
    showToast('头像已上传')
  } catch {
    // 拦截器已提示
  } finally {
    uploadingAvatar.value = false
    event.target.value = ''
  }
}

async function onSave() {
  const name = form.name.trim()
  if (!name) {
    showToast('请输入昵称')
    return
  }
  if (name.length > 50) {
    showToast('昵称不超过 50 字')
    return
  }
  if (saving.value) return

  saving.value = true
  try {
    const data = await updateMyProfile({
      name,
      avatarUrl: form.avatarUrl.trim(),
      dietaryPreferences: form.dietaryPreferences,
      dietaryRestrictions: form.dietaryRestrictions,
      allergies: form.allergies,
      medicalConditions: form.medicalConditions,
    })
    authStore.applyProfile(data)
    showToast('已保存')
  } catch (error) {
    if (error?.fieldErrors) showToast(Object.values(error.fieldErrors)[0] || '请检查填写内容')
  } finally {
    saving.value = false
  }
}

load()
</script>

<template>
  <section class="profile-page">
    <header class="profile-head">
      <p class="eyebrow">HOME CHEF · 我的</p>
      <h1>个人资料</h1>
      <p class="page-description">完善昵称、头像与饮食偏好，让家人更了解你的口味。</p>
    </header>

    <van-loading v-if="loading" class="profile-loading">加载中...</van-loading>

    <BaseCard v-else-if="loadError" class="profile-card">
      <div class="profile-error">
        <van-empty description="资料加载失败，请重试" />
        <van-button type="primary" @click="load">重新加载</van-button>
      </div>
    </BaseCard>

    <BaseCard v-else class="profile-card">
      <van-cell-group inset>
        <van-field v-model="form.name" label="昵称" placeholder="你的称呼" maxlength="50" />
        <div class="profile-avatar-field">
          <div class="profile-avatar">
            <van-image v-if="form.avatarUrl" :src="form.avatarUrl" fit="cover" round />
            <span v-else class="profile-avatar-fallback">{{ (form.name || '').trim().charAt(0) || '我' }}</span>
          </div>
          <div class="profile-avatar-actions">
            <van-button
              size="small"
              plain
              type="primary"
              icon="photograph"
              :loading="uploadingAvatar"
              loading-text="上传中..."
              @click="pickAvatar"
            >
              上传头像
            </van-button>
            <van-button v-if="form.avatarUrl" size="small" plain @click="form.avatarUrl = ''">移除</van-button>
          </div>
          <input
            ref="avatarInput"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            hidden
            @change="onAvatarChange"
          />
        </div>
        <van-field
          v-model="form.avatarUrl"
          label="头像链接"
          placeholder="https://…（可选）"
          maxlength="500"
        />
      </van-cell-group>

      <div class="profile-prefs">
        <p class="section-kicker">饮食偏好</p>
        <p class="profile-prefs-hint">可多选，最多 5 个，用于家人协作与后续推荐参考。</p>
        <div class="pref-chips">
          <button
            v-for="p in PREFERENCES"
            :key="p.value"
            type="button"
            class="pref-chip"
            :class="{ 'is-active': form.dietaryPreferences.includes(p.value) }"
            @click="togglePreference(p.value)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>

      <div class="profile-prefs">
        <p class="section-kicker">忌口</p>
        <p class="profile-prefs-hint">可多选，最多 5 个，下单时会自动填入订单备注。</p>
        <div class="pref-chips">
          <button
            v-for="r in RESTRICTIONS"
            :key="r.value"
            type="button"
            class="pref-chip"
            :class="{ 'is-active': form.dietaryRestrictions.includes(r.value) }"
            @click="toggleRestriction(r.value)"
          >
            {{ r.label }}
          </button>
          <button
            v-for="v in customRestrictions"
            :key="v"
            type="button"
            class="pref-chip is-active"
            @click="toggleRestriction(v)"
          >
            {{ v }}
            <van-icon name="cross" />
          </button>
        </div>
        <div class="custom-tag-add">
          <van-field v-model="customRestrictionInput" placeholder="输入自定义忌口，如「不吃茄子」" maxlength="32" />
          <van-button size="small" type="primary" plain @click="addCustomRestriction">添加</van-button>
        </div>
      </div>

      <div class="profile-prefs">
        <p class="section-kicker">过敏</p>
        <p class="profile-prefs-hint">可多选，最多 8 个，下单时会自动填入订单备注，提醒大厨注意。</p>
        <div class="pref-chips">
          <button
            v-for="a in ALLERGIES"
            :key="a.value"
            type="button"
            class="pref-chip"
            :class="{ 'is-active': form.allergies.includes(a.value) }"
            @click="toggleAllergy(a.value)"
          >
            {{ a.label }}
          </button>
          <button
            v-for="v in customAllergies"
            :key="v"
            type="button"
            class="pref-chip is-active"
            @click="toggleAllergy(v)"
          >
            {{ v }}
            <van-icon name="cross" />
          </button>
        </div>
        <div class="custom-tag-add">
          <van-field v-model="customAllergyInput" placeholder="输入自定义过敏，如「菠萝」" maxlength="32" />
          <van-button size="small" type="primary" plain @click="addCustomAllergy">添加</van-button>
        </div>
      </div>

      <div class="profile-prefs">
        <p class="section-kicker">健康情况</p>
        <p class="profile-prefs-hint">可多选，最多 8 个，仅用于饮食参考，不构成医疗建议。</p>
        <div class="pref-chips">
          <button
            v-for="c in CONDITIONS"
            :key="c.value"
            type="button"
            class="pref-chip"
            :class="{ 'is-active': form.medicalConditions.includes(c.value) }"
            @click="toggleCondition(c.value)"
          >
            {{ c.label }}
          </button>
          <button
            v-for="v in customConditions"
            :key="v"
            type="button"
            class="pref-chip is-active"
            @click="toggleCondition(v)"
          >
            {{ v }}
            <van-icon name="cross" />
          </button>
        </div>
        <div class="custom-tag-add">
          <van-field v-model="customConditionInput" placeholder="输入自定义情况，如「甲状腺结节」" maxlength="32" />
          <van-button size="small" type="primary" plain @click="addCustomCondition">添加</van-button>
        </div>
      </div>

      <van-button
        class="profile-save"
        block
        type="primary"
        :loading="saving"
        loading-text="保存中..."
        @click="onSave"
      >
        保存
      </van-button>
    </BaseCard>
  </section>
</template>

<style scoped>
.profile-head {
  margin-bottom: 20px;
}

.profile-loading {
  margin-top: 24px;
}

.profile-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-avatar-field {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
}

.profile-avatar {
  display: grid;
  width: 72px;
  height: 72px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: var(--surface-soft);
}

.profile-avatar .van-image {
  width: 100%;
  height: 100%;
}

.profile-avatar-fallback {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-dark);
}

.profile-avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile-prefs {
  padding: 4px 16px;
}

.profile-prefs-hint {
  margin: 4px 0 12px;
  color: var(--text-secondary);
  font-size: 13px;
}

.pref-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pref-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 16px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-soft);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease, color 0.15s ease;
}

.pref-chip.is-active {
  border-color: var(--primary-dark);
  background: var(--primary);
  color: var(--primary-dark);
  font-weight: 600;
}

.custom-tag-add {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.custom-tag-add .van-field {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--card);
}

.profile-save {
  margin-top: 4px;
}

.profile-error {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  padding: 8px 0;
}
</style>
