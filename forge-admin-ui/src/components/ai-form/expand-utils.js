import {
  queryBusinessQuantityBalance,
  queryBusinessQuantityLedger,
  queryBusinessQuantityLock,
} from '@/api/business-app'
import { request } from '@/utils'
import { postEncrypt } from '@/utils/encrypt-request'

const DEFAULT_DATA_FIELD = 'records'
const DEFAULT_TOTAL_FIELD = 'total'
const QUANTITY_PANEL_TYPES = ['quantity-balance', 'quantity-ledger', 'quantity-lock']

export function normalizeExpandConfig(config = {}, childrenConfig = []) {
  if (!config || config.enabled !== true)
    return { enabled: false, panels: [] }

  const sourcePanels = Array.isArray(config.panels)
    ? config.panels
    : config.panel ? [config.panel] : []

  const panels = sourcePanels
    .map((panel, index) => normalizeExpandPanel(panel, index, childrenConfig))
    .filter(Boolean)

  return {
    ...config,
    enabled: panels.length > 0,
    trigger: config.trigger || 'icon',
    lazy: config.lazy !== false,
    cache: config.cache !== false,
    defaultExpanded: config.defaultExpanded === true,
    layout: {
      mode: panels.length > 1 ? 'tabs' : 'single',
      density: 'compact',
      padding: 12,
      ...(config.layout || {}),
    },
    panels,
  }
}

function normalizeExpandPanel(panel, index, childrenConfig = []) {
  if (!panel || panel.visible === false)
    return null
  const type = panel.type || 'descriptions'
  const key = panel.key || panel.field || `${type}_${index}`
  const childConfig = resolveChildConfig(panel, childrenConfig)
  return {
    ...panel,
    key,
    type,
    title: panel.title || childConfig?.title || childConfig?.label || defaultPanelTitle(type, index),
    dataSource: normalizeDataSource(panel.dataSource, type, childConfig),
    table: normalizeTableConfig(panel.table, childConfig, type),
    quantity: normalizeQuantityConfig(panel.quantity, type),
    descriptions: normalizeDescriptionsConfig(panel.descriptions),
    form: normalizeFormConfig(panel.form),
    panels: Array.isArray(panel.panels)
      ? panel.panels.map((child, childIndex) => normalizeExpandPanel(child, childIndex, childrenConfig)).filter(Boolean)
      : [],
  }
}

function resolveChildConfig(panel, childrenConfig = []) {
  const childKey = panel.childKey || panel.relationKey || panel.key
  if (!childKey)
    return null
  return childrenConfig.find(child => [child.key, child.field, child.relationKey, child.tableName].filter(Boolean).includes(childKey)) || null
}

function normalizeDataSource(dataSource, type, childConfig) {
  const source = dataSource || childConfig?.dataSource || {}
  if (source.type)
    return source
  if (isQuantityPanelType(type))
    return { ...source, type: 'quantity', queryType: type }
  if (source.api || source.url)
    return { ...source, type: 'api' }
  if (childConfig?.api || childConfig?.listApi)
    return { type: 'api', api: childConfig.api || childConfig.listApi, paramsMap: childConfig.paramsMap || {} }
  if (type === 'table' && childConfig?.field)
    return { type: 'row', field: childConfig.field }
  return { ...source, type: 'row' }
}

function normalizeTableConfig(table = {}, childConfig, type = '') {
  const source = table || {}
  const configuredColumns = Array.isArray(source.columns) && source.columns.length
    ? source.columns
    : Array.isArray(childConfig?.columns) && childConfig.columns.length
      ? childConfig.columns
      : Array.isArray(childConfig?.schema) && childConfig.schema.length
        ? childConfig.schema
        : null
  const columns = configuredColumns || defaultQuantityColumns(type)
  return {
    rowKey: source.rowKey || childConfig?.rowKey || 'id',
    columns,
    pagination: source.pagination === true ? {} : false,
    maxHeight: source.maxHeight ?? 320,
    scrollX: source.scrollX,
    bordered: source.bordered ?? false,
    striped: source.striped ?? false,
    size: source.size || 'small',
    hideSelection: source.hideSelection !== false,
    showToolbar: source.showToolbar === true,
    showRenderModeSwitch: false,
  }
}

