<template>
  <view class="language-switch" role="group" :aria-label="L('语言')">
    <button class="language-switch__option" :class="{ active: locale === 'zh-CN' }" :aria-pressed="locale === 'zh-CN'" @click="setLocale('zh-CN')">中文</button>
    <button class="language-switch__option" :class="{ active: locale === 'en-US' }" :aria-pressed="locale === 'en-US'" @click="setLocale('en-US')">English</button>
  </view>
</template>

<script setup>
import { L, locale, setLocale } from '@/locales'
</script>

<style scoped>
.language-switch { display: flex; gap: 8rpx; align-items: center; }
.language-switch__option { margin: 0; padding: 8rpx 14rpx; font-size: 24rpx; line-height: 1.5; color: #64748b; background: transparent; }
.language-switch__option::after { border: 0; }
.language-switch__option.active { color: #2563eb; background: #eff6ff; border-radius: 12rpx; }
</style>
