import { flushPromises, mount, shallowMount } from '@vue/test-utils'
import { afterEach, expect, it, vi } from 'vitest'
import { nextTick, reactive } from 'vue'
import { buildRuntimeCrudProps } from '@/components/lowcode-builder/shared/runtime-crud-props'
import RuntimeCrudPage from '@/views/ai/crud-page.vue'
import AiCrudPage from '../AiCrudPage.vue'
import AiCrudRowExpand from '../AiCrudRowExpand.vue'
import AiForm from '../AiForm.vue'

const mocks = vi.hoisted(() => ({
  user: null,
  request: vi.fn(async () => ({ code: 200, data: { records: [], total: 0 } })),
  encrypted: vi.fn(),
  resolveError: vi.fn(),
  checkedRows: vi.fn(() => []),
  setCheckedKeys: vi.fn(),
}))
vi.mock('@/store', () => ({ useUserStore: () => mocks.user, useTabStore: () => ({}) }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: {}, params: {}, path: '/example/record', meta: {} }), useRouter: () => ({ push: vi.fn(), replace: vi.fn() }) }))
vi.mock('@/utils', () => ({ request: mocks.request, postEncrypt: mocks.encrypted, downloadFile: vi.fn() }))
vi.mock('@/utils/encrypt-request', () => ({ postEncrypt: mocks.encrypted }))
vi.mock('@/utils/http/helpers', () => ({ isAuthErrorCode: code => ['-8', 401, '401', 11007, 11008].includes(code), resolveResError: mocks.resolveError }))
vi.mock('@/api/ai', () => ({ crudConfigRender: vi.fn(), customQueryExecute: vi.fn() }))
vi.mock('@/api/business-app', () => ({ businessDocumentRuntimeBatch: vi.fn(), executeBusinessAction: vi.fn() }))
vi.mock('@/api/business-process', () => ({ startBusinessProcess: vi.fn() }))
vi.mock('@/api/formula', () => ({ previewFormula: vi.fn() }))
vi.mock('@/catalog', () => ({ default: {} }))
vi.mock('@/composables/useDict', () => ({ getDictData: vi.fn() }))
vi.mock('@/utils/page-title', () => ({ getDefaultPageTitle: () => 'Records' }))
vi.mock('@/components/common/AuthImage.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/common/SystemTableCell.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/DictTag.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/page-templates/ChildTableEditor.vue', () => ({ default: { render: () => null } }))
vi.mock('../AiCrudFlowDetail.vue', () => ({ default: { render: () => null } }))
vi.mock('../AiCrudImportModal.vue', () => ({ default: { render: () => null } }))
vi.mock('../AiCrudRowExpand.vue', () => ({ default: { render: () => null } }))
vi.mock('../AiCustomQuery.vue', () => ({ default: { render: () => null } }))
vi.mock('../AiForm.vue', () => ({ default: { methods: { restoreValidation() {} }, render: () => null } }))
vi.mock('../AiSearch.vue', () => ({ default: { render: () => null } }))
vi.mock('../AiTable.vue', () => ({ default: { methods: { getCheckedRows: () => mocks.checkedRows(), setCheckedKeys: keys => mocks.setCheckedKeys(keys) }, render: () => null } }))
vi.mock('@/components/lowcode-builder/page/ListPageGridDesigner.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/lowcode-builder/shared/FieldValueRenderer.vue', () => ({ default: { render: () => null } }))

const permissions = {
  add: 'example:record:add', detail: 'example:record:query', edit: 'example:record:edit',
  delete: 'example:record:remove', batchDelete: 'example:record:remove',
  import: 'example:record:add', export: 'example:record:query',
}
const wrappers = []
const uiStubs = Object.fromEntries([
  'NCard', 'NSpace', 'NButton', 'NIcon', 'NDropdown', 'NTooltip', 'NModal', 'NDrawer',
  'NDrawerContent', 'NSpin', 'NTabs', 'NTabPane', 'NAlert', 'NTag', 'NProgress',
  'NInput', 'NEmpty', 'NDataTable', 'NScrollbar', 'NResult', 'NFormItem', 'NSelect',
].map(name => [name, true]))

function crud(extra = {}, renderStubDefaultSlot = false) {
  const wrapper = shallowMount(AiCrudPage, {
    props: { lazy: true, loadDetailOnEdit: false, showImport: true, showExport: true,
      actionPermissions: permissions, apiConfig: { add: 'post@/example/record', update: 'put@/example/record/:id', delete: 'delete@/example/record/:id' }, ...extra },
    global: {
      stubs: {
        ...uiStubs,
        AiTable: false,
        ...(renderStubDefaultSlot
          ? { AiForm: false, NModal: { props: ['show'], template: '<div v-if="show"><slot /></div>' } }
          : {}),
      },
      renderStubDefaultSlot,
    },
  })
  wrappers.push(wrapper)
  return wrapper
}

