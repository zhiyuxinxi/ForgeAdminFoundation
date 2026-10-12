import { getNestedRuntimeValue } from '@/components/lowcode-builder/shared/runtime-rules'

export const DEFAULT_VISIBLE_ROW_ACTIONS = 2

export function resolveVisibleRowActions(value) {
  const configured = value === null || value === '' || value === undefined
    ? Number.NaN
    : Number(value)
  return Number.isInteger(configured) && configured >= 0 ? configured : DEFAULT_VISIBLE_ROW_ACTIONS
}

export function resolveActionColumnWidth(column = {}, actionCount = 0) {
  if (column.width !== undefined && column.width !== null && column.width !== '')
    return column.width
  const visibleCount = resolveVisibleRowActions(column.maxActionButtons)
  return Math.max(120, Math.min(actionCount, visibleCount) * 58 + (actionCount > visibleCount ? 58 : 0))
}

export function resolveSemanticCell(row = {}, key = '', config = {}, fallbackValue) {
  const type = config.renderType || config.type
  if (!['entity', 'relations'].includes(type))
    return null
  const titleField = String(config.titleField || config.textField || config.targetField || key || '').trim()
  const value = (titleField ? getNestedRuntimeValue(row, titleField) : undefined) ?? fallbackValue
  if (type === 'relations') {
    return {
      values: (Array.isArray(value) ? value : [value])
        .flatMap(item => String(item ?? '').split(/[、,，]/))
        .map(item => item.trim())
        .filter(Boolean),
    }
  }
  const title = String(value ?? '')
  const subtitleField = String(config.subtitleField || '').trim()
  const subtitle = subtitleField ? String(getNestedRuntimeValue(row, subtitleField) ?? '') : ''
  return { entity: true, title, subtitle: subtitle === title ? '' : subtitle, avatar: config.avatar === true }
}
