<template>
  <div class="divider-line" :class="[`is-${option.direction}`, { glow: option.glow }]">
    <span v-if="option.showNode" class="node start"></span>
    <span class="line"></span>
    <span v-if="option.showNode" class="node end"></span>
  </div>
</template>

<script setup lang="ts">
import { computed, PropType } from 'vue'
import { CreateComponentType } from '@/packages/index.d'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType>,
    required: true
  }
})

const option = computed(() => props.chartConfig.option)
</script>

<style lang="scss" scoped>
.divider-line {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  color: v-bind('option.accentColor');
}

.line {
  flex: 1;
  height: v-bind('`${option.thickness}px`');
  background: linear-gradient(90deg, transparent, v-bind('option.accentColor'), v-bind('option.secondColor'), transparent);
}

.node {
  width: 9px;
  height: 9px;
  border: 1px solid v-bind('option.accentColor');
  background: v-bind('`${option.accentColor}33`');
  transform: rotate(45deg);
}

.glow .line,
.glow .node {
  box-shadow: 0 0 12px currentColor;
}

.is-vertical {
  flex-direction: column;
}

.is-vertical .line {
  width: v-bind('`${option.thickness}px`');
  height: auto;
  background: linear-gradient(180deg, transparent, v-bind('option.accentColor'), v-bind('option.secondColor'), transparent);
}
</style>
