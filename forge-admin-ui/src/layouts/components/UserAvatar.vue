<template>
  <n-dropdown :options="dropdownOptions" @select="handleSelect">
    <div id="user-dropdown" class="flex cursor-pointer items-center">
      <n-avatar
        v-if="avatarSrc"
        round
        :size="28"
        :src="avatarSrc"
        @error="handleAvatarError"
      />
      <span
        v-else
        class="avatar-fallback"
        role="img"
        :aria-label="avatarText || localizeLabel('用户头像')"
      >{{ avatarText }}</span>
      <div v-if="userStore.userInfo || userStore.staffInfo" class="ml-8 flex-col flex-shrink-0 items-center">
        <span class="text-14">{{ userStore.realName || userStore.staffInfo?.staffName }}</span>
      </div>
    </div>
  </n-dropdown>
</template>

<script setup>
import api from '@/api'

import { defaultThemeConfig } from '@/config/theme.config.js'
import { localizeLabel } from '@/locales'
import { useAuthStore, useUserStore } from '@/store'
import { resolveRenderableFileUrl } from '@/utils/file'
import { isSilentAuthError } from '@/utils/http/helpers'

const props = defineProps({
  /**
   * 应用门户提供应用内个人资料入口，避免把用户带回系统布局。
   * 未传入时保持系统布局原有的 /profile 路由。
   */
  profileRoute: {
    type: [String, Object, Function],
    default: null,
  },
})

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()

const avatarSrc = ref('')
const avatarText = computed(() => {
  const name = userStore.realName || userStore.username
  return name ? name.charAt(0) : 'U'
})

const dropdownOptions = computed(() => {
  const baseOptions = []
  baseOptions.push(
    {
      get label() { return localizeLabel('个人资料') },
      key: 'profile',
      icon: () => h('i', { class: 'i-material-symbols:person-outline text-14' }),
    },
    {
      get label() { return localizeLabel('退出登录') },
      key: 'logout',
      icon: () => h('i', { class: 'i-mdi:exit-to-app text-14' }),
    },
  )
  return baseOptions
})

async function loadAvatar() {
  const avatar = userStore.avatar
  if (!avatar) {
    avatarSrc.value = ''
    return
  }
  try {
    avatarSrc.value = await resolveRenderableFileUrl(avatar)
  }
  catch {
    avatarSrc.value = ''
  }
}

function handleAvatarError() {
  avatarSrc.value = ''
}

function handleSelect(key) {
  switch (key) {
    case 'profile':
      {
        const target = typeof props.profileRoute === 'function'
          ? props.profileRoute()
          : props.profileRoute
        if (target)
          router.push(target)
        else
          router.push('/profile')
      }
      break
    case 'logout':
      $dialog.confirm({
        get 'title'() { return localizeLabel('提示') },
        'type': 'info',
        get 'content'() { return localizeLabel('确认退出？') },
        'positive-button-props': {
          color: defaultThemeConfig.primaryColor,
        },
        async confirm() {
          authStore.beginLogout()
          try {
            await api.logout()
          }
          catch (error) {
            if (!isSilentAuthError(error))
              console.error('logout error', error)
          }
          authStore.logout()
          $message.success(localizeLabel('已退出登录'))
        },
      })
      break
  }
}

watch(() => userStore.avatar, () => {
  loadAvatar()
}, { immediate: true })
</script>

<style scoped>
/* 无头像时的首字母圆形：颜色跟随所在头部/布局当前文字颜色
   （彩色/深色头部 → 浅色半透明圆底 + 浅色字；浅色头部 → 深色字），
   不再固定使用主题主色，避免与布局头部颜色冲突。 */
.avatar-fallback {
  display: inline-flex;
  flex: 0 0 28px;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: currentColor;
  background: color-mix(in srgb, currentColor 16%, transparent);
  border: 1px solid color-mix(in srgb, currentColor 34%, transparent);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  user-select: none;
}
</style>
