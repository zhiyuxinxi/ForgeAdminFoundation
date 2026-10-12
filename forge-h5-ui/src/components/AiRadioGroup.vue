<template>
  <view class="ai-radio-group">
    <view 
      v-for="(option, index) in options" 
      :key="index"
      class="ai-radio-item"
      @click="handleClick(option)"
    >
      <view class="ai-radio-icon" :class="{ 'ai-radio-icon--checked': modelValue === option.value }">
        <view class="ai-radio-dot" v-if="modelValue === option.value"></view>
      </view>
      <text class="ai-radio-label">{{option.label}}</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  options: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: [String, Number],
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const handleClick = (option) => {
  emit('update:modelValue', option.value)
  emit('change', option.value)
}
</script>

<style lang="scss" scoped>
.ai-radio-group {
  display: flex;
  flex-direction: column;
}

.ai-radio-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
}

.ai-radio-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #ddd;
  margin-right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &--checked {
    border-color: var(--primary-color, #0891b2);
  }
}

.ai-radio-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--primary-color, #0891b2);
}

.ai-radio-label {
  font-size: 14px;
  color: #333;
}
</style>
