<template>
  <CollapseItem name="指标" :expanded="true">
    <SettingItemBox name="内容">
      <SettingItem name="名称">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="数值">
        <n-input-number v-model:value="optionData.dataset" size="small" />
      </SettingItem>
      <SettingItem name="单位">
        <n-input v-model:value="optionData.unit" size="small" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="颜色">
      <SettingItem name="强调色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="背景">
        <n-color-picker v-model:value="optionData.backgroundColor" size="small" :modes="['hex']" />
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
