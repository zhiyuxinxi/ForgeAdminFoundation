import assert from 'node:assert/strict'
import test from 'node:test'
import { adaptBusinessTaskFields } from '../utils/business-task-form-adapter.js'
import { localizeMetadataLabel } from './display.js'

test('approval form adaptation preserves language metadata, range behavior and stored values', () => {
  const source = [{ field: 'period', label: '有效期', labelI18n: { 'en-US': 'Validity period' },
    componentType: 'daterange', writable: true, readable: true,
    props: { startPlaceholder: '开始日期', endPlaceholder: '结束日期', startPlaceholderI18n: { 'en-US': 'Start date' } },
    defaultValue: ['2026-10-04', '2026-10-05'],
    options: [{ dictLabel: '中文原值', dictLabelEn: 'Display only', dictValue: '中文原值' }] }]
  const before = JSON.stringify(source)
  const [field] = adaptBusinessTaskFields(source)
  assert.equal(localizeMetadataLabel(field.label, field.labelI18n, 'en-US'), 'Validity period')
  assert.equal(field.type, 'daterange')
  assert.equal(field.readonly, false)
  assert.equal(field.props.disabled, false)
  assert.equal(field.props.startPlaceholderI18n['en-US'], 'Start date')
  assert.deepEqual(field.defaultValue, ['2026-10-04', '2026-10-05'])
  assert.equal(field.options[0].value, '中文原值')
  assert.equal(localizeMetadataLabel(field.options[0].label, field.options[0].labelI18n, 'en-US'), 'Display only')
  assert.equal(JSON.stringify(source), before)
})
