import assert from 'node:assert/strict'
import test from 'node:test'
import labels from './labels.js'
import { translateDisplay, localizeMetadataLabel, normalizeLocale } from './display.js'
import { normalizeDesignerField, normalizeDictOptions } from '../utils/lowcode-runtime.js'
import { locale, L, setLocale, localizeLabel } from './index.js'

test('locale aliases, Chinese fallback and English UI messages', () => {
  assert.equal(normalizeLocale('en_US'), 'en-US')
  assert.equal(normalizeLocale('zh-Hans'), 'zh-CN')
  assert.equal(translateDisplay('保存', 'en-US'), 'Save')
  assert.equal(translateDisplay('保存', 'zh-CN'), '保存')
  assert.equal(translateDisplay('客户自填的任意正文', 'en-US'), '客户自填的任意正文')
})

test('all fixed UI translations preserve interpolation tokens', () => {
  const tokens = value => [...value.matchAll(/\{\d+\}/g)].map(match => match[0]).sort()
  for (const [source, translation] of Object.entries(labels)) {
    assert.ok(translation.trim(), source)
    assert.deepEqual(tokens(translation), tokens(source), source)
  }
  assert.equal(translateDisplay('请输入{0}', 'en-US', ['字段原值 A']), 'Enter 字段原值 A')
})

test('designer metadata survives normalization without changing field codes or default values', () => {
  const source = { fieldBinding: { fieldCode: 'status' }, label: '状态', labelI18n: { 'en-US': 'Status' },
    placeholderI18n: { 'en-US': 'Choose a status' }, props: { defaultValue: '待处理' },
    validation: { required: true, requiredMessage: '请选择状态', requiredMessageI18n: { 'en-US': 'Choose a status' } } }
  const snapshot = JSON.stringify(source)
  const field = normalizeDesignerField(source)
  assert.equal(localizeMetadataLabel(field.label, field.labelI18n, 'en-US'), 'Status')
  assert.equal(field.placeholderI18n['en-US'], 'Choose a status')
  assert.equal(field.requiredMessageI18n['en-US'], 'Choose a status')
  assert.equal(field.field, 'status')
  assert.equal(field.defaultValue, '待处理')
  assert.equal(JSON.stringify(source), snapshot)
})

test('dictionary labels use explicit English while preserving value identities', () => {
  const options = normalizeDictOptions([{ dictLabel: '待处理', dictLabelEn: 'Pending', dictValue: 'PENDING', extra: 7 }])
  assert.equal(localizeMetadataLabel(options[0].label, options[0].labelI18n, 'en-US'), 'Pending')
  assert.equal(options[0].value, 'PENDING')
  assert.equal(options[0].extra, 7)
  assert.equal(localizeMetadataLabel('项目自定义名称', { 'en-US': '' }, 'en-US'), '项目自定义名称')
})

test('switching languages persists preference without rewriting caller drafts', () => {
  const saved = new Map()
  const originalStorage = globalThis.localStorage
  globalThis.localStorage = { setItem: (key, value) => saved.set(key, value) }
  try {
    const draft = { name: '中文姓名', status: 'PENDING', notes: '不要翻译我的记录' }
    const before = JSON.stringify(draft)
    setLocale('en-US')
    assert.equal(locale.value, 'en-US')
    assert.equal(L('保存'), 'Save')
    assert.equal(localizeLabel('状态', { 'en-US': 'Status' }), 'Status')
    assert.equal(saved.get('forge.ui.locale'), 'en-US')
    setLocale('zh-CN')
    assert.equal(L('保存'), '保存')
    assert.equal(JSON.stringify(draft), before)
  } finally {
    globalThis.localStorage = originalStorage
    setLocale('zh-CN')
  }
})
