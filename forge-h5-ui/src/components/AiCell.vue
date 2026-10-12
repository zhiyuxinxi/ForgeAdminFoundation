<template>
  <view
    class="ai-cell"
    :class="{
      'ai-cell--clickable': clickable,
      'ai-cell--border': border
    }"
    :hover-class="clickable ? 'ai-cell--hover' : 'none'"
    @click="handleClick"
  >
    <view
      v-if="$slots.icon || icon"
      class="ai-cell__icon"
      :style="{ background: iconBg }"
    >
      <slot name="icon">
        <image v-if="icon" class="ai-cell__icon-image" :src="resolvedIcon" mode="aspectFit" />
      </slot>
    </view>

    <view class="ai-cell__main">
      <text class="ai-cell__title">{{ title }}</text>
      <text v-if="label" class="ai-cell__label">{{ label }}</text>
      <slot name="label" />
    </view>

    <view class="ai-cell__right">
      <text v-if="value" class="ai-cell__value">{{ value }}</text>
      <slot name="value" />
      <text v-if="isLink" class="ai-cell__arrow">›</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { resolveStaticUrl } from '@/utils/assets'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },
  iconBg: {
    type: String,
    default: 'rgba(241, 245, 249, 0.86)'
  },
  isLink: {
    type: Boolean,
    default: false
  },
  border: {
    type: Boolean,
    default: true
  },
  clickable: {
    type: Boolean,
    default: false
  }
})

const resolvedIcon = computed(() => resolveStaticUrl(props.icon))

const emit = defineEmits(['click'])

const handleClick = (event) => {
  if (props.clickable || props.isLink) {
    emit('click', event)
  }
}
</script>

<style lang="scss" scoped>
.ai-cell {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 112rpx;
  padding: 24rpx 32rpx;
  transition: background 0.2s ease, transform 0.2s ease;

  &--border::after {
    position: absolute;
    right: 32rpx;
    bottom: 0;
    left: 112rpx;
    height: 1px;
    background: rgba(226, 232, 240, 0.66);
    content: '';
  }

  &:last-child::after {
    display: none;
  }

  &--clickable {
    cursor: pointer;
  }

  &--hover {
    background: rgba(241, 245, 249, 0.58);
    transform: scale(0.992);
  }
}

.ai-cell__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 80rpx;
  height: 80rpx;
  margin-right: 28rpx;
  border-radius: 24rpx;
}

.ai-cell__icon-image {
  width: 40rpx;
  height: 40rpx;
}

.ai-cell__main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 6rpx;
}

.ai-cell__title {
  overflow: hidden;
  color: #334155;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-cell__label {
  overflow: hidden;
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 500;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-cell__right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 12rpx;
  max-width: 45%;
  margin-left: 24rpx;
}

.ai-cell__value {
  overflow: hidden;
  color: #64748b;
  font-size: 28rpx;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-cell__arrow {
  color: #94a3b8;
  font-size: 48rpx;
  font-weight: 300;
  line-height: 1;
}
</style>
