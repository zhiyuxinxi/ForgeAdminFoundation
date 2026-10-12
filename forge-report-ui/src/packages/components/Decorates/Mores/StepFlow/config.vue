<template>
  <CollapseItem name="步骤流程" :expanded="true">
    <SettingItemBox name="颜色">
      <SettingItem name="当前">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="完成">
        <n-color-picker v-model:value="optionData.doneColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="待处理">
        <n-color-picker v-model:value="optionData.pendingColor" size="small" :modes="['hex']" />
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
