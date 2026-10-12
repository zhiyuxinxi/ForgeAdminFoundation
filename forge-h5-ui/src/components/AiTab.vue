<template>
  <view class="ai-tab" :class="{ 'ai-tab--active': isActive }">
    <slot></slot>
  </view>
</template>

<script setup>
import { computed, inject } from 'vue'

const props = defineProps({
  name: {
    type: [String, Number],
    default: ''
  },
  index: {
    type: Number,
    default: 0
  }
})

const activeIndex = inject('activeIndex', { value: 0 })

const isActive = computed(() => {
  return activeIndex.value === props.index
})
</script>

<style lang="scss" scoped>
.ai-tab {
  display: none;
  
  &--active {
    display: block;
  }
}
</style>
