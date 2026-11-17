<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import BaseCard from '../components/BaseCard.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { useFamilyStore } from '../store/family'
import { listFamilyMembers, getInviteCode, resetInviteCode, removeMember, updateMemberRole } from '../api/family'
import { getMyProfile } from '../api/user'

const router = useRouter()
const familyStore = useFamilyStore()

const members = ref([])
const loadingMembers = ref(false)
const loadError = ref(false)
const inviteCode = ref('')
const inviteLoadFailed = ref(false)
const resetting = ref(false)
const myId = ref(null)
const changingId = ref('')
const removingId = ref('')

const family = computed(() => familyStore.currentFamily)
const isAdmin = computed(() => family.value?.currentUserRole === 'ADMIN')
const roleLabel = { ADMIN: '管理员', MEMBER: '成员' }

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  })
}

function isMe(member) {
  return myId.value != null && member.userId === myId.value
}

async function loadMembers() {
  const familyId = familyStore.currentFamilyId
  if (!familyId) {
    members.value = []
    return
  }
  loadingMembers.value = true
  loadError.value = false
  try {
    members.value = await listFamilyMembers(familyId)
  } catch {
    loadError.value = true
  } finally {
    loadingMembers.value = false
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

function goManageFamily() {
  router.push({ name: 'family-select' })
}

async function loadInviteCode() {
  const familyId = familyStore.currentFamilyId
  if (!familyId) {
    inviteCode.value = ''
    inviteLoadFailed.value = false
    return
  }
  inviteLoadFailed.value = false
  try {
    const res = await getInviteCode(familyId)
    inviteCode.value = res.inviteCode
  } catch {
    inviteCode.value = ''
    inviteLoadFailed.value = true
  }
}

async function onCopyInviteCode() {
  if (!inviteCode.value) return
  try {
    await navigator.clipboard.writeText(inviteCode.value)
    showToast('邀请码已复制')
  } catch {
    showToast('复制失败，请手动复制')
  }
}

async function onResetInviteCode() {
  try {
    await showConfirmDialog({
      title: '重置邀请码',
      message: '重置后旧邀请码将失效，确定继续吗？',
      confirmButtonText: '重置',
      confirmButtonColor: '#ee0a24',
    })
  } catch {
    return
  }
  resetting.value = true
  try {
    const res = await resetInviteCode(familyStore.currentFamilyId)
    inviteCode.value = res.inviteCode
    showToast('邀请码已重置')
  } catch {
    // 拦截器已提示
  } finally {
    resetting.value = false
  }
}

async function onChangeRole(member) {
  if (changingId.value) return
  const next = member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN'
  const label = next === 'ADMIN' ? '设为管理员' : '降为成员'
  try {
    await showConfirmDialog({
      title: '修改成员角色',
      message: `确定将「${member.name}」${label}吗？`,
      confirmButtonText: label,
      confirmButtonColor: '#6B9E7C',
    })
  } catch {
    return
  }
  changingId.value = member.userId
  try {
    await updateMemberRole(familyStore.currentFamilyId, member.userId, next)
    showToast('角色已更新')
    await loadMembers()
  } catch {
    // 拦截器已提示
  } finally {
    changingId.value = ''
  }
}

async function onRemoveMember(member) {
  if (removingId.value) return
  try {
    await showConfirmDialog({
      title: '移除成员',
      message: `确定将「${member.name}」移出本家庭吗？移出后其在本家庭的购物车将被清空。`,
      confirmButtonText: '移除',
      confirmButtonColor: '#ee0a24',
    })
  } catch {
    return
  }
  removingId.value = member.userId
  try {
    await removeMember(familyStore.currentFamilyId, member.userId)
    showToast('已移除')
    await familyStore.loadFamilies()
    await loadMembers()
  } catch {
    // 拦截器已提示
  } finally {
    removingId.value = ''
  }
}

watch(() => familyStore.currentFamilyId, () => {
  loadMembers()
  loadInviteCode()
}, { immediate: true })

if (!familyStore.loaded) {
  familyStore.loadFamilies().catch(() => {})
}
loadMyId()
</script>

<template>
  <section class="family-page">
    <button class="dish-back" type="button" @click="router.push({ name: 'profile' })">
      <van-icon name="arrow-left" /> 返回我的
    </button>

    <header class="family-head">
      <p class="eyebrow">HOME CHEF · 家庭</p>
      <h1>家庭空间</h1>
      <p class="page-description">查看当前家庭与成员，或切换、创建家庭。</p>
    </header>

    <BaseCard v-if="!family" class="family-empty-card">
      <EmptyState
        title="还没有选择家庭"
        description="选择一个家庭，或创建属于你们的新家庭，一起安排每一餐。"
      />
      <div class="family-empty-actions">
        <van-button block type="primary" @click="goManageFamily">选择 / 创建家庭</van-button>
      </div>
    </BaseCard>

    <template v-else>
      <BaseCard class="family-overview">
        <div class="family-overview-head">
          <div>
            <p class="section-kicker">当前家庭</p>
            <h2>{{ family.name }}</h2>
          </div>
          <StatusTag :label="roleLabel[family.currentUserRole] || '成员'" tone="primary" />
        </div>
        <p class="family-overview-meta">
          <span>{{ family.memberCount }} 位成员</span>
          <span v-if="formatTime(family.createdAt)">创建于 {{ formatTime(family.createdAt) }}</span>
        </p>
      </BaseCard>

      <BaseCard class="family-invite">
        <div class="family-invite-head">
          <p class="section-kicker">邀请家人</p>
          <h3>邀请码</h3>
        </div>
        <div class="family-invite-row">
          <span class="family-invite-code">{{ inviteLoadFailed ? '邀请码加载失败' : (inviteCode || '加载中…') }}</span>
          <van-button
            v-if="inviteLoadFailed"
            size="small"
            type="primary"
            plain
            @click="loadInviteCode"
          >
            重试
          </van-button>
          <van-button
            v-else
            size="small"
            type="primary"
            plain
            :disabled="!inviteCode"
            @click="onCopyInviteCode"
          >
            复制
          </van-button>
        </div>
        <p class="family-invite-hint">把邀请码发给家人，对方登录后输入即可加入本家庭。</p>
        <van-button
          v-if="isAdmin"
          size="small"
          type="danger"
          plain
          :loading="resetting"
          @click="onResetInviteCode"
        >
          重置邀请码
        </van-button>
      </BaseCard>

      <BaseCard class="family-members">
        <div class="family-members-head">
          <p class="section-kicker">家庭成员</p>
          <h3>成员列表</h3>
        </div>

        <van-loading v-if="loadingMembers" class="family-loading">加载中...</van-loading>

        <div v-else-if="loadError" class="family-error">
          <van-empty description="成员加载失败，请重试" />
          <van-button type="primary" @click="loadMembers">重新加载</van-button>
        </div>

        <ul v-else-if="members.length" class="family-member-list">
          <li v-for="member in members" :key="member.userId">
            <div class="family-member-info">
              <span class="family-member-name">{{ member.name }}</span>
              <span v-if="isMe(member)" class="family-member-me">我</span>
            </div>
            <div class="family-member-side">
              <StatusTag
                :label="roleLabel[member.role] || '成员'"
                :tone="member.role === 'ADMIN' ? 'warm' : 'neutral'"
              />
              <div v-if="isAdmin && !isMe(member)" class="family-member-actions">
                <van-button
                  size="mini"
                  :type="member.role === 'ADMIN' ? 'default' : 'primary'"
                  plain
                  :loading="changingId === member.userId"
                  @click="onChangeRole(member)"
                >
                  {{ member.role === 'ADMIN' ? '降为成员' : '设为管理员' }}
                </van-button>
                <van-button
                  size="mini"
                  type="danger"
                  plain
                  :loading="removingId === member.userId"
                  @click="onRemoveMember(member)"
                >
                  移除
                </van-button>
              </div>
            </div>
          </li>
        </ul>

        <EmptyState v-else title="暂无成员" description="当前家庭还没有成员信息。" />
      </BaseCard>

      <div class="family-actions">
        <van-button block type="primary" @click="goManageFamily">切换 / 创建家庭</van-button>
      </div>
    </template>
  </section>
</template>
