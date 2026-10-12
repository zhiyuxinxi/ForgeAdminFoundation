import type { DataBusinessAiContext, DataBusinessDatasetContext } from '@/api/data/business'
import type { DataDatasetField } from '@/api/data/dataset'
import { queryDataDataset } from '@/api/data/dataset'

export type DatasetRuntimePreviewStatus = 'ready' | 'empty' | 'failed' | 'skipped'

export interface DatasetRuntimePreviewItem {
  datasetId: number
  datasetCode?: string
  datasetName?: string
  isPrimary?: number
  status: DatasetRuntimePreviewStatus
  fields: string[]
  rowCount: number
  sampleRows: Record<string, unknown>[]
  durationMs: number
  message?: string
}

export interface BusinessRuntimePreviewSummary {
  total: number
  ready: number
  empty: number
  failed: number
  skipped: number
  generatedAt: number
  items: DatasetRuntimePreviewItem[]
  suggestions: string[]
}

interface PreviewOptions {
  maxDatasets?: number
  maxFields?: number
  maxRows?: number
}

const DEFAULT_MAX_DATASETS = 6
const DEFAULT_MAX_FIELDS = 10
const DEFAULT_MAX_ROWS = 3

function getFieldName(field: DataDatasetField) {
  return String(field.fieldName || '').trim()
}

function normalizeRole(field: DataDatasetField) {
  return String(field.fieldRole || '').trim().toLowerCase()
}

function isRuntimePreviewSafeField(field: DataDatasetField) {
  const sensitiveLevel = String(field.sensitiveLevel || '').trim().toUpperCase()
  return sensitiveLevel !== 'HIDDEN' && sensitiveLevel !== 'MASK'
}

function choosePreviewFields(dataset: DataBusinessDatasetContext, maxFields: number) {
  const fields = dataset.fields || []
  const safeFields = fields.filter(isRuntimePreviewSafeField)
  const displayFields = safeFields.filter(field => field.displayEnabled !== 0 && getFieldName(field))
  const sourceFields = displayFields.length ? displayFields : safeFields.filter(field => getFieldName(field))
  const priorityRoles = new Set(['dimension', 'date', 'time', 'category', 'name', 'metric', 'measure', 'value', 'number'])
  const priorityFields = sourceFields.filter(field => priorityRoles.has(normalizeRole(field)))
  const orderedFields = [...priorityFields, ...sourceFields]
  const seen = new Set<string>()
  const result: string[] = []

  orderedFields.forEach(field => {
    const fieldName = getFieldName(field)
    if (!fieldName || seen.has(fieldName) || result.length >= maxFields) return
    seen.add(fieldName)
    result.push(fieldName)
  })

  return result
}

function normalizeCellValue(value: unknown): unknown {
  if (value === null || value === undefined) return value
  if (value instanceof Date) return value.toISOString()
  if (typeof value === 'number' || typeof value === 'boolean') return value
  const text = String(value)
  return text.length > 80 ? `${text.slice(0, 80)}...` : text
}

function compactRows(rows: Record<string, unknown>[], fields: string[]) {
  return rows.slice(0, DEFAULT_MAX_ROWS).map(row => {
    const compactRow: Record<string, unknown> = {}
    fields.forEach(field => {
      if (Object.prototype.hasOwnProperty.call(row, field)) {
        compactRow[field] = normalizeCellValue(row[field])
      }
    })
    return compactRow
  })
}

async function previewDataset(
  dataset: DataBusinessDatasetContext,
  options: Required<PreviewOptions>
): Promise<DatasetRuntimePreviewItem> {
  const startedAt = Date.now()
  const fields = choosePreviewFields(dataset, options.maxFields)
  const baseItem = {
    datasetId: dataset.datasetId,
    datasetCode: dataset.datasetCode,
    datasetName: dataset.datasetName,
    isPrimary: dataset.isPrimary,
    fields,
    rowCount: 0,
    sampleRows: [],
    durationMs: 0
  }

  if (!dataset.datasetId || !fields.length) {
    return {
      ...baseItem,
      status: 'skipped',
      durationMs: Date.now() - startedAt,
      message: '数据集缺少可展示字段，已跳过运行时预检'
    }
  }

  try {
    const res = await queryDataDataset({
      datasetId: dataset.datasetId,
      fields,
      pageNum: 1,
      pageSize: options.maxRows,
      maxRows: options.maxRows,
      outputMode: 'preview'
    })
    const source = Array.isArray(res?.data?.source) ? res.data.source : []
    const resultFields = Array.isArray(res?.data?.dimensions) && res.data.dimensions.length
      ? res.data.dimensions
      : fields

    return {
      ...baseItem,
      fields: resultFields,
      status: source.length ? 'ready' : 'empty',
      rowCount: source.length,
      sampleRows: compactRows(source, resultFields),
      durationMs: Date.now() - startedAt,
      message: source.length ? '运行时查询可用' : '运行时查询成功，但当前返回空数据'
    }
  } catch (error: any) {
    return {
      ...baseItem,
      status: 'failed',
      durationMs: Date.now() - startedAt,
      message: error?.message || '运行时查询失败'
    }
  }
}

function summarize(items: DatasetRuntimePreviewItem[]): BusinessRuntimePreviewSummary {
  const summary: BusinessRuntimePreviewSummary = {
    total: items.length,
    ready: items.filter(item => item.status === 'ready').length,
    empty: items.filter(item => item.status === 'empty').length,
    failed: items.filter(item => item.status === 'failed').length,
    skipped: items.filter(item => item.status === 'skipped').length,
    generatedAt: Date.now(),
    items,
    suggestions: []
  }

  if (summary.ready === 0 && summary.total > 0) {
    summary.suggestions.push('当前业务绑定数据集没有可用样例，AI 生成时可能会使用更多静态兜底。')
  }
  if (summary.failed > 0) {
    summary.suggestions.push('部分数据集运行时查询失败，建议检查必填参数、数据连接和 SQL。')
  }
  if (summary.empty > 0) {
    summary.suggestions.push('部分数据集查询为空，AI 会参考字段语义但无法判断真实取值分布。')
  }
  if (summary.skipped > 0) {
    summary.suggestions.push('部分数据集缺少可展示字段，建议重新同步字段或开启字段展示。')
  }

  return summary
}

export async function previewBusinessRuntimeData(
  context?: DataBusinessAiContext | null,
  options: PreviewOptions = {}
): Promise<BusinessRuntimePreviewSummary | null> {
  if (!context) return null
  const normalizedOptions: Required<PreviewOptions> = {
    maxDatasets: options.maxDatasets || DEFAULT_MAX_DATASETS,
    maxFields: options.maxFields || DEFAULT_MAX_FIELDS,
    maxRows: options.maxRows || DEFAULT_MAX_ROWS
  }
  const datasets = [...(context.datasets || [])]
    .sort((left, right) => {
      const primaryScore = (right.isPrimary || 0) - (left.isPrimary || 0)
      if (primaryScore !== 0) return primaryScore
      return (left.sort || 0) - (right.sort || 0)
    })
    .slice(0, normalizedOptions.maxDatasets)

  const items = await Promise.all(datasets.map(dataset => previewDataset(dataset, normalizedOptions)))
  return summarize(items)
}
