<template>
  <div :style="getStyle(borderRadius)">
    <fg-auth-image
      :src="option.dataset"
      :fallback="requireErrorImg()"
      :img-style="{ width: `${w}px`, height: `${h}px`, objectFit: fit }"
    />
  </div>
</template>

<script setup lang="ts">
import { PropType, shallowReactive, watch, toRefs } from 'vue'
import { requireErrorImg } from '@/utils'
import { useChartDataFetch } from '@/hooks'
import { CreateComponentType } from '@/packages/index.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import FgAuthImage from '@/components/FgAuthImage/index.vue'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType>,
    required: true
  }
})

const { w, h } = toRefs(props.chartConfig.attr)
const { dataset, fit, borderRadius } = toRefs(props.chartConfig.option)

const option = shallowReactive({
  dataset: ''
})

const getStyle = (radius: number) => {
  return {
    borderRadius: `${radius}px`,
    overflow: 'hidden'
  }
}

// 编辑更新
watch(
  () => props.chartConfig.option.dataset,
  (newData: any) => {
    option.dataset = newData
  },
  {
    immediate: true
  }
)

// 预览更新
useChartDataFetch(props.chartConfig, useChartEditStore, (newData: any) => {
  option.dataset = newData
})
</script>
