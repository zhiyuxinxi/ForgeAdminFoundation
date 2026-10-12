<template>
  <div class="crud-table" :style="rootStyle">
    <div v-if="option.showToolbar" class="crud-toolbar">
      <div class="toolbar-title">
        <span class="title-mark"></span>
        <div>
          <div class="title-text">{{ option.title }}</div>
          <div v-if="option.subtitle" class="subtitle-text">{{ option.subtitle }}</div>
        </div>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="fetchRows">{{ $ui("刷新") }}</button>
    </div>

    <div v-if="option.showSearch" class="crud-search">
      <label v-for="field in option.searchFields" :key="field.field" class="search-field">
        <span>{{ field.label }}</span>
        <input
          v-if="field.type === 'input'"
          v-model="queryForm[field.field]"
          :placeholder="field.placeholder || $ui(&quot;请输入{0}&quot;, { 0: (field.label) })"
          @keyup.enter="handleSearch"
        />
        <input
          v-else-if="field.type === 'date'"
          v-model="queryForm[field.field]"
          type="date"
        />
        <div v-else-if="field.type === 'dateRange'" class="range-control">
          <input v-model="queryForm[field.startKey || `${field.field}Start`]" type="date" />
          <input v-model="queryForm[field.endKey || `${field.field}End`]" type="date" />
        </div>
        <input
          v-else-if="field.type === 'number'"
          v-model="queryForm[field.field]"
          type="number"
          :placeholder="field.placeholder || $ui(&quot;请输入{0}&quot;, { 0: (field.label) })"
          @keyup.enter="handleSearch"
        />
        <div v-else-if="field.type === 'numberRange'" class="range-control">
          <input v-model="queryForm[field.startKey || `${field.field}Min`]" type="number" :placeholder="$ui(&quot;最小值&quot;)" />
          <input v-model="queryForm[field.endKey || `${field.field}Max`]" type="number" :placeholder="$ui(&quot;最大值&quot;)" />
        </div>
        <label v-else-if="field.type === 'switch'" class="switch-control">
          <input v-model="queryForm[field.field]" type="checkbox" />
          <span>{{ queryForm[field.field] ? $ui("是") : $ui("否") }}</span>
        </label>
        <div v-else-if="field.type === 'radio'" class="radio-control">
          <label v-for="item in getFieldOptions(field)" :key="`${field.field}-${item.value}`">
            <input v-model="queryForm[field.field]" type="radio" :value="item.value" />
            <span>{{ item.label }}</span>
          </label>
        </div>
        <select v-else-if="field.type === 'multiSelect'" v-model="queryForm[field.field]" multiple>
          <option
            v-for="item in getFieldOptions(field)"
            :key="`${field.field}-${item.value}`"
            :value="item.value"
          >
            {{ item.label }}
          </option>
        </select>
        <select v-else v-model="queryForm[field.field]">
          <option value="">{{ $ui("全部") }}</option>
          <option
            v-for="item in getFieldOptions(field)"
            :key="`${field.field}-${item.value}`"
            :value="item.value"
          >
            {{ item.label }}
          </option>
        </select>
      </label>
      <div class="search-actions">
        <button class="primary-btn" @click="handleSearch">{{ $ui("查询") }}</button>
        <button class="ghost-btn" @click="handleReset">{{ $ui("重置") }}</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th v-if="option.showIndex" class="index-cell">#</th>
            <th
              v-for="column in option.columns"
              :key="column.key"
              :style="{ width: column.width ? `${column.width}px` : undefined, textAlign: column.align || 'left' }"
            >
              {{ column.title }}
            </th>
            <th v-if="option.showActions && option.actions.length" class="action-cell">{{ $ui("操作") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td :colspan="colspan" class="state-cell">{{ $ui("加载中...") }}</td>
          </tr>
          <tr v-else-if="!pagedRows.length">
            <td :colspan="colspan" class="state-cell">{{ $ui("暂无数据") }}</td>
          </tr>
          <template v-else>
            <tr v-for="(row, rowIndex) in pagedRows" :key="getRowKey(row, rowIndex)">
              <td v-if="option.showIndex" class="index-cell">{{ (page - 1) * pageSize + rowIndex + 1 }}</td>
              <td
                v-for="column in option.columns"
                :key="column.key"
                :style="{ textAlign: column.align || 'left' }"
              >
                <span
                  v-if="column.type === 'dict' || column.dictType || column.options"
                  class="dict-tag"
                >
                  {{ formatCell(row, column) }}
                </span>
                <img v-else-if="column.type === 'image'" class="cell-image" :src="formatCell(row, column)" alt="" />
                <a v-else-if="column.type === 'link'" class="cell-link" :href="formatLink(row, column)" :target="column.openTarget || '_self'">
                  {{ formatCell(row, column) }}
                </a>
                <span v-else-if="column.type === 'progress'" class="progress-cell">
                  <span class="progress-track"><span class="progress-bar" :style="{ width: `${clampPercent(formatCell(row, column))}%` }"></span></span>
                  <span>{{ clampPercent(formatCell(row, column)) }}%</span>
                </span>
                <span v-else-if="column.type === 'switch'" class="switch-tag" :class="{ active: Boolean(getByPath(row, column.key)) }">
                  {{ getByPath(row, column.key) ? $ui("开启") : $ui("关闭") }}
                </span>
                <span v-else :class="{ ellipsis: column.ellipsis !== false }">{{ formatCell(row, column) }}</span>
              </td>
              <td v-if="option.showActions && option.actions.length" class="action-cell">
                <button
                  v-for="action in option.actions"
                  :key="action.label"
                  class="row-action"
                  :class="`is-${action.style || 'primary'}`"
                  :disabled="isActionDisabled(action, row) || actionLoading[`${getRowKey(row, rowIndex)}-${action.label}`]"
                  v-show="isActionVisible(action, row)"
                  @click="handleRowAction(action, row)"
                >
                  {{ actionLoading[`${getRowKey(row, rowIndex)}-${action.label}`] ? $ui("处理中") : action.label }}
                </button>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <div class="crud-pagination">
      <span>{{ $ui("共") }} {{ total }} {{ $ui("条") }}</span>
      <button :disabled="page <= 1" @click="changePage(page - 1)">{{ $ui("上一页") }}</button>
      <span>{{ page }} / {{ totalPage }}</span>
      <button :disabled="page >= totalPage" @click="changePage(page + 1)">{{ $ui("下一页") }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted, PropType, reactive, ref, unref, watch } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { get, post, put, del } from '@/api/http'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { switchPreviewPage } from '@/views/preview/utils/storage'
import { PREVIEW_PAGE_CONTEXT_KEY } from '@/utils/requestDynamicParams'
import { option as defaultOption } from './config'
import type { CrudColumn, CrudCondition, CrudRowAction, CrudSearchField } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const chartEditStore = useChartEditStore()
const pageContext = inject(PREVIEW_PAGE_CONTEXT_KEY, ref({}))
const loading = ref(false)
const rows = ref<Record<string, any>[]>([])
const total = ref(0)
const page = ref(1)
const dictMap = reactive<Record<string, Array<{ label: string; value: string | number }>>>({})
const queryForm = reactive<Record<string, any>>({})
const actionLoading = reactive<Record<string, boolean>>({})
const requestVersion = ref(0)

const option = computed(() => ({
  ...defaultOption,
  ...(props.chartConfig.option || {}),
  api: { ...defaultOption.api, ...(props.chartConfig.option?.api || {}) },
  style: { ...defaultOption.style, ...(props.chartConfig.option?.style || {}) },
  contextParamMap: props.chartConfig.option?.contextParamMap || {},
  searchFields: props.chartConfig.option?.searchFields || [],
  columns: props.chartConfig.option?.columns || [],
  actions: props.chartConfig.option?.actions || [],
  staticRows: props.chartConfig.option?.staticRows || []
}))
const pageSize = computed(() => Math.max(1, Number(option.value.pageSize || 10)))
const totalPage = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const colspan = computed(() =>
  option.value.columns.length + (option.value.showIndex ? 1 : 0) + (option.value.showActions && option.value.actions.length ? 1 : 0)
)
const pagedRows = computed(() => {
  if (option.value.api.listUrl) return rows.value
  const start = (page.value - 1) * pageSize.value
  return rows.value.slice(start, start + pageSize.value)
})
const rootStyle = computed(() => ({
  '--crud-accent': option.value.style.accentColor,
  '--crud-success': option.value.style.successColor,
  '--crud-warning': option.value.style.warningColor,
  '--crud-error': option.value.style.errorColor,
  '--crud-text': option.value.style.textColor,
  '--crud-muted': option.value.style.mutedColor,
  '--crud-panel': option.value.style.panelColor,
  '--crud-header': option.value.style.headerColor,
  '--crud-row': option.value.style.rowColor,
  '--crud-border': option.value.style.borderColor,
  '--crud-radius': `${option.value.style.radius}px`,
  '--crud-font-size': `${option.value.style.fontSize}px`
}))

const getByPath = (target: any, path?: string) => {
  if (!path) return target
  return path.split('.').reduce((current, key) => current?.[key], target)
}

const getRuntimeContext = () => unref(pageContext) || chartEditStore.getRuntimePageContext || {}

const templateUrl = (template: string, row: Record<string, any>) =>
  template.replace(/\$\{([^}]+)\}/g, (_, key) => String(getByPath(row, key) ?? ''))

const compareCondition = (condition: CrudCondition, row: Record<string, any>) => {
  const current = getByPath(row, condition.field)
  const target = condition.value
  if (condition.operator === 'empty') return current === undefined || current === null || current === ''
  if (condition.operator === 'notEmpty') return current !== undefined && current !== null && current !== ''
  if (condition.operator === 'contains') return String(current ?? '').includes(String(target ?? ''))
  const leftNumber = Number(current)
  const rightNumber = Number(target)
  const canCompareNumber = !Number.isNaN(leftNumber) && !Number.isNaN(rightNumber)
  if (condition.operator === 'eq') return String(current) === String(target)
  if (condition.operator === 'ne') return String(current) !== String(target)
  if (condition.operator === 'gt') return canCompareNumber && leftNumber > rightNumber
  if (condition.operator === 'gte') return canCompareNumber && leftNumber >= rightNumber
  if (condition.operator === 'lt') return canCompareNumber && leftNumber < rightNumber
  if (condition.operator === 'lte') return canCompareNumber && leftNumber <= rightNumber
  return false
}

const evaluateConditions = (conditions: CrudCondition[] | undefined, row: Record<string, any>, defaultValue: boolean) => {
  if (!conditions?.length) return defaultValue
  return conditions.every(condition => compareCondition(condition, row))
}

const evaluateLegacyExpression = (expression: string | undefined, row: Record<string, any>) => {
  if (!expression?.trim()) return false
  const matched = expression.trim().match(/^([\w.]+)\s*(===|==|!==|!=|>=|<=|>|<)\s*['"]?([^'"]+)['"]?$/)
  if (!matched) return false
  const [, field, operator, value] = matched
  const operatorMap: Record<string, CrudCondition['operator']> = {
    '===': 'eq',
    '==': 'eq',
    '!==': 'ne',
    '!=': 'ne',
    '>': 'gt',
    '>=': 'gte',
    '<': 'lt',
    '<=': 'lte'
  }
  return compareCondition({ field, operator: operatorMap[operator], value }, row)
}

const applyContextParams = (params: Record<string, any>) => {
  const context = getRuntimeContext()
  Object.entries(option.value.contextParamMap || {}).forEach(([targetKey, sourceKey]) => {
    const value = getByPath(context, sourceKey)
    if (value !== undefined && value !== null && value !== '') params[targetKey] = value
  })
  return params
}

const normalizeRows = (payload: any) => {
  const data = option.value.api.dataPath ? getByPath(payload, option.value.api.dataPath) : payload?.data
  if (Array.isArray(data)) return data
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload)) return payload
  return []
}

