<template>
  <div class="nexus-sidebar">
    <!-- Logo 区域 -->
    <div class="nexus-logo">
      <div class="logo-icon">
        <TheLogo />
      </div>
      <span class="logo-text">{{ systemName }}</span>
    </div>

    <!-- 菜单区域 -->
    <nav class="nexus-nav">
      <template v-for="item in menuItems" :key="item.key">
        <!-- 有子项：折叠菜单 -->
        <div v-if="item.children && item.children.length" class="nav-collapsible">
          <button
            class="nav-collapsible-btn"
            :class="{ active: isCollapsibleActive(item), expanded: isExpanded(item) }"
            @click="toggleExpand(item)"
          >
            <div class="nav-btn-content">
              <IconRenderer
                v-if="item.icon"
                :icon="item.icon"
                :size="19"
                class="nav-icon"
              />
              <span class="nav-label">{{ item.label }}</span>
            </div>
            <i
              class="i-material-symbols:chevron-right nav-chevron"
              :class="{ expanded: isExpanded(item) }"
            />
          </button>
          <transition name="nav-collapse">
            <div v-if="isExpanded(item)" class="nav-children">
              <div class="nav-children-line" />
              <template v-for="child in item.children" :key="child.key">
                <div v-if="child.children && child.children.length" class="nav-collapsible">
                  <button
                    class="nav-collapsible-btn sub"
                    :class="{ active: isCollapsibleActive(child), expanded: isExpanded(child) }"
                    @click="toggleExpand(child)"
                  >
                    <div class="nav-btn-content">
                      <span class="nav-sub-dot" :class="{ active: isCollapsibleActive(child) }" />
                      <span class="nav-sub-label">{{ child.label }}</span>
                    </div>
                    <i
                      class="i-material-symbols:chevron-right nav-chevron"
                      :class="{ expanded: isExpanded(child) }"
                    />
                  </button>
                  <transition name="nav-collapse">
                    <div v-if="isExpanded(child)" class="nav-children">
                      <div class="nav-children-line" />
                      <button
                        v-for="grandchild in child.children"
                        :key="grandchild.key"
                        class="nav-sub-btn"
                        :class="{ active: isActive(grandchild) }"
                        @click="handleNavClick(grandchild)"
                      >
                        <span class="nav-sub-dot" :class="{ active: isActive(grandchild) }" />
                        <span class="nav-sub-label">{{ grandchild.label }}</span>
                      </button>
                    </div>
                  </transition>
                </div>
                <button
                  v-else
                  class="nav-sub-btn"
                  :class="{ active: isActive(child) }"
                  @click="handleNavClick(child)"
                >
                  <span class="nav-sub-dot" :class="{ active: isActive(child) }" />
                  <span class="nav-sub-label">{{ child.label }}</span>
                </button>
              </template>
            </div>
          </transition>
        </div>

        <!-- 无子项：普通菜单项 -->
        <button
          v-else
          class="nav-item"
          :class="{ active: isActive(item) }"
          @click="handleNavClick(item)"
        >
          <div v-if="isActive(item)" class="nav-item-bg" />
          <IconRenderer
            v-if="item.icon"
            :icon="item.icon"
            :size="18"
            class="nav-icon"
          />
          <span class="nav-label">{{ item.label }}</span>
        </button>
      </template>
    </nav>

    <!-- 底部工具区 -->
    <div class="nexus-user">
      <MenuCollapse />
      <n-dropdown
        :show="userDropdownVisible"
        :options="userDropdownOptions"
        placement="top-start"
        @select="handleUserSelect"
        @clickoutside="userDropdownVisible = false"
      >
        <div class="user-info" @click="userDropdownVisible = !userDropdownVisible">
          <img
            v-if="showUserAvatarImage"
            :src="userAvatar"
            alt="avatar"
            class="user-avatar-img"
            @error="handleUserAvatarError"
          >
          <div v-else class="user-avatar">
            {{ userAvatarText }}
          </div>
          <div class="user-details">
            <span class="user-name">{{ userName }}</span>
          </div>
        </div>
      </n-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import TheLogo from '@/components/common/TheLogo.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenu, useUser } from '@/composables'
import MenuCollapse from '@/layouts/components/MenuCollapse.vue'
import { usePermissionStore, useTenantStore } from '@/store'
import { getDefaultPageTitle } from '@/utils/page-title'

const tenantStore = useTenantStore()

