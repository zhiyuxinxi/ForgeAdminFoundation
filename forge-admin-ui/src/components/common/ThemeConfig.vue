<template>
  <n-drawer v-model:show="visible" :width="400" placement="right">
    <n-drawer-content :title="translateUiText(&quot;主题配置&quot;)" closable>
      <n-tabs type="line" animated>
        <!-- Header 配置 -->
        <n-tab-pane name="header" tab="Header">
          <n-form label-placement="left" label-width="120">
            <n-form-item :label="translateUiText(&quot;背景颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.header.backgroundColor"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;文字颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.header.textColor"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;字体大小&quot;)">
              <n-input-number
                v-model:value="fontSize.header"
                :min="12"
                :max="20"
                @update:value="(val) => updateFontSize('header', val)"
              >
                <template #suffix>
                  px
                </template>
              </n-input-number>
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;边框颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.header.borderColor"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 顶部菜单配置 -->
        <n-tab-pane name="topMenu" :tab="translateUiText(&quot;顶部菜单&quot;)">
          <n-form label-placement="left" label-width="120">
            <n-form-item :label="translateUiText(&quot;文字颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.topMenu.textColor"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;悬停颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.topMenu.textColorHover"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;选中文字色&quot;)">
              <n-color-picker
                v-model:value="localConfig.topMenu.textColorActive"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;选中悬停色&quot;)">
              <n-color-picker
                v-model:value="localConfig.topMenu.textColorActiveHover"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;Horizontal选中色&quot;)">
              <n-color-picker
                v-model:value="localConfig.topMenu.textColorActiveHorizontal"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;背景悬停色&quot;)">
              <n-color-picker
                v-model:value="localConfig.topMenu.backgroundColorHover"
                :modes="['hex', 'rgb']"
                :show-alpha="true"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;背景选中色&quot;)">
              <n-color-picker
                v-model:value="localConfig.topMenu.backgroundColorActive"
                :modes="['hex', 'rgb']"
                :show-alpha="true"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;选中悬停背景&quot;)">
              <n-color-picker
                v-model:value="localConfig.topMenu.backgroundColorActiveHover"
                :modes="['hex', 'rgb']"
                :show-alpha="true"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;字体大小&quot;)">
              <n-input-number
                v-model:value="fontSize.topMenu"
                :min="12"
                :max="18"
                @update:value="(val) => updateFontSize('topMenu', val)"
              >
                <template #suffix>
                  px
                </template>
              </n-input-number>
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 侧边菜单配置 -->
        <n-tab-pane name="sideMenu" :tab="translateUiText(&quot;侧边菜单&quot;)">
          <n-form label-placement="left" label-width="120">
            <n-form-item :label="translateUiText(&quot;背景颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.backgroundColor"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;文字颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.textColor"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;悬停颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.textColorHover"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;选中文字色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.textColorActive"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;父级提示文字&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.parentTextColorActive"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;父级提示背景&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.parentBackgroundColorActive"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;背景悬停色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.backgroundColorHover"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;背景选中色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.backgroundColorActive"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;图标颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.iconColor"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;选中图标色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.iconColorActive"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;字体大小&quot;)">
              <n-input-number
                v-model:value="fontSize.sideMenu"
                :min="12"
                :max="18"
                @update:value="(val) => updateFontSize('sideMenu', val)"
              >
                <template #suffix>
                  px
                </template>
              </n-input-number>
            </n-form-item>
            <n-form-item :label="translateUiText(&quot;边框颜色&quot;)">
              <n-color-picker
                v-model:value="localConfig.sideMenu.borderColor"
                :modes="['hex']"
                @update:value="handleUpdateConfig"
              />
            </n-form-item>
          </n-form>
        </n-tab-pane>
      </n-tabs>

      <template #footer>
        <n-space>
          <n-button type="primary" @click="handleApply">
            {{ translateUiText("应用主题") }}
          </n-button>
          <n-button @click="handleReset">
            {{ translateUiText("重置为默认") }}
          </n-button>
          <n-button @click="handleExport">
            {{ translateUiText("导出配置") }}
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { defaultThemeConfig } from '@/config/theme.config'
import { useAppStore } from '@/store'

const appStore = useAppStore()
const visible = ref(false)

// 本地配置（深拷贝）
const localConfig = ref(JSON.parse(JSON.stringify(appStore.themeConfig)))

// 字体大小（转换为数字）
const fontSize = ref({
  header: Number.parseInt(localConfig.value.header.fontSize) || 14,
  topMenu: Number.parseInt(localConfig.value.topMenu.fontSize) || 14,
  sideMenu: Number.parseInt(localConfig.value.sideMenu.fontSize) || 14,
})

// 更新字体大小
function updateFontSize(type, value) {
  localConfig.value[type].fontSize = `${value}px`
  handleUpdateConfig()
}

// 更新配置
function handleUpdateConfig() {
  // 实时预览
  appStore.setThemeConfig(localConfig.value)
}

// 应用主题
function handleApply() {
  appStore.setThemeConfig(localConfig.value)
  $message.success(translateUiText("主题已应用"))
  visible.value = false
}

// 重置为默认
function handleReset() {
  localConfig.value = JSON.parse(JSON.stringify(defaultThemeConfig))
  fontSize.value = {
    header: 14,
    topMenu: 14,
    sideMenu: 14,
  }
  handleUpdateConfig()
  $message.success(translateUiText("已重置为默认主题"))
}

// 导出配置
function handleExport() {
  const configStr = JSON.stringify(localConfig.value, null, 2)
  const blob = new Blob([configStr], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'theme-config.json'
  a.click()
  URL.revokeObjectURL(url)
  $message.success(translateUiText("配置已导出"))
}

// 打开配置面板
function open() {
  localConfig.value = JSON.parse(JSON.stringify(appStore.themeConfig))
  fontSize.value = {
    header: Number.parseInt(localConfig.value.header.fontSize) || 14,
    topMenu: Number.parseInt(localConfig.value.topMenu.fontSize) || 14,
    sideMenu: Number.parseInt(localConfig.value.sideMenu.fontSize) || 14,
  }
  visible.value = true
}

// 暴露方法
defineExpose({
  open,
})
</script>
