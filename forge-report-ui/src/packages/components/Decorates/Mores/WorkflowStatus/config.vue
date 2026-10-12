<template>
  <CollapseItem name="流程状态" :expanded="true">
    <SettingItemBox name="内容">
      <SettingItem name="标题">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="当前步骤">
        <n-input-number v-model:value="optionData.activeIndex" size="small" :min="0" :max="20" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="数据">
      <SettingItem name="接口地址">
        <n-input v-model:value="optionData.dataSource.url" size="small" placeholder="/forge-report-api/xxx" />
      </SettingItem>
      <SettingItem name="请求方式">
        <n-select v-model:value="optionData.dataSource.method" size="small" :options="methodOptions" />
      </SettingItem>
      <SettingItem name="数据路径">
        <n-input v-model:value="optionData.dataSource.dataPath" size="small" placeholder="data" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { ensureObject } from '@/packages/components/common/configCompat'
import { option } from './config'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

ensureObject(props.optionData, 'dataSource', option.dataSource)
ensureObject(props.optionData, 'style', option.style)

const methodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' }
]
</script>
