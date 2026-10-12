<template>
  <CollapseItem name="状态标签" :expanded="true">
    <SettingItemBox name="布局">
      <SettingItem name="列数">
        <n-input-number v-model:value="optionData.columns" size="small" :min="1" :max="6" />
      </SettingItem>
      <SettingItem name="单位">
        <n-input v-model:value="optionData.unit" size="small" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="颜色">
      <SettingItem name="背景">
        <n-color-picker v-model:value="optionData.backgroundColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="文字">
        <n-color-picker v-model:value="optionData.textColor" size="small" :modes="['hex']" />
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
