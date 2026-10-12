<template>
  <div class="progress-ring">
    <div class="ring">
      <div class="ring-core">
        <div class="value">{{ displayValue }}</div>
        <div class="unit">{{ option.unit }}</div>
      </div>
    </div>
    <div class="title">{{ option.title }}</div>
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
const percent = computed(() => {
  const max = Number(option.value.max) || 100
  const value = Number(option.value.dataset) || 0
  return Math.max(0, Math.min(100, (value / max) * 100))
})
const ringBackground = computed(() => {
  return `conic-gradient(${option.value.accentColor} 0deg, ${option.value.secondColor} ${percent.value * 3.6}deg, ${option.value.trackColor} ${percent.value * 3.6}deg 360deg)`
})
const displayValue = computed(() => {
  const value = Number(option.value.dataset)
  return Number.isFinite(value) ? value.toLocaleString() : option.value.dataset
})
</script>

<style lang="scss" scoped>
.progress-ring {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 10px;
  background:
    radial-gradient(circle at 50% 44%, v-bind('`${option.accentColor}18`'), transparent 58%),
    v-bind('option.backgroundColor');
}

.ring {
  position: relative;
  width: min(78%, 132px);
  aspect-ratio: 1;
  border-radius: 50%;
  background: v-bind('ringBackground');
  box-shadow: 0 0 20px v-bind('`${option.accentColor}33`');
}

.ring::before {
  content: '';
  position: absolute;
  inset: v-bind('`${option.ringWidth}px`');
  border-radius: 50%;
  background: #061426;
  box-shadow: inset 0 0 18px #00000066;
}

.ring-core {
  position: absolute;
  inset: v-bind('`${option.ringWidth}px`');
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.value {
  color: v-bind('option.textColor');
  font-size: 28px;
  font-weight: 800;
  line-height: 32px;
}

.unit {
  color: v-bind('option.accentColor');
  font-size: 12px;
}

.title {
  margin-top: 10px;
  color: v-bind('option.labelColor');
  font-size: 13px;
  letter-spacing: 1px;
}
</style>