it.each(['modal', 'flat'])('configured readable detail hides the default form in %s without hiding add or edit', async (formOpenMode) => {
  mocks.user = reactive({ isAdmin: true, permissions: [] })
  const wrapper = crud({
    formOpenMode,
    hideDefaultDetailContent: true,
    editSchema: [{ field: 'name', label: 'Name', type: 'input', required: true }],
    detailPanels: [{
      key: 'record',
      type: 'descriptions',
      title: 'Record',
      descriptions: { fields: [{ field: 'name', label: 'Name' }] },
    }],
  }, true)
  const setup = wrapper.vm.$.setupState
  const row = { id: 1, name: 'Readable record' }

  await setup.handleDetail(row)
  expect(wrapper.findComponent(AiForm).exists()).toBe(false)
  expect(wrapper.findComponent(AiCrudRowExpand).exists()).toBe(true)
  expect(setup.formData.name).toBe(row.name)

  await wrapper.setProps({ hideDefaultDetailContent: false })
  expect(wrapper.findComponent(AiForm).exists()).toBe(true)

  await wrapper.setProps({ hideDefaultDetailContent: true })
  await setup.handleAdd()
  expect(wrapper.findComponent(AiForm).exists()).toBe(true)
  expect(wrapper.findComponent(AiCrudRowExpand).exists()).toBe(false)

  await setup.handleEdit(row)
  expect(wrapper.findComponent(AiForm).exists()).toBe(true)
  expect(wrapper.findComponent(AiCrudRowExpand).exists()).toBe(false)
  expect(setup.formData.name).toBe(row.name)
})

function actionLabels(setup, actions) {
  const view = mount({ render: () => setup.renderActionColumn({ id: 1 }, actions, 10) })
  const text = view.text()
  view.unmount()
  return text
}

it('custom visible callbacks cannot bypass action permissions or display conditions', async () => {
  mocks.user = reactive({ isAdmin: false, permissions: [] })
  const wrapper = crud()
  const setup = wrapper.vm.$.setupState
  const action = { key: 'approve', label: 'Approve', permissionCode: 'example:record:approve', visible: () => true }
  expect(actionLabels(setup, [action])).toBe('-')
  mocks.user.permissions = [action.permissionCode]
  await nextTick()
  expect(actionLabels(setup, [action])).toBe('Approve')
  expect(actionLabels(setup, [{ ...action, visible: () => false }])).toBe('-')
  const displayCondition = { rules: [{ field: 'id', operator: 'EQ', value: 2 }] }
  expect(actionLabels(setup, [{ ...action, displayCondition }])).toBe('-')
})

it('custom callbacks recheck explicit permission before hooks and after permission revocation', async () => {
  const onClick = vi.fn()
  const beforeRowAction = vi.fn(() => true)
  const action = { key: 'approve', label: 'Approve', permissionCode: 'example:record:approve', visible: () => true, onClick }
  mocks.user = reactive({ isAdmin: false, permissions: [] })
  const wrapper = crud({ beforeRowAction })
  const setup = wrapper.vm.$.setupState
  await setup.handleCustomActionClick(action, { id: 1 })
  expect(beforeRowAction).not.toHaveBeenCalled()
  expect(onClick).not.toHaveBeenCalled()
  mocks.user.permissions = [action.permissionCode]
  await nextTick()
  await setup.handleCustomActionClick(action, { id: 1 })
  expect(onClick).toHaveBeenCalledOnce()
  mocks.user.permissions = []
  await nextTick()
  await setup.handleCustomActionClick(action, { id: 1 })
  expect(onClick).toHaveBeenCalledOnce()
  expect(beforeRowAction).toHaveBeenCalledOnce()
})

it('direct built-in and configured action entry points enforce explicit permissions', async () => {
  mocks.user = reactive({ isAdmin: false, permissions: [permissions.edit] })
  const action = { key: 'approve', actionCode: 'approve', permissionCode: 'example:record:approve', actionType: 'api', apiUrl: '/example/approve' }
  const beforeRowAction = vi.fn(() => true)
  const wrapper = crud({ runtimeActions: [action], beforeRowAction })
  await wrapper.vm.$.setupState.handleActionClick({ key: 'edit', permissionCode: action.permissionCode }, { id: 1 })
  await wrapper.vm.triggerAction('approve', { row: { id: 1 } })
  expect(wrapper.emitted('edit')).toBeUndefined()
  expect(beforeRowAction).not.toHaveBeenCalled()
  expect(mocks.request).not.toHaveBeenCalled()
})

