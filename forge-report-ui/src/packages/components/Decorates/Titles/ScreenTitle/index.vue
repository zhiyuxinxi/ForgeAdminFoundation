<template>
  <div class="screen-title" :class="[`variant-${option.styleVariant || 'wing'}`, { 'is-glow': option.glow }]">
    <div class="side-frame side-left"></div>
    <div class="side-frame side-right"></div>
    <div class="outer-track"></div>
    <div class="halo-ring halo-ring-one"></div>
    <div class="halo-ring halo-ring-two"></div>
    <div class="gate-panel gate-left"></div>
    <div class="gate-panel gate-right"></div>
    <div class="blade-stack blade-stack-left">
      <span></span>
      <span></span>
      <span></span>
    </div>
    <div class="blade-stack blade-stack-right">
      <span></span>
      <span></span>
      <span></span>
    </div>
    <div class="pulse-wave pulse-wave-left"></div>
    <div class="pulse-wave pulse-wave-right"></div>
    <div class="crystal-piece crystal-left"></div>
    <div class="crystal-piece crystal-right"></div>
    <div class="console-rail rail-top"></div>
    <div class="console-rail rail-bottom"></div>
    <div class="console-node node-left"></div>
    <div class="console-node node-right"></div>
    <div v-if="option.showBackground" class="title-bg"></div>
    <div v-if="option.showBorder" class="title-border"></div>
    <div class="light-sweep"></div>
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

.side-frame {
  position: absolute;
  top: 8px;
  width: 128px;
  height: calc(100% - 16px);
  opacity: .95;
  border-top: 1px solid v-bind('option.accentColor');
  border-bottom: 1px solid v-bind('option.secondaryColor');
}

.side-frame::before,
.side-frame::after {
  content: '';
  position: absolute;
  width: 34px;
  height: 34px;
  border-color: v-bind('option.accentColor');
}

.side-frame::before {
  top: 0;
  border-top: 3px solid;
}

.side-frame::after {
  bottom: 0;
  border-bottom: 3px solid;
}

.side-left {
  left: 0;
  border-left: 1px solid v-bind('`${option.accentColor}aa`');
}

.side-left::before,
.side-left::after {
  left: 0;
  border-left: 3px solid;
}

.side-right {
  right: 0;
  border-right: 1px solid v-bind('`${option.accentColor}aa`');
}

.side-right::before,
.side-right::after {
  right: 0;
  border-right: 3px solid;
}

.outer-track,
.halo-ring,
.gate-panel,
.blade-stack,
.pulse-wave,
.crystal-piece,
.console-rail,
.console-node {
  position: absolute;
  pointer-events: none;
}

.outer-track,
.halo-ring,
.gate-panel,
.blade-stack,
.pulse-wave,
.crystal-piece,
.console-rail,
.console-node {
  display: none;
}

