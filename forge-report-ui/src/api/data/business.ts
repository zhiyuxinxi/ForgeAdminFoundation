import { get } from '../http'
import type { DataDatasetField } from './dataset'

export interface DataBusinessOption {
  id: number
  businessCode: string
  businessName: string
  businessDesc?: string
  analysisGoal?: string
  metricDefinition?: string
  dimensionDefinition?: string
  usageGuide?: string
  status?: number
  datasetCount?: number
}

export interface DataBusinessDatasetContext {
  id?: number
  datasetId: number
  datasetCode?: string
  datasetName?: string
  datasetType?: string
  description?: string
  paramSchemaJson?: string
  isPrimary?: number
  sort?: number
  usageRemark?: string
  fields?: DataDatasetField[]
}

export interface DataBusinessAiContext {
  businessId: number
  businessCode: string
  businessName: string
  businessDesc?: string
  analysisGoal?: string
  metricDefinition?: string
  dimensionDefinition?: string
  usageGuide?: string
  datasets: DataBusinessDatasetContext[]
}

export function getDataBusinessList(): Promise<{ data: DataBusinessOption[] }> {
  return get('/forge-report-api/data/business/list')
}

export function getDataBusinessAiContext(id: number | string): Promise<{ data: DataBusinessAiContext }> {
  return get(`/forge-report-api/data/business/${id}/ai-context`)
}
