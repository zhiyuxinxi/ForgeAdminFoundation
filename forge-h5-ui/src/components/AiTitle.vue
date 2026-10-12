<template>
  <text class="ai-title">{{title}}</text>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  }
})
</script>

<style lang="scss" scoped>
.ai-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}
</style>
