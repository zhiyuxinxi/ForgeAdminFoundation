<template>
  <div
    :class="fullscreen ? 'mask-fullscreen' : 'mask'"
    :style="{
      zIndex: 9999,
      background: `rgba(${background})`,
      fontSize: `${fontSize}px`,
      color,
    }"
  >
    <div class="loading-container">
      <div class="loading-loader" aria-hidden="true" />
      <div v-if="text" class="loading-text">
        {{ text }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'

// 定义props
const props = defineProps({
  fullscreen: {
    type: Boolean,
    default: false,
  },
  background: {
    type: String,
    default: '0, 0, 0, 0.7', // 更暗的背景，更明显
  },
  text: {
    type: String,
    default: '加载中...',
  },
  color: {
    type: String,
    default: '#ffffff',
  },
  fontSize: {
    type: Number,
    default: 14,
  },
})

// 组件挂载时的处理
onMounted(() => {
  if (props.fullscreen) {
    document.body.style.overflow = 'hidden'
  }
})

// 组件销毁时的处理
onUnmounted(() => {
  if (props.fullscreen) {
    document.body.style.overflow = ''
  }
})
</script>

<style scoped>
.mask {
  position: absolute;
  z-index: 2000;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: var(--font-size-base);
}

.mask-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: var(--font-size-base);
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 24px;
}

.loading-text {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: v-bind(color);
}

.loading-loader {
  --size: 1px;
  --loader-color: var(--primary-color, var(--base-primary-color, #165dff));

  width: calc(48 * var(--size));
  height: calc(48 * var(--size));
  background: var(--loader-color);
  border-radius: 10%;
  box-shadow: 0 10px 30px color-mix(in srgb, var(--loader-color) 34%, transparent);
  animation: loading-loader-rotate 1s linear infinite;
}

@keyframes loading-loader-rotate {
  0% {
    transform: rotate(0deg) scale(0.2);
    border-radius: 10%;
  }

  50% {
    transform: rotate(180deg) scale(1.5);
    border-radius: 50%;
  }

  100% {
    transform: rotate(360deg) scale(0.2);
    border-radius: 10%;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-container {
    padding: 16px;
  }

  .loading-loader {
    --size: 0.84px;
  }

  .loading-text {
    font-size: var(--font-size-sm);
  }
}
</style>
