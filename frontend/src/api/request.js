import axios from 'axios'
import { showToast } from 'vant'

export const TOKEN_KEY = 'homechef.token'
export const FAMILY_ID_KEY = 'homechef.familyId'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

function isAuthUrl(url = '') {
  return url.includes('/auth/')
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  const familyId = localStorage.getItem(FAMILY_ID_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (familyId) {
    config.headers['X-Family-Id'] = familyId
  }

  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const { response, config } = error

    if (!response) {
      showToast('网络异常，请稍后重试')
      return Promise.reject(error)
    }

    const { status, data } = response
    const message = data?.message || '操作失败'
    const fieldErrors = data?.fieldErrors || null

    if (status === 401 && !isAuthUrl(config?.url)) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(FAMILY_ID_KEY)
      showToast('登录已过期，请重新登录')
      window.location.assign('/login')
      return Promise.reject(error)
    }

    const hasFieldErrors = fieldErrors && Object.keys(fieldErrors).length > 0
    if (!hasFieldErrors) {
      showToast(message)
    }

    return Promise.reject({ ...error, status, message, fieldErrors })
  },
)

export default request
