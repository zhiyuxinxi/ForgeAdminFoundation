<template>
  <base-config :option-data="optionData" />
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import BaseConfig from '../ScreenTitle/config.vue'
import { option } from './config'

defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})
</script>
