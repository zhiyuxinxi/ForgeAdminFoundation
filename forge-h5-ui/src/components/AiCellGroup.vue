<template>
  <view class="ai-cell-group" :class="{ 'ai-cell-group--compact': compact }">
    <text v-if="title" class="ai-cell-group__title">{{ title }}</text>
    <view class="ai-cell-group__body">
      <slot />
    </view>
  </view>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
.ai-cell-group {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  
  &--compact {
    gap: 10rpx;
  }
}

.ai-cell-group__title {
  margin: 8rpx 0 0 32rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #94a3b8;
  line-height: 1.2;
}

.ai-cell-group__body {
  position: relative;
  z-index: 1;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: 0 8rpx 30rpx rgba(15, 23, 42, 0.03);
  backdrop-filter: blur(24rpx);
}
</style>