const normalizeTotal = (payload: any, nextRows: any[]) => {
  const value = option.value.api.totalPath ? getByPath(payload, option.value.api.totalPath) : undefined
  return Number(value ?? payload?.data?.total ?? payload?.total ?? nextRows.length)
}

const requestList = async () => {
  const version = ++requestVersion.value
  const params = applyContextParams({
    ...queryForm,
    [option.value.api.pageNumKey || 'pageNum']: page.value,
    [option.value.api.pageSizeKey || 'pageSize']: pageSize.value
  })
  if (!option.value.api.listUrl) {
    const staticRows = option.value.staticRows || []
    rows.value = staticRows.filter(row => {
      return Object.entries(queryForm).every(([key, value]) => {
        if (value === undefined || value === null || value === '') return true
        return String(row[key] ?? '').includes(String(value))
      })
    })
    total.value = rows.value.length
    return
  }
  const method = option.value.api.method === 'post' ? post : get
  const res = await method(option.value.api.listUrl, params)
  if (version !== requestVersion.value) return
  const nextRows = normalizeRows(res)
  rows.value = nextRows
  total.value = normalizeTotal(res, nextRows)
}

const fetchRows = async () => {
  loading.value = true
  try {
    await requestList()
  } catch (error) {
    console.error(error)
    window['$message']?.error('CRUD 表格数据加载失败')
  } finally {
    loading.value = false
  }
}

