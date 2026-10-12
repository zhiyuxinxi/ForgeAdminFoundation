<template>
  <CollapseItem name="模块框" :expanded="true">
    <SettingItemBox name="内容">
      <SettingItem name="标题">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="单位">
        <n-input v-model:value="optionData.unit" size="small" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="样式">
      <SettingItem name="字体">
        <n-select v-model:value="optionData.fontFamily" size="small" :options="fontFamilyOptions" clearable filterable />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="颜色">
      <SettingItem name="强调色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="边框">
        <n-color-picker v-model:value="optionData.borderColor" size="small" :modes="['hex']" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>
</template>

<script setup lang="ts">
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
</script>
