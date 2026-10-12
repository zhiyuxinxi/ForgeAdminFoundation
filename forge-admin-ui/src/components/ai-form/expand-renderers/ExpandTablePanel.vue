<template>
  <div class="expand-table-panel">
    <AiTable
      :columns="columns"
      :data-source="rows"
      :row-key="tableConfig.rowKey || 'id'"
      :pagination="tableConfig.pagination"
      :loading="loading"
      :hide-selection="tableConfig.hideSelection !== false"
      :bordered="tableConfig.bordered"
      :striped="tableConfig.striped"
      :size="tableConfig.size || 'small'"
      :max-height="tableConfig.maxHeight"
      :scroll-x="tableConfig.scrollX"
      :show-toolbar="tableConfig.showToolbar === true"
      :show-render-mode-switch="false"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AiTable from '../AiTable.vue'

const props = defineProps({
  panel: { type: Object, required: true },
  data: { type: [Array, Object], default: () => [] },
  loading: { type: Boolean, default: false },
})

const tableConfig = computed(() => props.panel.table || {})
const columns = computed(() => tableConfig.value.columns || [])
const rows = computed(() => {
  if (Array.isArray(props.data))
    return props.data
  if (Array.isArray(props.data?.records))
    return props.data.records
  if (Array.isArray(props.data?.rows))
    return props.data.rows
  if (Array.isArray(props.data?.list))
    return props.data.list
  return []
})
</script>

<style scoped>
.expand-table-panel {
  min-width: 0;
}
</style>
