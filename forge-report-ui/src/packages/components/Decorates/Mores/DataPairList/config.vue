<template>
  <CollapseItem name="键值信息" :expanded="true">
    <SettingItemBox name="布局">
      <SettingItem name="列数">
        <n-input-number v-model:value="optionData.columns" size="small" :min="1" :max="4" />
      </SettingItem>
      <SettingItem name="行距">
        <n-input-number v-model:value="optionData.rowGap" size="small" :min="4" :max="24" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="颜色">
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
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
