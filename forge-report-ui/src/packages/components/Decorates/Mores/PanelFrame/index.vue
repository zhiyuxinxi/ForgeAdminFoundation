<template>
  <div class="panel-frame">
    <div v-if="option.showCorners" class="corner top-left"></div>
    <div v-if="option.showCorners" class="corner top-right"></div>
    <div v-if="option.showCorners" class="corner bottom-left"></div>
    <div v-if="option.showCorners" class="corner bottom-right"></div>
    <div v-if="option.showTitle" class="panel-header">
      <span class="mark"></span>
      <span class="panel-title">{{ option.title }}</span>
      <span v-if="option.subtitle" class="panel-subtitle">{{ option.subtitle }}</span>
      <span v-if="option.unit" class="panel-unit">{{ option.unit }}</span>
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
</script>

<style lang="scss" scoped>
.panel-frame {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background:
    linear-gradient(180deg, v-bind('`${option.accentColor}12`'), transparent 42%),
    v-bind('option.backgroundColor');
  border: 1px solid v-bind('`${option.borderColor}99`');
  box-shadow: inset 0 0 28px v-bind('`${option.accentColor}1f`');
}

.panel-frame::before {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, v-bind('option.accentColor'), transparent);
}

.panel-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  height: 42px;
  padding: 0 16px;
  color: v-bind('option.headerColor');
  background: linear-gradient(90deg, v-bind('`${option.accentColor}1c`'), transparent 72%);
  border-bottom: 1px solid v-bind('`${option.borderColor}55`');
}

.mark {
  width: 24px;
  height: 2px;
  margin-right: 10px;
  background: linear-gradient(90deg, v-bind('option.accentColor'), transparent);
  box-shadow: 0 0 12px v-bind('option.accentColor');
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

.panel-subtitle {
  margin-left: 10px;
  color: v-bind('`${option.headerColor}99`');
  font-size: 12px;
}

.panel-unit {
  margin-left: auto;
  color: v-bind('`${option.accentColor}cc`');
  font-size: 12px;
}

.corner {
  position: absolute;
  z-index: 1;
  width: 28px;
  height: 28px;
  border-color: v-bind('option.accentColor');
}

.top-left {
  top: 0;
  left: 0;
  border-top: 2px solid;
  border-left: 2px solid;
}

.top-right {
  top: 0;
  right: 0;
  border-top: 2px solid;
  border-right: 2px solid;
}

.bottom-left {
  left: 0;
  bottom: 0;
  border-left: 2px solid;
  border-bottom: 2px solid;
}

.bottom-right {
  right: 0;
  bottom: 0;
  border-right: 2px solid;
  border-bottom: 2px solid;
}
</style>
