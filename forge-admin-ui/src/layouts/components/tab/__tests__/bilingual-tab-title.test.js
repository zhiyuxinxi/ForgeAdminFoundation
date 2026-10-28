import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h, nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
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
        key: 'ehss-tab',
        name: 'report-form',
        path: '/accident/report-form/EHSS?mode=review#approval',
        title: '事故报告表',
        routeParams: { type: 'EHSS' },
      },
      {
        key: 'ops-tab',
        name: 'report-form',
        path: '/accident/report-form/OPS?mode=view#summary',
        title: '事故报告表',
        routeParams: { type: 'OPS' },
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
        id: 'dynamic-report',
        key: 'dynamic-report',
        path: '/accident/report-form/:type',
        label: '事故报告表',
        labelI18n: { 'en-US': 'API Report Form' },
      },
      {
        id: 'exact-ehss',
        key: 'exact-ehss',
        path: '/accident/report-form/EHSS',
        label: 'EHSS事故报告表',
        labelI18n: { 'en-US': 'Customer EHSS Report' },
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
      'Customer EHSS Report',
      'API Report Form',
      '客户自定义标签',
    ])
    expect(tabs).toEqual(originalTabs)
    expect(allMenus).toEqual(originalMenus)

    setLocale('zh-CN')
    await nextTick()
    expect(renderedTitles(wrapper)).toEqual(['事故报告表', '事故报告表', '客户自定义标签'])

    setLocale('en-US')
    await nextTick()
    expect(renderedTitles(wrapper)[0]).toBe('Customer EHSS Report')

    wrapper.unmount()
  })
})
