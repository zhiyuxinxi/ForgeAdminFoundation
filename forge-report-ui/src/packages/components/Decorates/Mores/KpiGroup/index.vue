<template>
  <div class="kpi-group">
    <div v-for="(item, index) in list" :key="`${item.title}-${index}`" class="kpi-cell">
      <div class="kpi-glow"></div>
      <div class="kpi-label">{{ item.title || item.name }}</div>
      <div class="kpi-value">
        <span>{{ formatValue(item.value ?? item.dataset) }}</span>
        <em>{{ item.unit }}</em>
      </div>
      <div v-if="item.trend || item.trendValue" class="kpi-trend">{{ item.trend || trendText(item) }}</div>
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
const list = computed(() => {
  const data = option.value.dataset || option.value.items
  return Array.isArray(data) ? data : []
})

const formatValue = (value: number | string) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toLocaleString() : value
}

const trendText = (item: any) => {
  const prefix = item.trendType === 'down' ? '-' : '+'
  return `${prefix}${item.trendValue}%`
}
</script>

<style lang="scss" scoped>
.kpi-group {
  display: grid;
  grid-template-columns: repeat(v-bind('option.columns'), minmax(0, 1fr));
  gap: 14px;
  width: 100%;
  height: 100%;
}

.kpi-cell {
  position: relative;
  min-width: 0;
  padding: 14px 16px;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(135deg, v-bind('option.backgroundColor'), #07122d99),
    radial-gradient(circle at 100% 0, v-bind('`${option.accentColor}2e`'), transparent 44%);
  border: 1px solid v-bind('`${option.borderColor}99`');
  box-shadow: inset 0 0 20px v-bind('`${option.accentColor}1f`'), 0 0 16px #00000035;
}

.kpi-cell::before {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, v-bind('option.accentColor'), transparent);
}

.kpi-glow {
  position: absolute;
  right: -18px;
  top: -18px;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: v-bind('`${option.accentColor}20`');
  filter: blur(4px);
}

.kpi-label {
  position: relative;
  color: v-bind('option.labelColor');
  font-size: 13px;
  line-height: 18px;
}

.kpi-value {
  position: relative;
  display: flex;
  align-items: baseline;
  margin-top: 8px;
}

.kpi-value span {
  color: v-bind('option.numberColor');
  font-size: 28px;
  font-weight: 800;
  line-height: 34px;
}

.kpi-value em {
  margin-left: 7px;
  color: v-bind('option.secondColor');
  font-size: 13px;
  font-style: normal;
}

.kpi-trend {
  position: absolute;
  right: 14px;
  bottom: 10px;
  color: v-bind('option.accentColor');
  font-size: 12px;
}
</style>