const loadDict = async (dictType: string) => {
  if (!dictType || dictMap[dictType]) return
  try {
    const res: any = await get(`${option.value.dictApiPrefix}/${dictType}`)
    const list = res?.data || []
    dictMap[dictType] = list.map((item: any) => ({
      label: item.dictLabel ?? item.label,
      value: item.dictValue ?? item.value
    }))
  } catch (error) {
    console.error(error)
  }
}

const collectDicts = () => {
  option.value.searchFields.forEach(field => field.dictType && loadDict(field.dictType))
  option.value.columns.forEach(column => column.dictType && loadDict(column.dictType))
}

const initQueryForm = () => {
  option.value.searchFields.forEach(field => {
    if (field.type === 'dateRange' || field.type === 'numberRange') {
      const startKey = field.startKey || `${field.field}${field.type === 'dateRange' ? 'Start' : 'Min'}`
      const endKey = field.endKey || `${field.field}${field.type === 'dateRange' ? 'End' : 'Max'}`
      if (!(startKey in queryForm)) queryForm[startKey] = ''
      if (!(endKey in queryForm)) queryForm[endKey] = ''
      return
    }
    if (!(field.field in queryForm)) {
      if (field.type === 'multiSelect') queryForm[field.field] = []
      else if (field.type === 'switch') queryForm[field.field] = Boolean(field.defaultValue)
      else queryForm[field.field] = field.defaultValue ?? ''
    }
  })
  Object.keys(queryForm).forEach(key => {
    const exists = option.value.searchFields.some(field => {
      if (field.type === 'dateRange') return key === (field.startKey || `${field.field}Start`) || key === (field.endKey || `${field.field}End`)
      if (field.type === 'numberRange') return key === (field.startKey || `${field.field}Min`) || key === (field.endKey || `${field.field}Max`)
      return key === field.field
    })
    if (!exists) delete queryForm[key]
  })
}

