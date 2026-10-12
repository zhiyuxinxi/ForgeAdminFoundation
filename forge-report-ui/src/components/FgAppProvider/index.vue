<template>
  <n-config-provider :locale="locale" :date-locale="dateLocale">
  <n-loading-bar-provider>
    <n-dialog-provider>
      <dialog-content></dialog-content>
      <loading-content></loading-content>
      <n-notification-provider>
        <n-message-provider>
          <message-content></message-content>
          <slot></slot>
        </n-message-provider>
      </n-notification-provider>
    </n-dialog-provider>
  </n-loading-bar-provider>
  </n-config-provider>
</template>

<script lang="ts" setup>
import {
  NConfigProvider,
  NDialogProvider,
  NNotificationProvider,
  NMessageProvider,
  NLoadingBarProvider
} from 'naive-ui'

import { MessageContent } from '@/components/Plugins/MessageContent'
import { DialogContent } from '@/components/Plugins/DialogContent'
import { LoadingContent } from '@/components/Plugins/LoadingContent'
import { useLang } from '@/hooks/useLang.hook'
const { locale, dateLocale } = useLang()

</script>
