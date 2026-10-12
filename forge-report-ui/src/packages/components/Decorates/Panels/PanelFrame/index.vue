<template>
  <div class="panel-frame" :class="`variant-${option.styleVariant || 'corner'}`">
    <div v-if="option.showCorners" class="corner top-left"></div>
    <div v-if="option.showCorners" class="corner top-right"></div>
    <div v-if="option.showCorners" class="corner bottom-left"></div>
    <div v-if="option.showCorners" class="corner bottom-right"></div>
    <div class="inner-line"></div>
    <div class="panel-scanline"></div>
    <div class="panel-grid"></div>
    <div class="panel-orbit orbit-one"></div>
    <div class="panel-orbit orbit-two"></div>
    <div class="panel-side side-left"></div>
    <div class="panel-side side-right"></div>
    <div class="panel-rivet rivet-one"></div>
    <div class="panel-rivet rivet-two"></div>
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
  width: 28px;
  height: 0;
  margin-right: 10px;
  border-top: 2px solid v-bind('option.accentColor');
  box-shadow: 0 0 10px v-bind('option.accentColor');
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  font-family: v-bind('option.fontFamily');
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
  display: none;
  z-index: 1;
  width: 34px;
  height: 34px;
  border: 0;
  color: v-bind('option.accentColor');
  filter: drop-shadow(0 0 5px v-bind('`${option.accentColor}88`'));
}

.corner::before,
.corner::after {
  content: '';
  position: absolute;
  display: block;
  background: currentColor;
}

.corner::before {
  width: 34px;
  height: 2px;
}

.corner::after {
  width: 2px;
  height: 34px;
}

.top-left {
  top: 9px;
  left: 9px;
}

.top-left::before,
.top-left::after {
  top: 0;
  left: 0;
}

.top-right {
  top: 9px;
  right: 9px;
}

.top-right::before {
  top: 0;
  right: 0;
}

.top-right::after {
  top: 0;
  right: 0;
}

.bottom-left {
  left: 9px;
  bottom: 9px;
}

.bottom-left::before {
  left: 0;
  bottom: 0;
}

.bottom-left::after {
  left: 0;
  bottom: 0;
}

.bottom-right {
  right: 9px;
  bottom: 9px;
}

.bottom-right::before {
  right: 0;
  bottom: 0;
}

.bottom-right::after {
  right: 0;
  bottom: 0;
}

.inner-line,
.panel-scanline,
.panel-grid,
.panel-orbit,
.panel-side,
.panel-rivet {
  position: absolute;
  pointer-events: none;
}

.panel-scanline,
.panel-grid,
.panel-orbit,
.panel-side,
.panel-rivet {
  display: none;
}

.inner-line {
  inset: 8px;
  border: 1px solid v-bind('`${option.borderColor}22`');
}

.variant-corner {
  border: 0;
  background:
    linear-gradient(135deg, v-bind('`${option.accentColor}44`') 0 2px, transparent 2px 24px),
    linear-gradient(315deg, v-bind('`${option.borderColor}55`') 0 2px, transparent 2px 24px),
    linear-gradient(180deg, v-bind('`${option.accentColor}10`'), transparent 44%),
    v-bind('option.backgroundColor');
  box-shadow: inset 0 0 0 1px v-bind('`${option.borderColor}6c`'), inset 0 0 30px v-bind('`${option.accentColor}1c`');
}

.variant-corner::before {
  left: 18px;
  right: 18px;
  top: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, v-bind('option.accentColor'), transparent);
}

.variant-corner::after {
  content: '';
  position: absolute;
  left: 16px;
  top: 14px;
  width: 132px;
  height: 18px;
  border-top: 1px solid v-bind('`${option.accentColor}aa`');
  background: linear-gradient(90deg, v-bind('`${option.accentColor}28`'), transparent);
  clip-path: polygon(0 0, 100% 0, 82% 100%, 0 100%);
}

.variant-corner .panel-header {
  top: 12px;
  left: 18px;
  right: 18px;
  height: 34px;
  border-bottom: 0;
  background: transparent;
}

.variant-corner .mark {
  width: 18px;
  height: 18px;
  border-top: 0;
  background: linear-gradient(135deg, v-bind('option.accentColor'), transparent 65%);
  clip-path: polygon(0 0, 100% 0, 32% 100%, 0 100%);
  box-shadow: none;
}

.variant-scan::after,
.variant-grid::after,
.variant-glow::after {
  content: '';
  position: absolute;
  inset: 42px 0 0;
  pointer-events: none;
}

.variant-scan::after {
  background: linear-gradient(180deg, transparent, v-bind('`${option.accentColor}2a`'), transparent);
  animation: panel-scan 3.2s linear infinite;
}

