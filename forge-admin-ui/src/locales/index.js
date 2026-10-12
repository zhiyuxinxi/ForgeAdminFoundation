import publicDeclaredMetadataLabels from './public-declared-metadata-labels'
import publicRenderExtraLabels from './public-render-extra-labels'
import publicRenderLabels from './public-render-labels'
import publicAppCenterLabels from './public-app-center-labels'
import publicFinalLabels from './public-final-labels'
import publicRemainingLabels from './public-remaining-labels'
import publicLowcodePageLabels from './public-lowcode-page-labels'
import publicAiCapabilityLabels from './public-ai-capability-labels'
import { watch } from 'vue'
import { createI18n } from 'vue-i18n'
import { collectBusinessMessages } from './business-messages'
import enUS from './en-US'
import originalUiLabels from './ui-labels'
import publicLabels from './public-labels'
import publicSystemLabels from './public-system-labels'
import publicModelLabels from './public-model-labels'
import publicMessageLabels from './public-message-labels'
import publicDatabaseLabels from './public-database-labels'
import publicFlowLabels from './public-flow-labels'
import sharedPublicLabels from './public-shared-labels'
import zhCN from './zh-CN'

const uiLabels = {
  ...publicDeclaredMetadataLabels,
  ...publicRenderExtraLabels,
  ...publicRenderLabels,
  ...publicAppCenterLabels,
  ...publicFinalLabels,
  ...publicRemainingLabels,
  ...publicLowcodePageLabels,
  ...publicAiCapabilityLabels, ...originalUiLabels, ...sharedPublicLabels, ...publicSystemLabels, ...publicModelLabels, ...publicDatabaseLabels, ...publicMessageLabels, ...publicFlowLabels, ...publicLabels }

export const LOCALE_STORAGE_KEY = 'forge.ui.locale'
export const SUPPORTED_LOCALES = Object.freeze(['zh-CN', 'en-US'])

const businessMessages = collectBusinessMessages(import.meta.glob([
  '../views/**/locales/zh-CN.js',
  '../views/**/locales/en-US.js',
], { eager: true, import: 'default' }))

export function normalizeLocale(value) {
  return /^en(?:[-_]|$)/i.test(String(value || '')) ? 'en-US' : 'zh-CN'
}

function savedLocale() {
  try {
    return normalizeLocale(globalThis.localStorage?.getItem(LOCALE_STORAGE_KEY))
  }
  catch {
    return 'zh-CN'
  }
}

export const i18n = createI18n({
  legacy: false,
  locale: savedLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': { ...zhCN, business: businessMessages['zh-CN'] },
    'en-US': { ...enUS, business: businessMessages['en-US'] },
  },
  missingWarn: false,
  fallbackWarn: false,
})

export const locale = i18n.global.locale
export const t = (...args) => i18n.global.t(...args)
export const getLocale = () => locale.value

export function setLocale(value) {
  locale.value = normalizeLocale(value)
  try {
    globalThis.localStorage?.setItem(LOCALE_STORAGE_KEY, locale.value)
  }
  catch {
    // Private/restricted browsers still support switching for the current tab.
  }
}

// This adapter is only for UI metadata. Never apply it to user-entered data.
const legacyKeys = new Map()
function collectKeys(messages, prefix = '') {
  for (const [key, value] of Object.entries(messages)) {
    const path = prefix ? `${prefix}.${key}` : key
    if (typeof value === 'string') {
      if (!value.includes('{'))
        legacyKeys.set(value, path)
    }
    else {
      collectKeys(value, path)
    }
  }
}
// Only the declared display namespace participates in legacy metadata localization.
// Regular business messages still require an explicit t('business...') call.
collectKeys(businessMessages['zh-CN'].display || {}, 'business.display')
collectKeys(zhCN)

export function localizeLabel(value, translations) {
  const current = locale.value
  if (translations && typeof translations[current] === 'string' && translations[current].trim())
    return translations[current]
  const key = typeof value === 'string' ? legacyKeys.get(value) : undefined
  return key ? t(key) : current === 'en-US' && typeof value === 'string' && Object.hasOwn(uiLabels, value) ? uiLabels[value] : value
}

// The template is product copy; values are inserted verbatim and are never translated.
export function formatUiText(template, values = []) {
  return String(localizeLabel(template)).replace(/\{(\d+)\}/g, (match, index) =>
    Number(index) < values.length ? String(values[Number(index)] ?? '') : match)
}

// Capture language when the request starts, including long-running exports and streams.
export function withLocaleHeaders(headers) {
  const result = new Headers(headers || {})
  result.set('Accept-Language', getLocale())
  return result
}

export function setupI18n(app) {
  app.use(i18n)
  watch(locale, (value) => {
    if (typeof document !== 'undefined')
      document.documentElement.lang = value
  }, { immediate: true })
  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (event) => {
      if (event.key === LOCALE_STORAGE_KEY)
        locale.value = normalizeLocale(event.newValue)
    })
  }
}
