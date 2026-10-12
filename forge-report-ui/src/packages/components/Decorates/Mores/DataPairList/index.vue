<template>
  <div class="data-pair-list">
    <div v-for="(item, index) in list" :key="`${item.label}-${index}`" class="pair-item">
      <span>{{ item.label || item.name }}</span>
      <strong>{{ item.value }}</strong>
    </div>
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
const list = computed(() => Array.isArray(option.value.dataset) ? option.value.dataset : [])
</script>

<style lang="scss" scoped>
.data-pair-list {
  display: grid;
  grid-template-columns: repeat(v-bind('option.columns'), minmax(0, 1fr));
  gap: v-bind('`${option.rowGap}px`') 12px;
  width: 100%;
  height: 100%;
  padding: 12px;
  overflow: hidden;
  background: linear-gradient(180deg, v-bind('option.backgroundColor'), #040d1f55);
}

.pair-item {
  position: relative;
  min-width: 0;
  padding: 8px 10px 8px 12px;
  background: linear-gradient(90deg, v-bind('`${option.accentColor}18`'), transparent);
  border-left: 2px solid v-bind('option.accentColor');
}

.pair-item span {
  display: block;
  color: v-bind('option.labelColor');
  font-size: 12px;
  line-height: 16px;
}

.pair-item strong {
  display: block;
  margin-top: 4px;
  color: v-bind('option.valueColor');
  font-size: 15px;
  line-height: 20px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
