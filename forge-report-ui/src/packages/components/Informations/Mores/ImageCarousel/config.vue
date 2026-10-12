<template>
  <collapse-item name="路径" :expanded="true">
      <setting-item>
        <material-asset-selector
          :value="optionData.dataset"
          mode="multiple"
          button-text="从素材库追加图片"
          @confirm="appendMaterialAssets"
          @clear="clearMaterialAssets"
        />
      </setting-item>
      <setting-item v-for="(item, index) in optionData.dataset" :key="index">
        <n-input-group>
          <n-input v-model:value="optionData.dataset[index]" size="small" :placeholder="$ui(&quot;请输入图片地址或素材引用&quot;)"></n-input>
          <n-button ghost @click="optionData.dataset.splice(index, 1)"> - </n-button>
        </n-input-group>
      </setting-item>
      <setting-item>
        <n-button size="small" @click="optionData.dataset.push('')"> {{ $ui("+ 新增") }}</n-button>
      </setting-item>
  </collapse-item>
  <collapse-item name="轮播属性" :expanded="true">
    <setting-item-box name="播放器">
      <setting-item>
        <n-space>
          <n-switch v-model:value="optionData.autoplay" size="small" />
          <n-text>{{ $ui("自动播放") }}</n-text>
        </n-space>
      </setting-item>
      <!-- 开启自动播放时，设置间隔时间 -->
      <setting-item name="间隔时间">
        <n-input-number v-model:value="optionData.interval" size="small" placeholder="">
          <template #suffix> {{ $ui("毫秒") }} </template>
        </n-input-number>
      </setting-item>
      <setting-item name="轮播方向">
        <n-select v-model:value="optionData.direction" :options="directions" :placeholder="$ui(&quot;选择方向&quot;)" />
      </setting-item>
      <setting-item name="过渡效果">
        <n-select v-model:value="optionData.effect" :options="effects" :placeholder="$ui(&quot;效果&quot;)" />
      </setting-item>
      <setting-item name="每页数量">
        <n-input-number v-model:value="optionData.slidesPerview" size="small" placeholder=""></n-input-number>
      </setting-item>
      <setting-item>
        <n-space>
          <n-switch v-model:value="optionData.centeredSlides" size="small" />
          <n-text>{{ $ui("居中显示") }}</n-text>
        </n-space>
      </setting-item>
      <setting-item name="图片样式">
        <n-select v-model:value="optionData.fit" :options="fitList" :placeholder="$ui(&quot;样式&quot;)" />
      </setting-item>
    </setting-item-box>
    <setting-item-box name="指示器">
      <setting-item name="样式">
        <n-select v-model:value="optionData.dotType" :options="dotTypes" :placeholder="$ui(&quot;选择样式&quot;)" />
      </setting-item>
      <setting-item name="位置">
        <n-select v-model:value="optionData.dotPlacement" :options="dotPlacements" :placeholder="$ui(&quot;选择位置&quot;)" />
      </setting-item>
      <setting-item>
        <n-space>
          <n-switch v-model:value="optionData.showDots" size="small" />
          <n-text>{{ $ui("显示") }}</n-text>
        </n-space>
      </setting-item>
      <setting-item>
        <n-space>
          <n-switch v-model:value="optionData.showArrow" size="small" />
          <n-text>{{ $ui("箭头") }}</n-text>
        </n-space>
      </setting-item>
      <setting-item>
        <n-space>
          <n-switch v-model:value="optionData.draggable" size="small" />
          <n-text>{{ $ui("拖曳切换") }}</n-text>
        </n-space>
      </setting-item>
    </setting-item-box>
  </collapse-item>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { PropType } from 'vue'
import { option } from './config'
import MaterialAssetSelector from '@/components/Pages/MaterialAssetSelector/index.vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const appendMaterialAssets = (values: string[]) => {
  if (!Array.isArray(values) || !values.length) {
    return
  }
  const existing = new Set(props.optionData.dataset)
  values.forEach((value) => {
    if (!existing.has(value)) {
      props.optionData.dataset.push(value)
    }
  })
}

const clearMaterialAssets = () => {
  props.optionData.dataset = []
}

// 字典
const dotTypes = [
  {
    get label() { return uiText("点") },
    value: 'dot'
  },
  {
    get label() { return uiText("线") },
    value: 'line'
  }
]
const directions = [
  {
    get label() { return uiText("水平方向") },
    value: 'horizontal'
  },
  {
    get label() { return uiText("垂直方向") },
    value: 'vertical'
  }
]
const effects = [
  {
    label: 'slide',
    value: 'slide'
  },
  {
    label: 'fade',
    value: 'fade'
  },
  {
    label: 'card',
    value: 'card'
  },
  {
    label: 'custom',
    value: 'custom'
  }
]
const dotPlacements = [
  {
    get label() { return uiText("上边") },
    value: 'top'
  },
  {
    get label() { return uiText("下边") },
    value: 'bottom'
  },
  {
    get label() { return uiText("左边") },
    value: 'left'
  },
  {
    get label() { return uiText("右边") },
    value: 'right'
  }
]

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
  }
]
</script>
