<template>
  <SharedSideMenu
    class="side-menu"
    :options="sideMenuOptions"
    :active-key-override="activeKey"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { findTopMenuByPath } from '@/composables'
import SharedSideMenu from '@/layouts/components/SideMenu.vue'
import { useAppStore, usePermissionStore } from '@/store'
import { findMenuIdByPath, processMenuData, processTopMenus } from '@/utils/menu-utils'

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

function isDirectoryMenu(menu) {
  return menu?.type === 'module' && Array.isArray(menu.children) && menu.children.length > 0
}

// Get side menu options based on selected top menu
const sideMenuOptions = computed(() => {
  const menus = permissionStore.menus || []

  if (!menus.length || !permissionStore.menuDataLoaded) {
    return []
  }

  const topMenus = processTopMenus(menus)
  let activeTopMenu = findTopMenuByPath(topMenus, route.path)

  // Fallback: path prefix match
  if (!activeTopMenu) {
    const pathSegments = route.path.split('/').filter(Boolean)
    if (pathSegments.length > 0) {
      const firstSegment = `/${pathSegments[0]}`
      activeTopMenu = topMenus.find((menu) => {
        if (menu.path && menu.path.startsWith(firstSegment)) {
          return true
        }
        if (menu.children && menu.children.length > 0) {
          return menu.children.some(child =>
            child.path && child.path.startsWith(firstSegment),
          )
        }
        return false
      })
    }
  }

  // Fallback: use store selected ID
  if (!activeTopMenu && appStore.selectedTopMenuId) {
    activeTopMenu = topMenus.find(item => item.id === appStore.selectedTopMenuId)
  }

  if (activeTopMenu && appStore.selectedTopMenuId !== activeTopMenu.id) {
    appStore.setSelectedTopMenuId(activeTopMenu.id)
  }

  if (isDirectoryMenu(activeTopMenu)) {
    return processMenuData(activeTopMenu.children)
  }
  return []
})

// Active key computed - override base to use sideMenuOptions
const activeKey = computed(() => {
  if (route.meta?.parentKey) {
    return route.meta.parentKey
  }

  let menuId = findMenuIdByPath(sideMenuOptions.value, route.path)

  if (!menuId) {
    const pathSegments = route.path.split('/').filter(Boolean)
    for (let i = pathSegments.length - 1; i > 0; i--) {
      const parentPath = `/${pathSegments.slice(0, i).join('/')}`
      menuId = findMenuIdByPath(sideMenuOptions.value, parentPath)
      if (menuId)
        break
    }
  }

  return menuId || route.path
})
</script>
