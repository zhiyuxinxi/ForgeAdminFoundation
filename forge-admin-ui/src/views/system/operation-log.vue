<template>
  <div class="operation-log-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/system/operationLog/page',
        detail: 'get@/system/operationLog/:id',
        export: 'post@/api/excel/export/sys_operation_log_export',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :show-export="true"
      export-button-text="导出日志"
      export-file-name="页面操作审计日志.xlsx"
      :before-search="handleBeforeSearch"
    />

    <n-modal
      v-model:show="detailVisible"
      :title="localizeLabel('页面操作审计详情')"
      preset="card"
      class="audit-detail-modal"
      :style="{ width: 'min(1080px, 94vw)' }"
    >
      <div v-if="currentLog" class="audit-detail">
        <div class="audit-detail__header">
          <div class="audit-detail__identity">
            <span class="audit-detail__page">{{ currentLog.operationPageTitle || currentLog.operationModule || '-' }}</span>
            <span class="audit-detail__desc">{{ currentLog.operationContent || currentLog.operationDesc || '-' }}</span>
          </div>
          <div class="audit-detail__badges">
            <DictTag :options="operationTypeTagOptions" :value="currentLog.operationType" size="small" />
            <DictTag :options="operationStatusOptions" :value="String(currentLog.operationStatus)" size="small" />
          </div>
        </div>

        <div class="audit-summary">
          <div class="audit-summary__item">
            <span class="audit-summary__label">{{ localizeLabel('操作账号') }}</span>
            <strong>{{ currentLog.username || '-' }}</strong>
          </div>
          <div class="audit-summary__item">
            <span class="audit-summary__label">{{ localizeLabel('操作人') }}</span>
            <strong>{{ currentLog.operatorName || currentLog.username || '-' }}</strong>
          </div>
          <div class="audit-summary__item">
            <span class="audit-summary__label">{{ localizeLabel('操作时间') }}</span>
            <strong>{{ currentLog.operationTime || '-' }}</strong>
          </div>
          <div class="audit-summary__item">
            <span class="audit-summary__label">{{ localizeLabel('IP 地址') }}</span>
            <strong>{{ currentLog.operationIp || '-' }}</strong>
          </div>
        </div>

        <div class="audit-section-grid">
          <section class="audit-section">
            <h4>{{ localizeLabel('页面与接口') }}</h4>
            <dl>
              <div>
                <dt>{{ localizeLabel('操作页面') }}</dt>
                <dd>{{ currentLog.operationPageTitle || '-' }}</dd>
              </div>
              <div>
                <dt>{{ localizeLabel('页面路径') }}</dt>
                <dd>{{ currentLog.operationPage || '-' }}</dd>
              </div>
              <div>
                <dt>{{ localizeLabel('操作模块') }}</dt>
                <dd>{{ currentLog.operationModule || '-' }}</dd>
              </div>
              <div>
                <dt>{{ localizeLabel('请求方法') }}</dt>
                <dd><DictTag :options="dict.sys_req_method" :value="currentLog.requestMethod" size="small" /></dd>
              </div>
              <div class="audit-section__full">
                <dt>{{ localizeLabel('请求 URL') }}</dt>
                <dd>{{ currentLog.requestUrl || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="audit-section">
            <h4>{{ localizeLabel('执行环境') }}</h4>
            <dl>
              <div>
                <dt>{{ localizeLabel('用户 ID') }}</dt>
                <dd>{{ currentLog.userId || '-' }}</dd>
              </div>
              <div>
                <dt>{{ localizeLabel('执行耗时') }}</dt>
                <dd>{{ currentLog.executeTime || 0 }}ms</dd>
              </div>
              <div>
                <dt>{{ localizeLabel('操作地点') }}</dt>
                <dd>{{ currentLog.operationLocation || '-' }}</dd>
              </div>
              <div class="audit-section__full">
                <dt>User-Agent</dt>
                <dd class="audit-section__muted">
                  {{ currentLog.userAgent || '-' }}
                </dd>
              </div>
            </dl>
          </section>
        </div>

        <section class="audit-section audit-section--wide">
          <div class="audit-section__title-row">
            <h4>{{ localizeLabel('数据快照') }}</h4>
            <n-tag :type="hasDiffData ? 'warning' : 'default'" size="small" :bordered="false">
              {{ hasDiffData ? localizeLabel('存在差异记录') : localizeLabel('无显式差异') }}
            </n-tag>
          </div>
          <div class="snapshot-grid">
            <div class="snapshot-block">
              <span>{{ localizeLabel('操作前') }}</span>
              <pre>{{ formatJson(currentLog.beforeData) }}</pre>
            </div>
            <div class="snapshot-block">
              <span>{{ localizeLabel('操作后') }}</span>
              <pre>{{ formatJson(currentLog.afterData) }}</pre>
            </div>
            <div class="snapshot-block snapshot-block--wide">
              <span>{{ localizeLabel('差异内容') }}</span>
              <pre>{{ formatJson(currentLog.diffData) }}</pre>
            </div>
          </div>
        </section>

        <section class="audit-section audit-section--wide">
          <h4>{{ localizeLabel('请求与响应') }}</h4>
          <div class="snapshot-grid snapshot-grid--two">
            <div class="snapshot-block">
              <span>{{ localizeLabel('请求参数') }}</span>
              <pre>{{ formatJson(currentLog.requestParams) }}</pre>
            </div>
            <div class="snapshot-block">
              <span>{{ localizeLabel('响应结果') }}</span>
              <pre>{{ formatJson(currentLog.responseResult) }}</pre>
            </div>
          </div>
        </section>

        <section v-if="currentLog.errorMsg" class="audit-section audit-section--wide audit-section--error">
          <h4>{{ localizeLabel('错误信息') }}</h4>
          <pre>{{ currentLog.errorMsg }}</pre>
        </section>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="detailVisible = false">
            {{ localizeLabel('关闭') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'

import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import { localizeLabel } from '@/locales'
import { formatDateTime, request } from '@/utils'

defineOptions({ name: 'OperationLog' })

const { dict } = useDict('sys_operation_type', 'sys_common_status', 'sys_req_method')

const operationTypeOptions = computed(() => normalizeOperationTypeOptions(dict.value.sys_operation_type || []))
const operationTypeTagOptions = computed(() => buildOperationTypeTagOptions(operationTypeOptions.value))
const operationStatusOptions = computed(() => dict.value.sys_common_status || [])

const crudRef = ref(null)
const detailVisible = ref(false)
const currentLog = ref(null)

const hasDiffData = computed(() => Boolean(currentLog.value?.diffData))

function normalizeOperationTypeValue(value) {
  return String(value) === 'INSERT' ? 'ADD' : value
}

function normalizeOperationTypeOptions(options) {
  const optionMap = new Map()
  for (const option of options) {
    const normalizedValue = normalizeOperationTypeValue(option.value)
    const normalizedOption = {
      ...option,
      value: normalizedValue,
      rawValues: [option.value],
    }
    const existing = optionMap.get(normalizedValue)
    if (!existing) {
      optionMap.set(normalizedValue, normalizedOption)
      continue
    }
    existing.rawValues.push(option.value)
    if (String(option.value) === String(normalizedValue)) {
      optionMap.set(normalizedValue, {
        ...existing,
        ...option,
        value: normalizedValue,
        rawValues: existing.rawValues,
      })
    }
  }
  return Array.from(optionMap.values())
}

function buildOperationTypeTagOptions(options) {
  const addOption = options.find(option => String(option.value) === 'ADD')
  if (!addOption)
    return options
  return [
    ...options,
    {
      ...addOption,
      value: 'INSERT',
    },
  ]
}

function handleBeforeSearch(params) {
  const result = { ...params }
  if (params.timeRange && params.timeRange.length === 2) {
    result.startTime = formatDateTime(params.timeRange[0])
    result.endTime = formatDateTime(params.timeRange[1])
    delete result.timeRange
  }
  return result
}

const searchSchema = computed(() => [
  {
    field: 'username',
    get label() { return localizeLabel('操作账号') },
    type: 'input',
    props: { clearable: true, get placeholder() { return localizeLabel('请输入操作账号') } },
  },
  {
    field: 'operatorName',
    get label() { return localizeLabel('操作人') },
    type: 'input',
    props: { clearable: true, get placeholder() { return localizeLabel('请输入操作人') } },
  },
  {
    field: 'operationPageTitle',
    get label() { return localizeLabel('操作页面') },
    type: 'input',
    props: { clearable: true, get placeholder() { return localizeLabel('请输入页面名称') } },
  },
  {
    field: 'operationType',
    get label() { return localizeLabel('操作类型') },
    type: 'select',
    props: { clearable: true, get placeholder() { return localizeLabel('请选择操作类型') }, options: operationTypeOptions.value },
  },
  {
    field: 'operationStatus',
    get label() { return localizeLabel('操作状态') },
    type: 'select',
    props: { clearable: true, get placeholder() { return localizeLabel('请选择状态') }, options: operationStatusOptions.value },
  },
  {
    field: 'operationIp',
    get label() { return localizeLabel('IP地址') },
    type: 'input',
    props: { clearable: true, get placeholder() { return localizeLabel('请输入IP地址') } },
  },
  {
    field: 'operationContent',
    get label() { return localizeLabel('操作内容') },
    type: 'input',
    props: { clearable: true, get placeholder() { return localizeLabel('请输入操作内容') } },
  },
  {
    field: 'requestUrl',
    get label() { return localizeLabel('请求URL') },
    type: 'input',
    props: { clearable: true, get placeholder() { return localizeLabel('请输入请求URL') } },
  },
  {
    field: 'timeRange',
    get label() { return localizeLabel('操作时间') },
    type: 'daterange',
    startPlaceholder: localizeLabel('开始时间'),
    endPlaceholder: localizeLabel('结束时间'),
    clearable: true,
    format: 'yyyy-MM-dd HH:mm:ss',
    valueFormat: 'yyyy-MM-dd HH:mm:ss',
    props: { type: 'datetimerange' },
  },
])

const tableColumns = computed(() => [
  { prop: 'username', get label() { return localizeLabel('操作账号') }, width: 110, ellipsis: { tooltip: true } },
  { prop: 'operatorName', get label() { return localizeLabel('操作人') }, width: 110, ellipsis: { tooltip: true }, render: row => row.operatorName || row.username || '-' },
  { prop: 'operationPageTitle', get label() { return localizeLabel('操作页面') }, minWidth: 150, ellipsis: { tooltip: true }, render: row => row.operationPageTitle || '-' },
  { prop: 'operationModule', get label() { return localizeLabel('模块') }, width: 130, ellipsis: { tooltip: true }, render: row => row.operationModule || '-' },
  {
    prop: 'operationType',
    get label() { return localizeLabel('类型') },
    width: 90,
    render: row => h(DictTag, { options: operationTypeTagOptions.value, value: row.operationType, size: 'small' }),
  },
  { prop: 'operationContent', get label() { return localizeLabel('操作内容') }, minWidth: 220, ellipsis: { tooltip: true }, render: row => row.operationContent || row.operationDesc || '-' },
  {
    prop: 'requestMethod',
    get label() { return localizeLabel('方法') },
    width: 90,
    render: row => h(DictTag, { options: dict.value.sys_req_method || [], value: row.requestMethod, size: 'small' }),
  },
  { prop: 'requestUrl', get label() { return localizeLabel('请求URL') }, minWidth: 220, ellipsis: { tooltip: true } },
  {
    prop: 'operationStatus',
    get label() { return localizeLabel('状态') },
    width: 80,
    render: row => h(DictTag, { options: operationStatusOptions.value, value: String(row.operationStatus), size: 'small' }),
  },
  { prop: 'operationIp', get label() { return localizeLabel('IP地址') }, width: 130 },
  { prop: 'executeTime', get label() { return localizeLabel('耗时') }, width: 90, render: row => `${row.executeTime || 0}ms` },
  { prop: 'operationTime', get label() { return localizeLabel('操作时间') }, width: 170 },
  {
    prop: 'action',
    get label() { return localizeLabel('操作') },
    width: 86,
    fixed: 'right',
    actions: [
      { get label() { return localizeLabel('详情') }, key: 'detail', type: 'primary', onClick: handleViewDetail },
    ],
  },
])

function formatJson(jsonStr) {
  if (!jsonStr)
    return '-'
  try {
    const obj = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
    return JSON.stringify(obj, null, 2)
  }
  catch {
    return String(jsonStr)
  }
}

async function handleViewDetail(row) {
  try {
    const res = await request.get(`/system/operationLog/${row.id}`)
    if (res.code === 200) {
      currentLog.value = res.data
      detailVisible.value = true
    }
  }
  catch (error) {
    window.$message?.error(error?.message || localizeLabel('获取审计详情失败'))
  }
}
</script>

<style scoped>
.operation-log-page {
  height: 100%;
  min-height: 0;
}

.operation-log-page :deep(.ai-crud-page) {
  height: 100%;
}

.audit-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.audit-detail__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e5e7eb;
}

.audit-detail__identity {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.audit-detail__page {
  color: #111827;
  font-size: 16px;
  font-weight: 650;
  line-height: 1.35;
}

.audit-detail__desc {
  color: #4b5563;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.audit-detail__badges {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.audit-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.audit-summary__item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}

.audit-summary__label {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.audit-summary__item strong {
  display: block;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.audit-section {
  min-width: 0;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
}

.audit-section--wide {
  width: 100%;
}

.audit-section--error {
  border-color: #fecdd3;
  background: #fff7f8;
}

.audit-section h4 {
  margin: 0 0 12px;
  color: #111827;
  font-size: 14px;
  font-weight: 650;
}

.audit-section__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.audit-section__title-row h4 {
  margin: 0;
}

.audit-section dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
  margin: 0;
}

.audit-section dl > div {
  min-width: 0;
}

.audit-section__full {
  grid-column: 1 / -1;
}

.audit-section dt {
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.audit-section dd {
  margin: 0;
  color: #111827;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
}

.audit-section__muted {
  color: #475569;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.snapshot-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.snapshot-block {
  min-width: 0;
}

.snapshot-block--wide {
  grid-column: 1 / -1;
}

.snapshot-block span {
  display: block;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
}

.snapshot-block pre,
.audit-section--error pre {
  min-height: 96px;
  max-height: 260px;
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.audit-section--error pre {
  border-color: #fecdd3;
  background: #fff1f2;
  color: #be123c;
}

@media (max-width: 900px) {
  .audit-detail__header,
  .audit-section-grid,
  .audit-summary,
  .snapshot-grid,
  .snapshot-grid--two {
    grid-template-columns: 1fr;
  }

  .audit-detail__header {
    display: grid;
  }
}
</style>
