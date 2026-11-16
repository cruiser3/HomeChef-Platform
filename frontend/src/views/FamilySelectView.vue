<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { useFamilyStore } from '../store/family'

const router = useRouter()
const route = useRoute()
const familyStore = useFamilyStore()

const loading = ref(true)
const loadError = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const createForm = reactive({ name: '' })
const showJoin = ref(false)
const joining = ref(false)
const joinForm = reactive({ code: '' })

const roleLabel = { ADMIN: '管理员', MEMBER: '成员' }

const redirect = computed(() => {
  const value = route.query.redirect
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//') ? value : ''
})

function familyLabel(family) {
  const role = roleLabel[family.currentUserRole] || '成员'
  return `${role} · ${family.memberCount} 位成员`
}

function goNext() {
  router.replace(redirect.value || { name: 'dishes' })
}

function onSelectFamily(family) {
  familyStore.selectFamily(family.id)
  goNext()
}

async function onSubmitCreate() {
  const name = createForm.name.trim()
  if (!name) {
    showToast('请输入家庭名称')
    return
  }
  if (name.length > 50) {
    showToast('家庭名称不超过 50 字')
    return
  }
  if (creating.value) return

  creating.value = true
  try {
    await familyStore.createFamily(name)
    showCreate.value = false
    createForm.name = ''
    showToast('创建成功')
    goNext()
  } catch {
    // 错误提示已由响应拦截器统一处理
  } finally {
    creating.value = false
  }
}

async function onSubmitJoin() {
  const code = joinForm.code.trim()
  if (!code) {
    showToast('请输入邀请码')
    return
  }
  if (joining.value) return

  joining.value = true
  try {
    await familyStore.joinFamily(code)
    showJoin.value = false
    joinForm.code = ''
    showToast('加入成功')
    goNext()
  } catch {
    // 错误提示已由响应拦截器统一处理
  } finally {
    joining.value = false
  }
}

async function loadFamilies() {
  loading.value = true
  loadError.value = false
  try {
    await familyStore.loadFamilies()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

loadFamilies()
</script>

<template>
  <main class="standalone-page">
    <section class="standalone-card family-select-card">
      <p class="eyebrow">HOME CHEF · 家庭</p>
      <h1>选择家庭</h1>
      <p class="standalone-copy">选择一个家庭开始安排餐食，或创建属于你们的新家庭。</p>

      <van-loading v-if="loading" class="family-loading">加载中...</van-loading>

      <div v-else-if="loadError" class="family-error">
        <van-empty description="家庭列表加载失败，请稍后重试" />
        <van-button type="primary" plain @click="loadFamilies">重新加载</van-button>
      </div>

      <template v-else>
        <van-empty v-if="!familyStore.hasFamilies" description="还没有家庭，先创建一个吧" />

        <van-cell-group v-else class="family-list">
          <van-cell
            v-for="family in familyStore.families"
            :key="family.id"
            :title="family.name"
            :label="familyLabel(family)"
            is-link
            @click="onSelectFamily(family)"
          />
        </van-cell-group>

        <van-button
          class="family-create-btn"
          block
          :type="familyStore.hasFamilies ? 'default' : 'primary'"
          @click="showCreate = true"
        >
          创建新家庭
        </van-button>

        <van-button
          class="family-create-btn family-join-btn"
          block
          plain
          type="primary"
          @click="showJoin = true"
        >
          用邀请码加入家庭
        </van-button>
      </template>
    </section>

    <van-popup v-model:show="showCreate" round position="bottom">
      <div class="family-create-sheet">
        <h2>创建新家庭</h2>
        <van-field
          v-model="createForm.name"
          label="家庭名称"
          placeholder="例如：我家"
          maxlength="50"
        />
        <van-button
          class="family-create-btn family-create-submit"
          block
          type="primary"
          :loading="creating"
          loading-text="创建中..."
          @click="onSubmitCreate"
        >
          创建并进入
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showJoin" round position="bottom">
      <div class="family-create-sheet">
        <h2>加入家庭</h2>
        <p class="family-join-tip">输入家人分享的邀请码，即可加入对方的家庭。</p>
        <van-field
          v-model="joinForm.code"
          label="邀请码"
          placeholder="8 位邀请码"
          maxlength="16"
        />
        <van-button
          class="family-create-btn family-create-submit"
          block
          type="primary"
          :loading="joining"
          loading-text="加入中..."
          @click="onSubmitJoin"
        >
          加入家庭
        </van-button>
      </div>
    </van-popup>
  </main>
</template>
