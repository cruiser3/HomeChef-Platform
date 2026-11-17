import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi } from '../api/auth'
import { getMyProfile } from '../api/user'
import { TOKEN_KEY } from '../api/request'

const USER_KEY = 'homechef.user'

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readStoredUser(),
    profileLoaded: false,
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    displayName: (state) => state.user?.name || state.user?.email || '',
  },

  actions: {
    async login(payload) {
      const data = await loginApi(payload)
      this.setSession(data)
      return data
    },

    async register(payload) {
      const data = await registerApi(payload)
      this.setSession(data)
      return data
    },

    setSession(data) {
      this.token = data.token
      this.user = { email: data.email, name: data.name, role: data.role, avatarUrl: data.avatarUrl || '' }
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },

    applyProfile(data) {
      this.user = {
        email: data.email ?? this.user?.email,
        name: data.name ?? this.user?.name,
        role: this.user?.role,
        avatarUrl: data.avatarUrl ?? this.user?.avatarUrl ?? '',
      }
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },

    // 拉取当前用户资料（头像/昵称）。登录响应不含头像时，首次使用头像前调用。
    async fetchProfile(force = false) {
      if (!force && this.profileLoaded) return
      try {
        const data = await getMyProfile()
        this.applyProfile(data)
        this.profileLoaded = true
      } catch (e) {
        // 头像/昵称获取失败时前端有首字兜底，不阻塞
      }
    },

    clearSession() {
      this.token = ''
      this.user = null
      this.profileLoaded = false
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
