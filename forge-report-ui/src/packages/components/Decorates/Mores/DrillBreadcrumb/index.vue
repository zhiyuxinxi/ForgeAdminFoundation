<template>
  <nav class="drill-breadcrumb" :style="rootStyle">
    <button
      v-for="(item, index) in items"
      :key="`${item.label}-${index}`"
      :class="{ active: index === items.length - 1 }"
      @click="go(item)"
    >
      <span>{{ item.label }}</span>
      <i v-if="index < items.length - 1">/</i>
    </button>
  </nav>
</template>

<script setup lang="ts">
import { computed, inject, PropType, ref, unref } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { switchPreviewPage } from '@/views/preview/utils/storage'
import { PREVIEW_PAGE_CONTEXT_KEY } from '@/utils/requestDynamicParams'
import type { option as defaultOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const pageContext = inject(PREVIEW_PAGE_CONTEXT_KEY, ref({}))
const option = computed(() => props.chartConfig.option)
const items = computed(() => {
  const context = unref(pageContext) as any
  return option.value.useContextBreadcrumbs && Array.isArray(context?.breadcrumbs) && context.breadcrumbs.length
    ? context.breadcrumbs
    : option.value.items
})
const rootStyle = computed(() => ({
  '--bc-accent': option.value.style.accentColor,
  '--bc-text': option.value.style.textColor,
  '--bc-muted': option.value.style.mutedColor,
  '--bc-panel': option.value.style.panelColor,
  '--bc-border': option.value.style.borderColor
}))

const go = (item: any) => {
  if (!item.pageId) return
  switchPreviewPage(item.pageId, item.context || {})
}
</script>

<style scoped lang="scss">
.drill-breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  height: 100%;
  padding: 0 14px;
  overflow: hidden;
  border: 1px solid var(--bc-border);
  border-radius: 8px;
  background: var(--bc-panel);
}

button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 180px;
  height: 34px;
  padding: 0 4px;
  color: var(--bc-muted);
  border: 0;
  cursor: pointer;
  background: transparent;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  i {
    color: rgba(255, 255, 255, 0.3);
    font-style: normal;
  }
}

button.active {
  color: var(--bc-accent);
  cursor: default;
}
</style>
