export const AUTH_TOKEN_KEY = 'homechef.token'

function readStoredToken() {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage.getItem(AUTH_TOKEN_KEY)
}

function getSafeRedirect(redirect) {
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }

  return { name: 'dishes' }
}

export function createAuthGuard(getToken = readStoredToken) {
  return (to) => {
    const hasToken = Boolean(getToken())
    const requiresAuth = to.matched?.some((record) => record.meta?.requiresAuth)

    if (requiresAuth && !hasToken) {
      return {
        name: 'login',
        query: { redirect: to.fullPath },
      }
    }

    if (to.name === 'login' && hasToken) {
      return getSafeRedirect(to.query?.redirect)
    }

    return true
  }
}
