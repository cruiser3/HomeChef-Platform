export const AUTH_TOKEN_KEY = 'homechef.token'
export const FAMILY_ID_KEY = 'homechef.familyId'

function readStoredToken() {
  if (typeof window === 'undefined') return null
  return window.localStorage.getItem(AUTH_TOKEN_KEY)
}

function readStoredFamilyId() {
  if (typeof window === 'undefined') return null
  return window.localStorage.getItem(FAMILY_ID_KEY)
}

function getSafeRedirect(redirect) {
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }
  return { name: 'dishes' }
}

export function createAuthGuard() {
  return (to) => {
    const hasToken = Boolean(readStoredToken())
    const hasFamily = Boolean(readStoredFamilyId())
    const requiresAuth = to.matched?.some((record) => record.meta?.requiresAuth)
    const requiresFamily = to.matched?.some((record) => record.meta?.requiresFamily)

    if (requiresAuth && !hasToken) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    if (requiresFamily && hasToken && !hasFamily) {
      return { name: 'family-select', query: { redirect: to.fullPath } }
    }

    if (to.name === 'login' && hasToken) {
      return hasFamily ? getSafeRedirect(to.query?.redirect) : { name: 'family-select' }
    }

    return true
  }
}
