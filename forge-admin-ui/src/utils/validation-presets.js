import { localizeLabel } from '@/locales'

export const COMMON_VALIDATION_PRESETS = [
  {
    label: '手机号',
    value: 'PHONE',
    pattern: '^1[3-9]\\d{9}$',
    message: '请输入正确的手机号',
  },
  {
    label: '邮箱',
    value: 'EMAIL',
    pattern: '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$',
    message: '请输入正确的邮箱地址',
  },
  {
    label: '身份证号',
    value: 'ID_CARD',
    pattern: '^(\\d{15}|\\d{17}[0-9Xx])$',
    message: '请输入正确的身份证号',
  },
  {
    label: '银行卡号',
    value: 'BANK_CARD',
    pattern: '^\\d{12,19}$',
    message: '请输入正确的银行卡号',
  },
  {
    label: '固定电话',
    value: 'TEL',
    pattern: '^(0\\d{2,3}-?)?\\d{7,8}$',
    message: '请输入正确的固定电话',
  },
  {
    label: '统一社会信用代码',
    value: 'USCI',
    pattern: '^[0-9A-Z]{18}$',
    message: '请输入正确的统一社会信用代码',
  },
  {
    label: '整数',
    value: 'INTEGER',
    pattern: '^-?\\d+$',
    message: '请输入整数',
  },
  {
    label: '非负数',
    value: 'NON_NEGATIVE_NUMBER',
    pattern: '^(0|[1-9]\\d*)(\\.\\d+)?$',
    message: '请输入非负数',
  },
  {
    label: '网址',
    value: 'URL',
    pattern: '^(https?:\\/\\/)?[\\w.-]+\\.[A-Za-z]{2,}([/?#].*)?$',
    message: '请输入正确的网址',
  },
]

export function getValidationPreset(value) {
  return COMMON_VALIDATION_PRESETS.find(item => item.value === value) || null
}

export function buildValidationRuleFromPreset(value, patch = {}) {
  const preset = getValidationPreset(value)
  if (!preset)
    return null
  return {
    key: value,
    preset: value,
    pattern: preset.pattern,
    get message() { return localizeLabel(patch.message || preset.message, patch.messageI18n) },
    trigger: patch.trigger || ['blur', 'change'],
  }
}

export function normalizeValidationRules(validation = {}) {
  const rules = []
  const presetCodes = normalizePresetCodes(validation)
  presetCodes.forEach((code) => {
    const rule = buildValidationRuleFromPreset(code, validation)
    if (rule)
      rules.push(rule)
  })
  if (validation.pattern) {
    rules.push({
      key: validation.key || 'CUSTOM_PATTERN',
      pattern: validation.pattern,
      get message() { return localizeLabel(validation.patternMessage || validation.message || '字段格式不正确', validation.patternMessageI18n || validation.messageI18n) },
      trigger: validation.trigger || ['blur', 'change'],
    })
  }
  if (Array.isArray(validation.rules))
    rules.push(...validation.rules.map(rule => ({ ...(rule || {}), get message() { return localizeLabel(rule?.message, rule?.messageI18n) } })))
  return rules
}

export function normalizeRulePattern(rule = {}) {
  const next = { ...rule }
  if (typeof next.pattern === 'string' && next.pattern) {
    try {
      next.pattern = new RegExp(next.pattern)
    }
    catch {
      delete next.pattern
    }
  }
  return next
}

function normalizePresetCodes(validation = {}) {
  const value = validation.presetCodes || validation.presets || validation.preset || validation.presetCode || validation.commonRule
  if (Array.isArray(value))
    return value.filter(Boolean)
  return value ? [value] : []
}
