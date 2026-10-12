<template>
  <div class="designer-async-loader" :class="{ 'is-overlay': overlay }" role="status" aria-live="polite">
    <div class="loader-head">
      <span class="loader-status-dot" />
      <div class="loader-copy">
        <strong>{{ title }}</strong>
        <span>{{ description }}</span>
      </div>
    </div>
    <div class="loader-track">
      <span class="loader-segment" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '正在打开设计器',
  },
  description: {
    type: String,
    default: '首次加载需要准备组件资源',
  },
  overlay: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.designer-async-loader {
  display: grid;
  gap: 16px;
  width: min(420px, 100%);
  margin: 56px auto;
  padding: 16px 18px 18px;
  border: 1px solid rgba(203, 213, 225, 0.78);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98)), #fff;
  box-shadow:
    0 18px 42px rgba(15, 23, 42, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.85) inset;
}

.designer-async-loader.is-overlay {
  width: min(480px, calc(100% - 40px));
  margin: 0 auto;
}

.loader-head {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  align-items: start;
  gap: 10px;
}

.loader-status-dot {
  position: relative;
  width: 9px;
  height: 9px;
  margin-top: 5px;
  border-radius: 999px;
  background: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.loader-status-dot::after {
  position: absolute;
  inset: -5px;
  border: 1px solid rgba(37, 99, 235, 0.28);
  border-radius: inherit;
  animation: loader-pulse 1.6s ease-out infinite;
  content: '';
}

.loader-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.loader-copy strong {
  color: #18181b;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.loader-copy span {
  color: #71717a;
  font-size: 12px;
  line-height: 18px;
}

.loader-track {
  position: relative;
  height: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.08);
}

.loader-segment {
  position: absolute;
  top: 0;
  left: -42%;
  height: 100%;
  width: 42%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb 0%, #14b8a6 68%, #22c55e 100%);
  box-shadow: 0 0 16px rgba(37, 99, 235, 0.28);
  animation: loader-slide 1.35s cubic-bezier(0.7, 0, 0.3, 1) infinite;
}

@keyframes loader-slide {
  0% {
    transform: translateX(0) scaleX(0.72);
    opacity: 0.8;
  }

  45% {
    transform: translateX(160%) scaleX(1);
    opacity: 1;
  }

  100% {
    transform: translateX(360%) scaleX(0.78);
    opacity: 0.86;
  }
}

@keyframes loader-pulse {
  0% {
    transform: scale(0.75);
    opacity: 0.72;
  }

  100% {
    transform: scale(1.8);
    opacity: 0;
  }
}
</style>
