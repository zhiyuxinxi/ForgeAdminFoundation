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
  executeBusinessAction: vi.fn(),
  openLinkedBusinessTask: vi.fn(),
  routerPush: vi.fn(),
}))
vi.mock('@/store', () => ({ useUserStore: () => mocks.user, useTabStore: () => ({}) }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: {}, params: {}, path: '/example/record', meta: {} }), useRouter: () => ({ push: mocks.routerPush, replace: vi.fn() }) }))
vi.mock('@/utils', () => ({ request: mocks.request, postEncrypt: mocks.encrypted, downloadFile: vi.fn() }))
vi.mock('@/utils/encrypt-request', () => ({ postEncrypt: mocks.encrypted }))
vi.mock('@/utils/http/helpers', () => ({ isAuthErrorCode: code => ['-8', 401, '401', 11007, 11008].includes(code), resolveResError: mocks.resolveError }))
vi.mock('@/api/ai', () => ({ crudConfigRender: vi.fn(), customQueryExecute: vi.fn() }))
vi.mock('@/api/business-app', () => ({ businessDocumentRuntimeBatch: vi.fn(), executeBusinessAction: mocks.executeBusinessAction }))
vi.mock('@/api/business-process', () => ({ startBusinessProcess: vi.fn() }))
vi.mock('@/api/business-task', () => ({ openLinkedBusinessTask: mocks.openLinkedBusinessTask, businessTaskActions: vi.fn() }))
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
      actionPermissions: permissions, apiConfig: { add: 'post@/example/record', update: 'put@/example/record/:id', delete: 'delete@/example/record/:id', detail: 'get@/example/record/:id', import: 'post@/example/record/import', export: 'get@/example/record/export' }, ...extra },
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

it('public task row actions distinguish source query/edit permissions and navigate to the real inbox', async () => {
  mocks.user = reactive({ isAdmin: false, permissions: ['ai:business:RECORD:query'] })
  mocks.openLinkedBusinessTask.mockResolvedValue({ id: '9007199254740997' })
  const wrapper = crud()
  const setup = wrapper.vm.$.setupState
  const action = { key: 'publicTask:review', actionType: 'PUBLIC_TASK', label: '检查', configKey: 'records', objectCode: 'RECORD', bindingKey: 'review', taskIdField: 'taskId' }
  const unlinked = setup.normalizeRuntimeAction(action, { id: 'record-1' })
  expect(actionLabels(setup, [unlinked])).toBe('-')
  await setup.handleConfiguredAction(unlinked, { id: 'record-1' })
  expect(mocks.openLinkedBusinessTask).not.toHaveBeenCalled()
  const linkedRow = { id: 'record-1', taskId: '9007199254740997' }
  const linked = setup.normalizeRuntimeAction(action, linkedRow)
  expect(actionLabels(setup, [linked])).toContain('查看任务')
  await setup.handleConfiguredAction(linked, linkedRow)
  expect(mocks.openLinkedBusinessTask).toHaveBeenCalledWith(linked, linkedRow, 'id')
  expect(mocks.routerPush).toHaveBeenCalledWith({ path: '/workspace/todo', query: { tab: 'business', taskId: '9007199254740997' } })
  mocks.user.permissions.push('ai:business:RECORD:edit')
  expect(actionLabels(setup, [unlinked])).toContain('派发任务')
  mocks.openLinkedBusinessTask.mockRejectedValueOnce(new Error('来源状态不允许派发'))
  window.$message = { error: vi.fn() }
  await setup.handleConfiguredAction(unlinked, { id: 'record-1' })
  expect(window.$message.error).toHaveBeenCalledWith('来源状态不允许派发')
})

