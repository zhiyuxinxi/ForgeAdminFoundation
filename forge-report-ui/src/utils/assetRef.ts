import { resolveFileAccessUrl } from '@/utils/fileAccess'

export const FILE_ASSET_REF_PREFIX = 'forge-file://'

export const createFileAssetRef = (fileId?: string) => {
  if (!fileId) {
    return ''
  }
  return `${FILE_ASSET_REF_PREFIX}${fileId}`
}

export const isFileAssetRef = (value?: string) => {
  return !!value && value.startsWith(FILE_ASSET_REF_PREFIX)
}

export const parseFileAssetRef = (value?: string) => {
  if (!value) {
    return ''
  }
  return isFileAssetRef(value) ? value.slice(FILE_ASSET_REF_PREFIX.length) : value
}

export const resolveAssetSourceUrl = async (value?: string, expires = 43200) => {
  if (!value) {
    return ''
  }
  return resolveFileAccessUrl(parseFileAssetRef(value), expires)
}
