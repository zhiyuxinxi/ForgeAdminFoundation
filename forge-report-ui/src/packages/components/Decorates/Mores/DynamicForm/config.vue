<template>
  <CollapseItem name="动态表单" :expanded="true">
    <SettingItemBox name="接口">
      <SettingItem name="标题">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="提交地址">
        <n-input v-model:value="optionData.submitUrl" size="small" placeholder="/forge-report-api/xxx" />
      </SettingItem>
      <SettingItem name="请求方式">
        <n-select v-model:value="optionData.submitMethod" size="small" :options="methodOptions" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="字段">
      <SettingItem name="表单字段">
        <div class="setting-line">
          <n-button size="tiny" secondary type="primary" @click="editorVisible = true">{{ $ui("配置") }}</n-button>
          <span>{{ optionData.fields?.length || 0 }} {{ $ui("项") }}</span>
        </div>
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>

  <n-modal v-model:show="editorVisible" preset="card" :title="$ui(&quot;配置表单字段&quot;)" :bordered="false" class="form-field-modal">
    <div class="modal-toolbar">
      <span>{{ $ui("配置弹窗/二级页里的提交表单字段。") }}</span>
      <n-button size="small" type="primary" @click="addField">{{ $ui("新增字段") }}</n-button>
    </div>
    <div class="field-list">
      <div v-for="(field, index) in optionData.fields" :key="index" class="field-card">
        <div class="field-head">
          <strong>{{ $ui("字段") }} {{ index + 1 }}</strong>
          <n-button size="tiny" quaternary type="error" @click="optionData.fields.splice(index, 1)">{{ $ui("删除") }}</n-button>
        </div>
        <n-grid :cols="2" :x-gap="12" :y-gap="10">
          <n-grid-item>
            <div class="field-label">{{ $ui("显示名称") }}</div>
            <n-input v-model:value="field.label" size="small" />
          </n-grid-item>
          <n-grid-item>
            <div class="field-label">{{ $ui("字段名") }}</div>
            <n-input v-model:value="field.field" size="small" />
          </n-grid-item>
          <n-grid-item>
            <div class="field-label">{{ $ui("类型") }}</div>
            <n-select v-model:value="field.type" size="small" :options="fieldTypeOptions" />
          </n-grid-item>
          <n-grid-item>
            <div class="field-label">{{ $ui("必填") }}</div>
            <n-switch v-model:value="field.required" size="small" />
          </n-grid-item>
        </n-grid>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { PropType, ref } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { ensureArray, ensureObject } from '@/packages/components/common/configCompat'
import { option } from './config'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

ensureArray(props.optionData, 'fields', option.fields)
ensureObject(props.optionData, 'style', option.style)

const methodOptions = [
  { label: 'POST', value: 'post' },
  { label: 'PUT', value: 'put' }
]

const editorVisible = ref(false)
const fieldTypeOptions = [
  { get label() { return uiText("输入框") }, value: 'input' },
  { get label() { return uiText("多行文本") }, value: 'textarea' },
  { get label() { return uiText("下拉选择") }, value: 'select' },
  { get label() { return uiText("日期") }, value: 'date' },
  { get label() { return uiText("数字") }, value: 'number' }
]

const addField = () => {
  props.optionData.fields.push({
    label: '新字段',
    field: `field${props.optionData.fields.length + 1}`,
    type: 'input'
  })
}
</script>

<style scoped lang="scss">
.setting-line,
.modal-toolbar,
.field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

:deep(.form-field-modal) {
  width: min(760px, 92vw);
}

.modal-toolbar {
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-card {
  padding: 12px;
  border: 1px solid rgba(120, 172, 255, 0.18);
  border-radius: 8px;
  background: rgba(13, 20, 38, 0.72);
}

.field-head {
  margin-bottom: 10px;
}

.field-label {
  margin-bottom: 5px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
}
</style>
