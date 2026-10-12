<template>
  <view
    class="ai-section"
    :class="[
      `ai-section--${variant}`,
      {
        'ai-section--no-padding': noPadding,
        'ai-section--compact': compact
      }
    ]"
  >
    <view v-if="title || desc || $slots.extra" class="ai-section__head">
      <view class="ai-section__copy">
        <text v-if="title" class="ai-section__title">{{ title }}</text>
        <text v-if="desc" class="ai-section__desc">{{ desc }}</text>
      </view>
      <view v-if="$slots.extra" class="ai-section__extra">
        <slot name="extra" />
      </view>
    </view>
    <slot />
  </view>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  desc: {
    type: String,
    default: ''
  },
  variant: {
    type: String,
    default: 'glass',
    validator: value => ['glass', 'plain', 'solid'].includes(value)
  },
  noPadding: {
    type: Boolean,
    default: false
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
.ai-section {
  overflow: hidden;
  padding: 28rpx;
  border-radius: 36rpx;
}

.ai-section--glass {
  border: 1rpx solid rgba(255, 255, 255, 0.86);
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 10rpx 32rpx rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(24rpx);
}

.ai-section--solid {
  border: 1rpx solid rgba(226, 232, 240, 0.8);
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.04);
}

.ai-section--plain {
  padding: 0;
  border-radius: 0;
  background: transparent;
}

.ai-section--no-padding {
  padding: 0;
}

.ai-section--compact {
  padding: 22rpx;
  border-radius: 28rpx;
}

.ai-section__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.ai-section__copy {
  min-width: 0;
  flex: 1;
}

.ai-section__title,
.ai-section__desc {
  display: block;
  min-width: 0;
}

.ai-section__title {
  overflow: hidden;
  color: #1e293b;
  font-size: 32rpx;
  font-weight: 950;
  line-height: 1.18;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-section__desc {
  margin-top: 8rpx;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 600;
  line-height: 1.36;
}

.ai-section__extra {
  flex-shrink: 0;
}
</style>
