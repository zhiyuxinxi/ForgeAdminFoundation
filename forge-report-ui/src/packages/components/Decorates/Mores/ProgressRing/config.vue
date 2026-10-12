<template>
  <CollapseItem name="环形进度" :expanded="true">
    <SettingItemBox name="内容">
      <SettingItem name="标题">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="数值">
        <n-input-number v-model:value="optionData.dataset" size="small" />
      </SettingItem>
      <SettingItem name="最大值">
        <n-input-number v-model:value="optionData.max" size="small" :min="1" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="样式">
      <SettingItem name="环宽">
        <n-input-number v-model:value="optionData.ringWidth" size="small" :min="4" :max="32" />
      </SettingItem>
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="轨道">
        <n-color-picker v-model:value="optionData.trackColor" size="small" :modes="['hex']" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { option } from './config'

defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})
</script>
