<template>
  <div class="status-badge-list">
    <div v-for="(item, index) in list" :key="`${item.label}-${index}`" class="status-badge" :style="{ '--badge-color': item.color }">
      <span class="dot"></span>
      <span class="label">{{ item.label || item.name }}</span>
      <strong>{{ item.value }}</strong>
      <em>{{ item.unit || option.unit }}</em>
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
.status-badge-list {
  display: grid;
  grid-template-columns: repeat(v-bind('option.columns'), minmax(0, 1fr));
  gap: 10px;
  width: 100%;
  height: 100%;
}

.status-badge {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 10px 12px;
  overflow: hidden;
  color: v-bind('option.textColor');
  background:
    linear-gradient(135deg, v-bind('option.backgroundColor'), #07142988),
    radial-gradient(circle at 100% 0, color-mix(in srgb, var(--badge-color), transparent 72%), transparent 46%);
  border: 1px solid color-mix(in srgb, var(--badge-color), transparent 35%);
  box-shadow: inset 0 0 16px color-mix(in srgb, var(--badge-color), transparent 82%);
}

.dot {
  width: 8px;
  height: 8px;
  margin-right: 8px;
  border-radius: 50%;
  background: var(--badge-color);
  box-shadow: 0 0 10px var(--badge-color);
}

.label {
  min-width: 0;
  color: v-bind('option.mutedColor');
  font-size: 12px;
  white-space: nowrap;
}

strong {
  margin-left: auto;
  color: #fff;
  font-size: 18px;
  line-height: 22px;
}

em {
  margin-left: 4px;
  color: var(--badge-color);
  font-size: 11px;
  font-style: normal;
}
</style>
