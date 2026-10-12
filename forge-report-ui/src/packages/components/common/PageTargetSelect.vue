<template>
  <n-select
    :value="value"
    size="small"
    filterable
    clearable
    :placeholder="placeholder"
    :options="options"
    @update:value="emit('update:value', $event)"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'

const props = withDefaults(defineProps<{
  value?: string
  pageType?: 'page' | 'modal' | 'all'
  placeholder?: string
}>(), {
  pageType: 'all',
  placeholder: '选择项目页面'
})

const emit = defineEmits<{
  (event: 'update:value', value: string | null): void
}>()

const chartEditStore = useChartEditStore()
const options = computed(() =>
  chartEditStore.getProjectPages
    .filter(page => {
      if (props.pageType === 'all') return true
      if (props.pageType === 'modal') return page.pageType === 'modal'
      return page.pageType !== 'modal'
    })
    .map(page => ({
      label: `${page.name}${page.id === chartEditStore.getHomePageId ? '（首页）' : ''}${page.pageType === 'modal' ? '（弹窗）' : ''}`,
      value: page.id
    }))
)
</script>