const getFieldOptions = (field: CrudSearchField) => {
  return field.dictType ? dictMap[field.dictType] || [] : field.options || []
}

const getColumnOptions = (column: CrudColumn) => {
  return column.dictType ? dictMap[column.dictType] || [] : column.options || []
}

const formatDate = (value: any) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const formatCell = (row: Record<string, any>, column: CrudColumn) => {
  const value = getByPath(row, column.key)
  if (column.type === 'dict' || column.dictType || column.options) {
    const matched = getColumnOptions(column).find(item => String(item.value) === String(value))
    return matched?.label ?? value ?? ''
  }
  if (column.type === 'date') return formatDate(value)
  if (column.type === 'money') {
    const amount = Number(value || 0)
    const display = column.moneyUnit === 'cent' ? amount / 100 : amount
    return display.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
  return value ?? ''
}

const getRowKey = (row: Record<string, any>, index: number) => row.id || row.key || `${page.value}-${index}`

const formatLink = (row: Record<string, any>, column: CrudColumn) => {
  if (column.urlTemplate) return templateUrl(column.urlTemplate, row)
  return String(getByPath(row, column.key) || '#')
}

const clampPercent = (value: any) => Math.min(100, Math.max(0, Number(value) || 0))

const isActionVisible = (action: CrudRowAction, row: Record<string, any>) => {
  if (action.visibleConditions?.length) return evaluateConditions(action.visibleConditions, row, true)
  if (action.visibleWhen) return evaluateLegacyExpression(action.visibleWhen, row)
  return true
}

const isActionDisabled = (action: CrudRowAction, row: Record<string, any>) => {
  if (action.disabledConditions?.length) return evaluateConditions(action.disabledConditions, row, false)
  if (action.disabledWhen) return evaluateLegacyExpression(action.disabledWhen, row)
  return false
}

const handleSearch = () => {
  page.value = 1
  fetchRows()
}

const handleReset = () => {
  Object.keys(queryForm).forEach(key => {
    queryForm[key] = ''
  })
  handleSearch()
}

const changePage = (nextPage: number) => {
  page.value = Math.min(Math.max(1, nextPage), totalPage.value)
  fetchRows()
}

const buildActionParams = (action: CrudRowAction, row: Record<string, any>) => {
  const params: Record<string, any> = {}
  Object.entries(action.paramMap || {}).forEach(([targetKey, sourceKey]) => {
    params[targetKey] = getByPath(row, sourceKey)
  })
  return { ...getRuntimeContext(), ...params }
}

const runRequestAction = async (action: CrudRowAction, row: Record<string, any>) => {
  if (!action.url) return
  const params = buildActionParams(action, row)
  const methodMap = { get, post, put, delete: del }
  const method = methodMap[action.method || 'post']
  await method(templateUrl(action.url, row), params)
  window['$message']?.success('操作成功')
}

const runAfterAction = (action: CrudRowAction, params: Record<string, any>) => {
  if (!action.afterAction || action.afterAction === 'none') return
  if (action.afterAction === 'refresh') fetchRows()
  else if (action.afterAction === 'closeModal') chartEditStore.closeModal()
  else if (action.afterAction === 'goPage' && action.afterTargetPageId) switchPreviewPage(action.afterTargetPageId, params)
  else if (action.afterAction === 'openModal' && action.afterTargetPageId) chartEditStore.openModal(action.afterTargetPageId, params)
}

const handleRowAction = async (action: CrudRowAction, row: Record<string, any>) => {
  if (isActionDisabled(action, row)) return
  if (action.confirm && !window.confirm(action.confirmText || `确认执行“${action.label}”？`)) return
  const params = buildActionParams(action, row)
  const loadingKey = `${getRowKey(row, rows.value.indexOf(row))}-${action.label}`
  actionLoading[loadingKey] = true
  try {
  if (action.type === 'goPage' && action.targetPageId) {
    switchPreviewPage(action.targetPageId, params)
  } else if (action.type === 'openModal' && action.targetPageId) {
    chartEditStore.openModal(action.targetPageId, params)
  } else if (action.type === 'closeModal') {
    chartEditStore.closeModal()
  } else if (action.type === 'link' && action.url) {
    window.open(templateUrl(action.url, row), action.openTarget || '_self')
  } else if (action.type === 'request') {
    await runRequestAction(action, row)
  }
    runAfterAction(action, params)
  } finally {
    actionLoading[loadingKey] = false
  }
}

watch(
  () => [option.value.searchFields, option.value.columns, option.value.contextParamMap],
  () => {
    initQueryForm()
    collectDicts()
  },
  { immediate: true, deep: true }
)

watch(
  () => [option.value.api.listUrl, option.value.pageSize, option.value.staticRows],
  () => {
    page.value = 1
    fetchRows()
  },
  { deep: true }
)

onMounted(() => {
  fetchRows()
  window.addEventListener('forge-report-refresh', fetchRows)
})

onUnmounted(() => {
  window.removeEventListener('forge-report-refresh', fetchRows)
})
</script>

<style lang="scss" scoped>
.crud-table {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 14px;
  overflow: hidden;
  color: var(--crud-text);
  font-size: var(--crud-font-size);
  background:
    linear-gradient(135deg, rgba(37, 216, 255, 0.08), transparent 42%),
    var(--crud-panel);
  border: 1px solid var(--crud-border);
  border-radius: var(--crud-radius);
  box-shadow: inset 0 0 28px rgba(37, 216, 255, 0.08);
}

.crud-toolbar,
.crud-search,
.crud-pagination {
  flex: 0 0 auto;
}

.crud-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-title {
  display: flex;
  align-items: center;
  min-width: 0;
}

.title-mark {
  width: 4px;
  height: 28px;
  margin-right: 10px;
  border-radius: 3px;
  background: var(--crud-accent);
  box-shadow: 0 0 14px var(--crud-accent);
}

.title-text {
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
}

.subtitle-text {
  margin-top: 2px;
  color: var(--crud-muted);
  font-size: 11px;
}

.crud-search {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid var(--crud-border);
  border-radius: 6px;
  background: rgba(3, 13, 28, 0.38);
}

.search-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 150px;
  color: var(--crud-muted);
  font-size: 12px;
}

