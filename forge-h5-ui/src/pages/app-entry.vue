<template>
  <AiLayoutPage :title="L(title)" :subtitle="L(&quot;已从全部应用打开&quot;)">
    <view class="app-entry">
      <view class="app-entry__icon">
        <AiIcon icon="/static/icons/ai-icon/layout.svg" color="#2563eb" size="lg" />
      </view>
      <text class="app-entry__title">{{ L(title) }}</text>
      <text class="app-entry__desc">{{ L("该功能已由后台菜单授权。移动端页面完成配置后，将自动从这里进入。") }}</text>
      <AiButton block @click="goHome">{{ L("返回首页") }}</AiButton>
    </view>
  </AiLayoutPage>
</template>

<script setup>
import { L } from '@/locales'
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiButton from '@/components/AiButton.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiLayoutPage from '@/components/AiLayoutPage.vue'
import { buildRoutePayload, unpackRoutePayload } from '@/utils/route-payload'

const title = ref('应用功能')

onLoad((query = {}) => {
  // 单次传输协议：解包 versioned base64url payload（只解包一次，不做递归 URL decode）；
  // 缺失/非法时安全降级为默认值；无 q 的既有深链走 legacy 只读严格校验。
  const params = unpackRoutePayload(query || {})
  title.value = params.title || '应用功能'
  const { configKey, path, mode, recordId } = params
  // 安全边界：path 仅用于解析本地低代码 configKey，绝不按 path 跳转或重定向到任意外部地址
  if (configKey || /(?:crud-page|crud)\//.test(path)) {
    const payload = buildRoutePayload({
      configKey: configKey || resolveConfigKey(path),
      title: title.value,
      ...(mode ? { mode } : {}),
      ...(recordId ? { recordId } : {}),
    })
    if (payload) {
      uni.redirectTo({ url: `/pages/lowcode-runtime?q=${payload}` })
    }
  }
})

function resolveConfigKey(path) {
  return String(path || '').match(/(?:crud-page|crud)\/([^/?]+)/)?.[1] || ''
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.app-entry { display: flex; min-height: 560rpx; flex-direction: column; align-items: center; justify-content: center; padding: 48rpx 28rpx; text-align: center; box-sizing: border-box; }
.app-entry__icon { display: flex; width: 100rpx; height: 100rpx; align-items: center; justify-content: center; border-radius: 26rpx; background: #eaf3ff; }
.app-entry__title { display: block; margin-top: 26rpx; color: var(--text-strong); font-size: 34rpx; font-weight: 750; }
.app-entry__desc { display: block; max-width: 520rpx; margin: 14rpx 0 32rpx; color: var(--text-muted); font-size: 25rpx; line-height: 1.65; }
</style>
