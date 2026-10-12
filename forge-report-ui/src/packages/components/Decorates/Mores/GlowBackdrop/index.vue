<template>
  <div class="glow-backdrop" :class="[`variant-${option.variant}`, { animate: option.animate }]">
    <svg viewBox="0 0 720 420" preserveAspectRatio="none">
      <defs>
        <radialGradient :id="coreId" cx="50%" cy="50%" r="52%">
          <stop offset="0%" :stop-color="option.accentColor" stop-opacity="0.72" />
          <stop offset="42%" :stop-color="option.secondColor" stop-opacity="0.22" />
          <stop offset="100%" stop-color="#000000" stop-opacity="0" />
        </radialGradient>
        <linearGradient :id="lineId" x1="0%" y1="50%" x2="100%" y2="50%">
          <stop offset="0%" stop-color="#000000" stop-opacity="0" />
          <stop offset="45%" :stop-color="option.accentColor" stop-opacity="0.78" />
          <stop offset="55%" :stop-color="option.secondColor" stop-opacity="0.78" />
          <stop offset="100%" stop-color="#000000" stop-opacity="0" />
        </linearGradient>
        <filter :id="filterId" x="-30%" y="-50%" width="160%" height="200%">
          <feGaussianBlur stdDeviation="5" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <rect width="720" height="420" :fill="option.backgroundColor" />
      <ellipse class="core" cx="360" cy="210" rx="260" ry="126" :fill="`url(#${coreId})`" />
      <g class="reactor" :filter="`url(#${filterId})`">
        <ellipse cx="360" cy="210" rx="235" ry="74" :stroke="`url(#${lineId})`" />
        <ellipse cx="360" cy="210" rx="150" ry="42" :stroke="option.secondColor" />
        <path d="M90 210 L270 184 L360 210 L450 184 L630 210" :stroke="`url(#${lineId})`" />
        <path d="M130 250 L282 228 L360 250 L438 228 L590 250" :stroke="`url(#${lineId})`" opacity=".55" />
      </g>
      <g class="grid" :filter="`url(#${filterId})`">
        <path v-for="i in 9" :key="`h-${i}`" :d="`M${60 + i * 26} 320 L360 112 L${660 - i * 26} 320`" :stroke="option.accentColor" opacity=".22" />
        <path v-for="i in 6" :key="`v-${i}`" :d="`M80 ${180 + i * 24} L640 ${180 + i * 24}`" :stroke="option.secondColor" opacity=".25" />
      </g>
      <g class="wing" :filter="`url(#${filterId})`">
        <path d="M40 210 C160 130, 260 130, 342 208 C250 250, 150 268, 40 210Z" :fill="option.accentColor" opacity=".18" />
        <path d="M680 210 C560 130, 460 130, 378 208 C470 250, 570 268, 680 210Z" :fill="option.secondColor" opacity=".18" />
        <path d="M70 210 L300 196 M650 210 L420 196" :stroke="`url(#${lineId})`" />
      </g>
      <g class="stargate" :filter="`url(#${filterId})`">
        <ellipse cx="360" cy="210" rx="292" ry="118" :stroke="`url(#${lineId})`" opacity=".72" />
        <ellipse cx="360" cy="210" rx="232" ry="88" :stroke="option.secondColor" opacity=".52" />
        <ellipse cx="360" cy="210" rx="168" ry="58" :stroke="option.accentColor" opacity=".68" />
        <path d="M102 210 H618 M360 84 V336 M178 136 L542 284 M178 284 L542 136" :stroke="`url(#${lineId})`" opacity=".46" />
        <path d="M120 176 L184 132 H536 L600 176 M120 244 L184 288 H536 L600 244" :stroke="option.thirdColor" opacity=".5" />
        <circle v-for="i in 16" :key="`gate-node-${i}`" :cx="360 + Math.cos(i * Math.PI / 8) * 238" :cy="210 + Math.sin(i * Math.PI / 8) * 82" r="3.2" :fill="i % 3 === 0 ? option.thirdColor : option.accentColor" />
        <circle v-for="i in 10" :key="`gate-inner-${i}`" :cx="360 + Math.cos(i * Math.PI / 5) * 146" :cy="210 + Math.sin(i * Math.PI / 5) * 48" r="2.4" :fill="option.secondColor" opacity=".86" />
      </g>
      <g class="radar" :filter="`url(#${filterId})`">
        <circle cx="360" cy="210" r="42" :stroke="option.thirdColor" opacity=".55" />
        <circle cx="360" cy="210" r="92" :stroke="option.accentColor" opacity=".42" />
        <circle cx="360" cy="210" r="150" :stroke="option.secondColor" opacity=".35" />
        <circle cx="360" cy="210" r="212" :stroke="option.accentColor" opacity=".28" />
        <path d="M360 210 L602 92 L520 210 L630 305 Z" :fill="option.accentColor" opacity=".12" />
        <path v-for="i in 12" :key="`radar-ray-${i}`" :d="`M360 210 L${360 + Math.cos(i * Math.PI / 6) * 285} ${210 + Math.sin(i * Math.PI / 6) * 165}`" :stroke="i % 2 ? option.accentColor : option.secondColor" opacity=".35" />
        <path d="M122 210 C224 166, 292 154, 360 210 C442 276, 518 270, 626 210" :stroke="`url(#${lineId})`" opacity=".7" />
        <path d="M168 286 C260 236, 310 236, 360 286 C432 340, 512 326, 594 286" :stroke="option.thirdColor" opacity=".42" />
        <circle v-for="i in 18" :key="`radar-dot-${i}`" :cx="80 + i * 34" :cy="i % 2 ? 126 + (i % 5) * 16 : 276 - (i % 4) * 18" r="2.8" :fill="i % 4 === 0 ? option.thirdColor : option.secondColor" opacity=".85" />
      </g>
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
const uid = computed(() => props.chartConfig.id || 'glow-backdrop')
const coreId = computed(() => `${uid.value}-core`)
const lineId = computed(() => `${uid.value}-line`)
const filterId = computed(() => `${uid.value}-filter`)
</script>