.search-field input,
.search-field select {
  height: 30px;
  min-width: 0;
  padding: 0 9px;
  color: var(--crud-text);
  border: 1px solid var(--crud-border);
  border-radius: 5px;
  outline: none;
  background: rgba(3, 12, 24, 0.76);
}

.search-field select[multiple] {
  height: 62px;
  padding: 4px 9px;
}

.range-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 6px;
}

.switch-control,
.radio-control {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
}

.radio-control {
  flex-wrap: wrap;
}

.switch-control input,
.radio-control input {
  width: auto;
  height: auto;
}

.search-actions {
  display: flex;
  gap: 8px;
}

button {
  height: 30px;
  padding: 0 12px;
  color: var(--crud-text);
  border: 1px solid var(--crud-border);
  border-radius: 5px;
  cursor: pointer;
  background: rgba(8, 30, 58, 0.7);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.primary-btn,
.refresh-btn {
  color: #00131f;
  border-color: transparent;
  background: linear-gradient(135deg, var(--crud-accent), #7dd3fc);
}

.ghost-btn {
  color: var(--crud-muted);
}

.table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--crud-border);
  border-radius: 6px;
}

table {
  width: 100%;
  min-width: 100%;
  border-collapse: collapse;
}

th,
td {
  height: 38px;
  padding: 0 12px;
  border-bottom: 1px solid var(--crud-border);
}

