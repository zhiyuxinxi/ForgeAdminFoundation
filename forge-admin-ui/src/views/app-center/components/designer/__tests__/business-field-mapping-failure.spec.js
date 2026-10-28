import { shallowMount } from '@vue/test-utils'
import { afterEach, expect, it, vi } from 'vitest'
import BusinessFieldList from '../BusinessFieldList.vue'
import BusinessFieldManager from '../BusinessFieldManager.vue'

vi.mock('naive-ui', () => ({ useMessage: () => ({ success: vi.fn(), error: vi.fn() }) }))
vi.mock('@/api/business-app', () => ({
  businessObjectDesigner: vi.fn(),
  businessObjectFields: vi.fn(),
  businessObjectList: vi.fn(),
  createBusinessObjectField: vi.fn(),
  deleteBusinessObjectField: vi.fn(),
  sortBusinessObjectFields: vi.fn(),
  updateBusinessObjectField: vi.fn(),
}))
vi.mock('@/components/lowcode-builder/shared/DictTypeSelect.vue', () => ({ default: { render: () => null } }))
vi.mock('../BusinessFieldPropertyPanel.vue', () => ({ default: { render: () => null } }))

let wrapper
afterEach(() => {
  wrapper?.unmount()
  vi.unstubAllGlobals()
})

it('shows unchecked fields after a failed database inspection and restores real differences after recovery', async () => {
  vi.stubGlobal('matchMedia', () => ({ matches: false, addEventListener() {}, removeEventListener() {} }))
  const mapping = { fieldCode: 'name', columnName: 'name', syncStatus: 'MISSING_DATABASE_COLUMN' }
  wrapper = shallowMount(BusinessFieldManager, {
    props: {
      fields: [{ fieldCode: 'name', fieldName: '名称', columnName: 'name', fieldType: 'TEXT' }],
      tableMapping: { syncStatus: 'CHECK_FAILED', fields: [mapping] },
    },
    global: {
      stubs: {
        BusinessFieldList: false,
        ...Object.fromEntries(['NButton', 'NIcon', 'NTag', 'NInput', 'NSelect', 'NSwitch', 'NEmpty', 'NModal', 'NDrawer', 'NDrawerContent', 'NForm', 'NFormItem', 'NGrid', 'NGi'].map(name => [name, true])),
      },
      renderStubDefaultSlot: true,
    },
  })
  const fields = () => wrapper.findComponent(BusinessFieldList)
  expect(fields().text()).toContain('待检查')
  expect(fields().text()).not.toContain('数据库缺列')
  await wrapper.setProps({ tableMapping: { syncStatus: 'OUT_OF_SYNC', fields: [mapping] } })
  expect(fields().text()).toContain('数据库缺列')
  await wrapper.setProps({ tableMapping: { syncStatus: 'IN_SYNC', fields: [{ ...mapping, syncStatus: 'IN_SYNC' }] } })
  expect(fields().text()).toContain('结构一致')
})
