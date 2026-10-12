<template>
  <collapse-item name="表格设置" :expanded="true">
    <n-tag type="primary">{{ $ui("若配置无响应，请在预览页面查看效果") }}</n-tag>
    <setting-item-box :alone="true" name="对齐方式">
      <setting-item :alone="true">
        <n-select
          v-model:value="optionData.align"
          size="small"
          :options="[
            { label: '靠左', value: 'left' },
            { label: '居中', value: 'center' },
            { label: '靠右', value: 'right' }
          ]"
        />
      </setting-item>
    </setting-item-box>
    <setting-item-box :alone="false" name="分页设置">
      <setting-item name="默认页码" :alone="true">
        <n-input-number v-model:value="optionData.pagination.page" size="small" :placeholder="$ui(&quot;字体大小&quot;)"></n-input-number>
      </setting-item>
      <setting-item name="分页" :alone="true">
        <n-select v-model:value="optionData.pagination.pageSize" size="small" :options="page" />
      </setting-item>
    </setting-item-box>
    <setting-item-box :alone="false" name="表格数据">
      <SettingItem name="表头名称" class="form_name">
        <div style="width: 260px">
          <n-input v-model:value="header" size="small" :placeholder="$ui(&quot;表头数据(英文','分割)&quot;)"></n-input>
        </div>
      </SettingItem>
    </setting-item-box>
    <setting-item-box :alone="false" name="表格样式">
      <SettingItem name="显示边框" :alone="true">
        <n-select v-model:value="(optionData as any).style.border" size="small" :options="borderFlag" />
      </SettingItem>
      <SettingItem name="底部边框" :alone="true">
        <n-select
          v-model:value="(optionData as any).style.bottomBordered"
          size="small"
          :options="bottom_borderedFlag"
        />
      </SettingItem>
      <SettingItem name="列分割线" :alone="true">
        <n-select v-model:value="(optionData as any).style.singleLine" size="small" :options="columnFlag" />
      </SettingItem>
      <SettingItem name="行分割线" :alone="true">
        <n-select v-model:value="(optionData as any).style.singleColumn" size="small" :options="lineFlag" />
      </SettingItem>
      <SettingItem name="斑马条纹" :alone="true">
        <n-select v-model:value="(optionData as any).style.striped" size="small" :options="stripedFlag" />
      </SettingItem>
      <setting-item name="字体大小" :alone="true">
        <n-input-number
          v-model:value="optionData.style.fontSize"
          :min="12"
          size="small"
          :placeholder="$ui(&quot;字体大小&quot;)"
        ></n-input-number>
      </setting-item>
      <setting-item name="边框宽度" :alone="true">
        <n-input-number
          v-model:value="optionData.style.borderWidth"
          :min="0"
          size="small"
          :placeholder="$ui(&quot;字体大小&quot;)"
        ></n-input-number>
      </setting-item>
      <setting-item name="边框颜色" :alone="true">
        <n-color-picker size="small" :modes="['rgb']" v-model:value="optionData.style.borderColor"></n-color-picker>
      </setting-item>
      <setting-item name="边框样式" :alone="true">
        <n-select v-model:value="optionData.style.borderStyle" size="small" :options="borderStyleFlag" />
      </setting-item>
      <SettingItem name="表格搜索（前端静态搜索）" :alone="true">
        <n-select v-model:value="optionData.inputShow" size="small" :options="inputSelect" />
      </SettingItem>
    </setting-item-box>
  </collapse-item>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { PropType, watch, ref } from 'vue'
import { option } from './config'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'

const page = [
  { label: '2', value: 2 },
  { label: '5', value: 5 },
  { label: '10', value: 10 },
  { label: '15', value: 15 },
  { label: '30', value: 30 }
]
const borderFlag = [
  { get label() { return uiText("显示") }, value: 'on' },
  { get label() { return uiText("不显示") }, value: 'off' }
]
const columnFlag = [
  { get label() { return uiText("显示") }, value: 'off' },
  { get label() { return uiText("不显示") }, value: 'on' }
]
const lineFlag = [
  { get label() { return uiText("显示") }, value: 'off' },
  { get label() { return uiText("不显示") }, value: 'on' }
]
const bottom_borderedFlag = [
  { get label() { return uiText("显示") }, value: 'on' },
  { get label() { return uiText("不显示") }, value: 'off' }
]
const stripedFlag = [
  { get label() { return uiText("显示") }, value: 'on' },
  { get label() { return uiText("不显示") }, value: 'off' }
]
const borderStyleFlag = [
  { get label() { return uiText("实线边框") }, value: 'solid' },
  { get label() { return uiText("虚线边框") }, value: 'dashed' },
  { get label() { return uiText("点状边框") }, value: 'dotted' },
  { get label() { return uiText("双线边框") }, value: 'double' }
]
const inputSelect = [
  { get label() { return uiText("停用") }, value: 'none' },
  { get label() { return uiText("启用") }, value: 'flex' }
]
const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const header = ref()
const median = ref<string[]>([])
props.optionData.dataset.dimensions.forEach(item => {
  median.value.push(item.title)
})

//转string
watch(
  () => props.optionData,
  () => {
    median.value = []
    props.optionData.dataset.dimensions.forEach(item => {
      median.value.push(item.title)
    })
    header.value = median.value.toString()
  },
  {
    deep: false,
    immediate: true
  }
)

//更新columns
watch([header], ([headerNew], [headerOld]) => {
  if (headerNew !== headerOld) {
    headerNew.split(',').forEach((item: string, index: number) => {
      if (index + 1 <= props.optionData.dataset.dimensions.length) {
        props.optionData.dataset.dimensions[index].title = headerNew.split(',')[index]
      }
    })
  }
})
</script>
