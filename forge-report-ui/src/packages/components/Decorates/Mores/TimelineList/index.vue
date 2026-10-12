<template>
  <div class="timeline-list">
    <div v-for="(item, index) in list" :key="`${item.time}-${index}`" class="timeline-row">
      <div class="time">{{ item.time }}</div>
      <div class="line">
        <span :class="item.status || 'normal'"></span>
      </div>
      <div class="event">
        <div class="event-title">{{ item.title || item.name }}</div>
        <div v-if="item.level || item.desc" class="event-desc">{{ item.level || item.desc }}</div>
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
</script>

<style lang="scss" scoped>
.timeline-list {
  width: 100%;
  height: 100%;
  padding: 14px 16px;
  overflow: hidden;
  background:
    linear-gradient(180deg, v-bind('option.backgroundColor'), #040d1f55),
    radial-gradient(circle at 0 0, v-bind('`${option.accentColor}22`'), transparent 42%);
}

.timeline-row {
  display: grid;
  grid-template-columns: 48px 18px minmax(0, 1fr);
  column-gap: 10px;
  min-height: 38px;
  margin-bottom: v-bind('`${option.rowGap}px`');
}

.time {
  color: v-bind('option.mutedColor');
  font-size: 12px;
  line-height: 20px;
}

.line {
  position: relative;
  display: flex;
  justify-content: center;
}

.line::before {
  content: '';
  position: absolute;
  top: 18px;
  bottom: -20px;
  width: 1px;
  background: linear-gradient(180deg, v-bind('`${option.accentColor}99`'), transparent);
}

.line span {
  position: relative;
  z-index: 1;
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
  background: v-bind('option.accentColor');
  box-shadow: 0 0 10px v-bind('option.accentColor');
}

.line span.warning {
  background: v-bind('option.warningColor');
  box-shadow: 0 0 10px v-bind('option.warningColor');
}

.line span.danger {
  background: v-bind('option.dangerColor');
  box-shadow: 0 0 10px v-bind('option.dangerColor');
}

.event {
  min-width: 0;
  padding-bottom: 2px;
}

.event-title {
  color: v-bind('option.textColor');
  font-size: 13px;
  line-height: 19px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-desc {
  margin-top: 3px;
  color: v-bind('option.mutedColor');
  font-size: 11px;
}
</style>
