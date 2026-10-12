import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { setLocale } from '@/locales'
import { usePermissionStore, useTabStore } from '@/store'
import TabBar from '../index.vue'

vi.mock('@/composables', () => ({
  useMenu: () => ({ handleMenuSelect: vi.fn() }),
}))

const VueDraggableStub = defineComponent({
  name: 'VueDraggable',
  props: {
    modelValue: {
      type: Array,
      default: () => [],
    },
  },
  setup(props, { slots }) {
    return () => h('div', props.modelValue.map(element => slots.item?.({ element })))
  },
})

function mountTabBar(tabs, allMenus) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const tabStore = useTabStore()
  const permissionStore = usePermissionStore()
  tabStore.tabs = tabs
  tabStore.activeTab = tabs[0].path
  permissionStore.allMenus = allMenus

  return mount(TabBar, {
    global: {
      plugins: [pinia],
      stubs: {
        ContextMenu: true,
        IconRenderer: true,
        NInput: true,
        NPopover: true,
        VueDraggable: VueDraggableStub,
      },
    },
  })
}

function renderedTitles(wrapper) {
  return wrapper.findAll('.top-tab-label').map(item => item.text())
}

describe('shared bilingual tab titles', () => {
  afterEach(() => setLocale('zh-CN'))

  it('renders API English metadata for exact and parameter routes without mutating tab or menu data', async () => {
    const tabs = [
      {
        key: 'order-1001-tab',
        name: 'order-form',
        path: '/orders/1001?mode=review#approval',
        title: '订单表单',
        routeParams: { id: '1001' },
      },
      {
        key: 'order-1002-tab',
        name: 'order-form',
        path: '/orders/1002?mode=view#summary',
        title: '订单表单',
        routeParams: { id: '1002' },
      },
      {
        key: 'custom-tab',
        name: 'custom-form',
        path: '/custom/42?draft=true#details',
        title: '客户自定义标签',
        routeParams: { id: '42' },
      },
    ]
    const allMenus = [
      {
        id: 'dynamic-order',
        key: 'dynamic-order',
        path: '/orders/:id',
        label: '订单表单',
        labelI18n: { 'en-US': 'API Order Form' },
      },
      {
        id: 'exact-order-1001',
        key: 'exact-order-1001',
        path: '/orders/1001',
        label: '指定订单表单',
        labelI18n: { 'en-US': 'Custom Order 1001' },
      },
      {
        id: 'custom-route',
        key: 'custom-route',
        path: '/custom/:id',
        label: '客户自定义标签',
        labelI18n: {},
      },
    ]
    const originalTabs = structuredClone(tabs)
    const originalMenus = structuredClone(allMenus)

    setLocale('en-US')
    const wrapper = mountTabBar(tabs, allMenus)
    await nextTick()

    expect(renderedTitles(wrapper)).toEqual([
      'Custom Order 1001',
      'API Order Form',
      '客户自定义标签',
    ])
    expect(tabs).toEqual(originalTabs)
    expect(allMenus).toEqual(originalMenus)

    setLocale('zh-CN')
    await nextTick()
    expect(renderedTitles(wrapper)).toEqual(['订单表单', '订单表单', '客户自定义标签'])

    setLocale('en-US')
    await nextTick()
    expect(renderedTitles(wrapper)[0]).toBe('Custom Order 1001')

    wrapper.unmount()
  })
})
