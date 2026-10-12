import pagePathes from 'isme:page-pathes'
import { manualRoutes } from '@/router'

const VIEW_PREFIX = '/src/views/'
const VIEW_SUFFIX = '.vue'

const ignorePaths = new Set([
  '/',
  '/404',
  '/403',
  '/login',
  '/login/callback',
])

function trimSlash(value) {
  return String(value || '').replace(/^\/+|\/+$/g, '')
}

function normalizeRoutePath(path) {
  const normalized = String(path || '').replace(/\/+/g, '/')
  if (!normalized || normalized === '/')
    return ''
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

function componentFromViewPath(viewPath) {
  if (!viewPath || !viewPath.startsWith(VIEW_PREFIX) || !viewPath.endsWith(VIEW_SUFFIX))
    return ''
  return viewPath.slice(VIEW_PREFIX.length, -VIEW_SUFFIX.length)
}

function pagePathFromComponent(component) {
  const normalized = trimSlash(component)
  if (!normalized)
    return ''
  const pagePath = normalized.endsWith('/index')
    ? normalized.slice(0, -'/index'.length)
    : normalized
  return `/${pagePath}`
}

function makeRouteOption(component, source = 'local', path) {
  const routePath = normalizeRoutePath(path || pagePathFromComponent(component))
  if (!routePath || ignorePaths.has(routePath))
    return null
  return {
    label: routePath,
    value: routePath,
    path: routePath,
    component,
    group: routePath.split('/')[1] || 'root',
    source,
  }
}

function flattenManualRoutes(routes) {
  const result = []

  function visit(route, parentPath = '') {
    const routePath = normalizeRoutePath(
      route.path?.startsWith('/')
        ? route.path
        : `${parentPath}/${route.path || ''}`,
    )
    const component = routePath ? trimSlash(routePath) : ''
    const option = makeRouteOption(component, 'manual', routePath)
    if (option)
      result.push(option)
    ;(route.children || []).forEach(child => visit(child, routePath))
  }

  routes.forEach(route => visit(route))
  return result
}

export function getMenuRouteOptions() {
  const options = [
    ...pagePathes
      .map(componentFromViewPath)
      .map(component => makeRouteOption(component, 'local'))
      .filter(Boolean),
    ...flattenManualRoutes(manualRoutes),
  ]

  const optionMap = new Map()
  options.forEach((option) => {
    if (!optionMap.has(option.path)) {
      optionMap.set(option.path, option)
      return
    }
    const current = optionMap.get(option.path)
    if (!current.component && option.component)
      optionMap.set(option.path, option)
  })

  return Array.from(optionMap.values()).sort((a, b) => a.path.localeCompare(b.path))
}