.variant-scan .panel-scanline {
  display: block;
  left: 18px;
  right: 18px;
  top: 74px;
  height: 2px;
  background: linear-gradient(90deg, transparent, v-bind('option.accentColor'), transparent);
  box-shadow: 0 0 16px v-bind('option.accentColor');
  animation: panel-scanline 2.4s ease-in-out infinite alternate;
}

.variant-scan .panel-header {
  height: 48px;
  padding-left: 24px;
  background: linear-gradient(90deg, v-bind('`${option.accentColor}2f`'), transparent 58%);
}

.variant-grid::after {
  opacity: .22;
  background-image:
    linear-gradient(v-bind('`${option.accentColor}55`') 1px, transparent 1px),
    linear-gradient(90deg, v-bind('`${option.accentColor}55`') 1px, transparent 1px);
  background-size: 24px 24px;
}

.variant-grid .panel-grid {
  display: block;
  inset: 52px 14px 14px;
  border: 1px solid v-bind('`${option.accentColor}33`');
  background:
    linear-gradient(90deg, v-bind('`${option.accentColor}30`') 1px, transparent 1px),
    linear-gradient(v-bind('`${option.accentColor}30`') 1px, transparent 1px);
  background-size: 42px 34px;
  mask-image: linear-gradient(180deg, transparent, #000 20%, #000 86%, transparent);
}

.variant-grid .corner {
  display: none;
  width: 22px;
  height: 22px;
}

.variant-grid .corner::before {
  width: 22px;
}

.variant-grid .corner::after {
  height: 22px;
}

.variant-glow {
  box-shadow: inset 0 0 34px v-bind('`${option.accentColor}33`'), 0 0 18px v-bind('`${option.accentColor}26`');
}

.variant-glow .panel-orbit {
  display: block;
  right: 30px;
  bottom: 22px;
  border: 1px solid v-bind('`${option.accentColor}70`');
  border-radius: 50%;
  box-shadow: 0 0 18px v-bind('`${option.accentColor}44`');
}

.variant-glow .orbit-one {
  width: 150px;
  height: 58px;
  transform: rotate(-16deg);
}

.variant-glow .orbit-two {
  width: 110px;
  height: 42px;
  border-color: v-bind('`${option.borderColor}70`');
  transform: rotate(16deg);
}

.variant-cut {
  clip-path: polygon(18px 0, calc(100% - 18px) 0, 100% 18px, 100% calc(100% - 18px), calc(100% - 18px) 100%, 18px 100%, 0 calc(100% - 18px), 0 18px);
}

.variant-cut .panel-header {
  left: 18px;
  right: 18px;
  top: 8px;
  height: 40px;
  border: 1px solid v-bind('`${option.borderColor}44`');
  clip-path: polygon(14px 0, 100% 0, calc(100% - 14px) 100%, 0 100%);
}

.variant-cut .inner-line {
  clip-path: polygon(16px 0, calc(100% - 16px) 0, 100% 16px, 100% calc(100% - 16px), calc(100% - 16px) 100%, 16px 100%, 0 calc(100% - 16px), 0 16px);
}

.variant-heavy .corner {
  display: block;
  width: 46px;
  height: 46px;
}

.variant-heavy .corner::before {
  width: 46px;
  height: 4px;
}

.variant-heavy .corner::after {
  width: 4px;
  height: 46px;
}

.variant-heavy .panel-side {
  display: block;
  top: 76px;
  bottom: 28px;
  width: 8px;
  background: linear-gradient(180deg, transparent, v-bind('`${option.accentColor}bb`'), transparent);
  box-shadow: 0 0 14px v-bind('`${option.accentColor}55`');
}

.variant-heavy .side-left {
  left: 17px;
}

.variant-heavy .side-right {
  right: 17px;
}

.variant-card {
  border-radius: 8px;
  border-color: v-bind('`${option.borderColor}66`');
}

.variant-card .corner {
  display: none;
}

.variant-card .panel-rivet {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: v-bind('option.accentColor');
  box-shadow: 0 0 10px v-bind('option.accentColor');
}

.variant-card .rivet-one {
  top: 18px;
  right: 18px;
}

.variant-card .rivet-two {
  bottom: 18px;
  left: 18px;
}

.variant-card .panel-header {
  left: 12px;
  right: 12px;
  top: 10px;
  border-radius: 6px;
  border-bottom: 0;
  background: v-bind('`${option.accentColor}18`');
}

@keyframes panel-scan {
  from {
    transform: translateY(-100%);
  }
  to {
    transform: translateY(100%);
  }
}

@keyframes panel-scanline {
  from {
    top: 68px;
    opacity: .45;
  }
  to {
    top: calc(100% - 42px);
    opacity: .9;
  }
}
</style>
