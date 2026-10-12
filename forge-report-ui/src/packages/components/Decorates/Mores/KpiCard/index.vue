<template>
  <div class="kpi-card">
    <div class="kpi-icon">{{ option.iconText }}</div>
    <div class="kpi-main">
      <div class="kpi-title">{{ option.title }}</div>
      <div class="kpi-value">
        <span>{{ valueText }}</span>
        <em>{{ option.unit }}</em>
      </div>
    </div>
    <div v-if="option.showTrend" class="kpi-trend" :class="option.trendType">
      {{ option.trendLabel }} {{ trendPrefix }}{{ option.trendValue }}%
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
const valueText = computed(() => {
  const value = Number(option.value.dataset)
  if (!Number.isFinite(value)) return option.value.dataset
  return value.toLocaleString(undefined, {
    minimumFractionDigits: option.value.precision,
    maximumFractionDigits: option.value.precision
  })
})
const trendPrefix = computed(() => option.value.trendType === 'down' ? '-' : '+')
</script>

<style lang="scss" scoped>
.kpi-card {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  padding: 14px 18px;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(135deg, v-bind('option.backgroundColor'), #07122d99),
    radial-gradient(circle at 100% 0, v-bind('`${option.accentColor}2e`'), transparent 42%);
  border: 1px solid v-bind('`${option.borderColor}99`');
  box-shadow: inset 0 0 22px v-bind('`${option.accentColor}20`'), 0 0 18px #00000040;
}

.kpi-card::before {
  content: '';
  position: absolute;
  left: 76px;
  right: 20px;
  top: 10px;
  height: 1px;
  background: linear-gradient(90deg, v-bind('option.accentColor'), transparent);
}

.kpi-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 48px;
  height: 48px;
  margin-right: 14px;
  color: v-bind('option.accentColor');
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  border: 1px solid v-bind('`${option.accentColor}77`');
  background: v-bind('`${option.accentColor}18`');
}

.kpi-main {
  min-width: 0;
}

.kpi-title {
  color: v-bind('option.labelColor');
  font-size: 14px;
  line-height: 18px;
}

.kpi-value {
  display: flex;
  align-items: baseline;
  margin-top: 4px;
}

.kpi-value span {
  color: v-bind('option.numberColor');
  font-size: 30px;
  font-weight: 700;
  line-height: 34px;
}

.kpi-value em {
  margin-left: 8px;
  color: v-bind('option.accentColor');
  font-size: 14px;
  font-style: normal;
}

.kpi-trend {
  position: absolute;
  right: 14px;
  bottom: 10px;
  color: v-bind('option.accentColor');
  font-size: 12px;
}

.kpi-trend.down {
  color: v-bind('option.dangerColor');
}

.kpi-trend.warn {
  color: v-bind('option.warningColor');
}
</style>
