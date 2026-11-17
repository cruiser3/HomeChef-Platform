import { defineStore } from 'pinia'
import { streamChat, listSessions, createSession, getSessionMessages, removeSession } from '../api/ai'

// 「小铲」悬浮球助手状态：多会话 + 流式对话，历史持久化到后端。
export const useAssistantStore = defineStore('assistant', {
  state: () => ({
    open: false,
    messages: [],
    streaming: false,
    sessions: [],
    currentSessionId: null,
  }),
  actions: {
    toggle() {
      this.open = !this.open
    },
    openPanel() {
      this.open = true
      this.loadSessions().catch(() => {})
    },
    closePanel() {
      this.open = false
    },

    async loadSessions() {
      this.sessions = await listSessions()
    },
    async newSession() {
      this.currentSessionId = null
      this.messages = []
    },
    async switchSession(id) {
      this.currentSessionId = id
      const msgs = await getSessionMessages(id)
      this.messages = msgs.map((m) => ({ role: m.role, content: m.content }))
    },
    async removeSession(id) {
      await removeSession(id)
      if (this.currentSessionId === id) {
        this.currentSessionId = null
        this.messages = []
      }
      await this.loadSessions()
    },

    async send(content) {
      const text = (content || '').trim()
      if (!text || this.streaming) return

      this.streaming = true
      try {
        if (!this.currentSessionId) {
          const session = await createSession(text)
          this.currentSessionId = session.id
        }
      } catch (e) {
        this.streaming = false
        this.messages.push({ role: 'assistant', content: '会话创建失败，请稍后重试' })
        return
      }

      this.messages.push({ role: 'user', content: text })
      this.messages.push({ role: 'assistant', content: '' })
      const assistantIndex = this.messages.length - 1

      const history = this.messages.map((m) => ({ role: m.role, content: m.content }))

      try {
        await streamChat(this.currentSessionId, history, (t) => {
          this.messages[assistantIndex].content += t
        })
      } catch (e) {
        if (!this.messages[assistantIndex].content) {
          const msg = e?.message || ''
          this.messages[assistantIndex].content = msg === 'Failed to fetch'
            ? '网络异常，请稍后重试'
            : (msg || '抱歉，我暂时无法回复，请稍后再试')
        }
      } finally {
        this.streaming = false
        this.loadSessions().catch(() => {})
      }
    },

    reset() {
      this.messages = []
      this.sessions = []
      this.currentSessionId = null
      this.open = false
      this.streaming = false
    },
  },
})
