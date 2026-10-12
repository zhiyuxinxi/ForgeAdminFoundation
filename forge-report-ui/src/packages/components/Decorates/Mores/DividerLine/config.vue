<template>
  <CollapseItem name="分割线" :expanded="true">
    <SettingItemBox name="样式">
      <SettingItem name="方向">
        <n-select v-model:value="optionData.direction" size="small" :options="directionOptions" />
      </SettingItem>
      <SettingItem name="线宽">
        <n-input-number v-model:value="optionData.thickness" size="small" :min="1" :max="12" />
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
import { uiText } from '@/i18n/display'
import { PropType } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { option } from './config'

defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const directionOptions = [
  { get label() { return uiText("横向") }, value: 'horizontal' },
  { get label() { return uiText("纵向") }, value: 'vertical' }
]
</script>
