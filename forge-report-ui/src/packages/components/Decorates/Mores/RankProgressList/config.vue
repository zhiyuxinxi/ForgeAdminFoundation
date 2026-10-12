<template>
  <CollapseItem name="排行" :expanded="true">
    <SettingItemBox name="基础">
      <SettingItem name="单位">
        <n-input v-model:value="optionData.unit" size="small" />
      </SettingItem>
      <SettingItem name="最大值">
        <n-input-number v-model:value="optionData.max" size="small" :min="1" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="颜色">
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="辅色">
        <n-color-picker v-model:value="optionData.secondColor" size="small" :modes="['hex']" />
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
