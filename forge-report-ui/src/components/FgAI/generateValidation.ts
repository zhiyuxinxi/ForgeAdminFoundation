import type { AIGenerateResponse, AIComponentSchema } from '@/api/ai/ai.d'
import type { DataBusinessAiContext, DataBusinessDatasetContext } from '@/api/data/business'
import type { DataDatasetField } from '@/api/data/dataset'
import { getComponentRegistry } from './componentRegistry'

export type GenerateValidationStatus = 'bound' | 'static' | 'staticFallback' | 'repaired' | 'unverified' | 'skipped'

export interface GenerateValidationItem {
  index: number
  key: string
  title: string
  status: GenerateValidationStatus
  datasetId?: number
  datasetName?: string
  fields: string[]
  messages: string[]
}

export interface GenerateValidationSummary {
  total: number
  accepted: number
  skipped: number
  bound: number
  staticFallback: number
  static: number
  unverified: number
  repaired: number
  items: GenerateValidationItem[]
  warnings: string[]
}

interface ValidateOptions {
  businessContext?: DataBusinessAiContext | null
  canvasWidth: number
  canvasHeight: number
}

interface DatasetCheckResult {
  status: GenerateValidationStatus
  datasetId?: number
  datasetName?: string
  fields: string[]
  repaired: boolean
  messages: string[]
}

interface DatasetMatchResult {
  dataset: DataBusinessDatasetContext
  fields: string[]
  score: number
  reason: string
}

const DEFAULT_CANVAS_WIDTH = 1920
const DEFAULT_CANVAS_HEIGHT = 1080

function cloneResponse(response: AIGenerateResponse): AIGenerateResponse {
  return JSON.parse(JSON.stringify(response || {})) as AIGenerateResponse
}

function toFiniteNumber(value: unknown, fallback: number) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

function normalizeStringArray(value: unknown): string[] {
  if (!value) return []
  if (Array.isArray(value)) {
    return value.map(item => String(item).trim()).filter(Boolean)
  }
  if (typeof value === 'string') {
    return value.split(/[,，\n]/).map(item => item.trim()).filter(Boolean)
  }
  return []
}

function getFieldName(field: DataDatasetField) {
  return String(field.fieldName || '').trim()
}

function getDisplayFields(fields: DataDatasetField[]) {
  const enabledFields = fields.filter(field => field.displayEnabled !== 0 && getFieldName(field))
  const source = enabledFields.length ? enabledFields : fields.filter(field => getFieldName(field))
  return source.slice(0, 8).map(field => getFieldName(field))
}

function normalizeText(value: unknown) {
  return String(value || '').trim().toLowerCase()
}

function getFieldSet(dataset: DataBusinessDatasetContext) {
  return new Set((dataset.fields || []).map(field => getFieldName(field)).filter(Boolean))
}

function getOptionDatasetDimensions(comp: AIComponentSchema): string[] {
  const optionDataset = comp.option?.dataset || comp.dataset
  if (!optionDataset || typeof optionDataset !== 'object') return []
  const dimensions = (optionDataset as Record<string, unknown>).dimensions
  if (Array.isArray(dimensions)) {
    return dimensions.map(item => String(item).trim()).filter(Boolean)
  }
  const source = (optionDataset as Record<string, unknown>).source
  if (Array.isArray(source) && source.length && source[0] && typeof source[0] === 'object') {
    return Object.keys(source[0] as Record<string, unknown>).filter(Boolean)
  }
  return []
}

function getRequestFieldCandidates(request: Record<string, unknown>, comp: AIComponentSchema) {
  const mapping = (request.datasetMapping || request.mapping || {}) as Record<string, unknown>
  const fieldMap = mapping.fieldMap && typeof mapping.fieldMap === 'object'
    ? Object.values(mapping.fieldMap as Record<string, unknown>)
    : []

  return [
    ...normalizeStringArray(request.datasetFields || request.fields),
    ...normalizeStringArray(mapping.outputFields),
    ...fieldMap.map(item => String(item || '').trim()).filter(Boolean),
    ...getOptionDatasetDimensions(comp)
  ].filter(Boolean)
}

