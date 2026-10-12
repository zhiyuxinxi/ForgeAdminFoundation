<template>
  <CollapseItem name="返回按钮" :expanded="true">
    <SettingItemBox name="内容">
      <SettingItem name="主文案">
        <n-input v-model:value="optionData.text" size="small" />
      </SettingItem>
      <SettingItem name="副文案">
        <n-input v-model:value="optionData.subText" size="small" />
      </SettingItem>
      <SettingItem name="风格">
        <n-select v-model:value="optionData.theme" size="small" :options="themeOptions" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="行为">
      <SettingItem name="先关弹窗">
        <n-switch v-model:value="optionData.closeModalFirst" size="small" />
      </SettingItem>
      <SettingItem name="兜底回首页">
        <n-switch v-model:value="optionData.fallbackHome" size="small" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="样式">
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="文字">
        <n-color-picker v-model:value="optionData.textColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="背景">
        <n-color-picker v-model:value="optionData.backgroundColor" size="small" :modes="['hex', 'rgb']" />
      </SettingItem>
      <SettingItem name="字号">
        <n-input-number v-model:value="optionData.fontSize" size="small" :min="10" :max="24" />
      </SettingItem>
      <SettingItem name="圆角">
        <n-input-number v-model:value="optionData.radius" size="small" :min="0" :max="999" />
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

const themeOptions = [
  { get label() { return uiText("玻璃") }, value: 'glass' },
  { get label() { return uiText("霓虹") }, value: 'neon' },
  { get label() { return uiText("实色") }, value: 'solid' },
  { get label() { return uiText("极简") }, value: 'minimal' }
]
</script>
