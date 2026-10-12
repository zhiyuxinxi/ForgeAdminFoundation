<template>
  <div class="mini-trend-card">
    <div class="trend-copy">
      <span>{{ option.title }}</span>
      <strong>{{ formatValue(option.dataset) }}<em>{{ option.unit }}</em></strong>
      <i>{{ option.trend }}</i>
    </div>
    <svg class="sparkline" viewBox="0 0 220 76" preserveAspectRatio="none">
      <defs>
        <linearGradient :id="gradientId" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" :stop-color="option.accentColor" />
          <stop offset="100%" :stop-color="option.secondColor" />
        </linearGradient>
        <filter :id="glowId" x="-30%" y="-80%" width="160%" height="260%">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <polyline class="sparkline-shadow" :points="polylinePoints" fill="none" :stroke="`url(#${gradientId})`" :filter="`url(#${glowId})`" />
      <polyline class="sparkline-line" :points="polylinePoints" fill="none" :stroke="`url(#${gradientId})`" />
    </svg>
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
const gradientId = computed(() => `mini-trend-${props.chartConfig.id || 'default'}`)
const glowId = computed(() => `mini-trend-glow-${props.chartConfig.id || 'default'}`)
const polylinePoints = computed(() => {
  const values = Array.isArray(option.value.points) ? option.value.points.map(Number) : []
  if (!values.length) return ''
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  return values.map((value: number, index: number) => {
    const x = (index / Math.max(1, values.length - 1)) * 220
    const y = 68 - ((value - min) / range) * 58
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
})
const formatValue = (value: number | string) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toLocaleString() : value
}
</script>

<style lang="scss" scoped>
.mini-trend-card {
  position: relative;
  width: 100%;
  height: 100%;
  padding: 14px 16px;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(145deg, v-bind('option.backgroundColor'), #07122d99),
    radial-gradient(circle at 100% 0, v-bind('`${option.accentColor}2a`'), transparent 44%);
  border: 1px solid v-bind('`${option.accentColor}88`');
  box-shadow: inset 0 0 22px v-bind('`${option.accentColor}20`');
}

.trend-copy {
  position: relative;
  z-index: 1;
}

.trend-copy span {
  display: block;
  color: v-bind('option.labelColor');
  font-size: 13px;
}

.trend-copy strong {
  display: block;
  margin-top: 7px;
  color: v-bind('option.numberColor');
  font-size: 30px;
  line-height: 34px;
}

.trend-copy em {
  margin-left: 6px;
  color: v-bind('option.accentColor');
  font-size: 13px;
  font-style: normal;
}

.trend-copy i {
  display: inline-block;
  margin-top: 6px;
  color: v-bind('option.secondColor');
  font-size: 12px;
  font-style: normal;
}

.sparkline {
  position: absolute;
  right: 10px;
  bottom: 8px;
  width: 68%;
  height: 52%;
  opacity: .95;
}

.sparkline-shadow {
  stroke-width: 7;
  opacity: .42;
}

.sparkline-line {
  stroke-width: 2.5;
}
</style>