function inferDataset(
  comp: AIComponentSchema,
  datasets: DataBusinessDatasetContext[],
  request: Record<string, unknown>
): DatasetMatchResult | null {
  if (!datasets.length) return null

  const requestFields = Array.from(new Set(getRequestFieldCandidates(request, comp)))
  const datasetCode = normalizeText(request.datasetCode || request.dataSetCode)
  const datasetName = normalizeText(request.datasetName || request.name)
  const titleText = normalizeText([
    comp.title,
    comp.key,
    request.datasetName,
    request.name,
    request.datasetCode,
    request.dataSetCode
  ].filter(Boolean).join(' '))

  let best: DatasetMatchResult | null = null
  datasets.forEach(dataset => {
    const fieldSet = getFieldSet(dataset)
    const matchedFields = requestFields.filter(fieldName => fieldSet.has(fieldName))
    const code = normalizeText(dataset.datasetCode)
    const name = normalizeText(dataset.datasetName)
    const usage = normalizeText(`${dataset.usageRemark || ''} ${dataset.description || ''}`)
    let score = 0
    const reasons: string[] = []

    if (datasetCode && code && datasetCode === code) {
      score += 90
      reasons.push('数据集编码精确匹配')
    } else if (datasetCode && code && (datasetCode.includes(code) || code.includes(datasetCode))) {
      score += 55
      reasons.push('数据集编码相似')
    }

    if (datasetName && name && datasetName === name) {
      score += 80
      reasons.push('数据集名称精确匹配')
    } else if (datasetName && name && (datasetName.includes(name) || name.includes(datasetName))) {
      score += 45
      reasons.push('数据集名称相似')
    }

    if (name && titleText.includes(name)) {
      score += 28
      reasons.push('组件标题命中数据集名称')
    }
    if (code && titleText.includes(code)) {
      score += 32
      reasons.push('组件标题命中数据集编码')
    }

    if (matchedFields.length) {
      score += Math.min(50, matchedFields.length * 14)
      reasons.push(`字段匹配 ${matchedFields.length} 个`)
      if (requestFields.length && matchedFields.length === requestFields.length) {
        score += 12
      }
    }

    if (usage && titleText && usage.includes(titleText)) {
      score += 8
      reasons.push('用途说明相似')
    }
    if (dataset.isPrimary === 1 && score > 0) {
      score += 3
    }

    if (score < 35) return

    const fields = matchedFields.length ? matchedFields : getDisplayFields(dataset.fields || [])
    if (!best || score > best.score) {
      best = {
        dataset,
        fields,
        score,
        reason: reasons.join('，') || '数据集画像匹配'
      }
    }
  })

  return best
}

function normalizeDatasetMapping(mapping: Record<string, unknown>, validFieldSet: Set<string>) {
  const normalized: Record<string, unknown> = {
    mode: mapping.mode || 'auto',
    syncHeader: mapping.syncHeader !== false
  }

  const fieldMap = mapping.fieldMap
  if (fieldMap && typeof fieldMap === 'object' && !Array.isArray(fieldMap)) {
    const nextFieldMap: Record<string, string> = {}
    Object.entries(fieldMap as Record<string, unknown>).forEach(([targetKey, fieldName]) => {
      const normalizedField = String(fieldName || '').trim()
      if (normalizedField && validFieldSet.has(normalizedField)) {
        nextFieldMap[targetKey] = normalizedField
      }
    })
    normalized.fieldMap = nextFieldMap
  } else {
    normalized.fieldMap = {}
  }

  const outputFields = normalizeStringArray(mapping.outputFields)
    .filter(fieldName => validFieldSet.has(fieldName))
  normalized.outputFields = outputFields

  return normalized
}

function buildDatasetMap(context?: DataBusinessAiContext | null) {
  const datasetMap = new Map<number, DataBusinessDatasetContext>()
  const datasets = context?.datasets || []
  datasets.forEach(dataset => {
    const datasetId = Number(dataset.datasetId)
    if (Number.isFinite(datasetId) && datasetId > 0) {
      datasetMap.set(datasetId, dataset)
    }
  })
  return datasetMap
}

