<template>
  <div class="flow-page">
    <!-- 数据标签页 -->
    <div class="tabs-container">
      <n-tabs v-model:value="activeTab" type="line" @update:value="handleTabChange">
        <n-tab name="received">
          <div class="tab-content">
            <i class="i-material-symbols:inbox" />
            {{ localizeLabel('抄送给我的') }}
            <span v-if="unreadCc > 0" class="tab-badge">{{ unreadCc > 99 ? '99+' : unreadCc }}{{ localizeLabel('未读') }}</span>
          </div>
        </n-tab>
        <n-tab v-if="canViewSent" name="sent">
          <div class="tab-content">
            <i class="i-material-symbols:outbox" />
            {{ localizeLabel('我发送的') }}
          </div>
        </n-tab>
      </n-tabs>
    </div>

    <!-- 抄送列表 -->
    <FlowTaskCardList
      v-model:selected-keys="selectedRowKeys"
      v-model:search-value="queryParams.title"
      :title="activeTab === 'received' ? localizeLabel('抄送给我的') : localizeLabel('我发送的')"
      :items="dataSource"
      :loading="loading"
      :pagination="pagination"
      :selectable="activeTab === 'received'"
      row-key="id"
      unread-key="isRead"
      :search-placeholder="translateUiText(&quot;通过名称搜索&quot;)"
      :empty-text="translateUiText(&quot;暂无抄送记录&quot;)"
      @search="handleSearch"
      @refresh="loadData"
      @row-click="openCcDetail"
      @update:page="pagination.onChange"
      @update:page-size="pagination.onUpdatePageSize"
    >
      <template #filters>
        <n-select
          v-if="activeTab === 'received'"
          v-model:value="queryParams.isRead"
          :placeholder="localizeLabel('阅读状态')"
          clearable
          class="read-select"
          :options="readOptions"
          @update:value="handleSearch"
        />
        <NButton secondary @click="handleReset">
          {{ localizeLabel('重置') }}
        </NButton>
      </template>
      <template #batch-actions>
        <NButton v-if="activeTab === 'received' && selectedRowKeys.length > 0" size="small" secondary @click="handleBatchMarkRead">
          {{ localizeLabel('批量已读') }}
        </NButton>
        <NButton v-if="activeTab === 'received'" size="small" secondary @click="handleMarkAllRead">
          {{ localizeLabel('全部已读') }}
        </NButton>
      </template>
      <template #status="{ row }">
        <span class="task-status-pill" :class="row.isRead === 1 ? 'read' : 'unread'">
          {{ getLabel('flow_read_status', row.isRead) }}
        </span>
      </template>
      <template #title="{ row }">
        {{ getRowDisplayTitle(row) }}
      </template>
      <template #meta="{ row }">
        <span>
          <span class="task-meta-label">{{ activeTab === 'received' ? localizeLabel('发送人') : localizeLabel('抄送人') }}</span>
          <span class="task-meta-value">{{ activeTab === 'received' ? (row.sendUserName || '-') : (row.ccUserName || '-') }}</span>
        </span>
        <span><span class="task-meta-label">{{ localizeLabel('抄送时间') }}</span> <span class="task-meta-value">{{ row.ccTime || '-' }}</span></span>
      </template>
      <template #summary="{ row }">
        {{ row.content || localizeLabel('暂无内容') }}
      </template>
      <template #actions="{ row }">
        <NButton size="small" type="primary" secondary @click="openCcDetail(row)">
          {{ localizeLabel('查看') }}
        </NButton>
        <NButton v-if="activeTab === 'received' && row.isRead === 0" size="small" type="primary" @click="handleMarkRead(row.id)">
          {{ localizeLabel('已读') }}
        </NButton>
      </template>
    </FlowTaskCardList>

    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      class="cc-detail-modal"
      closable
      :bordered="false"
      :segmented="{ content: true, footer: true }"
      content-style="padding: 0; overflow: hidden;"
    >
      <template #header>
        <div class="cc-detail-header">
          <div>
            <div class="cc-detail-title">
              {{ currentCc ? getRowDisplayTitle(currentCc) : localizeLabel('抄送详情') }}
            </div>
            <div class="cc-detail-meta">
              {{ currentCcTab === 'received' ? localizeLabel('抄送给我的') : localizeLabel('我发送的') }}
            </div>
          </div>
          <span class="read-tag" :class="currentCc?.isRead === 1 ? 'read' : 'unread'">
            {{ getLabel('flow_read_status', currentCc?.isRead) || '-' }}
          </span>
        </div>
      </template>

      <div v-if="currentCc" class="cc-detail-body">
        <div class="cc-info-grid">
          <div class="cc-info-card">
            <div class="cc-info-label">
              {{ currentCcTab === 'received' ? localizeLabel('发送人') : localizeLabel('抄送人') }}
            </div>
            <div class="cc-user-line">
              <UserAvatar :name="currentCcTab === 'received' ? (currentCc.sendUserName || localizeLabel('未知')) : (currentCc.ccUserName || localizeLabel('未知'))" :size="28" />
              <span>{{ currentCcTab === 'received' ? (currentCc.sendUserName || '-') : (currentCc.ccUserName || '-') }}</span>
            </div>
          </div>
          <div class="cc-info-card">
            <div class="cc-info-label">
              {{ localizeLabel('抄送时间') }}
            </div>
            <div class="cc-info-value">
              {{ currentCc.ccTime || '-' }}
            </div>
          </div>
        </div>
        <div class="cc-content-panel">
          <div class="cc-info-label">
            {{ localizeLabel('内容') }}
          </div>
          <div class="cc-content-text">
            {{ currentCc.content || localizeLabel('暂无内容') }}
          </div>
        </div>
        <div v-if="currentCcTab === 'received'" class="cc-business-form-panel">
          <div class="cc-business-form-head">
            <div>
              <div class="cc-business-form-title">
                {{ localizeLabel('业务表单') }}
              </div>
              <div class="cc-business-form-subtitle">
                {{ ccFormInfo?.title || getRowDisplayTitle(currentCc) }}
              </div>
            </div>
          </div>

          <div v-if="ccFormLoading" class="cc-form-loading">
            <n-spin size="small" />
            <span>{{ localizeLabel('加载表单中...') }}</span>
          </div>
          <div v-else-if="useCcBusinessCodeForm" class="cc-business-code-form readonly">
            <AiForm
              v-model:value="ccBusinessFormData"
              :schema="readonlyCcBusinessFields"
              :field-permissions="readonlyCcFieldPermissions"
              :show-actions="false"
              :show-feedback="false"
              :grid-cols="ccBusinessFormGridCols"
              :label-placement="ccBusinessFormLabelPlacement"
              :label-width="ccBusinessFormLabelWidth"
              :context="ccBusinessFormRenderContext"
              :form-assets="ccBusinessFormContext.formAssets || []"
            />
          </div>
          <FlowBusinessForm
            v-else-if="useCcExternalForm"
            :form-url="ccFormInfo.formUrl"
            :task-id="ccFormInfo.taskId"
            :business-key="ccFormInfo.businessKey"
            :process-instance-id="ccFormInfo.processInstanceId"
            :task-def-key="ccFormInfo.taskDefKey"
            :process-def-key="ccFormInfo.processDefKey"
            :variables="ccFormInfo.variables || {}"
            :read-only="true"
          />
          <n-empty v-else :description="ccFormError || localizeLabel('暂无业务表单')" size="small" />
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <NButton @click="showDetailModal = false">
            {{ localizeLabel('关闭') }}
          </NButton>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { NButton } from 'naive-ui'

