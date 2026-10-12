
import { withLocaleHeaders } from '@/locales'
import { computed, reactive } from 'vue'

const DEFAULT_LOADING_TEXT = '数据加载中，请稍候...'
const ROUTE_LOADING_TEXT = '页面加载中，请稍候...'
const SUBMIT_LOADING_TEXT = '数据提交中，请稍候...'
const EXPORT_LOADING_TEXT = '文件导出处理中，请稍候...'
const DOWNLOAD_LOADING_TEXT = '文件下载处理中，请稍候...'
const UPLOAD_LOADING_TEXT = '文件上传中，请稍候...'
const DEFAULT_SHOW_DELAY = 280
const DEFAULT_MIN_VISIBLE_MS = 220

const state = reactive({
  entries: [],
  visible: false,
  message: DEFAULT_LOADING_TEXT,
})

let tokenSeed = 0
let showTimer = null
let hideTimer = null
let visibleSince = 0

function normalizeText(text) {
  return String(text || '').trim()
}

function normalizeUrl(url) {
  return String(url || '').toLowerCase()
}

function normalizeMethod(method) {
  return String(method || 'get').toLowerCase()
}

function readHeader(headers, name) {
  if (!headers)
    return undefined

  if (typeof headers.get === 'function')
    return headers.get(name)

  const lowerName = name.toLowerCase()
  const foundKey = Object.keys(headers).find(key => key.toLowerCase() === lowerName)
  return foundKey ? headers[foundKey] : undefined
}

function hasExplicitGlobalLoading(options = {}) {
  return options.globalLoading === true
    || options.forceGlobalLoading === true
    || normalizeText(options.globalLoadingType || options.loadingType)
    || normalizeText(options.globalLoadingText || options.loadingText || options.text || options.message)
}

function hasTransferGlobalLoadingHint(options = {}) {
  const url = normalizeUrl(options.url)
  return options.data instanceof FormData
    || url.includes('upload')
    || url.includes('import')
    || url.includes('export')
    || url.includes('download')
}

function shouldAttachRequestGlobalLoading(options = {}) {
  return hasExplicitGlobalLoading(options) || hasTransferGlobalLoadingHint(options)
}

function isGlobalLoadingSkipped(options = {}) {
  const headerSkip = readHeader(options.headers, 'X-Skip-Global-Loading')
  return options.skipGlobalLoading === true
    || options.globalLoading === false
    || headerSkip === true
    || String(headerSkip || '').toLowerCase() === 'true'
}

function normalizeDelay(value, fallback) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue >= 0 ? numberValue : fallback
}

function resolveShowDelay(options = {}) {
  return normalizeDelay(options.globalLoadingDelay ?? options.loadingDelay, DEFAULT_SHOW_DELAY)
}

export function resolveRequestLoadingText(options = {}) {
  const explicitText = normalizeText(options.globalLoadingText || options.loadingText || options.text || options.message)
  if (explicitText)
    return explicitText

  const type = normalizeText(options.globalLoadingType || options.loadingType).toLowerCase()
  if (type === 'route')
    return ROUTE_LOADING_TEXT
  if (type === 'upload')
    return UPLOAD_LOADING_TEXT
  if (type === 'export')
    return EXPORT_LOADING_TEXT
  if (type === 'download')
    return DOWNLOAD_LOADING_TEXT
  if (type === 'submit')
    return SUBMIT_LOADING_TEXT

  const url = normalizeUrl(options.url)
  if (url.includes('upload') || options.data instanceof FormData)
    return UPLOAD_LOADING_TEXT
  if (url.includes('export'))
    return EXPORT_LOADING_TEXT
  if (url.includes('download'))
    return DOWNLOAD_LOADING_TEXT

  const method = normalizeMethod(options.method)
  if (['post', 'put', 'patch', 'delete'].includes(method))
    return SUBMIT_LOADING_TEXT

  return DEFAULT_LOADING_TEXT
}

function applyDocumentLock(locked) {
  if (typeof document === 'undefined')
    return

  const className = 'forge-global-loading-locked'
  document.documentElement.classList.toggle(className, locked)
  document.body?.classList.toggle(className, locked)
}

function syncMessage() {
  state.message = state.entries[state.entries.length - 1]?.text || DEFAULT_LOADING_TEXT
}

function clearShowTimer() {
  if (!showTimer)
    return

  clearTimeout(showTimer)
  showTimer = null
}

function clearHideTimer() {
  if (!hideTimer)
    return

  clearTimeout(hideTimer)
  hideTimer = null
}