function defaultQuantityColumns(type = '') {
  if (type === 'quantity-balance') {
    return [
      { prop: 'accountCode', label: '账户', minWidth: 120 },
      { prop: 'itemCode', label: '数量项', minWidth: 120 },
      { prop: 'dimensionKey', label: '维度', minWidth: 140 },
      { prop: 'quantity', label: '余额', width: 120 },
      { prop: 'lockedQuantity', label: '锁定', width: 120 },
      { prop: 'availableQuantity', label: '可用', width: 120 },
    ]
  }
  if (type === 'quantity-ledger') {
    return [
      { prop: 'operationType', label: '操作', width: 120 },
      { prop: 'accountCode', label: '账户', minWidth: 120 },
      { prop: 'itemCode', label: '数量项', minWidth: 120 },
      { prop: 'dimensionKey', label: '维度', minWidth: 140 },
      { prop: 'quantityDelta', label: '变动', width: 120 },
      { prop: 'balanceQuantity', label: '余额', width: 120 },
      { prop: 'sourceRecordId', label: '来源记录', minWidth: 140 },
      { prop: 'createTime', label: '发生时间', minWidth: 160 },
    ]
  }
  if (type === 'quantity-lock') {
    return [
      { prop: 'lockCode', label: '锁定号', minWidth: 140 },
      { prop: 'accountCode', label: '账户', minWidth: 120 },
      { prop: 'itemCode', label: '数量项', minWidth: 120 },
      { prop: 'dimensionKey', label: '维度', minWidth: 140 },
      { prop: 'lockQuantity', label: '锁定数量', width: 120 },
      { prop: 'remainingQuantity', label: '剩余', width: 120 },
      { prop: 'lockStatus', label: '状态', width: 120 },
    ]
  }
  return []
}

function normalizeQuantityConfig(config = {}, type = '') {
  if (!isQuantityPanelType(type))
    return config || {}
  return {
    queryType: type,
    pageNum: 1,
    pageSize: 20,
    ...(config || {}),
  }
}

function normalizeDescriptionsConfig(config = {}) {
  return {
    columns: config.columns || 3,
    labelPlacement: config.labelPlacement || 'left',
    fields: Array.isArray(config.fields) ? config.fields : [],
  }
}

function normalizeFormConfig(config = {}) {
  return {
    schema: Array.isArray(config.schema) ? config.schema : [],
    gridCols: config.gridCols || 2,
    labelWidth: config.labelWidth || 'auto',
    labelPlacement: config.labelPlacement || 'left',
    size: config.size || 'small',
  }
}

function defaultPanelTitle(type, index) {
  const titleMap = {
    'table': '明细',
    'descriptions': '概览',
    'form': '详情',
    'tabs': '更多',
    'custom': '扩展',
    'quantity-balance': '数量余额',
    'quantity-ledger': '数量流水',
    'quantity-lock': '数量锁定',
  }
  return titleMap[type] || `面板 ${index + 1}`
}

export function shouldExpandRow(rowExpandable, row, context = {}) {
  if (!rowExpandable)
    return true
  if (typeof rowExpandable === 'function')
    return rowExpandable(row, context)
  if (rowExpandable === false)
    return false
  if (typeof rowExpandable === 'string')
    return Boolean(resolveExpressionValue(rowExpandable, { row, ...context }))
  if (rowExpandable.type === 'expression')
    return Boolean(resolveExpressionValue(rowExpandable.expression, { row, ...context }))
  if (rowExpandable.field)
    return Boolean(resolveExpressionValue(`row.${rowExpandable.field}`, { row, ...context }))
  return true
}

export function buildExpandParams(paramsMap = {}, row = {}, context = {}) {
  const sourceContext = { row, ...context }
  return Object.entries(paramsMap || {}).reduce((params, [key, expression]) => {
    const value = typeof expression === 'string'
      ? resolveExpressionValue(expression, sourceContext)
      : expression
    if (value !== undefined && value !== null && value !== '')
      params[key] = value
    return params
  }, {})
}

export function resolveExpressionValue(expression, context = {}) {
  if (!expression && expression !== 0)
    return undefined
  if (typeof expression !== 'string')
    return expression
  const text = expression.trim()
  if (!text)
    return undefined
  if ((text.startsWith('\'') && text.endsWith('\'')) || (text.startsWith('"') && text.endsWith('"')))
    return text.slice(1, -1)
  const templateMatched = text.match(/^\$\{(.+)\}$/)
  if (templateMatched)
    return resolveExpressionValue(templateMatched[1], context)
  if (/^-?\d+(?:\.\d+)?$/.test(text))
    return Number(text)
  return text.split('.').reduce((value, key) => {
    if (value === undefined || value === null)
      return undefined
    return value[key]
  }, context)
}