// 优先使用租户配置的系统名称，否则使用默认值
const systemName = computed(() => {
  return tenantStore.systemName || getDefaultPageTitle()
})
const route = useRoute()
const permissionStore = usePermissionStore()
const { activeKey: currentActiveKey, handleMenuSelect: baseHandleMenuSelect } = useMenu()

const { userName, userAvatarText, userAvatar, userDropdownOptions, dropdownVisible: userDropdownVisible, handleDropdownSelect } = useUser()

const userAvatarLoadFailed = ref(false)
const expandedKeys = ref(new Set())
const showUserAvatarImage = computed(() => Boolean(userAvatar.value) && !userAvatarLoadFailed.value)

// Process menu data with recursive children support and module type filtering
const menuItems = computed(() => {
  const menus = permissionStore.menus || []

  function processItem(item) {
    const isModule = item.type === 'module'
    const children = (item.children || []).map(processItem)

    return {
      key: item.key || String(item.id),
      name: item.name || item.label || '',
      label: item.label || item.name || item.meta?.title || '',
      path: (!isModule && item.path) ? item.path : '',
      icon: item.icon || '',
      type: item.type || 'menu',
      children,
    }
  }

  return menus.map(processItem)
})

function resolveMenuKey(item) {
  const key = item?.key ?? item?.id
  return key === undefined || key === null ? '' : String(key)
}

// Check if menu item is active
function isActive(item) {
  const menuKey = resolveMenuKey(item)
  if (!menuKey)
    return false
  return menuKey === String(currentActiveKey.value || '')
}

function hasActiveDescendant(item) {
  if (!item?.children?.length)
    return false
  return item.children.some(child => isActive(child) || hasActiveDescendant(child))
}

// Check if collapsible menu has active child
function isCollapsibleActive(item) {
  return isActive(item) || hasActiveDescendant(item)
}

// Check if menu is expanded
function isExpanded(item) {
  return expandedKeys.value.has(item.key)
}

// Toggle expand/collapse
function toggleExpand(item) {
  if (expandedKeys.value.has(item.key)) {
    expandedKeys.value.delete(item.key)
  }
  else {
    expandedKeys.value.add(item.key)
  }
}

// Handle menu click - only navigate for items with a path (non-module)
function handleNavClick(item) {
  if (item.children && item.children.length) {
    toggleExpand(item)
  }
  else if (item.path) {
    baseHandleMenuSelect(item.key || item.id, item.path)
  }
}

// Auto-expand menus with active children
watch(
  () => route.path,
  () => {
    menuItems.value.forEach((item) => {
      if (item.children && isCollapsibleActive(item)) {
        expandedKeys.value.add(item.key)
      }
    })
  },
  { immediate: true },
)

watch(userAvatar, () => {
  userAvatarLoadFailed.value = false
}, { immediate: true })

function handleUserSelect(key) {
  handleDropdownSelect(key)
}

function handleUserAvatarError() {
  userAvatarLoadFailed.value = true
}
</script>

<style scoped>
.nexus-sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Logo */
.nexus-logo {
  position: relative;
  height: 54px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 14px;
  border-bottom: 1px solid color-mix(in srgb, var(--nexus-border) 72%, transparent);
  flex-shrink: 0;
  justify-content: flex-start;
  overflow: hidden;
}

.nexus-logo::after {
  content: '';
  position: absolute;
  top: -45%;
  bottom: -45%;
  left: -55%;
  width: 46%;
  pointer-events: none;
  background: linear-gradient(
    100deg,
    transparent 0%,
    rgb(255 255 255 / 0%) 12%,
    rgb(255 255 255 / 62%) 44%,
    rgb(255 255 255 / 82%) 50%,
    rgb(255 255 255 / 0%) 82%,
    transparent 100%
  );
  filter: blur(0.5px);
  opacity: 0;
  transform: translateX(-120%) skewX(-18deg);
}

.nexus-logo:hover::after {
  opacity: 1;
  animation: nexus-logo-shine 1.1s ease-in-out;
}

@keyframes nexus-logo-shine {
  0% {
    transform: translateX(-120%) skewX(-18deg);
  }

  100% {
    transform: translateX(520%) skewX(-18deg);
  }
}

.logo-icon {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--nexus-border) 80%, transparent);
}

.logo-icon :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-text {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0;
  white-space: nowrap;
  overflow: hidden;
}

