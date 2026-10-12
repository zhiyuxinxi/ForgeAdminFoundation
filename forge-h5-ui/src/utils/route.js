export const LOGIN_PAGE = '/pages/login/index'
export const HOME_PAGE = '/pages/index/index'

export function getCurrentPage() {
  const pages = getCurrentPages()
  return pages[pages.length - 1]
}

export function getCurrentRoutePath(withQuery = true) {
  const current = getCurrentPage()
  if (!current?.route) {
    return HOME_PAGE
  }

  const path = `/${current.route}`
  if (!withQuery) {
    return path
  }

  const options = current?.options || {}
  const query = Object.keys(options)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(options[key])}`)
    .join('&')
  return query ? `${path}?${query}` : path
}

export function buildUrl(url, params = {}) {
  const query = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  if (!query) {
    return url
  }
  return `${url}${url.includes('?') ? '&' : '?'}${query}`
}

export function buildLoginUrl(redirect = getCurrentRoutePath()) {
  return buildUrl(LOGIN_PAGE, redirect ? { redirect } : {})
}

function callUniRoute(method, options = {}, fallbackMethod = 'reLaunch') {
  if (typeof uni !== 'undefined' && typeof uni?.[method] === 'function') {
    uni[method](options)
    return
  }

  if (fallbackMethod && fallbackMethod !== method && typeof uni !== 'undefined' && typeof uni?.[fallbackMethod] === 'function') {
    uni[fallbackMethod](options)
    return
  }

  if (typeof window !== 'undefined' && options.url) {
    window.location.href = options.url
  }
}

export function safeNavigateBack(options = {}) {
  const {
    delta = 1,
    fallback = HOME_PAGE,
    fallbackType = 'switchTab',
    success,
    fail,
  } = options
  const pages = getCurrentPages()

  if (pages.length > delta) {
    uni.navigateBack({ delta, success, fail })
    return
  }

  const method = fallbackType === 'reLaunch'
    ? 'reLaunch'
    : fallbackType === 'redirectTo'
      ? 'redirectTo'
      : 'switchTab'

  callUniRoute(method, {
    url: fallback,
    success,
    fail: (error) => {
      if (method !== 'reLaunch') {
        callUniRoute('reLaunch', { url: fallback, success, fail })
        return
      }
      fail?.(error)
    },
  })
}

export function redirectToLogin(options = {}) {
  const {
    redirect = getCurrentRoutePath(),
    method = 'reLaunch',
  } = options
  const url = buildLoginUrl(redirect)
  callUniRoute(method, { url }, 'redirectTo')
}

export function navigateWithLogin(url, options = {}) {
  const {
    isLogin = true,
    redirect = getCurrentRoutePath(),
    method = 'navigateTo',
    loginMethod = 'reLaunch',
  } = options

  if (!isLogin) {
    redirectToLogin({ redirect, method: loginMethod })
    return false
  }

  callUniRoute(method, {
    url,
    fail: () => {
      callUniRoute('navigateTo', { url })
    },
  })
  return true
}
