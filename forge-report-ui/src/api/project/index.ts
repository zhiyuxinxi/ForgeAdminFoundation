import { del, get, post, put } from '@/api/http'
import { extractPageStorage, isMultiPageStorage } from '@/utils/reportPages'
import type { ChartEditStorage, ReportMultiPageStorage } from '@/store/modules/chartEditStore/chartEditStore.d'

export interface ForgeProject {
  id: number | string
  directoryId?: number | string
  projectName: string
  remark?: string
  indexImg?: string
  status?: string
  canvasWidth?: number
  canvasHeight?: number
  backgroundColor?: string
  componentData?: string
  publishStatus?: string
  publishUrl?: string
  publishTime?: string
  createTime?: string
}

export interface ReportDirectory {
  id: number | string
  parentId?: number | string
  ancestors?: string
  directoryName: string
  sort?: number
  remark?: string
  createTime?: string
  updateTime?: string
  children?: ReportDirectory[]
}

export interface ProjectPageResponse {
  records: ForgeProject[]
  total: number
  size: number
  current: number
}

export interface ReportProjectVersion {
  id: number | string
  projectId: number | string
  projectName?: string
  versionNo?: number
  versionName?: string
  operationType?: 'publish' | 'rollback' | string
  sourceVersionId?: number | string
  publisherId?: number | string
  publisherName?: string
  publishTime?: string
  publishUrl?: string
  indexImg?: string
  canvasWidth?: number
  canvasHeight?: number
  backgroundColor?: string
  componentData?: string
  remark?: string
  createTime?: string
}

export interface ProjectVersionPageResponse {
  records: ReportProjectVersion[]
  total: number
  size: number
  current: number
}

export const getProjectPageApi = (params?: { pageNum?: number; pageSize?: number; projectName?: string; directoryId?: number | string }) => {
  return get('/forge-report-api/report/project/page', params) as unknown as Promise<{ code: number; data: ProjectPageResponse; msg: string }>
}

export const getProjectDetailApi = (id: string | number) => {
  return get(`/forge-report-api/report/project/${id}`) as unknown as Promise<{ code: number; data: ForgeProject; msg: string }>
}

export const createProjectApi = (data: Partial<ForgeProject>) => {
  return post('/forge-report-api/report/project', data) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const updateProjectApi = (data: Partial<ForgeProject>) => {
  return put('/forge-report-api/report/project', data) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const deleteProjectApi = (id: string | number) => {
  return del(`/forge-report-api/report/project/${id}`) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const publishProjectApi = (id: string | number, publishUrl: string) => {
  return post(`/forge-report-api/report/project/publish/${id}?publishUrl=${encodeURIComponent(publishUrl)}`) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const getProjectVersionPageApi = (projectId: string | number, params?: { pageNum?: number; pageSize?: number }) => {
  return get(`/forge-report-api/report/project/${projectId}/versions`, params) as unknown as Promise<{ code: number; data: ProjectVersionPageResponse; msg: string }>
}

export const getProjectVersionDetailApi = (versionId: string | number) => {
  return get(`/forge-report-api/report/project/version/${versionId}`) as unknown as Promise<{ code: number; data: ReportProjectVersion; msg: string }>
}

export const rollbackProjectVersionApi = (versionId: string | number) => {
  return post(`/forge-report-api/report/project/version/${versionId}/rollback`) as unknown as Promise<{ code: number; data: ReportProjectVersion; msg: string }>
}

export const getProjectDirectoryTreeApi = () => {
  return get('/forge-report-api/report/directory/tree') as unknown as Promise<{ code: number; data: ReportDirectory[]; msg: string }>
}

export const createProjectDirectoryApi = (data: Partial<ReportDirectory>) => {
  return post('/forge-report-api/report/directory', data) as unknown as Promise<{ code: number; data: ReportDirectory; msg: string }>
}

export const updateProjectDirectoryApi = (data: Partial<ReportDirectory>) => {
  return put('/forge-report-api/report/directory', data) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const moveProjectDirectoryApi = (data: { id: string | number; targetParentId?: string | number | null }) => {
  return put('/forge-report-api/report/directory/move', data) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const deleteProjectDirectoryApi = (id: string | number) => {
  return del(`/forge-report-api/report/directory/${id}`) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const buildProjectPayload = (
  rawId: string | string[] | number,
  storageInfo: ChartEditStorage | ReportMultiPageStorage,
  indexImg?: string,
  baseProject?: Partial<ForgeProject>
) => {
  const id = Array.isArray(rawId) ? rawId[0] : rawId
  const activeStorage = isMultiPageStorage(storageInfo)
    ? extractPageStorage(storageInfo, storageInfo.activePageId)
    : storageInfo
  const normalizeName = (value?: string | null) => {
    const nextValue = typeof value === 'string' ? value.trim() : ''
    return nextValue || undefined
  }
  const projectName = isMultiPageStorage(storageInfo)
    ? normalizeName(storageInfo.projectName) || normalizeName(baseProject?.projectName)
    : normalizeName(storageInfo.editCanvasConfig?.projectName) || normalizeName(baseProject?.projectName)
  const normalizeBackgroundColor = (value?: string | null) => {
    const color = typeof value === 'string' ? value.trim().replace(/\s+/g, '') : ''
    if (!color) return ''
    if (/^#[0-9a-fA-F]{3,8}$/.test(color) && color.length <= 20) return color
    const rgbaMatch = color.match(/^rgba?\((\d{1,3}),(\d{1,3}),(\d{1,3})(?:,([01]?(?:\.\d+)?))?\)$/i)
    if (!rgbaMatch) return ''
    const toHex = (input: string) => {
      const value = Math.max(0, Math.min(255, Number(input) || 0))
      return value.toString(16).padStart(2, '0')
    }
    const alpha = rgbaMatch[4] === undefined
      ? ''
      : Math.round(Math.max(0, Math.min(1, Number(rgbaMatch[4]) || 0)) * 255).toString(16).padStart(2, '0')
    return `#${toHex(rgbaMatch[1])}${toHex(rgbaMatch[2])}${toHex(rgbaMatch[3])}${alpha}`
  }
  const payload: Partial<ForgeProject> = {
    id,
    canvasWidth: activeStorage.editCanvasConfig?.width,
    canvasHeight: activeStorage.editCanvasConfig?.height,
    backgroundColor: normalizeBackgroundColor(activeStorage.editCanvasConfig?.background),
    componentData: JSON.stringify(storageInfo)
  }
  if (projectName) {
    payload.projectName = projectName
  }
  if (baseProject?.directoryId) {
    payload.directoryId = baseProject.directoryId
  }
  if (baseProject?.status) {
    payload.status = baseProject.status
  }
  if (indexImg !== undefined) {
    payload.indexImg = indexImg
  }
  return payload
}
