<template>
  <section class="settings-section-card">
    <header>
      <h2>{{ translateUiText("基础属性") }}</h2>
      <p>{{ translateUiText("这些信息用于应用中心、门户顶栏和发布快照。") }}</p>
    </header>
    <n-form label-placement="top" :model="modelValue">
      <n-grid :cols="2" :x-gap="16" responsive="screen">
        <n-form-item-gi :label="translateUiText(&quot;应用名称&quot;)" required>
          <n-input :value="modelValue.applicationName" :placeholder="translateUiText(&quot;请输入应用名称&quot;)" @update:value="patch({ applicationName: $event })" />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;应用编码&quot;)">
          <n-input :value="modelValue.applicationCode" disabled />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;应用图标&quot;)">
          <IconSelector :model-value="modelValue.icon" @update:model-value="patch({ icon: $event })" />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;启用状态&quot;)">
          <div class="settings-switch-row">
            <n-switch :value="modelValue.status" :checked-value="1" :unchecked-value="0" @update:value="patch({ status: $event })" />
            <span>{{ modelValue.status === 1 ? translateUiText("应用已启用") : translateUiText("应用已停用") }}</span>
          </div>
        </n-form-item-gi>
      </n-grid>
      <n-form-item :label="translateUiText(&quot;应用说明&quot;)">
        <n-input
          :value="modelValue.description"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 6 }"
          maxlength="500"
          show-count
          @update:value="patch({ description: $event })"
        />
      </n-form-item>
      <n-divider title-placement="left">
        {{ translateUiText("应用主题") }}
      </n-divider>
      <n-form-item :label="translateUiText(&quot;主题色&quot;)">
        <div class="theme-color-row">
          <button
            v-for="color in presetColors"
            :key="color"
            type="button"
            class="theme-color-swatch"
            :class="{ active: modelValue.themeColor === color }"
            :style="{ backgroundColor: color }"
            :aria-label="`选择主题色 ${color}`"
            @click="patch({ themeColor: color })"
          />
          <n-color-picker
            :value="modelValue.themeColor"
            :modes="['hex']"
            :show-alpha="false"
            class="theme-color-picker"
            @update:value="patch({ themeColor: $event })"
          />
        </div>
      </n-form-item>
      <n-divider title-placement="left">
        {{ translateUiText("应用水印") }}
      </n-divider>
      <n-grid :cols="2" :x-gap="16" responsive="screen">
        <n-form-item-gi :label="translateUiText(&quot;启用水印&quot;)">
          <n-switch :value="modelValue.watermark?.enabled" @update:value="patchWatermark({ enabled: $event })" />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;水印范围&quot;)">
          <n-radio-group :value="modelValue.watermark?.scope" :disabled="!modelValue.watermark?.enabled" @update:value="patchWatermark({ scope: $event })">
            <n-radio-button value="content">
              {{ translateUiText("内容区") }}
            </n-radio-button>
            <n-radio-button value="full">
              {{ translateUiText("全屏") }}
            </n-radio-button>
          </n-radio-group>
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;自定义文字&quot;)">
          <n-input
            :value="modelValue.watermark?.text"
            :disabled="!modelValue.watermark?.enabled"
            maxlength="50"
            :placeholder="translateUiText(&quot;例如：内部资料&quot;)"
            @update:value="patchWatermark({ text: $event })"
          />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;动态信息&quot;)">
          <n-space>
            <n-checkbox :checked="modelValue.watermark?.showUsername" :disabled="!modelValue.watermark?.enabled" @update:checked="patchWatermark({ showUsername: $event })">
              {{ translateUiText("用户名") }}
            </n-checkbox>
            <n-checkbox :checked="modelValue.watermark?.showTime" :disabled="!modelValue.watermark?.enabled" @update:checked="patchWatermark({ showTime: $event })">
              {{ translateUiText("当前时间") }}
            </n-checkbox>
          </n-space>
        </n-form-item-gi>
      </n-grid>
    </n-form>
  </section>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import IconSelector from '@/components/IconSelector.vue'

const props = defineProps({ modelValue: { type: Object, required: true } })
const emit = defineEmits(['update:modelValue'])
const presetColors = ['#3370ff', '#165dff', '#0f766e', '#7c3aed', '#c2410c', '#be123c']

function patch(value) {
  emit('update:modelValue', { ...props.modelValue, ...value })
}

function patchWatermark(value) {
  patch({ watermark: { ...(props.modelValue.watermark || {}), ...value } })
}
</script>

<style scoped>
.settings-switch-row,
.theme-color-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.settings-switch-row span {
  color: var(--text-secondary, #646a73);
  font-size: 13px;
}

.theme-color-swatch {
  width: 28px;
  height: 28px;
  border: 3px solid #fff;
  border-radius: 7px;
  box-shadow: 0 0 0 1px #d9dce1;
  cursor: pointer;
}

.theme-color-swatch.active {
  box-shadow: 0 0 0 2px var(--primary-color, #3370ff);
}

.theme-color-picker {
  width: 116px;
}
</style>