it('keeps semantic entity detail activation inside the existing permission boundary', async () => {
  mocks.user = reactive({ isAdmin: false, permissions: [permissions.detail] })
  const wrapper = crud({ columns: [{ prop: 'name', render: { type: 'entity', subtitleField: 'code', clickAction: 'detail' } }] })
  const setup = wrapper.vm.$.setupState
  const row = { id: 4, name: 'Inspection', code: 'IN-04' }
  const cell = setup.tableColumns[0].render(row)
  expect(cell.props).toMatchObject({ entity: true, title: 'Inspection', subtitle: 'IN-04', interactive: true })
  await cell.props.onActivate()
  expect(wrapper.emitted('detail')).toEqual([[row]])
  mocks.user.permissions = []
  await nextTick()
  expect(setup.tableColumns[0].render(row).props.interactive).toBe(false)
})

it('keeps lowcode action columns compact and preserves explicitly configured width and count', async () => {
  mocks.user = reactive({ isAdmin: true, permissions: [] })
  const actions = ['edit', 'delete', 'export', 'submit', 'withdraw', 'archive'].map(key => ({ key, label: key }))
  const config = { configKey: 'example_record', columnsSchema: [], editSchema: [], searchSchema: [], apiConfig: {}, options: { rowActions: actions }, modelSchema: {}, pageSchema: {} }
  const runtime = shallowMount(RuntimeCrudPage, { props: { runtimeConfig: config, syncDocumentTitle: false }, global: { stubs: uiStubs } })
  wrappers.push(runtime)
  await flushPromises()
  const actionColumn = runtime.findComponent(AiCrudPage).props('columns').find(column => column.key === 'actions')
  expect(actionColumn.width).toBe(174)
  expect(actionColumn.maxActionButtons).toBeUndefined()
  await runtime.setProps({ runtimeConfig: { ...config, columnsSchema: [{ key: 'actions', width: 260, maxActionButtons: 1, actions }] } })
  await flushPromises()
  expect(runtime.findComponent(AiCrudPage).props('columns').find(column => column.key === 'actions')).toMatchObject({ width: 260, maxActionButtons: 1 })
})

afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.clearAllMocks()
  mocks.request.mockImplementation(async () => ({ code: 200, data: { records: [], total: 0 } }))
  delete window.$dialog
  delete window.$message
})

