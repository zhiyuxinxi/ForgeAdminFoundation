<template>
  <div class="legacy-config-page">
    <n-result
      status="info"
      :title="translateUiText(&quot;配置入口已迁移&quot;)"
      :description="translateUiText(&quot;业务页面配置、功能代码预览和下载已统一收敛到应用管理。&quot;)"
    >
      <template #footer>
        <n-space justify="center">
          <n-button type="primary" @click="router.push('/app-center')">
            {{ translateUiText("进入应用管理") }}
          </n-button>
        </n-space>
      </template>
    </n-result>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.legacy-config-page {
  display: grid;
  min-height: 100%;
  place-items: center;
  background: #f6f8fb;
  padding: 24px;
}
</style>
