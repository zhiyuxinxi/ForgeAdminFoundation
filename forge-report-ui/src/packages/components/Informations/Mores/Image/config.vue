<template>
  <collapse-item name="属性" :expanded="true">
    <setting-item-box name="素材" :alone="true">
      <setting-item>
        <material-asset-selector
          :value="optionData.dataset"
          button-text="从素材库选择图片"
          @confirm="handleMaterialSelect"
          @clear="handleMaterialClear"
        />
      </setting-item>
    </setting-item-box>
    <setting-item-box name="路径" :alone="true">
      <setting-item>
        <n-input v-model:value="optionData.dataset" size="small" :placeholder="$ui(&quot;可手填外链，或从素材库选择&quot;)"></n-input>
      </setting-item>
    </setting-item-box>
    <setting-item-box name="样式">
      <setting-item name="类型">
        <n-select
          v-model:value="optionData.fit"
          size="small"
          :options="fitList"
        ></n-select>
      </setting-item>
      <setting-item name="圆角">
        <n-input-number
          v-model:value="optionData.borderRadius"
          size="small"
          :min="0"
          :placeholder="$ui(&quot;圆角&quot;)"
        ></n-input-number>
      </setting-item>
    </setting-item-box>
  </collapse-item>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { option } from './config'
import MaterialAssetSelector from '@/components/Pages/MaterialAssetSelector/index.vue'
import {
  CollapseItem,
  SettingItemBox,
  SettingItem,
} from '@/components/Pages/ChartItemSetting'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true,
  },
})

const handleMaterialSelect = (value: string) => {
  props.optionData.dataset = value
}

const handleMaterialClear = () => {
  props.optionData.dataset = ''
}

// 适应类型
const fitList = [
  {
    value: 'fill',
    label: 'fill'
  },
  {
    value: 'contain',
    label: 'contain'
  },
  {
    value: 'cover',
    label: 'cover'
  },
  {
    value: 'scale-down',
    label: 'scale-down'
  },
  {
    value: 'none',
    label: 'none'
  },
]
</script>
