<template>
  <div>
    <n-carousel
      :autoplay="autoplay"
      :interval="interval"
      :centered-slides="centeredSlides"
      :direction="direction"
      :dot-placement="dotPlacement"
      :dot-type="dotType"
      :draggable="draggable"
      :effect="effect"
      :slides-per-view="slidesPerview"
      :show-arrow="showArrow"
      :show-dots="showDots"
    >
      <fg-auth-image
        v-for="(url, index) in option.dataset"
        :key="index"
        :src="url"
        :fallback="requireErrorImg()"
        :img-style="{ width: `${w}px`, height: `${h}px`, objectFit: fit }"
      ></fg-auth-image>
    </n-carousel>
  </div>
</template>
<script setup lang="ts">
import { PropType, toRefs, shallowReactive, watch } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { requireErrorImg } from '@/utils'
import { useChartDataFetch } from '@/hooks'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { option as configOption } from './config'
import FgAuthImage from '@/components/FgAuthImage/index.vue'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType>,
    required: true
  }
})

const option = shallowReactive({
  dataset: configOption.dataset
})

const { w, h } = toRefs(props.chartConfig.attr)
const {
  autoplay,
  interval,
  slidesPerview,
  direction,
  draggable,
  centeredSlides,
  effect,
  dotType,
  dotPlacement,
  showArrow,
  showDots,
  fit
} = toRefs(props.chartConfig.option)

watch(
  () => props.chartConfig.option.dataset,
  (newData: any) => {
    option.dataset = newData
  },
  {
    immediate: true,
    deep: false
  }
)

useChartDataFetch(props.chartConfig, useChartEditStore, (newData: any) => {
  option.dataset = newData
})
</script>
