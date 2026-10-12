<template>
  <NSelect
    class="language-switcher"
    :value="locale"
    :options="translateUiMetadata(options)"
    :aria-label="t('language.label')"
    size="small"
    @update:value="setLocale"
  />
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { NSelect } from 'naive-ui'
import { locale, setLocale, t } from '@/locales'

const options = [
  { label: '简体中文', value: 'zh-CN' },
  { label: 'English', value: 'en-US' },
]
</script>

<style scoped>
.language-switcher {
  width: 112px;
  flex: 0 0 112px;
  margin-inline: 8px;
}
</style>
