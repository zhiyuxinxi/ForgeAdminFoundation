<template>
  <div class="step-flow">
    <div v-for="(item, index) in steps" :key="`${item.title}-${index}`" class="step" :class="item.status || 'pending'">
      <div class="node">{{ index + 1 }}</div>
      <div class="name">{{ item.title || item.name }}</div>
      <div v-if="index < steps.length - 1" class="connector"></div>
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
const steps = computed(() => Array.isArray(option.value.dataset) ? option.value.dataset : [])
</script>

<style lang="scss" scoped>
.step-flow {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  padding: 12px 16px;
  overflow: hidden;
  background: linear-gradient(90deg, transparent, v-bind('option.backgroundColor'), transparent);
}

.step {
  position: relative;
  display: flex;
  flex: 1;
  align-items: center;
  min-width: 0;
}

.node {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: v-bind('option.mutedColor');
  font-size: 12px;
  border: 1px solid v-bind('option.pendingColor');
  background: #07162c;
  transform: rotate(45deg);
}

.node::first-letter {
  transform: rotate(-45deg);
}

.name {
  margin-left: 12px;
  color: v-bind('option.textColor');
  font-size: 13px;
  white-space: nowrap;
}

.connector {
  flex: 1;
  height: 1px;
  margin: 0 12px;
  background: linear-gradient(90deg, currentColor, transparent);
  color: v-bind('option.pendingColor');
}

.done .node {
  color: #fff;
  border-color: v-bind('option.doneColor');
  box-shadow: 0 0 12px v-bind('`${option.doneColor}aa`');
}

.done .connector {
  color: v-bind('option.doneColor');
}

.active .node {
  color: #fff;
  border-color: v-bind('option.accentColor');
  background: v-bind('`${option.accentColor}22`');
  box-shadow: 0 0 16px v-bind('option.accentColor');
}

.active .name {
  color: v-bind('option.accentColor');
}
</style>