.light-sweep {
  position: absolute;
  left: -20%;
  top: 0;
  width: 18%;
  height: 100%;
  opacity: .34;
  background: linear-gradient(90deg, transparent, v-bind('option.accentColor'), transparent);
  transform: skewX(-24deg);
  animation: title-sweep 3.8s linear infinite;
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

.variant-halo .title-bg {
  inset: 0 230px 0;
  border-radius: 50%;
  clip-path: none;
  background:
    radial-gradient(ellipse at 50% 50%, v-bind('`${option.accentColor}50`'), transparent 68%),
    radial-gradient(ellipse at 50% 50%, transparent 46%, v-bind('`${option.secondaryColor}36`') 47%, transparent 58%);
}

.variant-halo .title-border {
  inset: 6px 260px;
  border: 1px solid v-bind('`${option.accentColor}aa`');
  border-radius: 50%;
  clip-path: none;
}

.variant-halo .halo-ring {
  display: block;
  left: 50%;
  top: 50%;
  border: 1px solid v-bind('`${option.accentColor}90`');
  border-radius: 50%;
  transform: translate(-50%, -50%) rotate(-8deg);
  box-shadow: 0 0 18px v-bind('`${option.accentColor}50`');
}

.variant-halo .halo-ring-one {
  width: 620px;
  height: 62px;
}

.variant-halo .halo-ring-two {
  width: 500px;
  height: 38px;
  border-color: v-bind('`${option.secondaryColor}88`');
  transform: translate(-50%, -50%) rotate(8deg);
}

.variant-halo .side-frame,
.variant-halo .wing {
  opacity: .4;
}

.variant-blade .title-bg {
  inset: 14px 170px;
  clip-path: polygon(0 50%, 13% 0, 88% 0, 100% 50%, 88% 100%, 13% 100%);
  background:
    linear-gradient(90deg, transparent, v-bind('`${option.accentColor}32`') 22%, v-bind('option.backgroundColor') 50%, v-bind('`${option.secondaryColor}32`') 78%, transparent),
    repeating-linear-gradient(115deg, transparent 0 18px, v-bind('`${option.accentColor}22`') 18px 20px);
}

.variant-blade .wing i {
  width: 106px;
  height: 12px;
}

.variant-blade .blade-stack {
  display: flex;
  top: 18px;
  width: 180px;
  height: 44px;
  gap: 12px;
  align-items: center;
}

.variant-blade .blade-stack-left {
  left: 86px;
}

.variant-blade .blade-stack-right {
  right: 86px;
  transform: rotate(180deg);
}

.variant-blade .blade-stack span {
  display: block;
  width: 48px;
  height: 18px;
  background: linear-gradient(90deg, transparent, v-bind('`${option.accentColor}cc`'));
  clip-path: polygon(0 50%, 70% 0, 100% 50%, 70% 100%);
  box-shadow: 0 0 12px v-bind('`${option.accentColor}88`');
}

.variant-gate .side-frame {
  width: 190px;
  background: linear-gradient(90deg, v-bind('`${option.accentColor}2a`'), transparent);
}

.variant-gate .side-right {
  background: linear-gradient(270deg, v-bind('`${option.accentColor}2a`'), transparent);
}

.variant-gate .gate-panel {
  display: block;
  top: 6px;
  width: 142px;
  height: calc(100% - 12px);
  border-top: 2px solid v-bind('option.accentColor');
  border-bottom: 2px solid v-bind('option.secondaryColor');
  background:
    linear-gradient(180deg, v-bind('`${option.accentColor}28`'), transparent 48%, v-bind('`${option.secondaryColor}18`')),
    repeating-linear-gradient(90deg, transparent 0 18px, v-bind('`${option.accentColor}22`') 18px 20px);
}

.variant-gate .wing,
.variant-gate .title-border {
  display: none;
}

.variant-gate .gate-left {
  left: 18px;
  clip-path: polygon(0 0, 72% 0, 100% 50%, 72% 100%, 0 100%);
}

.variant-gate .gate-right {
  right: 18px;
  clip-path: polygon(28% 0, 100% 0, 100% 100%, 28% 100%, 0 50%);
}

.variant-gate .title-bg {
  inset: 10px 230px;
  clip-path: polygon(6% 0, 94% 0, 100% 50%, 94% 100%, 6% 100%, 0 50%);
}

.variant-pulse .title-border {
  animation: title-pulse 1.8s ease-in-out infinite alternate;
}

.variant-pulse .wing,
.variant-pulse .side-frame {
  display: none;
}

.variant-pulse .pulse-wave {
  display: block;
  top: 50%;
  width: 220px;
  height: 34px;
  transform: translateY(-50%);
  background:
    linear-gradient(90deg, transparent, v-bind('`${option.accentColor}bb`'), transparent),
    repeating-linear-gradient(90deg, transparent 0 18px, v-bind('`${option.accentColor}66`') 18px 20px, transparent 20px 32px);
  clip-path: polygon(0 58%, 9% 58%, 13% 10%, 20% 92%, 27% 32%, 34% 58%, 100% 58%, 100% 68%, 33% 68%, 27% 44%, 20% 100%, 13% 22%, 9% 68%, 0 68%);
  opacity: .86;
  filter: drop-shadow(0 0 8px v-bind('option.accentColor'));
}

.variant-pulse .pulse-wave-left {
  left: 72px;
}

.variant-pulse .pulse-wave-right {
  right: 72px;
  transform: translateY(-50%) rotate(180deg);
}

.variant-pulse .title-bg {
  inset: 18px 310px;
  border-radius: 999px;
  clip-path: none;
  background:
    radial-gradient(circle at 50% 50%, v-bind('`${option.accentColor}36`'), transparent 58%),
    linear-gradient(90deg, transparent, v-bind('option.backgroundColor'), transparent);
}

.variant-crystal .title-bg {
  inset: 7px 180px;
  clip-path: polygon(12% 0, 88% 0, 100% 35%, 92% 100%, 8% 100%, 0 35%);
  background:
    linear-gradient(120deg, transparent 0 15%, v-bind('`${option.accentColor}2a`') 16% 30%, transparent 31%),
    linear-gradient(240deg, transparent 0 20%, v-bind('`${option.secondaryColor}28`') 21% 38%, transparent 39%),
    v-bind('option.backgroundColor');
}

.variant-crystal .wing,
.variant-crystal .side-frame,
.variant-crystal .title-border {
  display: none;
}

.variant-crystal .title-text {
  filter: drop-shadow(0 0 8px v-bind('option.accentColor'));
}

.variant-crystal .crystal-piece {
  display: block;
  top: 10px;
  width: 170px;
  height: calc(100% - 20px);
  background:
    linear-gradient(135deg, v-bind('`${option.accentColor}3d`'), transparent 48%),
    linear-gradient(45deg, transparent 40%, v-bind('`${option.secondaryColor}38`'));
  border: 1px solid v-bind('`${option.accentColor}88`');
  filter: drop-shadow(0 0 10px v-bind('`${option.accentColor}66`'));
}

.variant-crystal .crystal-left {
  left: 38px;
  clip-path: polygon(0 50%, 22% 0, 100% 0, 76% 50%, 100% 100%, 22% 100%);
}

.variant-crystal .crystal-right {
  right: 38px;
  clip-path: polygon(0 0, 78% 0, 100% 50%, 78% 100%, 0 100%, 24% 50%);
}

.variant-wing .outer-track {
  display: block;
  inset: 4px 20px;
  border-top: 1px solid v-bind('`${option.accentColor}99`');
  border-bottom: 1px solid v-bind('`${option.secondaryColor}66`');
  clip-path: polygon(0 50%, 5% 0, 95% 0, 100% 50%, 95% 100%, 5% 100%);
}

.variant-wing .side-frame {
  width: 210px;
}

.variant-wing .wing {
  width: 250px;
}

.variant-console .console-rail {
  display: block;
  left: 44px;
  right: 44px;
  height: 8px;
  background:
    linear-gradient(90deg, transparent, v-bind('`${option.accentColor}dd`') 18%, transparent 32%, transparent 68%, v-bind('`${option.secondaryColor}dd`') 82%, transparent),
    repeating-linear-gradient(90deg, transparent 0 28px, v-bind('`${option.accentColor}66`') 28px 34px);
  filter: drop-shadow(0 0 8px v-bind('option.accentColor'));
}

.variant-console .title-border,
.variant-console .side-frame,
.variant-console .wing {
  display: none;
}

.variant-console .rail-top {
  top: 6px;
}

.variant-console .rail-bottom {
  bottom: 6px;
  transform: rotate(180deg);
}

.variant-console .console-node {
  display: block;
  top: 50%;
  width: 118px;
  height: 42px;
  border: 1px solid v-bind('`${option.accentColor}aa`');
  background:
    radial-gradient(circle at 50% 50%, v-bind('`${option.accentColor}55`'), transparent 42%),
    linear-gradient(90deg, v-bind('`${option.accentColor}24`'), transparent);
  clip-path: polygon(0 50%, 22% 0, 100% 0, 78% 50%, 100% 100%, 22% 100%);
  transform: translateY(-50%);
  box-shadow: inset 0 0 18px v-bind('`${option.accentColor}33`'), 0 0 14px v-bind('`${option.accentColor}33`');
}

.variant-console .node-left {
  left: 46px;
}

.variant-console .node-right {
  right: 46px;
  transform: translateY(-50%) rotate(180deg);
}

.variant-console .title-bg {
  inset: 12px 250px;
  border-radius: 4px;
  clip-path: polygon(8% 0, 92% 0, 100% 50%, 92% 100%, 8% 100%, 0 50%);
}

@keyframes title-sweep {
  from {
    left: -24%;
  }
  to {
    left: 106%;
  }
}

@keyframes title-pulse {
  from {
    box-shadow: inset 0 0 16px v-bind('`${option.accentColor}22`'), 0 0 8px v-bind('`${option.accentColor}33`');
  }
  to {
    box-shadow: inset 0 0 32px v-bind('`${option.accentColor}55`'), 0 0 22px v-bind('`${option.accentColor}88`');
  }
}
</style>
