import { defaultThemeConfig } from '@/config/theme.config'
import { useTenantStore } from '@/store'
import { resolveRenderableFileUrl } from '@/utils/file'
import { normalizePageTitle, setDocumentTitle } from '@/utils/page-title'

const MANAGED_FILE_ID_PATTERN = /^[\w-]{8,128}$/

function getRequestPrefix() {
  return import.meta.env.VITE_REQUEST_PREFIX || ''
}

function normalizeTenantId(value) {
  if (Array.isArray(value)) {
    const first = value.find(item => item !== null && item !== undefined && item !== '')
    return normalizeTenantId(first)
  }
  if (value === null || value === undefined || value === '')
    return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

function isDirectRenderableReference(value = '') {
  const text = String(value || '').trim().toLowerCase()
  return text.startsWith('http://')
    || text.startsWith('https://')
    || text.startsWith('data:')
    || text.startsWith('blob:')
}

function isManagedFileReference(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return false
  if (text.includes('/api/file/download/') || text.includes('/api/file/url/'))
    return true
  return !text.startsWith('/') && !text.includes('/') && !text.includes('\\') && MANAGED_FILE_ID_PATTERN.test(text)
}

function resolveAssetReference(tenantConfig, assetType) {
  if (!tenantConfig)
    return ''
  if (assetType === 'logo')
    return tenantConfig.systemLogo || ''
  return tenantConfig.browserIcon || ''
}

export function resolveTenantPublicAssetUrl(tenantConfig, assetType = 'logo') {
  const assetReference = String(resolveAssetReference(tenantConfig, assetType) || '').trim()
  if (!assetReference)
    return ''
  if (isDirectRenderableReference(assetReference))
    return assetReference
  if (assetReference.startsWith('/') && !isManagedFileReference(assetReference))
    return assetReference

  const tenantId = normalizeTenantId(tenantConfig?.tenantId ?? tenantConfig?.id)
  if (!tenantId || !isManagedFileReference(assetReference))
    return ''

  const normalizedAssetType = assetType === 'favicon' ? 'icon' : assetType
  return `${getRequestPrefix()}/auth/tenant/assets/${tenantId}/${normalizedAssetType}`
}

function parseThemeConfig(tenantConfig, tenantStore) {
  const rawThemeConfig = tenantConfig?.themeConfig
  if (rawThemeConfig) {
    try {
      return typeof rawThemeConfig === 'string' ? JSON.parse(rawThemeConfig) : rawThemeConfig
    }
    catch (error) {
      console.error('解析主题配置失败:', error)
    }
  }
  return tenantStore.themeConfig
}

export async function applyTenantConfig(tenantConfig, appStore) {
  const tenantStore = useTenantStore()

  appStore.resetAccountState()
  if (!tenantConfig)
    return

  if (tenantConfig.systemLayout) {
    appStore.setLayout(tenantConfig.systemLayout)
  }

  const themeConfigObj = parseThemeConfig(tenantConfig, tenantStore)
  if (themeConfigObj) {
    const primaryColor = tenantConfig.systemTheme || themeConfigObj.primaryColor || defaultThemeConfig.primaryColor
    appStore.setThemeConfig({
      primaryColor,
      header: {
        ...defaultThemeConfig.header,
        ...themeConfigObj.header,
      },
      headerDark: {
        ...defaultThemeConfig.headerDark,
        ...themeConfigObj.headerDark,
      },
      topMenu: {
        ...defaultThemeConfig.topMenu,
        ...themeConfigObj.topMenu,
      },
      topMenuDark: {
        ...defaultThemeConfig.topMenuDark,
        ...themeConfigObj.topMenuDark,
      },
      sideMenu: {
        ...defaultThemeConfig.sideMenu,
        ...themeConfigObj.sideMenu,
      },
      sideMenuDark: {
        ...defaultThemeConfig.sideMenuDark,
        ...themeConfigObj.sideMenuDark,
      },
    })
  }
  else if (tenantConfig.systemTheme) {
    appStore.setPrimaryColor(tenantConfig.systemTheme)
    appStore.setThemeColor(tenantConfig.systemTheme)
  }

  const pageBaseTitle = normalizePageTitle(tenantConfig.browserTitle) || normalizePageTitle(tenantConfig.systemName)
  if (pageBaseTitle) {
    setDocumentTitle(pageBaseTitle)
  }

  if (tenantConfig.browserIcon) {
    const link = document.querySelector('link[rel*=\'icon\']') || document.createElement('link')
    link.type = 'image/x-icon'
    link.rel = 'shortcut icon'
    try {
      let iconUrl = resolveTenantPublicAssetUrl(tenantConfig, 'icon')
      if (!iconUrl)
        iconUrl = await resolveRenderableFileUrl(tenantConfig.browserIcon)
      link.href = iconUrl || tenantConfig.browserIcon
    }
    catch {
      link.href = resolveTenantPublicAssetUrl(tenantConfig, 'icon') || tenantConfig.browserIcon
    }
    document.getElementsByTagName('head')[0].appendChild(link)
  }
}
