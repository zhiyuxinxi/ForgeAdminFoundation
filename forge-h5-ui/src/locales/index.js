import { ref, watch } from 'vue'
import { LOCALE_STORAGE_KEY, normalizeLocale, translateDisplay, localizeMetadataLabel } from './display.js'

function readSavedLocale() {
  try {
    return normalizeLocale(globalThis.localStorage?.getItem(LOCALE_STORAGE_KEY)
      || globalThis.uni?.getStorageSync(LOCALE_STORAGE_KEY))
  } catch {
    return 'zh-CN'
  }
}

export const locale = ref(readSavedLocale())
export const getLocale = () => locale.value
export const L = (value, parameters) => translateDisplay(value, locale.value, parameters)
export const localizeLabel = (value, translations) => localizeMetadataLabel(value, translations, locale.value)

export function setLocale(value) {
  locale.value = normalizeLocale(value)
  try {
    globalThis.localStorage?.setItem(LOCALE_STORAGE_KEY, locale.value)
    globalThis.uni?.setStorageSync(LOCALE_STORAGE_KEY, locale.value)
  } catch {
    // Switching still works when persistent storage is unavailable.
  }
}

const pageTitles = {
  'pages/login/index': '登录',
  'pages/index/index': 'Forge H5',
  'pages/message/index': '消息中心',
  'pages/todo': '待办',
  'pages/todo-detail': '审批详情',
  'pages/mine/index': '我的',
  'pages/demo/loading/index': '组件演示',
  'pages/app-entry': '应用功能',
  'pages/lowcode-runtime': '低代码应用',
}

function updateNativeLanguage() {
  const runtime = globalThis.uni
  if (typeof document !== 'undefined') document.documentElement.lang = locale.value
  runtime?.setLocale?.(locale.value === 'en-US' ? 'en' : 'zh-Hans')
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
  const route = pages[pages.length - 1]?.route
  if (pageTitles[route]) runtime?.setNavigationBarTitle?.({ title: L(pageTitles[route]) })
}

export function setupLocale(app) {
  app.config.globalProperties.$L = L
  app.mixin({ onShow: updateNativeLanguage })
  watch(locale, updateNativeLanguage, { immediate: true })
  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (event) => {
      if (event.key === LOCALE_STORAGE_KEY) locale.value = normalizeLocale(event.newValue)
    })
  }
}
