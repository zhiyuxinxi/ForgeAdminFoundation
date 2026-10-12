<template>
  <button class="back-button" :class="`theme-${option.theme}`" :style="rootStyle" @click="handleBack">
    <span class="icon-shell" aria-hidden="true">
      <svg viewBox="0 0 24 24">
        <path d="M15.8 5.2 9 12l6.8 6.8" />
        <path d="M10 12h10" />
        <path d="M4 5v14" />
      </svg>
    </span>
    <span class="text-shell">
      <span class="main-text">{{ option.text }}</span>
      <span v-if="option.subText" class="sub-text">{{ option.subText }}</span>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed, PropType } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { switchPreviewPage } from '@/views/preview/utils/storage'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType>,
    required: true
  }
})

const chartEditStore = useChartEditStore()
const option = computed(() => props.chartConfig.option)
const rootStyle = computed(() => ({
  '--back-accent': option.value.accentColor,
  '--back-text': option.value.textColor,
  '--back-muted': option.value.mutedColor,
  '--back-bg': option.value.backgroundColor,
  '--back-border': option.value.borderColor,
  '--back-radius': `${option.value.radius}px`,
  '--back-icon-size': `${option.value.iconSize}px`,
  '--back-font-size': `${option.value.fontSize}px`
}))

const handleBack = async () => {
  if (option.value.closeModalFirst && chartEditStore.getModalStack.length) {
    chartEditStore.closeModal()
    return
  }

  if (window.history.length > 1) {
    window.history.back()
    return
  }

  if (option.value.fallbackHome && chartEditStore.getHomePageId) {
    await switchPreviewPage(chartEditStore.getHomePageId, {})
  }
}
</script>

<style lang="scss" scoped>
.back-button {
  display: inline-flex;
  align-items: center;
  width: 100%;
  height: 100%;
  min-width: 0;
  padding: 0 16px 0 12px;
  color: var(--back-text);
  border: 1px solid var(--back-border);
  border-radius: var(--back-radius);
  cursor: pointer;
  background: var(--back-bg);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.back-button:hover {
  transform: translateX(-2px);
  border-color: var(--back-accent);
}

.icon-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  margin-right: 10px;
  border-radius: 50%;
  color: var(--back-accent);
  background: rgba(37, 216, 255, 0.12);
}

svg {
  width: var(--back-icon-size);
  height: var(--back-icon-size);
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.text-shell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  line-height: 1.1;
}

.main-text {
  max-width: 100%;
  overflow: hidden;
  font-size: var(--back-font-size);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub-text {
  margin-top: 3px;
  color: var(--back-muted);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.theme-glass {
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    0 10px 28px rgba(0, 0, 0, 0.24);
}

.theme-neon {
  box-shadow:
    0 0 18px rgba(37, 216, 255, 0.28),
    inset 0 0 18px rgba(37, 216, 255, 0.1);
}

.theme-solid {
  color: #02111f;
  border-color: transparent;
  background: linear-gradient(135deg, var(--back-accent), #8ae8ff);
}

.theme-minimal {
  border-color: transparent;
  background: transparent;
}
</style>
