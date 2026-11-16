<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '../store/auth'
import { useFamilyStore } from '../store/family'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const familyStore = useFamilyStore()

const mode = ref('login')
const submitting = ref(false)

const form = reactive({ name: '', email: '', password: '' })
const serverErrors = reactive({ name: '', email: '', password: '' })

const isRegister = computed(() => mode.value === 'register')
const title = computed(() => (isRegister.value ? '创建账号' : '欢迎回家'))
const copy = computed(() =>
  isRegister.value ? '注册后即可创建或加入家庭，一起安排每一餐。' : '登录后继续安排家里的一日三餐。',
)
const submitText = computed(() => (isRegister.value ? '注册并进入' : '登录'))

const emailRules = [
  { required: true, message: '请输入邮箱' },
  { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确' },
]
const nameRules = [{ required: true, message: '请输入昵称' }]
const passwordRules = computed(() => {
  const rules = [{ required: true, message: '请输入密码' }]
  if (isRegister.value) {
    rules.push({ pattern: /^.{6,}$/, message: '密码至少 6 位' })
  }
  return rules
})

function switchMode(next) {
  mode.value = next
  clearServerErrors()
}

function clearServerErrors() {
  serverErrors.name = ''
  serverErrors.email = ''
  serverErrors.password = ''
}

function clearFieldError(key) {
  serverErrors[key] = ''
}

function mapFieldErrors(fieldErrors) {
  if (!fieldErrors) return
  ;['name', 'email', 'password'].forEach((key) => {
    serverErrors[key] = fieldErrors[key] || ''
  })
}

function getRedirect() {
  const value = route.query.redirect
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//') ? value : ''
}

function goNext() {
  const redirect = getRedirect()
  if (familyStore.currentFamilyId) {
    router.replace(redirect || { name: 'dishes' })
  } else {
    router.replace({ name: 'family-select', query: redirect ? { redirect } : {} })
  }
}

async function onSubmit() {
  if (submitting.value) return
  submitting.value = true

  const payload = isRegister.value
    ? { name: form.name.trim(), email: form.email.trim(), password: form.password }
    : { email: form.email.trim(), password: form.password }

  try {
    if (isRegister.value) {
      await authStore.register(payload)
      showToast('注册成功')
    } else {
      await authStore.login(payload)
      showToast('登录成功')
    }
    goNext()
  } catch (error) {
    if (error.fieldErrors) {
      mapFieldErrors(error.fieldErrors)
      showToast('请检查填写内容')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="standalone-page">
    <section class="standalone-card auth-card">
      <span class="brand-mark" aria-hidden="true">家</span>
      <p class="eyebrow">HOME CHEF · WEB</p>
      <h1>{{ title }}</h1>
      <p class="standalone-copy">{{ copy }}</p>

      <van-form class="auth-form" @submit="onSubmit">
        <van-field
          v-if="isRegister"
          v-model="form.name"
          name="name"
          label="昵称"
          placeholder="你的称呼"
          maxlength="50"
          :rules="nameRules"
          :error-message="serverErrors.name"
          @update:model-value="clearFieldError('name')"
        />
        <van-field
          v-model="form.email"
          name="email"
          type="email"
          label="邮箱"
          placeholder="you@example.com"
          :rules="emailRules"
          :error-message="serverErrors.email"
          @update:model-value="clearFieldError('email')"
        />
        <van-field
          v-model="form.password"
          name="password"
          type="password"
          label="密码"
          placeholder="至少 6 位"
          :rules="passwordRules"
          :error-message="serverErrors.password"
          @update:model-value="clearFieldError('password')"
        />

        <van-button
          class="auth-submit"
          block
          type="primary"
          native-type="submit"
          :loading="submitting"
          loading-text="处理中..."
        >
          {{ submitText }}
        </van-button>
      </van-form>

      <p class="auth-switch">
        <template v-if="isRegister">
          已有账号？
          <button type="button" @click="switchMode('login')">去登录</button>
        </template>
        <template v-else>
          还没有账号？
          <button type="button" @click="switchMode('register')">去注册</button>
        </template>
      </p>
    </section>
  </main>
</template>
