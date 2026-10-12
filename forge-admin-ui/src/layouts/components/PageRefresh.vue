<template>
  <button
    type="button"
    class="page-refresh-button"
    :title="localizeLabel('刷新当前页面')"
    :aria-label="localizeLabel('刷新当前页面')"
    @click="handleRefresh"
  >
    <i class="i-material-symbols:refresh-rounded" />
  </button>
</template>

<script setup>
import { useRoute } from 'vue-router'

import { localizeLabel } from '@/locales'
import { useTabStore } from '@/store'

const route = useRoute()
const tabStore = useTabStore()

function handleRefresh() {
  tabStore.reloadTab(route.fullPath, route.meta?.keepAlive)
}
</script>

<style scoped>
.page-refresh-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.page-refresh-button:hover {
  background: color-mix(in srgb, currentColor 10%, transparent);
  color: var(--top-menu-text-color-hover, inherit);
}

.page-refresh-button i {
  font-size: 20px;
}
</style>