function showNow() {
  clearShowTimer()
  if (state.entries.length === 0)
    return

  clearHideTimer()
  syncMessage()
  state.visible = true
  visibleSince = Date.now()
  applyDocumentLock(true)
}

function hideNow() {
  clearShowTimer()
  clearHideTimer()
  state.visible = false
  visibleSince = 0
  state.message = DEFAULT_LOADING_TEXT
  applyDocumentLock(false)
}

function scheduleShow() {
  clearHideTimer()
  syncMessage()
  if (state.visible) {
    applyDocumentLock(true)
    return
  }
  if (showTimer)
    return

  const delay = state.entries[state.entries.length - 1]?.delay ?? DEFAULT_SHOW_DELAY
  if (delay <= 0) {
    showNow()
    return
  }

  showTimer = setTimeout(showNow, delay)
}

function scheduleHide() {
  clearShowTimer()
  if (!state.visible) {
    hideNow()
    return
  }

  const elapsed = Date.now() - visibleSince
  if (elapsed >= DEFAULT_MIN_VISIBLE_MS) {
    hideNow()
    return
  }

  clearHideTimer()
  hideTimer = setTimeout(hideNow, DEFAULT_MIN_VISIBLE_MS - elapsed)
}

function syncState() {
  if (state.entries.length > 0)
    scheduleShow()
  else
    scheduleHide()
}

export function startGlobalLoading(options = {}) {
  if (isGlobalLoadingSkipped(options))
    return null

  const token = `global-loading-${Date.now()}-${++tokenSeed}`
  state.entries = [
    ...state.entries,
    {
      token,
      text: resolveRequestLoadingText(options),
      delay: resolveShowDelay(options),
      startedAt: Date.now(),
    },
  ]
  syncState()
  return token
}

export function finishGlobalLoading(token) {
  if (!token)
    return

  const nextEntries = state.entries.filter(item => item.token !== token)
  if (nextEntries.length === state.entries.length)
    return

  state.entries = nextEntries
  syncState()
}

export function clearGlobalLoading() {
  state.entries = []
  syncState()
}

export async function withGlobalLoading(task, options = {}) {
  const token = startGlobalLoading(options)
  try {
    return await task()
  }
  finally {
    finishGlobalLoading(token)
  }
}

export async function managedFetch(input, init = {}, options = {}) {
  const requestOptions = {
    ...options,
    method: init?.method || options.method || 'get',
    url: typeof input === 'string' ? input : input?.url,
    headers: init?.headers || options.headers,
  }
  const token = startGlobalLoading(requestOptions)
  let finished = false
  let fallbackTimer = null

  function finishOnce() {
    if (finished)
      return

    finished = true
    if (fallbackTimer) {
      clearTimeout(fallbackTimer)
      fallbackTimer = null
    }
    finishGlobalLoading(token)
  }

  try {
    const response = await fetch(input, { ...init, headers: withLocaleHeaders(init.headers || input?.headers) })
    const method = normalizeMethod(requestOptions.method)
    const hasReadableBody = response.body && method !== 'head' && response.status !== 204 && response.status !== 304

    if (!hasReadableBody) {
      finishOnce()
      return response
    }

    fallbackTimer = setTimeout(finishOnce, options.globalLoadingMaxBodyWaitMs || 60000)
    const bodyMethods = new Set(['arrayBuffer', 'blob', 'formData', 'json', 'text'])

    return new Proxy(response, {
      get(target, prop) {
        const value = Reflect.get(target, prop, target)
        if (bodyMethods.has(prop) && typeof value === 'function') {
          return async (...args) => {
            try {
              return await value.apply(target, args)
            }
            finally {
              finishOnce()
            }
          }
        }
        return value
      },
    })
  }
  catch (error) {
    finishOnce()
    throw error
  }
}

export function attachRequestGlobalLoading(config = {}) {
  if (config.__globalLoadingToken)
    return config
  if (!shouldAttachRequestGlobalLoading(config))
    return config

  const token = startGlobalLoading({
    ...config,
    url: config.url,
    method: config.method,
    data: config.data,
    headers: config.headers,
  })
  if (token)
    config.__globalLoadingToken = token

  return config
}

export function finishRequestGlobalLoading(config = {}) {
  const token = config?.__globalLoadingToken
  if (!token)
    return

  finishGlobalLoading(token)
  delete config.__globalLoadingToken
}

export function useGlobalLoading() {
  return {
    active: computed(() => state.visible),
    count: computed(() => state.entries.length),
    message: computed(() => state.message),
    state,
    start: startGlobalLoading,
    finish: finishGlobalLoading,
    clear: clearGlobalLoading,
  }
}