import { computed, onMounted, reactive, ref } from 'vue'
import { businessCcTaskFormReadonlyContext } from '@/api/business-app'
import flowApi from '@/api/flow'
import FlowBusinessForm from '@/components/common/FlowBusinessForm.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import FlowTaskCardList from '@/components/flow/FlowTaskCardList.vue'
import { useDict } from '@/composables/useDict'
import { localizeLabel } from '@/locales'
import { useUserStore } from '@/store'
import { getRowDisplayTitle } from './utils/processDisplay'

const userStore = useUserStore()
const { dict, getLabel } = useDict('flow_read_status')
const canViewSent = computed(() => {
  const permissions = userStore.getDataPermission || []
  return userStore.isAdmin
    || permissions.includes('**')
    || permissions.includes('*:*:*')
    || permissions.includes('flow:cc:sent')
})
const loading = ref(false)
const dataSource = ref([])
const activeTab = ref('received')
const selectedRowKeys = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  onChange: (page) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (size) => {
    pagination.pageSize = size
    pagination.page = 1
    loadData()
  },
})

const queryParams = reactive({ title: '', isRead: null })

const readOptions = computed(() => toNumberOptions(dict.value.flow_read_status))

const unreadCc = ref(0)
const showDetailModal = ref(false)
const currentCc = ref(null)
const currentCcTab = ref('received')
const ccFormInfo = ref(null)
const ccBusinessFormContext = ref(null)
const ccBusinessFormData = ref({})
const ccFormLoading = ref(false)
const ccFormError = ref('')
const useCcBusinessCodeForm = computed(() => ccBusinessFormContext.value?.configured === true
  && ccBusinessFormContext.value?.formType === 'business-code')
