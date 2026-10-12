<template>
  <CollapseItem name="时间线" :expanded="true">
    <SettingItemBox name="样式">
      <SettingItem name="行距">
        <n-input-number v-model:value="optionData.rowGap" size="small" :min="4" :max="30" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="颜色">
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="告警">
        <n-color-picker v-model:value="optionData.dangerColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="警告">
        <n-color-picker v-model:value="optionData.warningColor" size="small" :modes="['hex']" />
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
