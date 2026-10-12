<template>
  <div class="section-header">
    <span class="header-mark"></span>
    <div class="header-main">
      <span class="header-title">{{ option.title }}</span>
      <span v-if="option.subtitle" class="header-subtitle">{{ option.subtitle }}</span>
    </div>
    <span v-if="option.unit" class="header-unit">{{ option.unit }}</span>
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
.section-header {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  padding: 0 14px;
  overflow: hidden;
  color: v-bind('option.textColor');
  background:
    linear-gradient(90deg, v-bind('option.backgroundColor'), transparent 74%),
    linear-gradient(180deg, v-bind('`${option.accentColor}12`'), transparent);
}

.section-header::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  opacity: v-bind('option.showBottomLine ? 1 : 0');
  background: linear-gradient(90deg, v-bind('option.accentColor'), transparent);
}

.header-mark {
  width: 28px;
  height: 0;
  margin-right: 10px;
  border-top: 3px solid v-bind('option.accentColor');
  box-shadow: 0 0 10px v-bind('option.accentColor');
}

.header-main {
  display: flex;
  align-items: baseline;
  min-width: 0;
}

.header-title {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 1px;
  white-space: nowrap;
}

.header-subtitle {
  margin-left: 10px;
  color: v-bind('option.mutedColor');
  font-size: 11px;
  letter-spacing: 1px;
  white-space: nowrap;
}

.header-unit {
  margin-left: auto;
  color: v-bind('option.secondColor');
  font-size: 12px;
  white-space: nowrap;
}
</style>
