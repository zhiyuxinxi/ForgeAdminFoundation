import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { CrudTableConfig } from './index'

export type CrudFieldType = 'input' | 'select' | 'date' | 'dateRange' | 'number' | 'numberRange' | 'multiSelect' | 'radio' | 'switch'
export type CrudActionType = 'goPage' | 'openModal' | 'closeModal' | 'link' | 'request'
export type CrudColumnType = 'text' | 'dict' | 'date' | 'money' | 'image' | 'link' | 'progress' | 'switch'
export type CrudActionAfter = 'none' | 'refresh' | 'closeModal' | 'goPage' | 'openModal'
export type CrudConditionOperator = 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'contains' | 'empty' | 'notEmpty'

export interface CrudCondition {
  field: string
  operator: CrudConditionOperator
  value?: string | number | boolean
}

export interface CrudSearchField {
  label: string
  field: string
  type: CrudFieldType
  placeholder?: string
  dictType?: string
  options?: Array<{ label: string; value: string | number }>
  startKey?: string
  endKey?: string
  defaultValue?: string | number | boolean | Array<string | number>
}

export interface CrudColumn {
  title: string
  key: string
  type?: CrudColumnType
  width?: number
  align?: 'left' | 'center' | 'right'
  dictType?: string
  options?: Array<{ label: string; value: string | number }>
  format?: string
  moneyUnit?: 'yuan' | 'cent'
  urlTemplate?: string
  openTarget?: '_self' | '_blank'
  ellipsis?: boolean
}

export interface CrudRowAction {
  label: string
  type: CrudActionType
  targetPageId?: string
  url?: string
  method?: 'get' | 'post' | 'put' | 'delete'
  paramMap?: Record<string, string>
  openTarget?: '_self' | '_blank'
  style?: 'primary' | 'success' | 'warning' | 'error' | 'info'
  confirm?: boolean
  confirmText?: string
  visibleWhen?: string
  disabledWhen?: string
  visibleConditions?: CrudCondition[]
  disabledConditions?: CrudCondition[]
  afterAction?: CrudActionAfter
  afterTargetPageId?: string
}

export const option = {
  title: '业务数据',
  subtitle: '支持查询、字典、行操作和下钻',
  api: {
    listUrl: '',
    method: 'get',
    dataPath: 'data.records',
    totalPath: 'data.total',
    pageNumKey: 'pageNum',
    pageSizeKey: 'pageSize'
  },
  contextParamMap: {} as Record<string, string>,
  searchFields: [
    { label: '关键词', field: 'keyword', type: 'input', placeholder: '输入关键词' }
  ] as CrudSearchField[],
  columns: [
    { title: '名称', key: 'name', type: 'text', width: 160, align: 'left' },
    { title: '状态', key: 'status', type: 'dict', width: 100, align: 'center', options: [{ label: '正常', value: '1' }] }
  ] as CrudColumn[],
  actions: [
    { label: '查看', type: 'openModal', style: 'primary', afterAction: 'none', paramMap: { id: 'id', name: 'name' } }
  ] as CrudRowAction[],
  staticRows: [
    { id: 1, name: '示例数据 A', status: '1' },
    { id: 2, name: '示例数据 B', status: '0' }
  ],
  pageSize: 8,
  showToolbar: true,
  showSearch: true,
  showIndex: true,
  showActions: true,
  dictApiPrefix: '/forge-report-api/system/dict/data/type',
  style: {
    accentColor: '#25d8ff',
    successColor: '#34d399',
    warningColor: '#fbbf24',
    errorColor: '#fb7185',
    textColor: '#e7f4ff',
    mutedColor: '#7ea6c8',
    panelColor: 'rgba(4, 18, 38, 0.82)',
    headerColor: 'rgba(13, 38, 72, 0.92)',
    rowColor: 'rgba(8, 30, 58, 0.64)',
    borderColor: 'rgba(94, 234, 212, 0.24)',
    radius: 8,
    fontSize: 13
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = CrudTableConfig.key
  public attr = { ...chartInitConfig, w: 860, h: 480, zIndex: -1 }
  public chartConfig = cloneDeep(CrudTableConfig)
  public option = cloneDeep(option)
}