it('generated action permissions reach both consumers and control visible actions and direct CRUD handlers', async () => {
  mocks.user = reactive({ permissions: [permissions.detail], apiPermissions: [], getDataPermission: [], isAdmin: false, isTenantAdmin: false })
  window.$message = { warning: vi.fn(), success: vi.fn(), error: vi.fn() }
  window.$dialog = { warning: vi.fn() }
  const config = { configKey: 'example_record', columnsSchema: [{ key: 'actions', actions: [{ key: 'edit', label: '编辑' }, { key: 'delete', label: '删除' }] }], editSchema: [], searchSchema: [],
    apiConfig: {}, options: { actionPermissions: permissions }, modelSchema: {}, pageSchema: {} }
  expect(buildRuntimeCrudProps(config).actionPermissions).toEqual(permissions)
  expect(buildRuntimeCrudProps({ ...config, options: {}, actionPermissions: permissions }).actionPermissions).toEqual(permissions)
  expect(buildRuntimeCrudProps({}).actionPermissions).toEqual({})
  const runtime = shallowMount(RuntimeCrudPage, { props: { runtimeConfig: config, syncDocumentTitle: false }, global: { stubs: uiStubs } })
  wrappers.push(runtime)
  await flushPromises()
  expect(runtime.findComponent(AiCrudPage).props('actionPermissions')).toEqual(permissions)
  const generatedActions = runtime.findComponent(AiCrudPage).props('columns').find(column => column.key === 'actions').actions
  expect(generatedActions.map(action => action.key)).toContain('detail')

  const wrapper = crud()
  const setup = wrapper.vm.$.setupState
  expect(actionLabels(setup, generatedActions)).toContain('查看详情')
  expect(actionLabels(setup, generatedActions)).not.toMatch(/编辑|删除/)
  expect(setup.toolbarOverflowOptions.map(option => option.key)).toEqual(['export'])
  expect(wrapper.find('n-button-stub[type="primary"]').exists()).toBe(false)
  await wrapper.vm.showAdd()
  await wrapper.vm.showEdit({ id: 1 })
  await wrapper.vm.handleDelete({ id: 1 })
  await wrapper.vm.handleBatchDelete()
  setup.handleShowImport()
  await expect(setup.executeImport(new File(['x'], 'records.csv'))).rejects.toMatchObject({ code: 'CRUD_ACTION_PERMISSION_DENIED' })
  await wrapper.vm.submitForm()
  expect(setup.modalVisible).toBe(false)
  expect(setup.importModalVisible).toBe(false)
  expect(window.$dialog.warning).not.toHaveBeenCalled()
  expect(mocks.request).not.toHaveBeenCalled()
  expect(mocks.encrypted).not.toHaveBeenCalled()
  await wrapper.vm.showDetail({ id: 1 })
  expect(wrapper.emitted('detail')).toEqual([[{ id: 1 }]])

  mocks.user.permissions = []
  await nextTick()
  expect(actionLabels(setup, generatedActions)).toBe('-')
  expect(setup.toolbarOverflowOptions).toEqual([])
  await setup.handleExport()
  await setup.handleDownloadTemplate()
  await setup.handleOpenExportTasks()
  await wrapper.vm.showDetail({ id: 2 })
  expect(wrapper.emitted('detail')).toHaveLength(1)
  expect(mocks.request).not.toHaveBeenCalled()

  mocks.user.permissions = Object.values(permissions)
  await nextTick()
  expect(actionLabels(setup, generatedActions).split(' | ').sort()).toEqual(['查看详情', '编辑', '删除'].sort())
  await wrapper.vm.showEdit({ id: 1, name: 'original' })
  expect(wrapper.emitted('edit')).toHaveLength(1)
  await wrapper.vm.submitForm()
  expect(mocks.request.mock.calls.some(([arg]) => arg.method === 'put' && arg.url === '/example/record/1')).toBe(true)
  mocks.request.mockClear()
  await wrapper.vm.handleDelete({ id: 1 })
  const confirmation = window.$dialog.warning.mock.calls.at(-1)[0]
  mocks.user.permissions = []
  await confirmation.onPositiveClick()
  await wrapper.vm.submitForm()
  expect(mocks.request).not.toHaveBeenCalled()

  const legacy = crud({ actionPermissions: {} })
  await legacy.vm.showAdd()
  expect(legacy.emitted('add')).toHaveLength(1)
  expect(actionLabels(legacy.vm.$.setupState, [{ key: 'edit', label: '编辑' }, { key: 'delete', label: '删除' }])).toBe('编辑 | 删除')
})

const deleteIds = ['90071992547409931', '90071992547409932', '90071992547409933']

function deletionFixture(deleteRequest, deleteApi = 'post@/example/record/delete') {
  mocks.user = reactive({ isAdmin: true, permissions: [] })
  window.$message = { warning: vi.fn(), success: vi.fn(), error: vi.fn() }
  window.$dialog = { warning: vi.fn() }
  const records = deleteIds.map(id => ({ id, name: 'Kept query' }))
  mocks.request.mockImplementation(async (config) => {
    if (config.url === '/example/record/page')
      return { code: 200, data: { records: [...records], total: records.length } }
    return deleteRequest(config, records)
  })
  const wrapper = crud({ api: '/example/record', apiConfig: { list: 'post@/example/record/page', delete: deleteApi } })
  const setup = wrapper.vm.$.setupState
  setup.dataSource = [...records]
  setup.selectedKeys = [...deleteIds]
  setup.searchParams = { name: 'Kept query' }
  mocks.checkedRows.mockImplementation(() => records.filter(row => setup.selectedKeys.includes(row.id)))
  return { wrapper, setup, records }
}

async function confirmBatchDelete(wrapper) {
  await wrapper.vm.handleBatchDelete()
  const confirmation = window.$dialog.warning.mock.calls.at(-1)[0]
  await confirmation.onPositiveClick()
  await flushPromises()
}

it('public batch delete preserves legacy all-success responses and refreshes the active query', async () => {
  const { wrapper, setup } = deletionFixture((config, records) => {
    expect(config).toMatchObject({ data: deleteIds, needTip: false })
    records.splice(0)
    return { code: 200, data: true }
  })
  await confirmBatchDelete(wrapper)
  expect(setup.selectedKeys).toEqual([])
  expect(setup.dataSource).toEqual([])
  expect(mocks.request.mock.calls.find(([config]) => config.url.endsWith('/page'))[0].data).toMatchObject({ name: 'Kept query' })
  expect(window.$message.success).toHaveBeenCalledOnce()
  expect(mocks.resolveError).not.toHaveBeenCalled()
})

