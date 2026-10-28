import { formatUiText, locale, localizeLabel } from '@/locales'
import enUS from './locales/en-US'
import zhCN from './locales/zh-CN'

const messages = { 'zh-CN': zhCN.external, 'en-US': enUS.external }
const sourceKeys = Object.fromEntries(Object.entries(zhCN.external).map(([key, value]) => [value, key]))

export function L(value, values = []) {
  if (typeof value !== 'string')
    return value
  const key = sourceKeys[value]
  return formatUiText((key && messages[locale.value]?.[key]) || localizeLabel(value), values)
}

export function localizeExternalFields(value) {
  if (Array.isArray(value))
    return value.map(localizeExternalFields)
  if (!value || typeof value !== 'object')
    return value
  const result = { ...value }
  for (const key of ['label', 'title', 'placeholder', 'description', 'tooltip', 'message', 'confirmText']) {
    if (typeof value[key] === 'string')
      result[key] = L(value[key])
  }
  for (const key of ['props', 'rules', 'actions', 'columns', 'fields']) {
    if (value[key] && typeof value[key] === 'object')
      result[key] = localizeExternalFields(value[key])
  }
  return result
}
