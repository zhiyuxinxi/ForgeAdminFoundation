import { useUserStore } from '@/store/modules/userStore/userStore'

const ADMIN_WILDCARD = '*:*:*'

function hasWildcard(perms: string[]): boolean {
  return perms.includes(ADMIN_WILDCARD)
}

export function hasPermission(perm: string): boolean {
  const userStore = useUserStore()
  const perms = userStore.permissions
  if (hasWildcard(perms)) return true
  return perms.includes(perm)
}

export function hasAnyPermission(perms: string[]): boolean {
  const userStore = useUserStore()
  const all = userStore.permissions
  if (hasWildcard(all)) return true
  return perms.some(p => all.includes(p))
}

export function hasAllPermissions(perms: string[]): boolean {
  const userStore = useUserStore()
  const all = userStore.permissions
  if (hasWildcard(all)) return true
  return perms.every(p => all.includes(p))
}