function normalizeLayout(
  comp: AIComponentSchema,
  defaultW: number,
  defaultH: number,
  canvasWidth: number,
  canvasHeight: number
) {
  const messages: string[] = []
  const safeCanvasWidth = Math.max(320, toFiniteNumber(canvasWidth, DEFAULT_CANVAS_WIDTH))
  const safeCanvasHeight = Math.max(240, toFiniteNumber(canvasHeight, DEFAULT_CANVAS_HEIGHT))
  const minW = Math.min(80, Math.max(1, defaultW || 80))
  const minH = Math.min(40, Math.max(1, defaultH || 40))

  const original = {
    x: comp.x,
    y: comp.y,
    w: comp.w,
    h: comp.h
  }

  let w = toFiniteNumber(comp.w, defaultW || 500)
  let h = toFiniteNumber(comp.h, defaultH || 300)
  w = Math.min(Math.max(w, minW), safeCanvasWidth)
  h = Math.min(Math.max(h, minH), safeCanvasHeight)

  let x = toFiniteNumber(comp.x, 0)
  let y = toFiniteNumber(comp.y, 0)
  x = Math.max(0, Math.min(x, safeCanvasWidth - w))
  y = Math.max(0, Math.min(y, safeCanvasHeight - h))

  comp.x = Math.round(x)
  comp.y = Math.round(y)
  comp.w = Math.round(w)
  comp.h = Math.round(h)

  if (original.x !== comp.x || original.y !== comp.y || original.w !== comp.w || original.h !== comp.h) {
    messages.push('已修正组件位置或尺寸')
  }

  return messages
}

function validateDatasetRequest(
  comp: AIComponentSchema,
  datasetMap: Map<number, DataBusinessDatasetContext>,
  datasets: DataBusinessDatasetContext[],
  hasBusinessContext: boolean
): DatasetCheckResult {
  const request = comp.request || {}
  let datasetId = Number(request.datasetId || request.dataSetId)
  if (!Number.isFinite(datasetId) || datasetId <= 0) {
    if (hasBusinessContext) {
      const inferred = inferDataset(comp, datasets, request)
      if (inferred) {
        datasetId = Number(inferred.dataset.datasetId)
        comp.request = {
          ...request,
          datasetId,
          datasetName: inferred.dataset.datasetName || request.datasetName || request.name || '',
          datasetFields: inferred.fields
        }
        const result = validateDatasetRequest(comp, datasetMap, datasets, hasBusinessContext)
        return {
          ...result,
          status: result.status === 'bound' ? 'repaired' : result.status,
          repaired: true,
          messages: [`${inferred.reason}，已自动绑定数据集`, ...result.messages]
        }
      }
    }
    return {
      status: 'static',
      fields: [],
      repaired: false,
      messages: ['未声明数据集，使用静态数据']
    }
  }

  if (!hasBusinessContext) {
    return {
      status: 'unverified',
      datasetId,
      datasetName: String(request.datasetName || request.name || ''),
      fields: normalizeStringArray(request.datasetFields || request.fields),
      repaired: false,
      messages: ['未选择业务定义，保留数据集绑定但未校验']
    }
  }

  const dataset = datasetMap.get(datasetId)
  if (!dataset) {
    const inferred = inferDataset(comp, datasets, request)
    if (inferred) {
      datasetId = Number(inferred.dataset.datasetId)
      comp.request = {
        ...request,
        datasetId,
        datasetName: inferred.dataset.datasetName || request.datasetName || request.name || '',
        datasetFields: inferred.fields
      }
      const result = validateDatasetRequest(comp, datasetMap, datasets, hasBusinessContext)
      return {
        ...result,
        status: result.status === 'bound' ? 'repaired' : result.status,
        repaired: true,
        messages: [`原数据集 ${request.datasetId || request.dataSetId} 无效，${inferred.reason}，已自动改绑`, ...result.messages]
      }
    }
    delete comp.request
    return {
      status: 'staticFallback',
      datasetId,
      fields: [],
      repaired: true,
      messages: [`数据集 ${datasetId} 不在当前业务定义中，已降级为静态数据`]
    }
  }

  const datasetFields = dataset.fields || []
  const validFieldSet = new Set(datasetFields.map(field => getFieldName(field)).filter(Boolean))
  const requestedFields = normalizeStringArray(request.datasetFields || request.fields)
  let validFields = requestedFields.filter(fieldName => validFieldSet.has(fieldName))
  const messages: string[] = []
  let repaired = false

  if (requestedFields.length && validFields.length !== requestedFields.length) {
    messages.push('已移除不存在的数据集字段')
    repaired = true
  }

  if (!validFields.length && validFieldSet.size) {
    validFields = getDisplayFields(datasetFields)
    messages.push('已按数据集可展示字段补齐字段列表')
    repaired = true
  }

  const mappingSource = request.datasetMapping || request.mapping || {}
  const datasetMapping = normalizeDatasetMapping(mappingSource as Record<string, unknown>, validFieldSet)
  const mappingOutputFields = normalizeStringArray(datasetMapping.outputFields)
  if (mappingOutputFields.length && !validFields.length) {
    validFields = mappingOutputFields
  }
  if (!mappingOutputFields.length && validFields.length) {
    datasetMapping.outputFields = validFields
  }

  comp.request = {
    ...request,
    datasetId,
    datasetName: dataset.datasetName || request.datasetName || request.name || '',
    datasetFields: validFields,
    datasetMapping
  }

  return {
    status: repaired ? 'repaired' : 'bound',
    datasetId,
    datasetName: dataset.datasetName || '',
    fields: validFields,
    repaired,
    messages: messages.length ? messages : ['数据集绑定已确认']
  }
}