it('public task row actions are loaded only for a published runtime binding and reach the CRUD consumer', async () => {
  const { crudConfigRender } = await import('@/api/ai')
  const { businessTaskActions } = await import('@/api/business-task')
  mocks.user = reactive({ isAdmin: true, permissions: [] })
  crudConfigRender.mockResolvedValue({ data: {
    configKey: 'records',
    objectCode: 'RECORD',
    modelSchema: { fields: [] },
    editSchema: [],
    searchSchema: [],
    columnsSchema: [],
    apiConfig: {},
    options: { publicTaskBindings: [{ id: '50' }] },
  } })
  businessTaskActions.mockResolvedValue([{ bindingKey: 'review', title: '检查', taskIdField: 'taskId' }])
  const wrapper = shallowMount(RuntimeCrudPage, { props: { syncDocumentTitle: false }, global: { stubs: uiStubs } })
  wrappers.push(wrapper)
  const setup = wrapper.vm.$.setupState
  setup.route.params.configKey = 'records'
  setup.route.query = reactive({})
  await setup.loadConfig()
  expect(businessTaskActions).toHaveBeenCalledWith('records')
  expect(setup.crudProps.runtimeActions).toContainEqual(expect.objectContaining({
    key: 'publicTask:review',
    actionType: 'PUBLIC_TASK',
    configKey: 'records',
    bindingKey: 'review',
    taskIdField: 'taskId',
  }))
  businessTaskActions.mockClear()
  setup.route.query.designPreview = '1'
  await setup.loadConfig()
  expect(businessTaskActions).not.toHaveBeenCalled()
})

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
  mocks.executeBusinessAction.mockReset()
  delete mocks.request.get
  delete window.$dialog
  delete window.$message
  delete window.$loading
})