it.each([200, 500])('public batch delete consumes the real code %s partial result and retains only failed string IDs', async (code) => {
  const failedIds = [deleteIds[2]]
  const body = { code, message: 'Two deleted; one rejected', data: { successCount: 2, failureCount: 1, failedIds } }
  const { wrapper, setup } = deletionFixture((config, records) => {
    expect(config.needTip).toBe(false)
    records.splice(0, 2)
    if (code === 500)
      return Promise.reject({ code, message: body.message, error: body, detail: { status: 200, responseData: body } })
    return body
  })
  await confirmBatchDelete(wrapper)
  expect(setup.dataSource.map(row => row.id)).toEqual(failedIds)
  expect(setup.selectedKeys).toEqual(failedIds)
  expect(mocks.setCheckedKeys).toHaveBeenCalledWith(failedIds)
  expect(window.$message.warning).toHaveBeenCalledWith(expect.stringMatching(/2.*1/))
  expect(window.$message.success).not.toHaveBeenCalled()
  expect(mocks.resolveError).not.toHaveBeenCalled()
})

it.each([
  undefined,
  { successCount: 2, failureCount: 1 },
  { successCount: 2, failureCount: 1, failedIds: ['not-requested'] },
  { successCount: 1, failureCount: 1, failedIds: [deleteIds[2]] },
])('public batch delete preserves selections and normal errors for unknown or invalid results %j', async (data) => {
  const error = { code: 500, message: 'Actual server failure', error: { code: 500, data }, detail: { status: 200, traceId: 'original-trace' } }
  const { wrapper, setup } = deletionFixture(() => Promise.reject(error))
  await confirmBatchDelete(wrapper)
  expect(setup.selectedKeys).toEqual(deleteIds)
  expect(setup.dataSource).toHaveLength(3)
  expect(mocks.request).toHaveBeenCalledTimes(1)
  expect(mocks.resolveError).toHaveBeenCalledWith(500, error.message, true, error.detail)
  expect(window.$message.success).not.toHaveBeenCalled()
})

it('public batch delete aggregates actual sequential outcomes without losing the failed selection', async () => {
  const { wrapper, setup } = deletionFixture((config, records) => {
    const id = config.url.split('/').at(-1)
    expect(config.needTip).toBe(false)
    if (id === deleteIds[1])
      return Promise.reject({ code: 409, message: 'This record is in use', detail: { traceId: 'row-failure' } })
    records.splice(records.findIndex(row => row.id === id), 1)
    return { code: 200 }
  }, 'delete@/example/record/:id')
  await confirmBatchDelete(wrapper)
  expect(setup.selectedKeys).toEqual([deleteIds[1]])
  expect(setup.dataSource.map(row => row.id)).toEqual([deleteIds[1]])
  expect(mocks.request.mock.calls.filter(([config]) => config.method === 'delete').map(([config]) => config.url.split('/').at(-1))).toEqual(deleteIds)
  expect(mocks.resolveError).toHaveBeenCalledWith(409, expect.stringContaining('This record is in use'), true, { traceId: 'row-failure' })
  expect(window.$message.success).not.toHaveBeenCalled()
})

it('public batch delete retains unattempted IDs after an authentication failure', async () => {
  const { wrapper, setup } = deletionFixture((config, records) => {
    const id = config.url.split('/').at(-1)
    if (id === deleteIds[1])
      return Promise.reject({ code: 401, message: 'Session expired' })
    records.splice(records.findIndex(row => row.id === id), 1)
    return { code: 200 }
  }, 'delete@/example/record/:id')
  await confirmBatchDelete(wrapper)
  expect(setup.selectedKeys).toEqual(deleteIds.slice(1))
  expect(setup.dataSource.map(row => row.id)).toEqual(deleteIds.slice(1))
  expect(mocks.request.mock.calls.filter(([config]) => config.method === 'delete')).toHaveLength(2)
  expect(mocks.resolveError).toHaveBeenCalledWith(401, expect.stringContaining('Session expired'), true, undefined)
})

it('public batch delete changes keep the existing single-row deletion request compatible', async () => {
  const { wrapper, setup, records } = deletionFixture((config, currentRecords) => {
    expect(config.needTip).toBeUndefined()
    currentRecords.splice(0, 1)
    return { code: 200 }
  }, 'delete@/example/record/:id')
  await wrapper.vm.handleDelete(records[0])
  await window.$dialog.warning.mock.calls.at(-1)[0].onPositiveClick()
  expect(setup.dataSource).toHaveLength(2)
  expect(setup.selectedKeys).toEqual([])
  expect(window.$message.success).toHaveBeenCalledOnce()
  expect(mocks.resolveError).not.toHaveBeenCalled()
})
