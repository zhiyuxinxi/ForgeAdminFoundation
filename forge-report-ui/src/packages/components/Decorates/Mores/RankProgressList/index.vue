<template>
  <div class="rank-progress-list">
    <div v-for="(item, index) in list" :key="`${item.name}-${index}`" class="rank-row">
      <div v-if="option.showIndex" class="rank-index" :class="{ hot: index < 3 }">{{ index + 1 }}</div>
      <div class="rank-body">
        <div class="rank-meta">
          <span class="rank-name">{{ item.name }}</span>
          <span class="rank-value">{{ item.value }}{{ option.unit }}</span>
        </div>
        <div class="rank-track">
          <div class="rank-bar" :style="{ width: `${getPercent(item.value)}%` }"></div>
        </div>
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
const list = computed(() => Array.isArray(option.value.dataset) ? option.value.dataset : [])
const getPercent = (value: number) => {
  const max = Number(option.value.max) || 100
  return Math.max(0, Math.min(100, (Number(value) / max) * 100))
}
</script>

<style lang="scss" scoped>
.rank-progress-list {
  width: 100%;
  height: 100%;
  padding: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #07193688, #040d1f44);
}

.rank-row {
  display: flex;
  align-items: center;
  margin-bottom: v-bind('`${option.rowGap}px`');
}

.rank-index {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 26px;
  width: 26px;
  height: 26px;
  margin-right: 10px;
  color: v-bind('option.mutedColor');
  font-size: 13px;
  border: 1px solid #315477;
  background: #0c223d;
}

.rank-index.hot {
  color: #ffffff;
  border-color: v-bind('option.accentColor');
  background: v-bind('`${option.accentColor}33`');
  box-shadow: 0 0 12px v-bind('`${option.accentColor}66`');
}

.rank-body {
  flex: 1;
  min-width: 0;
}

.rank-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}

.rank-name {
  color: v-bind('option.textColor');
}

.rank-value {
  color: v-bind('option.secondColor');
  font-weight: 700;
}

.rank-track {
  height: 8px;
  overflow: hidden;
  background: v-bind('option.trackColor');
}

.rank-bar {
  height: 100%;
  background: linear-gradient(90deg, v-bind('option.accentColor'), v-bind('option.secondColor'));
  box-shadow: 0 0 10px v-bind('`${option.accentColor}88`');
}
</style>