function pendingResult() {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

it.each(['success', 'failure'])('obsolete runtime %s cannot replace the current record flow fallback', async (outcome) => {
  mocks.user = reactive({ isAdmin: true })
  const older = pendingResult()
  const newer = pendingResult()
  mocks.request.get = vi.fn().mockReturnValueOnce(older.promise).mockReturnValueOnce(newer.promise)
  const wrapper = crud({ businessObjectCode: 'inspection' })
  const setup = wrapper.vm.$.setupState
  const first = setup.handleDetail({ id: '1', processInstanceId: 'old-flow', status: 'IN_PROCESS' })
  await flushPromises()
  const second = setup.handleDetail({ id: '2', processInstanceId: 'current-flow', status: 'IN_PROCESS' })
  await flushPromises()
  newer.resolve({ data: null })
  await second
  expect(setup.detailRuntime).toMatchObject({ processInstanceId: 'current-flow', flowStatus: 'RUNNING' })
  if (outcome === 'success')
    older.resolve({ data: { processInstanceId: 'old-flow', flowStatus: 'COMPLETED' } })
  else
    older.reject(new Error('Obsolete runtime unavailable'))
  await first
  expect(setup.detailRuntime).toMatchObject({ processInstanceId: 'current-flow', flowStatus: 'RUNNING' })
  expect(setup.formData.id).toBe('2')
  expect(wrapper.emitted('modal-open')).toHaveLength(1)
})

it('async public list keeps the latest query when responses finish out of order', async () => {
  mocks.user = reactive({ isAdmin: true })
  const older = pendingResult()
  const newer = pendingResult()
  mocks.request.mockReturnValueOnce(older.promise).mockReturnValueOnce(newer.promise)
  const wrapper = crud({ api: '/example/record' })
  const setup = wrapper.vm.$.setupState
  const first = setup.loadList()
  await flushPromises()
  setup.searchParams = { name: 'latest' }
  const second = setup.loadList()
  await flushPromises()
  newer.resolve({ data: { records: [{ id: 2, name: 'latest' }], total: 1 } })
  await second
  older.resolve({ data: { records: [{ id: 1, name: 'old' }], total: 9 } })
  await first
  expect(setup.dataSource).toEqual([{ id: 2, name: 'latest' }])
  expect(setup.pagination.itemCount).toBe(1)
  expect(wrapper.emitted('load-list-success')).toHaveLength(1)
})

it('async public list ignores obsolete failures and keeps loading for the latest request', async () => {
  mocks.user = reactive({ isAdmin: true })
  window.$message = { error: vi.fn() }
  const older = pendingResult()
  const newer = pendingResult()
  mocks.request.mockReturnValueOnce(older.promise).mockReturnValueOnce(newer.promise)
  const wrapper = crud({ api: '/example/record' })
  const setup = wrapper.vm.$.setupState
  const first = setup.loadList()
  await flushPromises()
  const second = setup.loadList()
  await flushPromises()
  older.reject(new Error('Obsolete query failed'))
  await first
  expect(setup.tableLoading).toBe(true)
  expect(window.$message.error).not.toHaveBeenCalled()
  expect(wrapper.emitted('load-list-error')).toBeUndefined()
  newer.resolve({ data: { records: [], total: 0 } })
  await second
  expect(setup.tableLoading).toBe(false)
})

it.each(['handleEdit', 'handleDetail'])('async public %s does not open stale form data after detail failure', async (action) => {
  mocks.user = reactive({ isAdmin: true })
  window.$message = { warning: vi.fn(), error: vi.fn() }
  window.$loading = { show: vi.fn(), close: vi.fn() }
  mocks.request.mockRejectedValueOnce(new Error('Detail unavailable'))
  const wrapper = crud({ api: '/example/record', loadDetailOnEdit: true })
  const setup = wrapper.vm.$.setupState
  setup.formData = { id: 99, name: 'Previously loaded record' }
  await setup[action]({ id: 2 })
  expect(setup.modalVisible).toBe(false)
  expect(wrapper.emitted('modal-open')).toBeUndefined()
  expect(window.$loading.close).toHaveBeenCalledOnce()
  await wrapper.vm.submitForm()
  expect(mocks.request).toHaveBeenCalledOnce()
  expect(setup.formData).toEqual({})
})

it('async public save rejects repeated entry during asynchronous preparation and allows retry after cancellation', async () => {
  mocks.user = reactive({ isAdmin: true })
  window.$message = { warning: vi.fn(), success: vi.fn(), error: vi.fn() }
  const preparation = pendingResult()
  const beforeSubmit = vi.fn().mockReturnValueOnce(preparation.promise).mockImplementation(data => data)
  const wrapper = crud({ beforeSubmit })
  const setup = wrapper.vm.$.setupState
  setup.formData = { name: 'One save only' }
  const first = wrapper.vm.submitForm()
  await flushPromises()
  await wrapper.vm.submitForm()
  expect(beforeSubmit).toHaveBeenCalledOnce()
  expect(mocks.request).not.toHaveBeenCalled()
  preparation.resolve(false)
  await first
  expect(setup.confirmLoading).toBe(false)
  await wrapper.vm.submitForm()
  expect(mocks.request).toHaveBeenCalledOnce()
  expect(wrapper.emitted('submit-success')).toHaveLength(1)
})

it.each(['handleEdit', 'handleAdd'])('async public detail cannot replace a newer %s intent', async (nextAction) => {
  mocks.user = reactive({ isAdmin: true })
  window.$message = { warning: vi.fn(), error: vi.fn() }
  window.$loading = { show: vi.fn(), close: vi.fn() }
  const older = pendingResult()
  mocks.request.mockReturnValueOnce(older.promise).mockResolvedValueOnce({ data: { id: 2, name: 'Current record' } })
  const wrapper = crud({ api: '/example/record', loadDetailOnEdit: true })
  const setup = wrapper.vm.$.setupState
  const first = setup.handleEdit({ id: 1 })
  await flushPromises()
  await setup[nextAction]({ id: 2, name: 'Current record' })
  older.resolve({ data: { id: 1, name: 'Obsolete record' } })
  await first
  expect(setup.formData).toMatchObject({ id: 2, name: 'Current record' })
  expect(setup.modalStatus).toBe(nextAction === 'handleEdit' ? 'edit' : 'add')
  expect(wrapper.emitted('modal-open')).toHaveLength(1)
})

it('child toolbar preserves inputs on failure and binds retry identity to the selected record', async () => {
  mocks.user = reactive({ isAdmin: true })
  window.$message = { warning: vi.fn(), error: vi.fn(), success: vi.fn() }
  mocks.executeBusinessAction.mockRejectedValueOnce(new Error('Try again')).mockRejectedValueOnce(new Error('Try again')).mockResolvedValueOnce({ data: { executeStatus: 'SUCCESS' } })
  const wrapper = crud({ businessObjectCode: 'maintenance_order' })
  const setup = wrapper.vm.$.setupState
  const action = {
    key: 'inspect',
    actionCode: 'inspect',
    actionType: 'COMMAND',
    relationKey: 'equipment',
    successBehavior: 'none',
    actionConfig: { recordSelector: { labelField: 'name' }, inputSchema: [{ name: 'reason', type: 'text', required: true }] },
  }
  setup.formData = { id: '9223372036854775800' }
  setup.childFormData = { equipment: [{ id: '9223372036854775801', name: 'A' }, { id: '9223372036854775802', name: 'B' }] }
  await setup.handleChildToolbarAction({ action, child: { relationKey: 'logs' } })
  setup.childToolbarItemSelected = '9223372036854775801'
  setup.childToolbarActionFormData = { reason: 'Check bearing' }
  await setup.submitChildToolbarAction()
  expect(setup.childToolbarActionModalVisible).toBe(true)
  expect(setup.childToolbarActionFormData).toEqual({ reason: 'Check bearing' })
  await setup.submitChildToolbarAction()
  expect(mocks.executeBusinessAction.mock.calls[1][0].idempotencyKey).toBe(mocks.executeBusinessAction.mock.calls[0][0].idempotencyKey)
  setup.childToolbarItemSelected = '9223372036854775802'
  await setup.submitChildToolbarAction()
  const payload = mocks.executeBusinessAction.mock.calls[2][0]
  expect(payload).toMatchObject({ parentRecordId: '9223372036854775800', childRecordId: '9223372036854775802', relationKey: 'equipment', formData: { reason: 'Check bearing' } })
  expect(payload.idempotencyKey).not.toBe(mocks.executeBusinessAction.mock.calls[0][0].idempotencyKey)
  expect(setup.childToolbarActionModalVisible).toBe(false)
})

it('child toolbar direct entry respects action permission', async () => {
  mocks.user = reactive({ isAdmin: false, permissions: [] })
  const wrapper = crud()
  const setup = wrapper.vm.$.setupState
  setup.formData = { id: '1' }
  setup.childFormData = { equipment: [{ id: '2' }] }
  await setup.handleChildToolbarAction({ action: { key: 'inspect', permissionCode: 'equipment:inspect', relationKey: 'equipment' } })
  expect(setup.childToolbarActionModalVisible).toBe(false)
  expect(mocks.executeBusinessAction).not.toHaveBeenCalled()
})

it('public save preserves numeric business values and already formatted dates throughout the payload', async () => {
  mocks.user = reactive({ isAdmin: true })
  window.$message = { warning: vi.fn(), success: vi.fn(), error: vi.fn() }
  const wrapper = crud()
  const setup = wrapper.vm.$.setupState
  const payload = {
    amount: 1234567890,
    reference: 1234567890123,
    zero: 0,
    negative: -1234567890,
    occurredAt: '2026-10-13 08:30:00',
    period: ['2026-10-01', '2026-10-13'],
    children: [{ quantity: 1234567890, fileId: '1234567890123456789' }],
    nested: { value: 1234567890123, absent: null, enabled: false },
  }
  setup.modalStatus = 'add'
  setup.formData = structuredClone(payload)
  setup.modalVisible = true
  await wrapper.vm.submitForm()
  expect(mocks.request.mock.calls[0][0]).toMatchObject({ method: 'post', data: payload })
  expect(wrapper.emitted('submit-success')).toHaveLength(1)
  expect(window.$message.success).toHaveBeenCalledOnce()
})

it.each(['post', 'postEncrypt'])('public save retains draft and editor state after a silent auth failure via %s', async (method) => {
  mocks.user = reactive({ isAdmin: true })
  window.$message = { warning: vi.fn(), success: vi.fn(), error: vi.fn() }
  const error = { code: 401, data: null, message: 'Session expired', silentAuthError: true }
  const send = method === 'postEncrypt' ? mocks.encrypted : mocks.request
  send.mockResolvedValueOnce(error)
  const wrapper = crud({ apiConfig: { add: `${method}@/example/record` } })
  const setup = wrapper.vm.$.setupState
  setup.modalStatus = 'add'
  setup.formData = { name: 'Keep my changes' }
  setup.modalVisible = true
  setup.offlineDraftId = 'draft-auth-failure'
  await wrapper.vm.submitForm()
  expect(setup.offlineDraftId).toBe('draft-auth-failure')
  expect(setup.formData).toEqual({ name: 'Keep my changes' })
  expect(setup.modalVisible).toBe(true)
  expect(setup.confirmLoading).toBe(false)
  expect(wrapper.emitted('submit-success')).toBeUndefined()
  expect(wrapper.emitted('submit-error')?.[0][0].error).toBe(error)
  expect(window.$message.success).not.toHaveBeenCalled()
  expect(window.$message.error).not.toHaveBeenCalled()
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
  expect(mocks.resolveError).toHaveBeenCalledWith(500, error.message, true, error.detail, 'delete')
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
  expect(mocks.resolveError).toHaveBeenCalledWith(409, expect.stringContaining('This record is in use'), true, { traceId: 'row-failure' }, 'delete')
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
  expect(mocks.resolveError).toHaveBeenCalledWith(401, expect.stringContaining('Session expired'), true, undefined, 'delete')
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


it('hides unsupported built-in actions and preserves default filters and nested slots', () => {
  mocks.user = reactive({ isAdmin: true })
  const wrapper = crud({ apiConfig: { list: 'get@/example/records' }, defaultSearchParams: { state: 'OPEN' }, editSchema: [{ nodeType: 'group', type: 'group', children: [{ field: 'custom', type: 'slot', slotName: 'custom-editor' }] }] })
  const setup = wrapper.vm.$.setupState
  expect(setup.showAddButton).toBe(false)
  expect(setup.showDefaultEditAction).toBe(false)
  expect(setup.showDefaultDeleteAction).toBe(false)
  expect(setup.showBatchDeleteButton).toBe(false)
  expect(setup.showImportButton).toBe(false)
  expect(setup.showExportButton).toBe(false)
  expect(setup.searchParams).toEqual({ state: 'OPEN' })
  expect(setup.formSlots).toContain('custom-editor')
  setup.searchParams = { state: 'CLOSED' }
  setup.handleReset()
  expect(setup.searchParams).toEqual({ state: 'OPEN' })
})

it('checks custom click permission before dispatch and does not emit configured actions before confirmation', async () => {
  mocks.user = reactive({ permissions: [], apiPermissions: [], getDataPermission: [], isAdmin: false, isTenantAdmin: false })
  const wrapper = crud()
  const setup = wrapper.vm.$.setupState
  const onClick = vi.fn()
  await setup.handleActionClick({ key: 'custom', permissionCode: 'example:custom', onClick }, { id: 1 })
  expect(onClick).not.toHaveBeenCalled()
  mocks.user.permissions = ['example:custom']
  await setup.handleActionClick({ key: 'custom', permissionCode: 'example:custom', onClick }, { id: 1 })
  expect(onClick).toHaveBeenCalledOnce()
  const previousDialog = window.$dialog
  window.$dialog = { warning: vi.fn(options => { options.onNegativeClick() }) }
  try {
    await setup.handleConfiguredAction({ key: 'custom', confirmText: 'Continue?' }, { id: 1 })
    expect(wrapper.emitted('custom-action')).toBeUndefined()
  }
  finally { window.$dialog = previousDialog }
})