/* 导航菜单 */
.nexus-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 9px 7px;
}

/* 普通菜单项 */
.nav-item {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 34px;
  padding: 6px 9px 6px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-base);
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 2px;
}

.nav-item:hover {
  background: linear-gradient(90deg, color-mix(in srgb, var(--nexus-hover-bg) 92%, white), transparent);
  border-color: color-mix(in srgb, var(--nexus-border) 78%, transparent);
  color: var(--text-primary);
}

.nav-item.active {
  color: var(--nexus-active-text);
  border-color: color-mix(in srgb, var(--nexus-active-text) 16%, transparent);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 70%);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 3px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, #60a5fa, #2dd4bf);
  z-index: 2;
}

.nav-item-bg {
  position: absolute;
  inset: 1px;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--nexus-active-bg) 92%, white), rgb(255 255 255 / 38%)),
    var(--nexus-active-bg);
  border: 1px solid color-mix(in srgb, var(--nexus-active-text) 10%, transparent);
  border-radius: 9px;
}

.nav-item .nav-icon {
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: var(--text-tertiary);
  transition:
    background var(--transition-base),
    color var(--transition-base);
}

.nav-item:hover .nav-icon {
  color: var(--text-primary);
  background: rgb(15 23 42 / 4%);
}

.nav-item.active .nav-icon :deep(svg) {
  color: var(--nexus-active-text);
}

.nav-item.active .nav-icon {
  background: #fff;
  color: var(--nexus-active-text);
  box-shadow: 0 1px 4px rgb(30 64 175 / 10%);
}

.nav-label {
  flex: 1;
  text-align: left;
  position: relative;
  z-index: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-item.active .nav-label {
  font-weight: 600;
  color: var(--nexus-active-text);
}

/* 可折叠菜单 */
.nav-collapsible {
  margin-bottom: 3px;
}

.nav-collapsible-btn {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 6px 9px 6px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.nav-collapsible-btn:hover {
  background: linear-gradient(90deg, color-mix(in srgb, var(--nexus-hover-bg) 92%, white), transparent);
  border-color: color-mix(in srgb, var(--nexus-border) 78%, transparent);
  color: var(--text-primary);
}

.nav-collapsible-btn.active {
  color: var(--nexus-active-text);
  background: linear-gradient(90deg, color-mix(in srgb, var(--nexus-active-bg) 86%, white), transparent);
  border-color: color-mix(in srgb, var(--nexus-active-text) 12%, transparent);
}

.nav-collapsible-btn.active::before {
  content: '';
  position: absolute;
  left: 3px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, #60a5fa, #2dd4bf);
}

.nav-collapsible-btn.sub {
  min-height: 30px;
  padding: 5px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
}

.nav-collapsible-btn.sub .nav-sub-label {
  flex: 1;
  text-align: left;
}

.nav-collapsible-btn.sub:hover {
  background: color-mix(in srgb, var(--nexus-hover-bg) 88%, transparent);
  color: var(--text-secondary);
}

.nav-collapsible-btn.sub.active {
  background: color-mix(in srgb, var(--nexus-active-bg) 74%, white);
  border-color: color-mix(in srgb, var(--nexus-active-text) 10%, transparent);
  color: var(--nexus-active-text);
}

.nav-btn-content {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.nav-chevron {
  font-size: 15px;
  color: var(--text-tertiary);
  transition: transform var(--transition-base);
  flex-shrink: 0;
}

.nav-chevron.expanded {
  transform: rotate(90deg);
}

/* 子菜单 */
.nav-children {
  position: relative;
  margin: 2px 0 4px 16px;
  padding: 3px 0 3px 12px;
}

.nav-children-line {
  position: absolute;
  left: 0;
  top: 5px;
  bottom: 5px;
  width: 1px;
  background: linear-gradient(
    180deg,
    transparent,
    color-mix(in srgb, var(--nexus-active-text) 20%, var(--border-light)),
    transparent
  );
}

.nav-sub-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  min-height: 29px;
  padding: 5px 8px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 500;
}

.nav-sub-btn:hover {
  background: color-mix(in srgb, var(--nexus-hover-bg) 88%, transparent);
  color: var(--text-secondary);
}

.nav-sub-btn.active {
  background: color-mix(in srgb, var(--nexus-active-bg) 76%, white);
  border-color: color-mix(in srgb, var(--nexus-active-text) 10%, transparent);
  color: var(--nexus-active-text);
  font-weight: 600;
}

.nav-sub-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--border-default) 84%, transparent);
  background: var(--bg-primary);
  flex-shrink: 0;
  transition: all var(--transition-base);
}

