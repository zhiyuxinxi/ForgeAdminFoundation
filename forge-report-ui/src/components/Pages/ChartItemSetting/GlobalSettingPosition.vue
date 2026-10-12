<template>
  <setting-item-box v-if="targetData" name="位置">
    <setting-item :name="$ui(&quot;偏移 X：{0}px&quot;, { 0: (targetData.left || 0) })">
      <n-input-number v-model:value="targetData.left" size="small" step="10"></n-input-number>
    </setting-item>
    <setting-item :name="$ui(&quot;偏移 Y：{0}px&quot;, { 0: (targetData.top || 0) })">
      <n-input-number v-model:value="targetData.top" size="small" step="10"></n-input-number>
    </setting-item>
  </setting-item-box>
</template>

<script setup lang="ts">
import { PropType, reactive } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'

type positionType = {
  top?: number | string | null
  left?: number | string | null
  right?: number | string | null
  bottom?: number | string | null
}

const props = defineProps({
  targetData: {
    type: Object as PropType<positionType>,
    required: true
  }
})
</script>
