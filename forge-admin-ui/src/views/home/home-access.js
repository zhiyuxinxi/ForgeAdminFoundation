function normalizeRoutePath(path) {
  const value = String(path || '').trim()
  if (!value)
    return ''
  const [pathWithoutHash] = value.split('#')
  const [pathname] = pathWithoutHash.split('?')
  const normalized = String(pathname || '').replace(/\/+$/, '')
  if (!normalized)
    return '/'
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

export function hasAccessibleHomeRoute(accessRoutes = [], targetPath) {
  const normalizedTarget = normalizeRoutePath(targetPath)
  if (!normalizedTarget || !Array.isArray(accessRoutes))
    return false
  return accessRoutes.some(route => normalizeRoutePath(route?.path) === normalizedTarget)
}

export function filterAccessibleHomeItems(items = [], accessRoutes = []) {
  return items.filter(item => hasAccessibleHomeRoute(accessRoutes, item?.path))
}

export function resolveHomeAdminStatsAccess(accessRoutes = []) {
  return {
    online: hasAccessibleHomeRoute(accessRoutes, '/system/online'),
    users: hasAccessibleHomeRoute(accessRoutes, '/system/user'),
    loginLogs: hasAccessibleHomeRoute(accessRoutes, '/system/login-log'),
  }
}

function hasApiPermission(apiPermissions = [], permission, isAdmin = false) {
  if (isAdmin)
    return true
  const permissions = Array.isArray(apiPermissions) ? apiPermissions : []
  return permissions.includes(permission)
    || permissions.includes('**')
    || permissions.includes('*')
    || permissions.includes('*:*:*')
}

export function resolveHomeFlowApiAccess(apiPermissions = [], isAdmin = false) {
  return {
    todo: hasApiPermission(apiPermissions, 'flow:task:todo', isAdmin),
    done: hasApiPermission(apiPermissions, 'flow:task:done', isAdmin),
    started: hasApiPermission(apiPermissions, 'flow:task:started', isAdmin),
  }
}

export function canLoadHomeWorkbench(accessRoutes = []) {
  return hasAccessibleHomeRoute(accessRoutes, '/app-center')
}

export async function loadAccessibleHomeWorkbench(accessRoutes = [], loader) {
  if (!canLoadHomeWorkbench(accessRoutes))
    return []

  const response = await loader()
  return response?.data || []
}
