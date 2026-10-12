import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { computed, defineComponent, h, nextTick, ref } from 'vue'
import enUS from '../en-US'
import { getLocale, i18n, locale, LOCALE_STORAGE_KEY, localizeLabel, normalizeLocale, setLocale, t } from '../index'
import zhCN from '../zh-CN'

afterEach(() => {
  vi.restoreAllMocks()
  setLocale('zh-CN')
  localStorage.removeItem(LOCALE_STORAGE_KEY)
})

describe('pC language state', () => {
  it('normalizes supported language families and defaults to Chinese', () => {
    expect(normalizeLocale('en-GB')).toBe('en-US')
    expect(normalizeLocale('en_US')).toBe('en-US')
    expect(normalizeLocale('de-DE')).toBe('zh-CN')
    expect(normalizeLocale(null)).toBe('zh-CN')
  })

  it('uses the same language for translated content and request headers', () => {
    setLocale('en-US')
    expect(getLocale()).toBe('en-US')
    expect(t('common.save')).toBe('Save')
    expect(localStorage.getItem(LOCALE_STORAGE_KEY)).toBe('en-US')
    expect(t('common.total', { count: 12 })).toBe('12 records')
  })

  it('continues switching when browser storage is denied', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('denied')
    })
    expect(() => setLocale('en-US')).not.toThrow()
    expect(t('common.confirm')).toBe('Confirm')
  })

  it('switches labels without remounting or losing an unsaved form', async () => {
    let mounts = 0
    const Form = defineComponent({
      setup() {
        mounts++
        const value = ref('')
        return () => h('div', [h('label', t('common.name')), h('input', {
          value: value.value,
          onInput: event => value.value = event.target.value,
        })])
      },
    })
    const wrapper = mount(Form, { global: { plugins: [i18n] } })
    await wrapper.get('input').setValue('原始内容 / original data')
    setLocale('en-US')
    await nextTick()
    expect(wrapper.get('label').text()).toBe('Name')
    expect(wrapper.get('input').element.value).toBe('原始内容 / original data')
    expect(mounts).toBe(1)
    wrapper.unmount()
  })

  it('reacts to explicit metadata translations and preserves unknown text', () => {
    const translated = computed(() => localizeLabel('采购申请', { 'en-US': 'Purchase request' }))
    expect(translated.value).toBe('采购申请')
    locale.value = 'en-US'
    expect(translated.value).toBe('Purchase request')
    expect(localizeLabel('客户原始记录')).toBe('客户原始记录')
    expect(localizeLabel('客户原始记录', { 'en-US': ' ' })).toBe('客户原始记录')
    expect(localizeLabel('采购申请', { 'en-US': ' ' })).toBe('Purchase request')
  })

  it('has matching nonempty language resources', () => {
    function keys(value, prefix = '') {
      return Object.entries(value).flatMap(([key, item]) => {
        const path = prefix ? `${prefix}.${key}` : key
        if (typeof item === 'string') {
          expect(item.trim(), path).not.toBe('')
          return [path]
        }
        return keys(item, path)
      })
    }
    expect(keys(enUS).sort()).toEqual(keys(zhCN).sort())
  })
})
