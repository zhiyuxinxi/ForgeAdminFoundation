import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'
import { setLocale } from '@/locales'
import RolePermissionSettings from '@/views/system/components/RolePermissionSettings.vue'

afterEach(() => setLocale('zh-CN'))

const CheckboxStub = {
  name: 'NCheckbox',
  props: {
    checked: Boolean,
    disabled: Boolean,
  },
  emits: ['update:checked'],
  template: `
    <button
      type="button"
      class="checkbox-stub"
      :disabled="disabled"
      @click="$emit('update:checked', !checked)"
    >
      <slot />
    </button>
  `,
}

function mountSettings(overrides = {}) {
  return mount(RolePermissionSettings, {
    props: {
      checkedKeys: [],
      permissionModules: [{
        key: 'module:order',
        name: '订单',
        nameEn: 'Orders',
        pages: [{
          key: 'page:order',
          name: '订单权限',
          nameEn: 'Order permissions',
          accessItem: {
            key: 'access:11',
            resourceIds: ['11'],
          },
          actionItems: [{
            key: 'button:edit',
            label: '编辑',
            sectionKey: 'button',
            resourceIds: ['21'],
            sources: [{ type: 3, kind: 'button', label: '按钮' }],
          }],
          showDataScopePanel: false,
          showFunctionPanel: true,
        }],
      }],
      dataScopeSettings: { defaultDataScope: 5, modules: [] },
      dataScopeOptions: [{ label: '仅本人数据', value: 5 }],
      defaultScopeEditable: false,
      linkPageAndActions: false,
      ...overrides,
    },
    global: {
      stubs: {
        NCheckbox: CheckboxStub,
        NButton: { template: '<button type="button"><slot name="icon" /><slot /></button>' },
        NDropdown: { template: '<div><slot /></div>' },
        NEmpty: { template: '<div class="empty-stub" />' },
        NInput: {
          inheritAttrs: false,
          template: '<input />',
        },
        NSkeleton: { template: '<span />' },
        NSwitch: { template: '<button type="button" />' },
      },
    },
  })
}

describe('role permission settings application mode', () => {
  it('switches display language without changing selected permissions or scope values', async () => {
    const props = {
      checkedKeys: ['21'],
      dataScopeSettings: { defaultDataScope: 5, modules: [] },
    }
    const before = structuredClone(props)
    const wrapper = mountSettings(props)
    try {
      await flushPromises()
      expect(wrapper.text()).toContain('订单权限')
      setLocale('en-US')
      await flushPromises()
      expect(wrapper.text()).toContain('Order permissions')
      expect(wrapper.text()).toContain('Edit')
      expect(wrapper.text()).toContain('Role default:')
      expect(wrapper.emitted('update:checkedKeys')).toBeUndefined()
      expect(wrapper.emitted('update:dataScopeSettings')).toBeUndefined()
      expect(props).toEqual(before)
      await wrapper.find('.page-card-title .checkbox-stub').trigger('click')
      expect(wrapper.emitted('update:checkedKeys').at(-1)[0]).toEqual(['21', '11'])
      setLocale('zh-CN')
      await flushPromises()
      expect(wrapper.text()).toContain('订单权限')
      expect(props).toEqual(before)
    }
    finally {
      wrapper.unmount()
    }
  })

  it('keeps actions unchanged when page entry is selected', async () => {
    const wrapper = mountSettings()
    await flushPromises()

    await wrapper.find('.page-card-title .checkbox-stub').trigger('click')

    const updates = wrapper.emitted('update:checkedKeys') || []
    expect(updates.at(-1)?.[0]).toEqual(['11'])
    expect(updates.at(-1)?.[0]).not.toContain('21')
  })

  it('keeps page entry independent when an action is selected', async () => {
    const wrapper = mountSettings()
    await flushPromises()

    await wrapper.find('.function-grid .checkbox-stub').trigger('click')

    const updates = wrapper.emitted('update:checkedKeys') || []
    expect(updates.at(-1)?.[0]).toEqual(['21'])
    expect(updates.at(-1)?.[0]).not.toContain('11')
  })

  it('renders the role default data scope as read-only', async () => {
    const wrapper = mountSettings()
    await flushPromises()

    expect(wrapper.find('.default-scope-button.is-readonly').exists()).toBe(true)
    expect(wrapper.text()).toContain('角色默认：')
    expect(wrapper.text()).toContain('仅本人数据')
  })
})