const useCcExternalForm = computed(() => !useCcBusinessCodeForm.value
  && ccFormInfo.value?.formType === 'external' && ccFormInfo.value?.formUrl)
const readonlyCcBusinessFields = computed(() => (ccBusinessFormContext.value?.fields || [])
  .map(toReadonlyField))
const readonlyCcFieldPermissions = computed(() => (ccBusinessFormContext.value?.fieldPermissions || [])
  .map(toReadonlyField))
const ccBusinessFormGridCols = computed(() => Math.max(1, Number(ccBusinessFormContext.value?.gridCols || 2)))
const ccBusinessFormLabelPlacement = computed(() => ['left', 'top'].includes(ccBusinessFormContext.value?.labelPlacement)
  ? ccBusinessFormContext.value.labelPlacement
  : 'left')
const ccBusinessFormLabelWidth = computed(() => ccBusinessFormContext.value?.labelWidth || '110')
const ccBusinessFormRenderContext = computed(() => ({
  cc: currentCc.value,
  taskFormInfo: ccFormInfo.value,
  businessFormContext: ccBusinessFormContext.value,
  formAssets: ccBusinessFormContext.value?.formAssets || [],
  readOnly: true,
}))

function toReadonlyField(field = {}) {
  return {
    ...field,
    writable: false,
    readonly: true,
    disabled: true,
    props: {
      ...(field.props || {}),
      disabled: true,
      readonly: true,
    },
  }
}

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pagination.page, pageSize: pagination.pageSize, title: queryParams.title || undefined }
    let res
    if (activeTab.value === 'received') {
      if (queryParams.isRead !== null)
        params.isRead = queryParams.isRead
      res = await flowApi.getMyCc(params)
    }
    else {
      res = await flowApi.getSentCc(params)
    }
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
      if (activeTab.value === 'received')
        unreadCc.value = dataSource.value.filter(r => r.isRead === 0).length
    }
  }
  catch (error) { console.error('加载抄送列表失败:', error) }
  finally { loading.value = false }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  queryParams.title = ''
  queryParams.isRead = null
  pagination.page = 1
  loadData()
}

function handleTabChange() {
  pagination.page = 1
  selectedRowKeys.value = []
  loadData()
}

async function openCcDetail(row) {
  currentCc.value = row
  currentCcTab.value = activeTab.value
  showDetailModal.value = true
  ccFormInfo.value = null
  ccBusinessFormContext.value = null
  ccBusinessFormData.value = {}
  ccFormLoading.value = false
  ccFormError.value = ''
  if (currentCcTab.value === 'received') {
    loadCcFormInfo(row)
    if (row.isRead === 0)
      await handleMarkRead(row.id, false)
  }
}

async function loadCcFormInfo(row) {
  if (currentCcTab.value !== 'received' || !row?.id) {
    return
  }
  const ccId = row.id
  const isCurrentRecipientCc = () => currentCcTab.value === 'received' && currentCc.value?.id === ccId
  ccFormLoading.value = true
  try {
    const res = await flowApi.getCcFormInfo(ccId)
    if (!isCurrentRecipientCc()) {
      return
    }
    if (res.code !== 200) {
      ccFormError.value = res.message || localizeLabel('业务表单加载失败')
      return
    }

    ccFormInfo.value = res.data || null
    if (ccFormInfo.value?.formMode === 'BUSINESS_CODE_FORM') {
      const contextRes = await businessCcTaskFormReadonlyContext(ccId)
      if (!isCurrentRecipientCc()) {
        return
      }
      if (contextRes.code !== 200 || contextRes.data?.configured !== true) {
        ccFormError.value = contextRes.message || localizeLabel('业务表单加载失败')
        return
      }
      ccBusinessFormContext.value = contextRes.data
      ccBusinessFormData.value = { ...(contextRes.data.recordData || {}) }
    }
  }
  catch (error) {
    if (isCurrentRecipientCc())
      ccFormError.value = error?.message || localizeLabel('业务表单加载失败')
  }
  finally {
    if (isCurrentRecipientCc())
      ccFormLoading.value = false
  }
}

