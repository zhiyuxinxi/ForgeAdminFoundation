import { request } from '@/utils'

export interface DataBusinessDataset {
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
}

export interface DataBusinessDefinition {
  id?: number
  businessCode: string
  businessName: string
  businessDesc: string
  analysisGoal?: string
  metricDefinition?: string
  dimensionDefinition?: string
  usageGuide?: string
  status?: number
  datasetCount?: number
  datasetNames?: string
  createTime?: string
  updateTime?: string
  datasets?: DataBusinessDataset[]
}

export interface DataBusinessAiContext extends DataBusinessDefinition {
  businessId: number
}

export function getDataBusinessPage(params: {
  pageNum: number
  pageSize: number
  businessName?: string
  status?: number
}) {
  return request.get('/data/business/page', { params })
}

export function getDataBusinessList() {
  return request.get('/data/business/list')
}

export function getDataBusinessById(id: number) {
  return request.get(`/data/business/${id}`)
}

export function createDataBusiness(data: DataBusinessDefinition) {
  return request.post('/data/business', data)
}

export function updateDataBusiness(data: DataBusinessDefinition) {
  return request.put('/data/business', data)
}

export function deleteDataBusiness(id: number) {
  return request.delete(`/data/business/${id}`)
}

export function getDataBusinessAiContext(id: number) {
  return request.get(`/data/business/${id}/ai-context`)
}
