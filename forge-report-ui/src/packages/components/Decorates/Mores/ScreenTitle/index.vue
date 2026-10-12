<template>
  <div class="screen-title" :class="{ 'is-glow': option.glow }">
    <div v-if="option.showBackground" class="title-bg"></div>
    <div v-if="option.showBorder" class="title-border"></div>
    <div v-if="option.showDecorations" class="wing wing-left">
      <span></span>
      <i></i>
    </div>
    <div class="title-content">
      <div class="title-text">{{ option.dataset }}</div>
      <div v-if="option.subtitle" class="title-subtitle">{{ option.subtitle }}</div>
    </div>
    <div v-if="option.showDecorations" class="wing wing-right">
      <span></span>
      <i></i>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, PropType, toRefs } from 'vue'
import { CreateComponentType } from '@/packages/index.d'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType>,
    required: true
  }
})

const { w, h } = toRefs(props.chartConfig.attr)
const option = computed(() => props.chartConfig.option)
const titleFill = computed(() => {
  if (option.value.titleMode === 'solid') return option.value.fontColor
  return `linear-gradient(180deg, ${option.value.gradientFrom}, ${option.value.gradientTo})`
})
const titleShadow = computed(() => option.value.glow ? `0 0 16px ${option.value.accentColor}` : 'none')
</script>

<style lang="scss" scoped>
.screen-title {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: v-bind('`${w}px`');
  height: v-bind('`${h}px`');
  overflow: hidden;
  color: v-bind('option.fontColor');
}

.title-bg {
  position: absolute;
  inset: 6px 90px 8px;
  background:
    linear-gradient(90deg, transparent, v-bind('option.backgroundColor'), transparent),
    radial-gradient(circle at 50% 100%, v-bind('`${option.accentColor}55`'), transparent 62%);
  clip-path: polygon(8% 0, 92% 0, 100% 50%, 92% 100%, 8% 100%, 0 50%);
}

.title-border {
  position: absolute;
  inset: 3px 70px 5px;
  border-top: 1px solid v-bind('option.borderColor');
  border-bottom: 1px solid v-bind('`${option.borderColor}99`');
  clip-path: polygon(7% 0, 93% 0, 100% 50%, 93% 100%, 7% 100%, 0 50%);
  box-shadow: inset 0 0 24px v-bind('`${option.accentColor}33`');
}

.title-border::before,
.title-border::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 42px;
  height: 1px;
  background: v-bind('option.accentColor');
}

.title-border::before {
  left: 0;
}

.title-border::after {
  right: 0;
}

.title-content {
  position: relative;
  z-index: 1;
  min-width: 260px;
  padding: 0 34px;
  text-align: center;
}

.title-text {
  font-size: v-bind('`${option.fontSize}px`');
  font-weight: v-bind('option.fontWeight');
  font-style: v-bind('option.fontStyle');
  font-family: v-bind('option.fontFamily');
  line-height: 1.1;
  letter-spacing: v-bind('`${option.letterSpacing}px`');
  background: v-bind('titleFill');
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  text-shadow: v-bind('titleShadow');
  white-space: nowrap;
}

.title-subtitle {
  margin-top: 4px;
  color: v-bind('`${option.accentColor}cc`');
  font-size: 14px;
  letter-spacing: 3px;
}

.wing {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  width: 220px;
  height: 20px;
}

.wing span {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, v-bind('option.accentColor'));
}

.wing i {
  width: 72px;
  height: 8px;
  border-top: 2px solid v-bind('option.accentColor');
  border-bottom: 1px solid v-bind('option.secondaryColor');
  transform: skewX(-32deg);
  box-shadow: 0 0 10px v-bind('`${option.accentColor}99`');
}

.wing-right {
  transform: rotate(180deg);
}
</style>
