<template>
  <nav class="business-top-nav" :aria-label="localizeLabel('业务入口导航')">
    <NButton
      class="business-nav-button"
      :class="{ active: activeKey === 'appCenter' }"
      secondary
      strong
      @click="go('/app-center')"
    >
      <template #icon>
        <i class="i-material-symbols:dashboard-customize-outline" />
      </template>
      {{ localizeLabel('应用中心') }}
    </NButton>

    <NButton
      v-if="showCapabilityCenter"
      class="business-nav-button"
      :class="{ active: activeKey === 'capability' }"
      secondary
      strong
      @click="go('/app-center/engines')"
    >
      <template #icon>
        <i class="i-material-symbols:hub-outline" />
      </template>
      {{ localizeLabel('能力中心') }}
    </NButton>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

import { useRoute, useRouter } from 'vue-router'
import { localizeLabel } from '@/locales'
import { useUserStore } from '@/store'

const props = defineProps({
  active: {
    type: String,
    default: '',
  },
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeKey = computed(() => {
  if (props.active)
    return props.active
  if (route.path.startsWith('/app-center/engines'))
    return 'capability'
  return 'appCenter'
})

const roleKeys = computed(() => {
  const roles = userStore.roles || []
  return Array.isArray(roles) ? roles : Array.from(roles)
})

const showCapabilityCenter = computed(() => {
  return userStore.isAdmin || roleKeys.value.includes('superadmin')
})

function go(path) {
  if (route.path !== path)
    router.push(path)
}
</script>

<style scoped>
.business-top-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.business-nav-button {
  --nav-active-bg: #e8f1ff;
  --nav-active-border: #2563eb;
  border-radius: 8px;
  border: 1px solid transparent;
  color: #344256;
}

.business-nav-button :deep(.n-button__content) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.business-nav-button.active {
  border-color: var(--nav-active-border);
  background: var(--nav-active-bg);
  color: #1d4ed8;
}
</style>
