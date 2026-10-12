<template>
  <collapse-item name="信息" :expanded="true">
    <setting-item-box name="文字" :alone="true">
      <setting-item>
        <n-input v-model:value="optionData.dataset" type="textarea" size="small"></n-input>
      </setting-item>
    </setting-item-box>
  </collapse-item>
  <collapse-item name="样式" :expanded="true">
    <setting-item-box name="文字">
      <setting-item name="字体大小">
        <n-input-number v-model:value="optionData.size" size="small" :placeholder="$ui(&quot;字体大小&quot;)"></n-input-number>
      </setting-item>
      <setting-item name="字体">
        <n-select v-model:value="optionData.fontFamily" size="small" :options="fontFamilyOptions" clearable filterable />
      </setting-item>
      <setting-item name="字体粗细">
        <n-select v-model:value="optionData.fontWeight" size="small" :options="fontWeightOptions" />
      </setting-item>
      <setting-item name="字体样式">
        <n-select v-model:value="optionData.fontStyle" size="small" :options="fontStyleOptions" />
      </setting-item>
    </setting-item-box>
    <setting-item-box name="渐变色参数">
      <setting-item name="起始值">
        <n-color-picker size="small" :modes="['hex']" v-model:value="optionData.gradient.from"></n-color-picker>
      </setting-item>
      <setting-item name="结束值">
        <n-color-picker size="small" :modes="['hex']" v-model:value="optionData.gradient.to"></n-color-picker>
      </setting-item>
      <setting-item name="偏移角度">
        <n-input-number v-model:value="optionData.gradient.deg" size="small" :placeholder="$ui(&quot;颜色旋转&quot;)"></n-input-number>
      </setting-item>
    </setting-item-box>

  </collapse-item>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { PropType } from 'vue'
import { option } from './config'
import { fontFamilyOptions } from '@/settings/fonts'
import {
  CollapseItem,
  SettingItemBox,
  SettingItem
} from '@/components/Pages/ChartItemSetting'

const props = defineProps({
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
