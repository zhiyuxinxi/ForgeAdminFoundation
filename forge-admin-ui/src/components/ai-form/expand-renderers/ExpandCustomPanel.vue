<template>
  <div class="expand-custom-panel">
    <slot
      :name="slotName"
      :panel="panel"
      :row="row"
      :data="data"
      :context="context"
    >
      <n-empty size="small" :description="localizeLabel('未配置自定义展开内容')" />
    </slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'

import { localizeLabel } from '@/locales'

const props = defineProps({
  panel: { type: Object, required: true },
  row: { type: Object, default: () => ({}) },
  data: { type: null, default: null },
  context: { type: Object, default: () => ({}) },
})

const slotName = computed(() => props.panel.slot || props.panel.slotName || `expand-${props.panel.key}`)
</script>

<style scoped>
.expand-custom-panel {
  min-width: 0;
}
</style>
