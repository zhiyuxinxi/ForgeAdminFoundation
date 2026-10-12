import { afterEach, expect, it } from 'vitest'
import { buildValidationRuleFromPreset } from '@/utils/validation-presets'
import { localizeLabel, setLocale, t } from '../index'
import { localizeMetadata } from '../metadata'

afterEach(() => setLocale('zh-CN'))

it('localizes nested child/form-create display rules without changing values or validation patterns', () => {
  const source = { tabTitle: '子表明细', fields: [{ field: 'phone', label: '手机号', value: '手机号', validate: [{ message: '请输入正确的手机号', pattern: '^1[3-9]\\d{9}$' }], props: { endPlaceholder: '结束日期' } }], data: { title: '姓名' } }
  setLocale('en-US')
  const result = localizeMetadata(source)
  expect(result.tabTitle).toBe('Child records')
  expect(result.fields[0].validate[0]).toEqual({ message: 'Enter a valid mobile number', pattern: '^1[3-9]\\d{9}$' })
  expect(result.fields[0].value).toBe('手机号')
  expect(result.data).toBe(source.data)
  expect(source.tabTitle).toBe('子表明细')
})

it('changes validation text while preserving its business rule and custom translations', () => {
  const rule = buildValidationRuleFromPreset('EMAIL')
  const pattern = rule.pattern
  expect(rule.message).toBe('请输入正确的邮箱地址')
  setLocale('en-US')
  expect(rule.message).toBe('Enter a valid email address')
  expect(rule.pattern).toBe(pattern)
  expect(new RegExp(rule.pattern).test('person@example.com')).toBe(true)
  expect(new RegExp(rule.pattern).test('invalid')).toBe(false)
  expect(buildValidationRuleFromPreset('EMAIL', { message: '业务提示', messageI18n: { 'en-US': 'Use your work email' } }).message).toBe('Use your work email')
})

it('keeps interpolated customer data verbatim in confirmations and file errors', () => {
  setLocale('en-US')
  expect(t('customer.deleteFile', { value0: '姓名-{客户}.xlsx' })).toBe('Delete file "姓名-{客户}.xlsx"? This cannot be undone.')
  expect(t('customer.fileSize', { value0: 5 })).toBe('Files must not exceed 5 MB')
  expect(t('customer.childRequired', { value0: '项目甲', value1: 2, value2: 'Quantity' })).toBe('项目甲, row 2: fill in Quantity')
  expect(localizeLabel('客户自由输入')).toBe('客户自由输入')
})
