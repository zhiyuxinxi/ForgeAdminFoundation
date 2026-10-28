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

/**
 * 基于运行 location 构造同源 hash 路由并导航（子路径部署安全的登录重定向）。
 *
 * 背景：H5 部署在子路径（如 /forge-h5/）时，直接调用全局 uni.reLaunch 可能因全局 uni
 * 路由 API 缺失而落入整页导航兜底（window.location.href = '/pages/...'），丢失基址与
 * hash 导致 Nginx 404。只改写 location.hash 不会触碰 pathname，因此对开发根路径与
 * 任意受控子路径都成立，且不硬编码主机/IP。
 *
 * @param {string} pagePathWithQuery 页面路径（可带 query），如 /pages/login/index?redirect=…
 */
export function navigateToHashRoute(pagePathWithQuery) {
  if (typeof window === 'undefined' || typeof window.location === 'undefined') {
    return false
  }
  const nextHash = String(pagePathWithQuery || '').startsWith('#')
    ? String(pagePathWithQuery)
    : `#${pagePathWithQuery}`
  if (window.location.hash === nextHash) {
    return false
  }
  window.location.hash = nextHash
  return true
}

/**
 * 归一化登录回跳目标：仅允许本地页面路径，杜绝开放重定向。
 * 拒绝协议（http/https）、双斜线、javascript:、反斜线、路径穿越；非法值回退首页。
 *
 * @param {*} value 原始 redirect 值（onLoad 已由路由解码一次）
 * @returns {string} 安全的本地面路径或首页
 */
export function normalizeRedirectTarget(value) {
  const raw = String(value ?? '').trim()
  if (/^\/pages\//.test(raw)
    && !/https?:|javascript\s*:|\\|(^|\/)\.\./.test(raw)
    && !raw.startsWith('//')) {
    return raw
  }
  return HOME_PAGE
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
  // H5：用同源 hash 导航（只改 location.hash，保持任意子路径基址），避免全局 uni 路由
  // API 缺失时整页导航丢失 base/hash 导致 404；非 H5 平台继续走 uni 路由。
  if (typeof window !== 'undefined' && typeof window.location !== 'undefined') {
    navigateToHashRoute(url)
    return
  }
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
