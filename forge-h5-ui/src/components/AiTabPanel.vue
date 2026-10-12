<template>
  <view class="ai-tab-panel" v-show="active">
    <slot></slot>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  active: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
.ai-tab-panel {
  width: 100%;
}
</style>
