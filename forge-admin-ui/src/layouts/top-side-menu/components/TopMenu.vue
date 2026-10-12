<template>
  <TopMenuBar
    class="top-menu"
    :items="topMenuOptions"
    :active-key="activeKey"
    @select="handleMenuSelect"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { findFirstMenuWithPath, findTopMenuByPath, useMenu } from '@/composables'
import TopMenuBar from '@/layouts/components/TopMenuBar.vue'
import { useAppStore, usePermissionStore } from '@/store'
import { processTopMenus } from '@/utils/menu-utils'

const route = useRoute()
const permissionStore = usePermissionStore()
const appStore = useAppStore()

const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

// Extract first-level menu items
const topMenuOptions = computed(() => {
  const menus = permissionStore.menus || []
  const topMenus = processTopMenus(menus)

  return topMenus.map(item => ({
    ...item,
    key: item.id,
    label: item.label || item.name,
    children: null,
  }))
})

// Active menu key
const activeKey = computed(() => {
  const menus = permissionStore.menus || []

  if (!menus.length || !permissionStore.menuDataLoaded) {
    return null
  }

  const topMenus = processTopMenus(menus)
  const activeTopMenu = findTopMenuByPath(topMenus, route.path)

  if (activeTopMenu) {
    if (appStore.selectedTopMenuId !== activeTopMenu.id) {
      appStore.setSelectedTopMenuId(activeTopMenu.id)
    }
    return activeTopMenu.id
  }

  if (appStore.selectedTopMenuId) {
    return appStore.selectedTopMenuId
  }

  return null
})

function handleMenuSelect(item) {
  const key = item.key
  const menus = permissionStore.menus || []
  const topMenus = processTopMenus(menus)

  const selectedMenu = topMenus.find(m => m.id === key || String(m.id) === String(key))

  if (!selectedMenu)
    return

  appStore.setSelectedTopMenuId(key)

  if (selectedMenu.type === 'module') {
    const firstMenu = findFirstMenuWithPath(selectedMenu)
    if (firstMenu && firstMenu.path) {
      baseHandleMenuSelect(firstMenu.key || firstMenu.id, firstMenu.path)
    }
    return
  }

  if (selectedMenu.path) {
    baseHandleMenuSelect(selectedMenu.key || selectedMenu.id, selectedMenu.path)
  }
}
</script>
