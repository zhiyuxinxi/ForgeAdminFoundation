import { localizeLabel } from '@/locales'

export function getRowDisplayTitle(row = {}) {
  return firstText(
    row.businessSummary,
    localizeLabel(row.businessObjectName),
    localizeLabel(row.processName),
    row.processTitle,
    localizeLabel(row.modelName),
    localizeLabel(row.processDefinitionName),
    localizeLabel(row.taskName),
    row.title,
    row.processDefinitionKey,
    row.processDefKey,
    '-',
  )
}

export function getBusinessFormDisplayTitle(context = {}, fallback = '业务表单') {
  const objectName = localizeLabel(firstText(context.businessObjectName, context.objectName, context.appName, context.businessName))
  const summary = firstText(context.businessSummary, context.summary)
  if (objectName && summary && !summary.includes(objectName))
    return `${objectName} · ${summary}`
  return firstText(summary, objectName, localizeLabel(context.formName), localizeLabel(fallback))
}

export function getTaskDisplayName(rowOrName = {}, fallback = '-') {
  const row = rowOrName && typeof rowOrName === 'object' ? rowOrName : {}
  const taskName = firstText(
    typeof rowOrName === 'string' ? rowOrName : '',
    row.taskName,
    row.name,
  )
  const taskCode = firstText(
    row.taskDefKey,
    row.taskDefinitionKey,
    row.activityId,
    row.activityKey,
    row.nodeKey,
  )
  return firstText(localizeLabel(stripTrailingTaskCode(taskName, taskCode)), fallback)
}

export function firstText(...values) {
  for (const value of values) {
    const text = String(Array.isArray(value) ? value[0] || '' : value ?? '').trim()
    if (text)
      return text
  }
  return ''
}

function stripTrailingTaskCode(value, code) {
  let text = String(value ?? '').trim()
  const originalText = text
  const normalizedCode = String(code ?? '').trim()
  if (!text)
    return ''

  if (normalizedCode) {
    if (text === normalizedCode)
      return ''
    const escapedCode = escapeRegExp(normalizedCode)
    text = text
      .replace(new RegExp(`\\s*[（(【\\[]\\s*${escapedCode}\\s*[）)】\\]]\\s*$`), '')
      .replace(new RegExp(`\\s*[-:/|]\\s*${escapedCode}\\s*$`), '')
      .replace(new RegExp(`\\s+${escapedCode}\\s*$`), '')
      .trim()
    if (!text || text !== originalText)
      return text
  }

  const genericText = stripGenericTrailingCode(text)
  if (genericText && hasReadableLabel(genericText))
    return genericText
  return text
}

function stripGenericTrailingCode(value) {
  const text = String(value ?? '').trim()
  const pairedText = stripPairedTrailingCode(text)
  if (pairedText)
    return pairedText

  const parts = text.split(/\s+/)
  if (parts.length <= 1)
    return ''
  const candidate = parts.at(-1)
  if (!isTechnicalCode(candidate))
    return ''
  return text.slice(0, text.lastIndexOf(candidate)).trim()
}

function stripPairedTrailingCode(text) {
  const pairs = [
    ['（', '）'],
    ['(', ')'],
    ['【', '】'],
    ['[', ']'],
  ]
  for (const [open, close] of pairs) {
    if (!text.endsWith(close))
      continue
    const openIndex = text.lastIndexOf(open)
    if (openIndex <= 0)
      continue
    const candidate = text.slice(openIndex + open.length, -close.length).trim()
    if (isTechnicalCode(candidate))
      return text.slice(0, openIndex).trim()
  }
  return ''
}

function isTechnicalCode(value) {
  const text = String(value ?? '').trim()
  if (text.length < 3 || !isAsciiLetter(text.charCodeAt(0)))
    return false
  for (let index = 1; index < text.length; index += 1) {
    const code = text.charCodeAt(index)
    if (!isAsciiLetter(code) && !isDigit(code) && !['_', '.', '$', ':', '-'].includes(text[index]))
      return false
  }
  return true
}

function isAsciiLetter(code) {
  return (code >= 65 && code <= 90) || (code >= 97 && code <= 122)
}

function isDigit(code) {
  return code >= 48 && code <= 57
}

function hasReadableLabel(value) {
  return /[\u4E00-\u9FFF]/.test(value) || /\s/.test(value.trim())
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
