import { uiText } from '@/i18n/display'
import { fetchFileAccessUrlApi } from '@/api/file'
import { StorageEnum } from '@/enums/storageEnum'
import { getLocalStorage, setLocalStorage } from '@/utils/storage'

interface FileUrlCacheItem {
  url: string
  expiresAt: number
}

type FileUrlCacheStore = Record<string, FileUrlCacheItem>

const memoryCache = new Map<string, FileUrlCacheItem>()

const isDirectFilePath = (value: string) => {
  return value.startsWith('http://')
    || value.startsWith('https://')
    || value.startsWith('data:')
    || value.startsWith('blob:')
    || value.startsWith('/api/file/')
    || value.startsWith('/forge-report-api/api/file/')
}

const getCacheStore = (): FileUrlCacheStore => {
  return getLocalStorage(StorageEnum.GO_FILE_URL_CACHE) || {}
}

const setCacheStore = (store: FileUrlCacheStore) => {
  setLocalStorage(StorageEnum.GO_FILE_URL_CACHE, store)
}

const isExpired = (item?: FileUrlCacheItem | null) => {
  return !item || !item.url || item.expiresAt <= Date.now()
}

const cleanupCacheStore = (store: FileUrlCacheStore) => {
  const nextStore: FileUrlCacheStore = {}
  Object.keys(store).forEach((key) => {
    const item = store[key]
    if (!isExpired(item)) {
      nextStore[key] = item
    }
  })
  return nextStore
}

export const getCachedFileAccessUrl = (fileId: string) => {
  const memoryItem = memoryCache.get(fileId)
  if (!isExpired(memoryItem)) {
    return memoryItem?.url || ''
  }

  const store = cleanupCacheStore(getCacheStore())
  const cachedItem = store[fileId]
  setCacheStore(store)
  if (isExpired(cachedItem)) {
    return ''
  }

  memoryCache.set(fileId, cachedItem)
  return cachedItem.url
}

export const removeCachedFileAccessUrl = (fileId: string) => {
  memoryCache.delete(fileId)
  const store = cleanupCacheStore(getCacheStore())
  delete store[fileId]
  setCacheStore(store)
}

export const resolveFileAccessUrl = async (fileId: string, expires = 43200, forceRefresh = false) => {
  if (!fileId) {
    return ''
  }

  if (isDirectFilePath(fileId)) {
    return fileId
  }

  if (forceRefresh) {
    removeCachedFileAccessUrl(fileId)
  }

  const cached = getCachedFileAccessUrl(fileId)
  if (cached) {
    return cached
  }

  const response = await fetchFileAccessUrlApi(fileId, expires)
  const url = response?.data || ''
  if (!url) {
    throw new Error(uiText("素材地址获取失败"))
  }

  const cacheItem: FileUrlCacheItem = {
    url,
    expiresAt: Date.now() + Math.max(expires - 60, 60) * 1000
  }

  memoryCache.set(fileId, cacheItem)
  const store = cleanupCacheStore(getCacheStore())
  store[fileId] = cacheItem
  setCacheStore(store)

  return url
}