<style lang="scss" scoped>
.glow-backdrop {
  width: 100%;
  height: 100%;
  opacity: v-bind('option.opacity');
  overflow: hidden;
  transform: rotate(v-bind('`${option.rotate}deg`'));
  mix-blend-mode: screen;
}

svg {
  width: 100%;
  height: 100%;
}

ellipse,
path {
  fill: none;
  stroke-width: 2;
}

.core {
  fill: v-bind('`url(#${coreId})`');
  stroke: none;
}

.grid,
.wing,
.reactor,
.stargate,
.radar {
  display: none;
}

.variant-reactor .reactor,
.variant-grid .grid,
.variant-wing .wing,
.variant-stargate .stargate,
.variant-radar .radar {
  display: block;
}

.animate .reactor,
.animate .stargate,
.animate .radar {
  animation: breathe 4s ease-in-out infinite;
}

.animate.variant-stargate .stargate {
  animation: gate-drift 5.4s ease-in-out infinite;
}

.animate.variant-radar .radar {
  animation: radar-scan 4.6s linear infinite;
  transform-origin: 360px 210px;
}

.animate .core {
  animation: pulse-core 3.2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% { opacity: .72; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-4px); }
}

@keyframes pulse-core {
  0%, 100% { opacity: .5; }
  50% { opacity: .92; }
}

@keyframes gate-drift {
  0%, 100% { opacity: .7; transform: scale(1) translateY(0); }
  50% { opacity: 1; transform: scale(1.025) translateY(-3px); }
}

@keyframes radar-scan {
  0% { opacity: .68; transform: rotate(0deg) scale(1); }
  50% { opacity: 1; transform: rotate(1.4deg) scale(1.015); }
  100% { opacity: .68; transform: rotate(0deg) scale(1); }
}
</style>
