import { isExternal } from '@/utils/is'

function trimString(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeRoutePath(value, fallback = '') {
  const raw = trimString(value) || fallback
  if (!raw) {
    return ''
  }
  return raw.startsWith('/') ? raw : `/${raw}`
}

function normalizeRedirectPathValue(value, fallback = '/') {
  const raw = trimString(value)
  if (!raw || raw.includes('://') || raw.startsWith('//')) {
    return fallback
  }
  return raw.startsWith('/') ? raw : `/${raw}`
}

function parseEnvJsonMap(rawValue) {
  const value = trimString(rawValue)
  if (!value) {
    return {}
  }

  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch (error) {
    console.warn('[SSO] VITE_SSO_TARGET_BASE_URLS 解析失败:', error)
    return {}
  }
}

export function normalizeSsoBaseUrl(value) {
  return trimString(value).replace(/\/+$/, '')
}

export const SSO_BRIDGE_ROUTE = normalizeRoutePath(import.meta.env.VITE_SSO_BRIDGE_ROUTE)
export const DEFAULT_SSO_TARGET_CLIENT = trimString(import.meta.env.VITE_SSO_TARGET_CLIENT)
export const DEFAULT_REPORT_BASE_URL = normalizeSsoBaseUrl(import.meta.env.VITE_REPORT_UI_BASE_URL)
export const REPORT_HOST_FALLBACK = trimString(import.meta.env.VITE_REPORT_UI_HOST_FALLBACK)
export const REPORT_PATH_PREFIX = normalizeRoutePath(import.meta.env.VITE_REPORT_UI_PATH_PREFIX)

const envDefaultRedirectMap = parseEnvJsonMap(import.meta.env.VITE_SSO_DEFAULT_REDIRECTS)
const DEFAULT_SSO_ENTRY_MAP = Object.entries(envDefaultRedirectMap).reduce((acc, [key, value]) => {
  const redirectPath = normalizeRedirectPathValue(value, '')
  if (key && redirectPath) {
    acc[key] = redirectPath
  }
  return acc
}, {})

const reportDefaultRedirect = normalizeRedirectPathValue(import.meta.env.VITE_REPORT_UI_DEFAULT_REDIRECT, '')
if (DEFAULT_SSO_TARGET_CLIENT && reportDefaultRedirect && !DEFAULT_SSO_ENTRY_MAP[DEFAULT_SSO_TARGET_CLIENT]) {
  DEFAULT_SSO_ENTRY_MAP[DEFAULT_SSO_TARGET_CLIENT] = reportDefaultRedirect
}

export function getDefaultSsoRedirectPath(targetClient = DEFAULT_SSO_TARGET_CLIENT) {
  return DEFAULT_SSO_ENTRY_MAP[targetClient] || '/'
}

export function normalizeSsoRedirectPath(value, targetClient = DEFAULT_SSO_TARGET_CLIENT) {
  const raw = trimString(value)
  if (!raw || raw.includes('://') || raw.startsWith('//')) {
    return getDefaultSsoRedirectPath(targetClient)
  }
  return raw.startsWith('/') ? raw : `/${raw}`
}

export function parseExternalSsoTarget(rawTarget, targetClient = DEFAULT_SSO_TARGET_CLIENT) {
  const target = trimString(rawTarget)
  if (!target || !isExternal(target)) {
    return null
  }

  try {
    const targetUrl = new URL(target)
    const baseUrl = normalizeSsoBaseUrl(`${targetUrl.origin}${targetUrl.pathname}`)
    let redirectPath = ''

    if (targetUrl.hash) {
      const hashValue = targetUrl.hash.replace(/^#/, '')
      if (hashValue.startsWith('/')) {
        const [hashPath] = hashValue.split('?')
        redirectPath = hashPath
      }
    }

    return {
      baseUrl,
      redirectPath: normalizeSsoRedirectPath(redirectPath, targetClient),
    }
  }
  catch {
    return null
  }
}

export function getConfiguredSsoTargetBaseUrls() {
  const envMap = parseEnvJsonMap(import.meta.env.VITE_SSO_TARGET_BASE_URLS)
  const normalizedMap = Object.entries(envMap).reduce((acc, [key, value]) => {
    const normalizedValue = normalizeSsoBaseUrl(value)
    if (normalizedValue) {
      acc[key] = normalizedValue
    }
    return acc
  }, {})

  const legacyReportBaseUrl = DEFAULT_REPORT_BASE_URL

  if (DEFAULT_SSO_TARGET_CLIENT && legacyReportBaseUrl && !normalizedMap[DEFAULT_SSO_TARGET_CLIENT]) {
    normalizedMap[DEFAULT_SSO_TARGET_CLIENT] = legacyReportBaseUrl
  }

  return normalizedMap
}

export function resolveSsoTargetBaseUrl({ targetClient = DEFAULT_SSO_TARGET_CLIENT, preferredBaseUrl = '' } = {}) {
  const normalizedPreferredBaseUrl = normalizeSsoBaseUrl(preferredBaseUrl)
  if (normalizedPreferredBaseUrl) {
    return normalizedPreferredBaseUrl
  }

  const configuredBaseUrls = getConfiguredSsoTargetBaseUrls()
  return configuredBaseUrls[targetClient] || ''
}
