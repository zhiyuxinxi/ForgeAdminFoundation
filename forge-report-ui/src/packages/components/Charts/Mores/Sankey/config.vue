<template>
  <div>
    <CollapseItem name="桑基图" :expanded="true">
      <SettingItemBox name="样式">
        <SettingItem name="方向">
          <n-select
            v-model:value="sankeyConfig.orient"
            size="small"
            :options="orientList"
            :placeholder="$ui(&quot;选择方向&quot;)"
          />
        </SettingItem>
        <SettingItem name="提示标签">
          <n-select
            v-model:value="optionData.tooltip.show"
            size="small"
            :options="toolTipSwitch"
            :placeholder="$ui(&quot;是否开启&quot;)"
          />
        </SettingItem>
      </SettingItemBox>
    </CollapseItem>
  </div>
</template>

<script setup lang="ts">
import { PropType, computed } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { option, orientList, toolTipSwitch } from './config'
import { GlobalThemeJsonType } from '@/settings/chartThemes/index'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option & GlobalThemeJsonType>,
    required: true
  }
})

const sankeyConfig = computed<typeof option.series>(() => {
  return props.optionData.series
})

</script>