async function handleMarkRead(id, showToast = true) {
  try {
    const res = await flowApi.markCcRead(id)
    if (res.code === 200) {
      if (showToast)
        window.$message.success(localizeLabel('已标记已读'))
      if (currentCc.value?.id === id)
        currentCc.value.isRead = 1
      loadData()
    }
  }
  catch {
    window.$message.error(localizeLabel('操作失败'))
  }
}

async function handleBatchMarkRead() {
  if (selectedRowKeys.value.length === 0)
    return
  try {
    const res = await flowApi.batchMarkCcRead(selectedRowKeys.value)
    if (res.code === 200) {
      window.$message.success(localizeLabel('已批量标记已读'))
      selectedRowKeys.value = []
      loadData()
    }
  }
  catch {
    window.$message.error(localizeLabel('操作失败'))
  }
}

function handleMarkAllRead() {
  window.$dialog.warning({
    get title() { return localizeLabel('确认') },
    get content() { return localizeLabel('确定将所有未读抄送标记为已读吗？') },
    get positiveText() { return localizeLabel('确定') },
    get negativeText() { return localizeLabel('取消') },
    onPositiveClick: async () => {
      try {
        const unreadItems = dataSource.value.filter(item => item.isRead === 0)
        const ids = unreadItems.map(item => item.id)
        if (ids.length > 0) {
          await flowApi.batchMarkCcRead(ids)
          window.$message.success(localizeLabel('已全部标记已读'))
          loadData()
        }
      }
      catch {
        window.$message.error(localizeLabel('操作失败'))
      }
    },
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.flow-page {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  background: var(--bg-secondary);
}
.page-header {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}
.title-icon.cc {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
}
.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.read-select {
  width: 116px;
}
.tabs-container {
  flex: 0 0 auto;
  background: var(--bg-primary);
  padding: 0 12px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}
.tab-content {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-weight: 500;
}
.tab-badge {
  border: 1px solid #fecdd3;
  background: #fff1f2;
  color: #be123c;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 600;
}
.table-container {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  flex: 1;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}
:deep(.cc-title) {
  font-weight: 500;
  color: #0f172a;
  cursor: pointer;
}
:deep(.cc-title:hover) {
  color: #0369a1;
}
:deep(.table-user) {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.user-name-text) {
  font-weight: 500;
  color: #0f172a;
}
:deep(.read-tag) {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}
:deep(.read-tag.read) {
  background: #dcfce7;
  color: #15803d;
}
:deep(.read-tag.unread) {
  background: #fee2e2;
  color: #b91c1c;
}

:deep(.cc-actions) {
  display: flex;
  gap: 6px;
  align-items: center;
}

.cc-detail-modal {
  width: min(820px, calc(100vw - 32px));
}

.cc-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.cc-detail-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.cc-detail-meta {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.cc-detail-body {
  display: grid;
  gap: 14px;
  max-height: calc(100vh - 178px);
  overflow-y: auto;
  padding: 18px 20px 20px;
}

.cc-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.cc-info-card,
.cc-content-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px 14px;
}

.cc-info-label {
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
}

.cc-info-value,
.cc-user-line {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.cc-user-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cc-content-text {
  min-height: 120px;
  color: #172033;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.cc-business-form-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.cc-business-form-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf2f7;
}

.cc-business-form-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.cc-business-form-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.cc-form-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 120px;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 760px) {
  .cc-detail-modal {
    width: 100vw;
    height: 100vh;
    margin: 0;
  }

  .cc-detail-header,
  .header-right {
    align-items: stretch;
    flex-direction: column;
  }

  .cc-detail-body {
    max-height: calc(100vh - 126px);
    padding: 14px;
  }

  .cc-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
