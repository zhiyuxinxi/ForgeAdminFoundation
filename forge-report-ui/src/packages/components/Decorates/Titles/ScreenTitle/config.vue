<template>
  <CollapseItem name="标题" :expanded="true">
    <SettingItemBox name="内容">
      <SettingItem name="主标题">
        <n-input v-model:value="optionData.dataset" size="small" />
      </SettingItem>
      <SettingItem name="副标题">
        <n-input v-model:value="optionData.subtitle" size="small" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="样式">
      <SettingItem name="字号">
        <n-input-number v-model:value="optionData.fontSize" size="small" :min="20" />
      </SettingItem>
      <SettingItem name="字体">
        <n-select v-model:value="optionData.fontFamily" size="small" :options="fontFamilyOptions" clearable filterable />
      </SettingItem>
      <SettingItem name="字体粗细">
        <n-select v-model:value="optionData.fontWeight" size="small" :options="fontWeightOptions" />
      </SettingItem>
      <SettingItem name="字体样式">
        <n-select v-model:value="optionData.fontStyle" size="small" :options="fontStyleOptions" />
      </SettingItem>
      <SettingItem name="字距">
        <n-input-number v-model:value="optionData.letterSpacing" size="small" :min="0" />
      </SettingItem>
      <SettingItem name="强调色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { PropType } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { fontFamilyOptions } from '@/settings/fonts'
import { option } from './config'

defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const fontWeightOptions = [
  { get label() { return uiText("常规") }, value: 'normal' },
  { get label() { return uiText("加粗") }, value: 'bold' }
]

const fontStyleOptions = [
  { get label() { return uiText("常规") }, value: 'normal' },
  { get label() { return uiText("斜体") }, value: 'italic' }
]
</script>
