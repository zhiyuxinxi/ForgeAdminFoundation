<template>
  <view class="ai-result" :class="[`ai-result--${currentType}`]">
    <view class="ai-result__icon-wrap" :style="{ color: currentColor }">
      <AiIcon :name="currentIcon" :color="currentColor" size="xl" />
    </view>
    <text class="ai-result__title">{{ resolvedTitle }}</text>
    <text v-if="resolvedDescription" class="ai-result__desc">{{ resolvedDescription }}</text>

    <view v-if="$slots.default" class="ai-result__extra">
      <slot />
    </view>

    <view v-if="primaryText || secondaryText || $slots.actions" class="ai-result__actions">
      <slot name="actions">
        <AiButton v-if="secondaryText" block variant="secondary" @click="$emit('secondary')">
          {{ secondaryText }}
        </AiButton>
        <AiButton v-if="primaryText" block @click="$emit('primary')">
          {{ primaryText }}
        </AiButton>
      </slot>
    </view>
  </view>
</template>

<script setup>
import { L } from '@/locales'
import { computed } from 'vue'
import AiButton from './AiButton.vue'
import AiIcon from './AiIcon.vue'

const props = defineProps({
  type: {
    type: String,
    default: 'info',
    validator: value => ['success', 'error', 'warning', 'info', 'empty', 'forbidden', 'notFound', 'network'].includes(value)
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },
  primaryText: {
    type: String,
    default: ''
  },
  secondaryText: {
    type: String,
    default: ''
  }
})

defineEmits(['primary', 'secondary'])

const preset = {
  success: {
    icon: 'check-circle',
    color: '#10b981',
    get "title"() { return L("操作成功") },
    get "description"() { return L("当前操作已完成。") }
  },
  error: {
    icon: 'x-circle',
    color: '#ef4444',
    get "title"() { return L("操作失败") },
    get "description"() { return L("请稍后重试或检查当前信息。") }
  },
  warning: {
    icon: 'alert-triangle',
    color: '#d97706',
    get "title"() { return L("请注意") },
    get "description"() { return L("当前操作需要确认后继续。") }
  },
  info: {
    icon: 'info',
    color: '#2563eb',
    get "title"() { return L("提示信息") },
    get "description"() { return L("这里展示当前页面的提示内容。") }
  },
  empty: {
    icon: 'inbox',
    color: '#64748b',
    get "title"() { return L("暂无内容") },
    get "description"() { return L("当前没有可展示的数据。") }
  },
  forbidden: {
    icon: 'shield-off',
    color: '#8b5cf6',
    get "title"() { return L("暂无权限") },
    get "description"() { return L("你没有访问当前内容的权限。") }
  },
  notFound: {
    icon: 'compass',
    color: '#64748b',
    get "title"() { return L("页面不存在") },
    get "description"() { return L("当前页面可能已移动或被删除。") }
  },
  network: {
    icon: 'wifi-off',
    color: '#0891b2',
    get "title"() { return L("网络异常") },
    get "description"() { return L("请检查网络连接后重试。") }
  }
}

const currentType = computed(() => props.type || 'info')
const currentPreset = computed(() => preset[currentType.value] || preset.info)
const currentIcon = computed(() => props.icon || currentPreset.value.icon)
const currentColor = computed(() => currentPreset.value.color)
const resolvedTitle = computed(() => props.title || currentPreset.value.title)
const resolvedDescription = computed(() => props.description || currentPreset.value.description)
</script>

<style lang="scss" scoped>
.ai-result {
  display: flex;
  min-height: 520rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56rpx 36rpx;
  box-sizing: border-box;
  text-align: center;
}

.ai-result__icon-wrap {
  position: relative;
  display: flex;
  width: 132rpx;
  height: 132rpx;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(255, 255, 255, 0.88);
  border-radius: 38rpx;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(20rpx);
}

.ai-result__icon-wrap::before {
  position: absolute;
  inset: 20rpx;
  border-radius: 28rpx;
  background: currentColor;
  opacity: 0.08;
  content: '';
}

.ai-result__title {
  display: block;
  max-width: 620rpx;
  margin-top: 30rpx;
  color: #1e293b;
  font-size: 36rpx;
  font-weight: 950;
  line-height: 1.25;
}

.ai-result__desc {
  display: block;
  max-width: 620rpx;
  margin-top: 14rpx;
  color: #64748b;
  font-size: 25rpx;
  font-weight: 600;
  line-height: 1.55;
}

.ai-result__extra {
  width: 100%;
  margin-top: 28rpx;
}

.ai-result__actions {
  display: flex;
  width: 100%;
  max-width: 560rpx;
  gap: 18rpx;
  margin-top: 42rpx;
}
</style>
