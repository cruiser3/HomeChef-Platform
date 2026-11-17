import request from './request'

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
