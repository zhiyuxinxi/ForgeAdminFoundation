<template>
  <view class="ai-dropdown-item">
    <slot></slot>
  </view>
</template>

<script setup>
// Dropdown item component - can be extended with more functionality
defineProps({
  label: {
    type: String,
    default: ''
  },
  value: {
    type: [String, Number],
    default: ''
  }
})
</script>
