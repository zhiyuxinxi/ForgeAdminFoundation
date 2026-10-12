import { localizeLabel } from '@/locales'

const FALLBACK_APP_TITLE = '企业级中后台基础框架'

export function normalizePageTitle(value) {
  const title = String(value || '').trim()
  if (!title || /^%[^%]+%$/.test(title))
    return ''
  return title
}

export function getDefaultPageTitle() {
  return localizeLabel(normalizePageTitle(import.meta.env.VITE_TITLE) || FALLBACK_APP_TITLE)
}

export function getTenantPageBaseTitle(tenantStore) {
  return normalizePageTitle(tenantStore?.browserTitle)
    || normalizePageTitle(tenantStore?.systemName)
    || getDefaultPageTitle()
}

export function setDocumentTitle(title) {
  document.title = normalizePageTitle(title) || getDefaultPageTitle()
}
