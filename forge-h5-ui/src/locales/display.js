import labels from './labels.js'

export const LOCALE_STORAGE_KEY = 'forge.ui.locale'
export const SUPPORTED_LOCALES = Object.freeze(['zh-CN', 'en-US'])

export function normalizeLocale(value) {
  return /^en(?:[-_]|$)/i.test(String(value || '')) ? 'en-US' : 'zh-CN'
}

export function translateDisplay(value, language, parameters = []) {
  if (typeof value !== 'string') return value
  const pattern = normalizeLocale(language) === 'en-US' && Object.hasOwn(labels, value)
    ? labels[value]
    : value
  return pattern.replace(/\{(\d+)\}/g, (match, index) =>
    Object.hasOwn(parameters, index) ? String(parameters[index] ?? '') : match)
}

export function localizeMetadataLabel(value, translations, language) {
  const translated = translations?.[normalizeLocale(language)]
  return typeof translated === 'string' && translated.trim()
    ? translated
    : translateDisplay(value, language)
}
