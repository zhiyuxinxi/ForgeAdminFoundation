import { useAuthStore } from '@/store'
import { getCurrentRoutePath, redirectToLogin } from './route'

export { HOME_PAGE, LOGIN_PAGE, getCurrentRoutePath, redirectToLogin } from './route'

export function toLogin(redirect = getCurrentRoutePath()) {
  redirectToLogin({ redirect })
}

export async function ensureLogin(options = {}) {
  const { redirect = getCurrentRoutePath(), loadUser = true } = options
  const authStore = useAuthStore()
  if (!authStore.isLogin) {
    toLogin(redirect)
    return false
  }

  if (loadUser && !authStore.userInfo) {
    try {
      await authStore.fetchUserInfo()
    }
    catch (error) {
      authStore.resetAuth()
      toLogin(redirect)
      return false
    }
  }

  return true
}
