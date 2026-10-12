<template>
  <base-panel :chart-config="chartConfig" class="panel-frame-variant" />
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

<style scoped>
.panel-frame-variant {
  clip-path: polygon(18px 0, calc(100% - 18px) 0, 100% 18px, 100% calc(100% - 18px), calc(100% - 18px) 100%, 18px 100%, 0 calc(100% - 18px), 0 18px);
}
</style>
