<template>
  <view class="ai-image-preview">
    <image 
      :src="src" 
      :mode="mode"
      @click="previewImage"
      class="preview-image"
    ></image>
  </view>
</template>

<script setup>
const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  mode: {
    type: String,
    default: 'aspectFill'
  }
})

const previewImage = () => {
  if (props.src) {
    uni.previewImage({
      urls: [props.src],
      current: props.src
    })
  }
}
</script>

<style lang="scss" scoped>
.preview-image {
  width: 100%;
  height: auto;
}
</style>