export async function loadExpandPanelData(panel, row, context = {}) {
  const dataSource = panel?.dataSource || { type: 'row' }
  if (dataSource.type === 'none')
    return null
  if (dataSource.type === 'row') {
    const value = dataSource.field ? resolveExpressionValue(`row.${dataSource.field}`, { row, ...context }) : row
    return value ?? (panel.type === 'table' ? [] : {})
  }
  if (dataSource.type === 'static')
    return dataSource.data
  if (dataSource.type === 'api')
    return loadApiExpandData(panel, row, context)
  if (dataSource.type === 'quantity' || isQuantityPanelType(panel?.type))
    return loadQuantityExpandData(panel, row, context)
  return row
}

async function loadApiExpandData(panel, row, context) {
  const dataSource = panel.dataSource || {}
  const apiConfig = dataSource.api || dataSource.url || ''
  if (!apiConfig)
    return panel.type === 'table' ? [] : {}

  const { method, url } = parseExpandApiConfig(apiConfig, dataSource.method || 'get', row, context)
  const params = buildExpandParams(dataSource.paramsMap || dataSource.params || {}, row, context)
  const requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
  const useEncrypt = requestMethod === 'postEncrypt' || dataSource.isEncrypt === true

  let response
  if (useEncrypt && requestMethod === 'postEncrypt') {
    response = await postEncrypt(url, params)
  }
  else if (requestMethod === 'get') {
    response = await request.get(url, { params })
  }
  else {
    response = await request({
      method: requestMethod,
      url,
      data: params,
    })
  }
  return extractExpandData(response, panel)
}

function parseExpandApiConfig(apiConfig, defaultMethod, row, context) {
  const text = String(apiConfig || '')
  const parts = text.split('@')
  const method = parts.length > 1 ? parts[0] : defaultMethod
  let url = parts.length > 1 ? parts.slice(1).join('@') : text
  const sourceContext = { row, ...context }
  url = url.replace(/:(\w+)/g, (_, key) => {
    const value = resolveExpressionValue(`row.${key}`, sourceContext)
    return value ?? ''
  })
  url = url.replace(/\{([\w.]+)\}/g, (_, key) => {
    const expression = key.includes('.') ? key : `row.${key}`
    return resolveExpressionValue(expression, sourceContext) ?? ''
  })
  return { method, url }
}

export function extractExpandData(response, panel = {}) {
  const dataSource = panel.dataSource || {}
  const responseData = response?.data ?? response
  const payload = responseData?.data ?? responseData
  const dataField = dataSource.dataField || panel.dataField || DEFAULT_DATA_FIELD
  if (Array.isArray(payload))
    return payload
  if (!payload || typeof payload !== 'object')
    return payload
  if (dataField && payload[dataField] !== undefined)
    return payload[dataField]
  if (payload.rows !== undefined)
    return payload.rows
  if (payload.list !== undefined)
    return payload.list
  return payload
}

export function extractExpandTotal(response, panel = {}) {
  const dataSource = panel.dataSource || {}
  const responseData = response?.data ?? response
  const payload = responseData?.data ?? responseData
  const totalField = dataSource.totalField || panel.totalField || DEFAULT_TOTAL_FIELD
  if (!payload || typeof payload !== 'object')
    return 0
  return Number(payload[totalField] || payload.total || payload.count || 0)
}

function isQuantityPanelType(type = '') {
  return QUANTITY_PANEL_TYPES.includes(String(type || ''))
}

async function loadQuantityExpandData(panel, row, context = {}) {
  const queryType = panel?.quantity?.queryType || panel?.dataSource?.queryType || panel?.type
  const paramsMap = panel?.quantity?.paramsMap || panel?.dataSource?.paramsMap || panel?.dataSource?.params || {}
  const params = {
    ...buildExpandParams(paramsMap, row, context),
    pageNum: panel?.quantity?.pageNum || panel?.dataSource?.pageNum || 1,
    pageSize: panel?.quantity?.pageSize || panel?.dataSource?.pageSize || 20,
  }
  if (queryType === 'quantity-ledger')
    return extractExpandData(await queryBusinessQuantityLedger(params), panel)
  if (queryType === 'quantity-lock')
    return extractExpandData(await queryBusinessQuantityLock(params), panel)
  return extractExpandData(await queryBusinessQuantityBalance(params), panel)
}