th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #dff8ff;
  font-weight: 700;
  background: var(--crud-header);
}

td {
  color: var(--crud-text);
  background: var(--crud-row);
}

tr:hover td {
  background: rgba(37, 216, 255, 0.08);
}

.index-cell {
  width: 54px;
  text-align: center;
  color: var(--crud-muted);
}

.action-cell {
  width: 150px;
  text-align: center;
  white-space: nowrap;
}

.row-action {
  height: 26px;
  margin: 0 3px;
  padding: 0 8px;
  font-size: 12px;
}

.row-action.is-primary {
  color: var(--crud-accent);
}

.row-action.is-success {
  color: var(--crud-success);
}

.row-action.is-warning {
  color: var(--crud-warning);
}

.row-action.is-error {
  color: var(--crud-error);
}

.dict-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  color: var(--crud-accent);
  border: 1px solid rgba(37, 216, 255, 0.28);
  border-radius: 999px;
  background: rgba(37, 216, 255, 0.08);
}

.cell-image {
  width: 48px;
  height: 28px;
  object-fit: cover;
  border-radius: 4px;
}

.cell-link {
  color: var(--crud-accent);
  text-decoration: none;
}

.progress-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 120px;
}

.progress-track {
  position: relative;
  flex: 1;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
}

.progress-bar {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--crud-accent), var(--crud-success));
}

.switch-tag {
  color: var(--crud-muted);
}

.switch-tag.active {
  color: var(--crud-success);
}

.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.state-cell {
  height: 120px;
  color: var(--crud-muted);
  text-align: center;
}

.crud-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 10px;
  color: var(--crud-muted);
}
</style>
