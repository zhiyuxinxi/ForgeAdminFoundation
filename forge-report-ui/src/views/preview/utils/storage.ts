import { getSessionStorage, getLocalStorage } from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { getProjectDetailApi, getProjectVersionDetailApi } from '@/api/project'
import { normalizeProjectStorage, resolveInitialPreviewPage } from '@/utils/reportPages'
import type { ChartEditStorage, ReportPageTransition, ReportProjectStorage } from '@/store/modules/chartEditStore/chartEditStore.d'

const PAGE_ID_QUERY_KEY = 'pageId'
const MODAL_PAGE_ID_QUERY_KEY = 'modalPageId'
const VERSION_ID_QUERY_KEY = 'versionId'

export interface ChartEditStorageType extends ChartEditStorage {
  id: string
}

const formatQueryValue = (value: unknown): string => {
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return JSON.stringify(value)
}

const parseQueryValue = (value: string): unknown => {
  const trimmed = value.trim()
  if (!trimmed) return value
  if (!['{', '['].includes(trimmed[0])) return value
  try {
    return JSON.parse(trimmed)
  } catch {
    return value
  }
}

export const getPreviewHashInfo = () => {
  const [path, query = ''] = document.location.hash.split('?')
  const toPathArray = path.split('/')
  const searchParams = new URLSearchParams(query)
  const pageContext: Record<string, unknown> = {}
  searchParams.forEach((value, key) => {
    if (key === PAGE_ID_QUERY_KEY) return
    if (key === MODAL_PAGE_ID_QUERY_KEY) return
    if (key === VERSION_ID_QUERY_KEY) return
    pageContext[key] = parseQueryValue(value)
  })
  return {
    hashPath: path || '#/',
    id: (toPathArray && toPathArray[toPathArray.length - 1]) || '',
    pageId: searchParams.get(PAGE_ID_QUERY_KEY) || undefined,
    modalPageId: searchParams.get(MODAL_PAGE_ID_QUERY_KEY) || undefined,
    versionId: searchParams.get(VERSION_ID_QUERY_KEY) || undefined,
    pageContext
  }
}

const applyStorage = (storage: ReportProjectStorage, id: string, pageId?: string) => {
  const project = normalizeProjectStorage(storage)
  const initialPageId = resolveInitialPreviewPage(project, pageId)
  const chartEditStore = useChartEditStore()
  const pageStorage = chartEditStore.loadProjectStorage(project, initialPageId)
  return { ...pageStorage, id }
}

const applyPreviewRuntime = (
  context: Record<string, any>
) => {
  const chartEditStore = useChartEditStore()
  chartEditStore.setRuntimePageContext(context)
  chartEditStore.closeAllModals()
}

const updatePreviewUrl = (
  pageId: string,
  context: Record<string, any> = {},
  replace = false
) => {
  if (typeof window === 'undefined') return
  const { hashPath, versionId } = getPreviewHashInfo()
  const searchParams = new URLSearchParams()
  searchParams.set(PAGE_ID_QUERY_KEY, pageId)
  if (versionId) searchParams.set(VERSION_ID_QUERY_KEY, versionId)
  Object.entries(context || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    searchParams.set(key, formatQueryValue(value))
  })
  const nextHash = `${hashPath}?${searchParams.toString()}`
  if (document.location.hash === nextHash) return
  const nextUrl = `${document.location.pathname}${document.location.search}${nextHash}`
  window.history[replace ? 'replaceState' : 'pushState']({ pageId, pageContext: context }, '', nextUrl)
}

export const switchPreviewPage = async (
  pageId: string,
  context: Record<string, any> = {},
  transition?: ReportPageTransition
) => {
  const chartEditStore = useChartEditStore()
  chartEditStore.closeAllModals()
  chartEditStore.setRuntimePageTransition(transition || '')
  chartEditStore.setRuntimePageContext(context)
  const nextStorage = chartEditStore.switchPage(pageId)

  if (!nextStorage && chartEditStore.getHomePageId) {
    chartEditStore.switchPage(chartEditStore.getHomePageId)
  }
  const nextPageId = nextStorage ? pageId : chartEditStore.getActivePageId
  if (nextPageId) {
    updatePreviewUrl(nextPageId, context)
  }
}

export const restorePreviewPageFromUrl = async () => {
  const { pageId, modalPageId, pageContext } = getPreviewHashInfo()
  const chartEditStore = useChartEditStore()
  const targetPageId = pageId && chartEditStore.getProjectPages.some(page => page.id === pageId)
    ? pageId
    : chartEditStore.getHomePageId || chartEditStore.getActivePageId || chartEditStore.getProjectPages[0]?.id

  chartEditStore.setRuntimePageTransition('')
  chartEditStore.setRuntimePageContext(pageContext)
  chartEditStore.closeAllModals()
  if (targetPageId && targetPageId !== chartEditStore.getActivePageId) {
    chartEditStore.switchPage(targetPageId)
  }
  return { modalPageId, pageContext }
}

// 根据路由 id 获取存储数据的信息
// 优先从 sessionStorage 读取，如果没有，尝试从 localStorage 读取，最后从后端读取
export const getSessionStorageInfo = async () => {
  const { id, pageId, pageContext, versionId } = getPreviewHashInfo()

  if (versionId) {
    const res = await getProjectVersionDetailApi(versionId)
    const version = res?.data
    if (version?.componentData) {
      const parsed = JSON.parse(version.componentData)
      const storage = applyStorage(parsed, String(version.projectId || id), pageId)
      applyPreviewRuntime(pageContext)
      return storage
    }
    return null
  }

  const sessionList: Array<ChartEditStorageType | (ReportProjectStorage & { id: string })> = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST)
  if (sessionList) {
    for (let i = 0; i < sessionList.length; i++) {
      if (id.toString() === String(sessionList[i].id)) {
        const storage = applyStorage(sessionList[i], String(id), pageId)
        applyPreviewRuntime(pageContext)
        return storage
      }
    }
  }

  const localList: Array<ChartEditStorageType | (ReportProjectStorage & { id: string })> = getLocalStorage(StorageEnum.GO_CHART_STORAGE_LIST)
  if (localList) {
    for (let i = 0; i < localList.length; i++) {
      if (id.toString() === String(localList[i].id)) {
        const storage = applyStorage(localList[i], String(id), pageId)
        applyPreviewRuntime(pageContext)
        return storage
      }
    }
  }

  const res = await getProjectDetailApi(id)
  const project = res?.data
  if (project?.componentData) {
    const parsed = JSON.parse(project.componentData)
    const storage = applyStorage(parsed, String(id), pageId)
    applyPreviewRuntime(pageContext)
    return storage
  }

  return null
}
