<template>
  <n-dropdown
    trigger="hover"
    @select="handleSelect"
    :show-arrow="true"
    :options="options"
  >
    <div class="user-info-box">
      <person-icon v-if="fallback"></person-icon>
      <n-avatar
        v-if="!fallback"
        round
        object-fit="cover"
        size="medium"
        :src="Person"
        @error="errorHandle"
      ></n-avatar>
    </div>
  </n-dropdown>

  <!-- 系统设置 model -->
  <fg-system-set v-model:modelShow="modelShow"></fg-system-set>
  <!-- 关于软件 model -->
  <fg-system-info v-model:modelShow="modelShowInfo"></fg-system-info>
</template>

<script lang="ts" setup>
import { uiText } from '@/i18n/display'
import { h, ref, computed } from 'vue'
import { NAvatar, NText } from 'naive-ui'
import { renderIcon } from '@/utils'
import { logout, renderLang, goDialog } from '@/utils'
import { FgSystemSet } from '@/components/FgSystemSet/index'
import { FgSystemInfo } from '@/components/FgSystemInfo/index'
import { useUserStore } from '@/store/modules/userStore/userStore'
import Person from './person.png'

import { icon } from '@/plugins'
const {
  ChatboxEllipsesIcon,
  PersonIcon,
  LogOutOutlineIcon,
  SettingsSharpIcon
} = icon.ionicons5

const userStore = useUserStore()
const t = window['$t']

const modelShowInfo = ref(false)
const modelShow = ref(false)

const fallback = ref(false)

const displayName = computed(() => userStore.realName || userStore.username || '管理员')

const renderUserInfo = () => {
  return h(
    'div',
    {
      style: 'display: flex; align-items: center; padding: 8px 12px;'
    },
    [
      h(NAvatar, {
        round: true,
        style: 'margin-right: 12px;',
        src: Person
      }),
      h('div', null, [
        h('div', null, [
          h(NText, { depth: 2 }, { default: () => displayName.value })
        ])
      ])
    ]
  )
}
const options = ref([
  {
    get label() { return uiText("我的信息") },
    key: 'info',
    type: 'render',
    render: renderUserInfo
  },
  {
    type: 'divider',
    key: 'd1'
  },
  {
    label: renderLang('global.sys_set'),
    key: 'sysSet',
    icon: renderIcon(SettingsSharpIcon)
  },
  // {
  //   label: renderLang('global.contact'),
  //   key: 'contact',
  //   icon: renderIcon(ChatboxEllipsesIcon)
  // },
  {
    type: 'divider',
    key: 'd3'
  },
  {
    label: renderLang('global.logout'),
    key: 'logout',
    icon: renderIcon(LogOutOutlineIcon)
  }
])

// 图片渲染错误
const errorHandle = (e: Event) => {
  fallback.value = true
}

// 系统设置
const sysSetHandle = () => {
  modelShow.value = true
}

// 系统设置
const sysInfoHandle = () => {
  modelShowInfo.value = true
}

const handleSelect = (key: string) => {
  switch (key) {
    case 'contact':
      sysInfoHandle()
      break
    case 'sysSet':
      sysSetHandle()
      break
    case 'logout':
      goDialog({
        message: uiText("确定要退出登录吗？"),
        positiveText: uiText("退出"),
        negativeText: uiText("取消"),
        onPositiveCallback: () => {
          logout()
        }
      })
      break
  }
}
</script>

<style lang="scss" scoped>
.user-info-box {
  cursor: pointer;
  transform: scale(0.7);
}
</style>