.nav-sub-dot.active {
  border-color: transparent;
  background: linear-gradient(135deg, #60a5fa, #2dd4bf);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--nexus-active-text) 10%, transparent);
  transform: scale(1.05);
}

.nav-sub-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 折叠动画 */
.nav-collapse-enter-active,
.nav-collapse-leave-active {
  transition: all var(--transition-base);
  overflow: hidden;
}

.nav-collapse-enter-from,
.nav-collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

.nav-collapse-enter-to,
.nav-collapse-leave-from {
  opacity: 1;
  max-height: 200px;
}

/* 底部工具区 */
.nexus-user {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 10px;
  border-top: 1px solid var(--border-light);
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: background var(--transition-base);
  flex: 1;
  min-width: 0;
}

.user-info:hover {
  background: var(--nexus-hover-bg);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--primary-500);
  color: white;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(22, 93, 255, 0.2);
  flex-shrink: 0;
}

.user-avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 滚动条 */
.nexus-nav::-webkit-scrollbar {
  width: 4px;
}

.nexus-nav::-webkit-scrollbar-track {
  background: transparent;
}

.nexus-nav::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 2px;
}

.nexus-nav::-webkit-scrollbar-thumb:hover {
  background: var(--border-default);
}

/* ═══════════════════════════════════════
 * 深色模式适配
 * ═══════════════════════════════════════ */
:global(.dark) .nexus-logo {
  border-bottom-color: var(--nexus-border);
}

:global(.dark) .logo-text {
  color: var(--text-primary);
}

:global(.dark) .nexus-user {
  border-top-color: var(--nexus-border);
}

:global(.dark) .nexus-nav {
  background: transparent;
}

:global(.dark) .nav-item {
  color: #e2e8f0;
}

:global(.dark) .nav-item:hover {
  color: #f1f5f9;
  background: color-mix(in srgb, var(--nexus-hover-bg) 86%, transparent);
}

:global(.dark) .nav-item.active .nav-label {
  color: var(--nexus-active-text);
}

:global(.dark) .nav-item.active .nav-icon :deep(svg) {
  color: var(--primary-500);
}

:global(.dark) .nav-item .nav-icon {
  color: #94a3b8;
}

:global(.dark) .nav-item.active .nav-icon {
  color: var(--nexus-active-text);
  background: color-mix(in srgb, var(--nexus-active-bg) 72%, #0b1120);
  box-shadow: none;
}

:global(.dark) .nav-collapsible-btn {
  color: #e2e8f0;
}

:global(.dark) .nav-collapsible-btn:hover {
  color: #f1f5f9;
  background: color-mix(in srgb, var(--nexus-hover-bg) 86%, transparent);
}

:global(.dark) .nav-collapsible-btn.active {
  color: var(--nexus-active-text);
  background: color-mix(in srgb, var(--nexus-active-bg) 74%, transparent);
  border-color: color-mix(in srgb, var(--nexus-active-text) 18%, transparent);
}

:global(.dark) .nav-btn-content {
  color: #e2e8f0;
}

:global(.dark) .nav-chevron {
  color: #64748b;
}

:global(.dark) .nav-children-line {
  background: var(--nexus-border);
}

:global(.dark) .nav-sub-btn {
  color: #64748b;
}

:global(.dark) .nav-sub-btn:hover {
  color: #cbd5e1;
  background: color-mix(in srgb, var(--nexus-hover-bg) 86%, transparent);
}

:global(.dark) .nav-sub-btn.active {
  color: var(--nexus-active-text);
  background: color-mix(in srgb, var(--nexus-active-bg) 76%, transparent);
  border-color: color-mix(in srgb, var(--nexus-active-text) 16%, transparent);
}

:global(.dark) .nav-sub-dot {
  background: #475569;
}

:global(.dark) .nav-sub-dot.active {
  background: var(--primary-500);
}

:global(.dark) .nav-sub-label {
  color: #94a3b8;
}

:global(.dark) .user-name {
  color: var(--text-primary);
}

:global(.dark) .user-settings {
  color: #64748b;
}

:global(.dark) .user-settings:hover {
  color: var(--primary-500);
  background: var(--nexus-hover-bg);
}
</style>
