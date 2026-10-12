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
      <setting-item name="颜色">
        <n-color-picker size="small" :modes="['hex']" v-model:value="optionData.fontColor"></n-color-picker>
      </setting-item>
      <setting-item name="字体大小">
        <n-input-number v-model:value="optionData.fontSize" size="small" :placeholder="$ui(&quot;字体大小&quot;)"></n-input-number>
      </setting-item>
      <setting-item name="字体粗细">
        <n-select v-model:value="optionData.fontWeight" size="small" :options="fontWeightOptions" />
      </setting-item>
      <setting-item name="字体样式">
        <n-select v-model:value="optionData.fontStyle" size="small" :options="fontStyleOptions" />
      </setting-item>

      <setting-item name="字体">
        <n-select v-model:value="optionData.fontFamily" size="small" :options="fontFamilyOptions" clearable filterable />
      </setting-item>

      <setting-item name="字间距">
        <n-input-number v-model:value="optionData.letterSpacing" size="small" :placeholder="$ui(&quot;输入字间距&quot;)"></n-input-number>
      </setting-item>
    </setting-item-box>
    <setting-item-box name="阴影">
      <setting-item>
        <n-space>
          <n-switch v-model:value="optionData.showShadow" size="small" />
          <n-text>{{ $ui("展示阴影") }}</n-text>
        </n-space>
      </setting-item>
      <setting-item name="颜色">
        <n-color-picker size="small" :modes="['hex']" v-model:value="optionData.colorShadow"></n-color-picker
      ></setting-item>
      <setting-item name="x">
        <n-input-number v-model:value="optionData.hShadow" size="small"></n-input-number
      ></setting-item>
      <setting-item name="y">
        <n-input-number v-model:value="optionData.vShadow" size="small"></n-input-number
      ></setting-item>
      <setting-item name="模糊">
        <n-input-number v-model:value="optionData.blurShadow" size="small"></n-input-number
      ></setting-item>
    </setting-item-box>

    <setting-item-box name="动画">
      <setting-item name="动画速度">
        <n-input-number
          v-model:value="optionData.animationSpeed"
          size="small"
          :placeholder="$ui(&quot;动画速度&quot;)"
          :min="0"
        ></n-input-number>
      </setting-item>
      <setting-item name="动画间隔">
        <n-input-number
          v-model:value="optionData.animationTime"
          size="small"
          :placeholder="$ui(&quot;动画间隔&quot;)"
          :min="0"
        ></n-input-number>
      </setting-item>
    </setting-item-box>
  </collapse-item>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { option, FontWeightEnum, FontWeightObject, FontStyleEnum, FontStyleObject } from './config'
import { fontFamilyOptions } from '@/settings/fonts'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const fontWeightOptions = [
  {
    label: FontWeightEnum.NORMAL,
    value: FontWeightObject[FontWeightEnum.NORMAL]
  },
  {
    label: FontWeightEnum.BOLD,
    value: FontWeightObject[FontWeightEnum.BOLD]
  }
]
const fontStyleOptions = [
  {
    label: FontStyleEnum.NORMAL,
    value: FontStyleObject[FontStyleEnum.NORMAL]
  },
  {
    label: FontStyleEnum.ITALIC,
    value: FontStyleObject[FontStyleEnum.ITALIC]
  }
]
</script>
