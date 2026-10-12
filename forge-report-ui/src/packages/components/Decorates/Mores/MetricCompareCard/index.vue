<template>
  <div class="metric-card">
    <div class="metric-head">
      <span class="metric-title">{{ data.title || option.title }}</span>
      <span class="metric-chip">COMPARE</span>
    </div>
    <div class="metric-value">
      <span>{{ formatValue(data.value ?? data.dataset ?? option.dataset) }}</span>
      <em>{{ data.unit || option.unit }}</em>
    </div>
    <div class="compare-list">
      <div v-for="(item, index) in compareItems" :key="`${item.label}-${index}`" class="compare-item">
        <span>{{ item.label }}</span>
        <strong :class="item.type === 'down' ? 'down' : 'up'">
          {{ item.type === 'down' ? '-' : '+' }}{{ item.value }}{{ item.unit || '%' }}
        </strong>
      </div>
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
const data = computed(() => {
  const source = option.value.dataset
  return source && typeof source === 'object' && !Array.isArray(source) ? source : option.value
})
const compareItems = computed(() => {
  const items = data.value.compareItems || option.value.compareItems
  return Array.isArray(items) ? items : []
})

const formatValue = (value: number | string) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return value
  return num.toLocaleString(undefined, {
    minimumFractionDigits: option.value.precision,
    maximumFractionDigits: option.value.precision
  })
}
</script>

<style lang="scss" scoped>
.metric-card {
  position: relative;
  width: 100%;
  height: 100%;
  padding: 16px 18px;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(145deg, v-bind('option.backgroundColor'), #06101fcc),
    radial-gradient(circle at 88% 12%, v-bind('`${option.accentColor}28`'), transparent 42%);
  border: 1px solid v-bind('`${option.borderColor}99`');
  box-shadow: inset 0 0 22px v-bind('`${option.accentColor}20`');
}

.metric-card::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: repeating-linear-gradient(135deg, transparent 0 14px, v-bind('`${option.accentColor}12`') 14px 15px);
  opacity: .5;
}

.metric-head,
.metric-value,
.compare-list {
  position: relative;
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-title {
  color: v-bind('option.labelColor');
  font-size: 14px;
}

.metric-chip {
  color: v-bind('option.accentColor');
  font-size: 10px;
  letter-spacing: 1px;
}

.metric-value {
  display: flex;
  align-items: baseline;
  margin-top: 12px;
}

.metric-value span {
  color: v-bind('option.numberColor');
  font-size: 36px;
  font-weight: 800;
  line-height: 42px;
}

.metric-value em {
  margin-left: 8px;
  color: v-bind('option.accentColor');
  font-size: 14px;
  font-style: normal;
}

.compare-list {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

.compare-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 4px 10px;
  background: #07162b99;
  border: 1px solid #244563;
}

.compare-item span {
  color: v-bind('option.labelColor');
  font-size: 12px;
}

.compare-item strong {
  font-size: 13px;
}

.compare-item strong.up {
  color: v-bind('option.upColor');
}

.compare-item strong.down {
  color: v-bind('option.downColor');
}
</style>
