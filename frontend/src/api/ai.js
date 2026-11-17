import request, { TOKEN_KEY, FAMILY_ID_KEY } from './request'

// AI 识别耗时较长（本地 Ollama 首次加载模型约 10s+），单独覆盖默认 10s 超时。
export function parseRecipe(text) {
  return request.post('/ai/parse-recipe', { text }, { timeout: 120000 })
}

// 整餐分析同样耗时较长，覆盖默认超时。
export function analyzeCart(payload = {}) {
  return request.post('/ai/analyze-cart', payload, { timeout: 120000 })
}

// 卡路里估算耗时较长，覆盖默认超时。
export function analyzeCalories(payload) {
  return request.post('/ai/analyze-calories', payload, { timeout: 120000 })
}

// 「小铲」流式对话：axios 不支持读取流式响应，这里用原生 fetch 读取 SSE，逐 token 回调。
export function streamChat(sessionId, messages, onToken) {
  const token = localStorage.getItem(TOKEN_KEY)
  const familyId = localStorage.getItem(FAMILY_ID_KEY)

  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  if (familyId) headers['X-Family-Id'] = familyId

  return new Promise((resolve, reject) => {
    fetch('/api/ai/chat', {
      method: 'POST',
      headers,
      body: JSON.stringify({ sessionId, messages }),
    })
      .then(async (response) => {
        if (!response.ok) {
          let message = '对话请求失败，请稍后重试'
          try {
            const data = await response.json()
            message = data?.message || message
          } catch (e) {
            // 忽略非 JSON 错误体
          }
          throw new Error(message)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        // 终止读取并 settle Promise；收到 [DONE]/[ERROR] 即结束，不等连接关闭。
        const stop = (fn) => {
          reader.cancel().catch(() => {})
          fn()
        }

        const handleEvent = (raw) => {
          const dataLines = []
          for (const line of raw.split('\n')) {
            if (line.startsWith('data:')) {
              dataLines.push(line.slice(5).replace(/^ /, ''))
            }
          }
          const data = dataLines.join('\n')
          if (!data) return
          if (data === '[DONE]') {
            stop(resolve)
            return
          }
          if (data === '[ERROR]') {
            stop(() => reject(new Error('AI 服务异常，请稍后重试')))
            return
          }
          onToken?.(data)
        }

        const pump = async () => {
          try {
            while (true) {
              const { value, done } = await reader.read()
              if (done) break
              buffer += decoder.decode(value, { stream: true })

              let idx
              while ((idx = buffer.indexOf('\n\n')) >= 0) {
                const raw = buffer.slice(0, idx)
                buffer = buffer.slice(idx + 2)
                handleEvent(raw)
              }
            }

            buffer += decoder.decode()
            if (buffer.trim()) handleEvent(buffer)
            resolve()
          } catch (e) {
            reject(e)
          }
        }
        pump()
      })
      .catch((e) => reject(e))
  })
}

// 会话管理：多会话列表、新建、历史、删除。
export function listSessions() {
  return request.get('/ai/sessions')
}

export function createSession(title) {
  return request.post('/ai/sessions', { title })
}

export function getSessionMessages(id) {
  return request.get(`/ai/sessions/${id}/messages`)
}

export function removeSession(id) {
  return request.delete(`/ai/sessions/${id}`)
}

// 「小铲」用户配置：对话 Provider 与独立角色设定。
export function getAiConfig() {
  return request.get('/ai/config')
}

export function saveAiApiConfig(payload) {
  return request.put('/ai/config', payload)
}

export function saveAiPersona(payload) {
  return request.put('/ai/config/persona', payload)
}

export function resetAiConfig() {
  return request.delete('/ai/config')
}

