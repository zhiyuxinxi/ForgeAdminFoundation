import type { DataBusinessAiContext } from '@/api/data/business'

export interface BusinessReadinessResult {
  score: number
  level: 'high' | 'medium' | 'low'
  label: string
  missing: string[]
  suggestions: string[]
  datasetCount: number
  fieldCount: number
  primaryDatasetName?: string
}

function hasText(value?: string | null) {
  return !!String(value || '').trim()
}

function countDatasetFields(context: DataBusinessAiContext) {
  return (context.datasets || []).reduce((sum, dataset) => sum + (dataset.fields?.length || 0), 0)
}

export function evaluateBusinessReadiness(context?: DataBusinessAiContext | null): BusinessReadinessResult | null {
  if (!context) return null

  const missing: string[] = []
  const suggestions: string[] = []
  let score = 0

  const semanticFields: Array<{ key: keyof DataBusinessAiContext; label: string; score: number; suggestion: string }> = [
    { key: 'businessDesc', label: '业务定义描述', score: 12, suggestion: '补充业务边界、管理对象和使用场景。' },
    { key: 'analysisGoal', label: '分析目标', score: 12, suggestion: '明确大屏要回答的经营或运营问题。' },
    { key: 'metricDefinition', label: '指标口径', score: 14, suggestion: '补充指标单位、聚合方式、同比/环比口径。' },
    { key: 'dimensionDefinition', label: '分析维度', score: 12, suggestion: '补充时间、区域、组织、渠道等分析维度。' },
    { key: 'usageGuide', label: 'AI 使用建议', score: 10, suggestion: '说明哪些数据集适合 KPI、趋势、排行、明细或地图。' }
  ]

  semanticFields.forEach(field => {
    if (hasText(context[field.key] as string)) {
      score += field.score
      return
    }
    missing.push(field.label)
    suggestions.push(field.suggestion)
  })

  const datasets = context.datasets || []
  const datasetCount = datasets.length
  const fieldCount = countDatasetFields(context)
  const primaryDataset = datasets.find(dataset => dataset.isPrimary === 1) || datasets[0]

  if (datasetCount > 0) {
    score += Math.min(18, 10 + datasetCount * 2)
  } else {
    missing.push('绑定数据集')
    suggestions.push('至少绑定一个已发布数据集，否则 AI 只能生成静态数据。')
  }

  if (fieldCount >= 12) {
    score += 14
  } else if (fieldCount > 0) {
    score += 8
    suggestions.push('当前数据集字段偏少，建议补充字段同步或字段语义。')
  } else {
    missing.push('数据集字段')
    suggestions.push('数据集缺少字段上下文，AI 无法稳定输出 datasetFields。')
  }

  if (datasets.some(dataset => hasText(dataset.usageRemark))) {
    score += 5
  } else if (datasetCount > 0) {
    suggestions.push('为绑定数据集补充用途说明，可提升组件和数据集匹配率。')
  }

  if (datasets.some(dataset => dataset.isPrimary === 1)) {
    score += 3
  } else if (datasetCount > 1) {
    suggestions.push('建议设置一个主数据集，方便 AI 优先规划核心指标和主图。')
  }

  const normalizedScore = Math.min(100, Math.max(0, score))
  const level = normalizedScore >= 80 ? 'high' : normalizedScore >= 55 ? 'medium' : 'low'
  const label = level === 'high' ? '准备充分' : level === 'medium' ? '可生成' : '需补充'

  return {
    score: normalizedScore,
    level,
    label,
    missing,
    suggestions: suggestions.slice(0, 4),
    datasetCount,
    fieldCount,
    primaryDatasetName: primaryDataset?.datasetName
  }
}
