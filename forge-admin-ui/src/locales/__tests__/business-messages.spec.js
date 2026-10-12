import { afterEach, expect, it, vi } from 'vitest'
import { computed } from 'vue'
import { collectBusinessMessages } from '../business-messages'
import { i18n, localizeLabel, setLocale, t } from '../index'
import { localizeMetadata } from '../metadata'

afterEach(() => setLocale('zh-CN'))

it('discovers the real page-owned language files and reacts to the shared language', () => {
  const title = computed(() => t('business.display.addUserButton'))
  expect(title.value).toBe('新增用户')
  setLocale('en-US')
  expect(title.value).toBe('Add user')
  expect(t('common.save')).toBe('Save')
  expect(Object.keys(i18n.global.getLocaleMessage('zh-CN').business.display).sort())
    .toEqual(Object.keys(i18n.global.getLocaleMessage('en-US').business.display).sort())
})

it('merges independent feature namespaces without modifying their source objects', () => {
  const purchase = { purchase: { count: '{count} purchase orders' } }
  const messages = collectBusinessMessages({
    '../views/purchase/locales/en-US.js': purchase,
    '../views/leave/locales/en-US.js': { leave: { title: 'Leave requests' } },
    '../views/leave/locales/zh-CN.js': { leave: { title: '请假申请' } },
  })
  expect(messages['en-US']).toEqual({ purchase: { count: '{count} purchase orders' }, leave: { title: 'Leave requests' } })
  expect(messages['zh-CN']).toEqual({ leave: { title: '请假申请' } })
  expect(messages['en-US'].purchase).not.toBe(purchase.purchase)
})

it('localizes real runtime metadata while preserving record values, defaults and explicit English', () => {
  const source = [{ title: '名称', field: 'amount', value: '采购金额(分)', defaultValue: '采购金额(分)', props: { placeholder: '请输入' }, options: [{ label: '启用', value: 'CENTER' }] }]
  setLocale('en-US')
  const displayed = localizeMetadata(source)
  expect(displayed[0].title).toBe('Name')
  expect(displayed[0].props.placeholder).toBe('Enter a value')
  expect(displayed[0].options[0]).toEqual({ label: 'Enabled', value: 'CENTER' })
  expect(displayed[0].value).toBe('采购金额(分)')
  expect(displayed[0].defaultValue).toBe('采购金额(分)')
  expect(source[0].title).toBe('名称')
  expect(localizeLabel('采购金额(分)', { 'en-US': 'Purchase value in cents' })).toBe('Purchase value in cents')
  expect(localizeLabel('客户自己填写的采购说明')).toBe('客户自己填写的采购说明')
  setLocale('zh-CN')
  expect(localizeMetadata(source)).toEqual(source)
})

it('compiles the delivered display catalog and preserves names in management confirmations', () => {
  const error = vi.spyOn(console, 'error').mockImplementation(() => {})
  const warning = vi.spyOn(console, 'warn').mockImplementation(() => {})
  try {
    for (const language of ['zh-CN', 'en-US']) {
      setLocale(language)
      const messages = i18n.global.getLocaleMessage(language).business.display
      for (const [key, value] of Object.entries(messages))
        expect(t(`business.display.${key}`), key).toBe(value)
    }
    expect(localizeLabel('组织架构')).toBe('Organization structure')
    expect(t('business.admin.deleteUser', { value0: '张三' })).toBe('Delete user "张三"? This cannot be undone.')
    expect(error).not.toHaveBeenCalled()
    expect(warning).not.toHaveBeenCalled()
  }
  finally {
    error.mockRestore()
    warning.mockRestore()
  }
})

it('rejects ambiguous feature owners instead of silently overwriting their messages', () => {
  expect(() => collectBusinessMessages({
    '../views/a/locales/en-US.js': { purchase: { title: 'Purchase orders' } },
    '../views/b/locales/en-US.js': { purchase: { title: 'Invoices' } },
  })).toThrow('Duplicate business message')
})
