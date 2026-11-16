import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi } from '../api/auth'
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
      this.user = { email: data.email, name: data.name, role: data.role }
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },

    applyProfile(data) {
      this.user = {
        email: data.email ?? this.user?.email,
        name: data.name ?? this.user?.name,
        role: this.user?.role,
      }
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },

    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
