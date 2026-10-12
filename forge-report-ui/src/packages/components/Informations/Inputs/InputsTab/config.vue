<template>
  <collapse-item name="标签页配置" :expanded="true">
    <setting-item-box name="标签类型" :alone="true">
      <n-select size="small" v-model:value="optionData.tabType" :options="tabTypeOptions" />
    </setting-item-box>
    <setting-item-box name="默认值" :alone="true">
       <n-select size="small" v-model:value="optionData.tabLabel" value-field="label" :options="optionData.dataset" />
    </setting-item-box>
  </collapse-item>
</template>

<script lang="ts" setup>
import { uiText } from '@/i18n/display'
import { PropType } from 'vue'
import { CollapseItem, SettingItemBox } from '@/components/Pages/ChartItemSetting'
import { option } from './config'

defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const tabTypeOptions = [
  {
    get label() { return uiText("线条") },
    value: 'bar'
  },
  {
    get label() { return uiText("分段") },
    value: 'segment'
  }
]
</script>
