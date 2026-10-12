<template>
  <view
    class="nm-card"
    :style="{
      borderRadius,
      border: border ? `1px solid #EBEDF0` : ''
    }"
  >
    <view class="nm-header" v-if="title">
      <AiTitle :title="title"/>
    </view>
    <view class="nm-card-body" :style="{padding}">
      <slot></slot>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  borderRadius: {
    type: String,
    default: '8px'
  },
  border: {
    type: Boolean,
    default: true
  },
  title: {
    type: String,
    default: ''
  },
  padding: {
    type: String,
    default: '16px'
  }
})
</script>

<style lang="scss" scoped>
.nm-card {
  background: #ffffff;

  .nm-header {
    padding: 12px;
  }
}
</style>
