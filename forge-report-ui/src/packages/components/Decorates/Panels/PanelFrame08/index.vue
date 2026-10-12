<template>
  <base-panel :chart-config="chartConfig" />
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import BasePanel from '../PanelFrame/index.vue'

defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType>,
    required: true
  }
})
</script>
