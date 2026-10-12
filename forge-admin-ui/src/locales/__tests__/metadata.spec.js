import { afterEach, expect, it } from 'vitest'
import { computed } from 'vue'
import { setLocale } from '../index'
import { localizeMetadata } from '../metadata'

afterEach(() => setLocale('zh-CN'))

it('translates nested display metadata without translating values, defaults or mutating source', () => {
  const handler = () => true
  const source = [{ field: 'name', label: '名称', defaultValue: '名称', onChange: handler, props: { placeholder: '请输入', options: [{ label: '是', value: '是' }] }, rules: [{ message: '请输入用户名' }], children: [{ field: 'remark', label: '备注' }] }]
  const result = computed(() => localizeMetadata(source))
  expect(result.value[0].label).toBe('名称')
  setLocale('en-US')
  expect(result.value[0].label).toBe('Name')
  expect(result.value[0].props.options[0]).toEqual({ label: 'Yes', value: '是' })
  expect(result.value[0].children[0].label).toBe('Notes')
  expect(result.value[0].rules[0].message).toBe('Enter your username')
  expect(result.value[0].defaultValue).toBe('名称')
  expect(result.value[0].onChange).toBe(handler)
  expect(source[0].label).toBe('名称')
})

it('honors custom English field names and falls back for missing translations', () => {
  setLocale('en-US')
  expect(localizeMetadata({ label: '装置编码', labelI18n: { 'en-US': 'Unit code' }, value: '原值' }))
    .toMatchObject({ label: 'Unit code', value: '原值' })
  expect(localizeMetadata({ label: '未提供英文的业务字段' }).label).toBe('未提供英文的业务字段')
})
