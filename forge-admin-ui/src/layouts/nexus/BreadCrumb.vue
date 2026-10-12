<template>
  <n-breadcrumb separator="/">
    <!-- 面包屑匹配到菜单：显示父级链 -->
    <template v-if="breadItems?.length">
      <n-breadcrumb-item
        v-for="item of breadItems"
        :key="item.code"
        :clickable="!!item.path"
        @click="handleItemClick(item)"
      >
        <n-dropdown
          v-if="item.children && item.children.length > 0"
          :options="getDropdownOptions(item.children)"
          @select="handleDropdownSelect"
        >
          <div class="nexus-breadcrumb-entry">
            <IconRenderer
              v-if="item.icon"
              :icon="item.icon"
              :size="16"
            />
            <span class="nexus-breadcrumb-label">{{ localizeLabel(item.label, item.labelI18n) }}</span>
          </div>
        </n-dropdown>
        <div v-else class="nexus-breadcrumb-entry">
          <span class="nexus-breadcrumb-label">{{ localizeLabel(item.label, item.labelI18n) }}</span>
        </div>
      </n-breadcrumb-item>
    </template>

    <!-- 未匹配到菜单：直接显示页面标题 -->
    <template v-else>
      <n-breadcrumb-item v-if="route.meta.title" :clickable="false">
        {{ localizeLabel(route.meta.title) }}
      </n-breadcrumb-item>
    </template>
  </n-breadcrumb>
</template>

<script setup>
import { h, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenu } from '@/composables'
import { localizeLabel } from '@/locales'
import { usePermissionStore } from '@/store'

const route = useRoute()
const permissionStore = usePermissionStore()
const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

const breadItems = ref([])

// 根据当前路由 path 在菜单树中查找
watch(
  () => route.path,
  (path) => {
    if (!path) {
      breadItems.value = []
      return
    }
    breadItems.value = findBreadItemsByPath(permissionStore.menus, path)
  },
  { immediate: true },
)

function findBreadItemsByPath(tree, targetPath, parents = []) {
  if (!Array.isArray(tree))
    return []
  for (const item of tree) {
    const itemPath = item.path || ''
    if (itemPath === targetPath) {
      return [...parents, item]
    }
    if (item.children && item.children.length > 0) {
      const found = findBreadItemsByPath(item.children, targetPath, [...parents, item])
      if (found.length > 0)
        return found
    }
  }
  return []
}

function handleItemClick(item) {
  if (item.path) {
    // router.push(item.path)
  }
}

function getDropdownOptions(children = []) {
  if (!children || !Array.isArray(children))
    return []
  return children
    .filter(item => item.path)
    .map(child => ({
      label: localizeLabel(child.label || child.name, child.labelI18n),
      key: child.name,
      icon: child.icon ? () => h(IconRenderer, { icon: child.icon, size: 16 }) : undefined,
    }))
}

function handleDropdownSelect(key) {
  if (!key)
    return
  // 递归查找菜单项
  function findItem(tree) {
    if (!Array.isArray(tree))
      return null
    for (const item of tree) {
      if ((item.name || item.label) === String(key))
        return item
      if (item.children) {
        const found = findItem(item.children)
        if (found)
          return found
      }
    }
    return null
  }

  const item = findItem(permissionStore.menus)
  if (item?.path) {
    baseHandleMenuSelect(item.key || item.id, item.path)
  }
}
</script>