function emptySummary(total: number): GenerateValidationSummary {
  return {
    total,
    accepted: 0,
    skipped: 0,
    bound: 0,
    staticFallback: 0,
    static: 0,
    unverified: 0,
    repaired: 0,
    items: [],
    warnings: []
  }
}

export function validateAIGenerateResponse(
  sourceResponse: AIGenerateResponse,
  options: ValidateOptions
): { response: AIGenerateResponse; summary: GenerateValidationSummary } {
  const response = cloneResponse(sourceResponse)
  const components = Array.isArray(response.components) ? response.components : []
  const registry = getComponentRegistry()
  const datasetMap = buildDatasetMap(options.businessContext)
  const datasets = options.businessContext?.datasets || []
  const hasBusinessContext = !!options.businessContext
  const summary = emptySummary(components.length)
  const acceptedComponents: AIComponentSchema[] = []

  components.forEach((component, index) => {
    const key = String(component?.key || '').trim()
    const descriptor = registry.get(key)
    const title = String(component?.title || descriptor?.title || key || `组件 ${index + 1}`)
    const messages: string[] = []

    if (!descriptor) {
      summary.skipped += 1
      summary.warnings.push(`第 ${index + 1} 个组件 ${key || '未声明 key'} 不在组件库中，已跳过`)
      summary.items.push({
        index,
        key: key || '-',
        title,
        status: 'skipped',
        fields: [],
        messages: ['未知组件，已跳过']
      })
      return
    }

    component.key = key
    messages.push(...normalizeLayout(
      component,
      descriptor.defaultW,
      descriptor.defaultH,
      options.canvasWidth,
      options.canvasHeight
    ))

    const datasetResult = validateDatasetRequest(component, datasetMap, datasets, hasBusinessContext)
    messages.push(...datasetResult.messages)

    if (datasetResult.status === 'bound' || datasetResult.status === 'repaired') {
      summary.bound += 1
    } else if (datasetResult.status === 'staticFallback') {
      summary.staticFallback += 1
      summary.warnings.push(`${title} 的数据集绑定无效，已转为静态数据`)
    } else if (datasetResult.status === 'static') {
      summary.static += 1
    } else if (datasetResult.status === 'unverified') {
      summary.unverified += 1
      summary.warnings.push(`${title} 未选择业务定义，数据集绑定未校验`)
    }

    const repaired = messages.some(message => message.startsWith('已')) || datasetResult.repaired
    if (repaired) {
      summary.repaired += 1
    }

    summary.items.push({
      index,
      key,
      title,
      status: datasetResult.status,
      datasetId: datasetResult.datasetId,
      datasetName: datasetResult.datasetName,
      fields: datasetResult.fields,
      messages
    })
    acceptedComponents.push(component)
  })

  summary.accepted = acceptedComponents.length
  response.components = acceptedComponents
  if (!response.title) {
    response.title = options.businessContext?.businessName || 'AI 数据大屏'
  }

  return { response, summary }
}
