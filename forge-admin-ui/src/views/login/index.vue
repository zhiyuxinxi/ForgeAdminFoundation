<template>
  <div class="login-container">
    <div class="login-bg-animated" />

    <div class="login-card">
      <!-- Left side - Branding -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="logo-lockup">
            <img :src="brandLogoUrl" class="logo-img" alt="Logo" @error="handleBrandLogoError">
            <div class="logo-text">
              ForgeAdmin
            </div>
          </div>

          <div class="banner">
            <n-carousel
              autoplay
              :interval="4200"
              show-arrow
              class="stack-carousel"
            >
              <div class="carousel-item">
                <div class="carousel-copy">
                  <div class="carousel-title">
                    {{ localizeLabel('开箱即用的后台模板') }}
                  </div>
                  <div class="carousel-subtitle">
                    {{ localizeLabel('常见列表、表单、详情和权限场景快速落地') }}
                  </div>
                </div>
                <img :src="loginCarouselImage" class="carousel-image" :alt="localizeLabel('后台系统能力总览')">
              </div>
              <div class="carousel-item">
                <div class="carousel-copy">
                  <div class="carousel-title">
                    {{ localizeLabel('业务对象驱动生成') }}
                  </div>
                  <div class="carousel-subtitle">
                    {{ localizeLabel('字段、操作、列表和表单配置保持一致') }}
                  </div>
                </div>
                <img :src="loginCarouselImage" class="carousel-image" :alt="localizeLabel('业务对象驱动生成')">
              </div>
              <div class="carousel-item">
                <div class="carousel-copy">
                  <div class="carousel-title">
                    {{ localizeLabel('权限与流程统一治理') }}
                  </div>
                  <div class="carousel-subtitle">
                    {{ localizeLabel('多租户、数据权限和审批流程统一接入') }}
                  </div>
                </div>
                <img :src="loginCarouselImage" class="carousel-image" :alt="localizeLabel('权限与流程统一治理')">
              </div>
              <template #arrow="{ prev, next }">
                <button type="button" class="arco-carousel-arrow-left" :title="localizeLabel('上一页')" @click="prev">
                  <i class="ai-icon:chevron-left" />
                </button>
                <button type="button" class="arco-carousel-arrow-right" :title="localizeLabel('下一页')" @click="next">
                  <i class="ai-icon:chevron-right" />
                </button>
              </template>
              <template #dots="{ total, currentIndex, to }">
                <div class="arco-carousel-dots" role="tablist">
                  <button
                    v-for="index in total"
                    :key="index"
                    type="button"
                    class="arco-carousel-dot"
                    :class="{ active: currentIndex === index - 1 }"
                    :aria-selected="currentIndex === index - 1"
                    @click="to(index - 1)"
                  />
                </div>
              </template>
            </n-carousel>
          </div>
        </div>
      </div>

      <!-- Right side - Login form -->
      <div class="login-form-wrapper">
        <LanguageSwitcher class="login-language-switcher" />
        <div class="login-form">
          <div class="form-header">
            <h2 class="form-title">
              {{ localizeLabel('登录ForgeAdmin') }}
            </h2>
            <p class="form-subtitle">
              {{ loginSubtitle }}
            </p>
          </div>

          <div class="form-body">
            <!-- Tenant -->
            <div v-if="tenantSelectOptions.length > 0" class="form-group tenant-form-group">
              <label for="tenant" class="form-label">{{ localizeLabel('登录租户') }}</label>
              <div class="tenant-select-wrapper">
                <n-select
                  id="tenant"
                  v-model:value="selectedTenantId"
                  class="modern-select"
                  :options="tenantSelectOptions"
                  :loading="tenantConfigApplying"
                  :disabled="tenantConfigApplying"
                  filterable
                  size="large"
                  :placeholder="localizeLabel('请选择登录租户')"
                />
              </div>
            </div>

            <!-- Username -->
            <div class="form-group">
              <label for="username" class="form-label">{{ localizeLabel('用户名') }}</label>
              <div class="input-wrapper">
                <n-input
                  id="username"
                  v-model:value="loginInfo.username"
                  autofocus
                  class="modern-input"
                  :placeholder="localizeLabel('请输入用户名')"
                  :maxlength="20"
                  size="large"
                >
                  <template #prefix>
                    <i class="input-icon ai-icon:user" />
                  </template>
                </n-input>
              </div>
            </div>

            <!-- Password -->
            <div class="form-group">
              <label for="password" class="form-label">{{ localizeLabel('密码') }}</label>
              <div class="input-wrapper">
                <n-input
                  id="password"
                  v-model:value="loginInfo.password"
                  class="modern-input"
                  type="password"
                  show-password-on="click"
                  :placeholder="localizeLabel('请输入密码')"
                  :maxlength="20"
                  size="large"
                  @keydown.enter="handleLogin()"
                >
                  <template #prefix>
                    <i class="input-icon ai-icon:lock" />
                  </template>
                </n-input>
              </div>
            </div>

            <!-- 验证码区域 - 根据配置显示不同类型的验证码 -->
            <div v-if="captchaEnabled" class="form-group">
              <!-- 图形验证码 -->
              <template v-if="captchaType === 'graphical'">
                <label for="captcha" class="form-label">{{ localizeLabel('验证码') }}</label>
                <div class="captcha-wrapper">
                  <div class="input-wrapper flex-1">
                    <n-input
                      id="captcha"
                      v-model:value="loginInfo.code"
                      class="modern-input"
                      :placeholder="localizeLabel('请输入验证码')"
                      :maxlength="6"
                      size="large"
                      @keydown.enter="handleLogin()"
                    >
                      <template #prefix>
                        <i class="input-icon ai-icon:key" />
                      </template>
                    </n-input>
                  </div>
                  <div
                    class="captcha-image"
                    :title="localizeLabel('点击刷新验证码')"
                    role="button"
                    tabindex="0"
                    @click="refreshCaptcha"
                    @keydown.enter="refreshCaptcha"
                  >
                    <img
                      v-if="captchaImage"
                      :src="captchaImage"
                      :alt="localizeLabel('验证码')"
                      class="captcha-img"
                    >
                    <div v-else class="captcha-loading">
                      <i class="ai-icon:loader animate-spin" />
                    </div>
                  </div>
                </div>
              </template>

              <!-- 滑块验证码 - 已改为浮层弹出形式，点击登录按钮触发 -->
              <template v-if="captchaType === 'slider'">
                <button type="button" class="slider-verify-trigger" :disabled="loading" @click="openSliderModal">
                  <div class="trigger-icon">
                    <i class="ai-icon:shield" />
                  </div>
                  <span>{{ localizeLabel('点击进行安全验证') }}</span>
                  <i class="trigger-arrow ai-icon:chevron-right" />
                </button>
              </template>

              <!-- 短信验证码 -->
              <template v-if="captchaType === 'sms'">
                <label for="phone" class="form-label">{{ localizeLabel('手机号') }}</label>
                <div class="input-wrapper mb-3">
                  <n-input
                    id="phone"
                    v-model:value="loginInfo.phone"
                    class="modern-input"
                    :placeholder="localizeLabel('请输入手机号')"
                    :maxlength="11"
                    size="large"
                  >
                    <template #prefix>
                      <i class="input-icon ai-icon:phone" />
                    </template>
                  </n-input>
                </div>
                <label for="smsCode" class="form-label">{{ localizeLabel('短信验证码') }}</label>
                <div class="captcha-wrapper">
                  <div class="input-wrapper flex-1">
                    <n-input
                      id="smsCode"
                      v-model:value="loginInfo.code"
                      class="modern-input"
                      :placeholder="localizeLabel('请输入短信验证码')"
                      :maxlength="6"
                      size="large"
                      @keydown.enter="handleLogin()"
                    >
                      <template #prefix>
                        <i class="input-icon ai-icon:key" />
                      </template>
                    </n-input>
                  </div>
                  <n-button
                    :disabled="smsCountdown > 0 || !isValidPhone"
                    class="sms-button"
                    size="large"
                    @click="sendSmsCode"
                  >
                    {{ smsCountdown > 0 ? t('login.resendCountdown', { count: smsCountdown }) : localizeLabel('获取验证码') }}
                  </n-button>
                </div>
              </template>
            </div>

            <!-- Remember me -->
            <div class="form-options">
              <n-checkbox
                :checked="isRemember"
                :on-update:checked="(val) => (isRemember = val)"
              >
                <span class="checkbox-label">{{ localizeLabel('记住我') }}</span>
              </n-checkbox>
            </div>

            <!-- Submit button -->
            <n-button
              class="login-button"
              type="primary"
              size="large"
              :loading="loading"
              block
              @click="onLoginClick()"
            >
              <span class="button-text">{{ localizeLabel('登录') }}</span>
              <i v-if="!loading" class="button-icon ai-icon:arrow-right" />
            </n-button>

            <div class="login-trust-strip">
              <span><i class="ai-icon:shield" />{{ localizeLabel('安全登录') }}</span>
              <span><i class="ai-icon:layers" />{{ localizeLabel('租户隔离') }}</span>
              <span><i class="ai-icon:check-circle" />{{ localizeLabel('操作审计') }}</span>
            </div>

            <!-- Social login buttons -->
            <div v-if="socialPlatforms.length > 0" class="social-login-section">
              <div class="social-divider">
                <span class="divider-text">{{ localizeLabel('其他登录方式') }}</span>
              </div>
              <div class="social-buttons">
                <button
                  v-for="platform in socialPlatforms"
                  :key="platform.platform"
                  class="social-button"
                  :title="platform.platformName"
                  @click="handleSocialLogin(platform.platform)"
                >
                  <img
                    v-if="platform.platformLogoBase64"
                    :src="`data:image/png;base64,${platform.platformLogoBase64}`"
                    :alt="platform.platformName"
                    class="social-icon"
                  >
                  <i v-else class="social-icon-fallback ai-icon:link" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ICP备案号 -->
    <div class="icp-record">
      <span v-if="copyrightInfo">{{ copyrightInfo }}</span>
      <a v-else href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">{{ translateUiText("蒙ICP备2026004895号") }}</a>
    </div>
  </div>

  <!-- 滑块验证浮层 -->
  <Transition name="modal">
    <div v-if="showSliderModal" class="slider-modal-overlay" @click.self="closeSliderModal">
      <div class="slider-modal">
        <!-- 关闭按钮 -->
        <button class="slider-modal-close" @click="closeSliderModal">
          <i class="ai-icon:x" />
        </button>

        <!-- 标题区 -->
        <div class="slider-modal-header">
          <div class="slider-modal-icon">
            <i class="ai-icon:shield" />
          </div>
          <h3 class="slider-modal-title">
            {{ localizeLabel('安全验证') }}
          </h3>
          <p class="slider-modal-desc">
            {{ localizeLabel('请拖动滑块到正确位置，完成拼图') }}
          </p>
        </div>

        <!-- 滑块验证组件 -->
        <div class="slider-modal-body">
          <ServerSliderCaptcha ref="slideVerifyRef" :busy="loading" @submit="onSlideSubmit" />
        </div>

        <!-- 底部辅助文字 -->
        <p class="slider-modal-tip">
          <i class="ai-icon:info" />
          {{ localizeLabel('如果拖动困难，可点击刷新重试') }}
        </p>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { useStorage } from '@vueuse/core'

import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import mainApi from '@/api'
import loginCarouselImage from '@/assets/images/login-carousel-platform.png'
import defaultLogoUrl from '@/assets/images/logo_text.png'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import { localizeLabel, t, localizeLabel as translateUiText } from '@/locales'
import { useAppStore, useAuthStore, usePermissionStore, useTenantStore, useUserStore } from '@/store'
import { lStorage } from '@/utils'
import { loadRuntimeCryptoConfig } from '@/utils/crypto/crypto-config'
import { encryptPassword, initKeyExchange } from '@/utils/crypto/key-exchange'
import { request } from '@/utils/http'
import { normalizePageTitle } from '@/utils/page-title'
import { applyTenantConfig, resolveTenantPublicAssetUrl } from '@/utils/tenant-config'
import api from './api'
import ServerSliderCaptcha from './components/ServerSliderCaptcha.vue'

const authStore = useAuthStore()
const userStore = useUserStore()
const appStore = useAppStore()
const router = useRouter()
const route = useRoute()
const userClient = import.meta.env.VITE_USER_CLIENT || 'pc'
const LOGIN_TENANT_STORAGE_KEY = 'login_selected_tenant_id'
const SOCIAL_TENANT_MAP_KEY = 'login_social_tenant_map'

const tenantOptions = ref([])
const selectedTenantId = ref(null)
const tenantConfigApplying = ref(false)
const brandLogoUrl = ref(defaultLogoUrl)
const loginConfig = ref(null)
const selectedTenantOption = computed(() => tenantOptions.value.find(item => String(item.value) === String(selectedTenantId.value)) || null)
const loginSubtitle = computed(() => selectedTenantOption.value?.tenantName
  ? t('login.tenantEntry', { name: selectedTenantOption.value.tenantName })
  : localizeLabel('请输入账号密码'))
const copyrightInfo = computed(() => normalizePageTitle(loginConfig.value?.copyrightInfo))
const tenantSelectOptions = computed(() => tenantOptions.value.map(item => ({
  label: item.label,
  value: item.value,
})))
let tenantInitCompleted = false

const loginInfo = ref({
  username: '',
  password: '',
  code: '', // 验证码
  codeKey: '', // 验证码key
  phone: '', // 手机号（短信验证码使用）
})

const captchaImage = ref('') // 验证码图片（Base64）
const captchaExpires = ref(0) // 验证码过期时间

// 验证码类型：graphical(图形验证码), slider(滑块验证码), sms(短信验证码)
const captchaType = ref('graphical')
const captchaEnabled = computed(() => loginConfig.value?.enableCaptcha !== false)

// 滑块答案仅提交给服务端，不在浏览器判定验证成功。
const slideVerifyRef = ref(null)
const showSliderModal = ref(false)

// 短信验证码相关
const smsCountdown = ref(0)
const smsTimer = ref(null)

const localLoginInfo = lStorage.get('loginInfo')
if (localLoginInfo) {
  loginInfo.value.username = localLoginInfo.username || ''
  loginInfo.value.password = localLoginInfo.password || ''
}

const isRemember = useStorage('isRemember', true)
const loading = ref(false)

// 三方登录平台列表
const socialPlatforms = ref([])
const socialLoading = ref(false)

watch(selectedTenantId, (tenantId) => {
  if (!tenantInitCompleted || tenantConfigApplying.value)
    return
  syncSelectedTenantToStorage(tenantId)
  tenantConfigApplying.value = true
  refreshLoginContext()
    .finally(() => {
      tenantConfigApplying.value = false
    })
})

// 手机号验证
const isValidPhone = computed(() => {
  const phone = loginInfo.value.phone
  return phone && /^1[3-9]\d{9}$/.test(phone)
})

function normalizeTenantId(value) {
  if (Array.isArray(value)) {
    const first = value.find(item => item !== null && item !== undefined && item !== '')
    return normalizeTenantId(first)
  }
  if (value === null || value === undefined || value === '')
    return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

function syncSelectedTenantToStorage(tenantId) {
  const normalizedTenantId = normalizeTenantId(tenantId)
  if (normalizedTenantId === null) {
    lStorage.remove(LOGIN_TENANT_STORAGE_KEY)
    return
  }
  lStorage.set(LOGIN_TENANT_STORAGE_KEY, normalizedTenantId)
}

function getSelectedTenantFromStorage() {
  return normalizeTenantId(lStorage.get(LOGIN_TENANT_STORAGE_KEY))
}

function getSocialTenantMap() {
  return lStorage.get(SOCIAL_TENANT_MAP_KEY) || {}
}

function setSocialTenantMap(map) {
  lStorage.set(SOCIAL_TENANT_MAP_KEY, map)
}

function rememberSocialTenant(state, tenantId) {
  const normalizedTenantId = normalizeTenantId(tenantId)
  if (!state || normalizedTenantId === null)
    return
  const map = getSocialTenantMap()
  map[state] = normalizedTenantId
  setSocialTenantMap(map)
}

function applyBrandLogo(config) {
  brandLogoUrl.value = resolveTenantPublicAssetUrl(config, 'logo') || defaultLogoUrl
}

function handleBrandLogoError() {
  if (brandLogoUrl.value !== defaultLogoUrl)
    brandLogoUrl.value = defaultLogoUrl
}

async function applyLoginPageConfig(config) {
  loginConfig.value = config || null
  await applyTenantConfig(config, appStore)
  applyBrandLogo(config)
}

async function loadLoginTenantOptions() {
  try {
    const res = await api.getLoginTenantOptions()
    if (res.code !== 200) {
      tenantOptions.value = []
      return
    }

    tenantOptions.value = (res.data || []).map(item => ({
      ...item,
      value: normalizeTenantId(item.tenantId),
      label: item.tenantName || item.systemName || item.browserTitle || String(item.tenantId),
    })).filter(item => item.value !== null)

    const storedTenantId = getSelectedTenantFromStorage()
    const initialTenantId = tenantOptions.value.find(item => String(item.value) === String(storedTenantId))?.value
      ?? tenantOptions.value[0]?.value
      ?? null
    if (initialTenantId !== null) {
      selectedTenantId.value = initialTenantId
      syncSelectedTenantToStorage(initialTenantId)
    }
  }
  catch (error) {
    console.error('获取登录租户列表失败:', error)
  }
}

// 获取登录配置
async function loadLoginConfig() {
  const currentTenantId = normalizeTenantId(selectedTenantId.value)
  try {
    const res = await api.getLoginConfig(userClient, currentTenantId)
    if (res.code === 200 && res.data) {
      await applyLoginPageConfig(res.data)
      captchaType.value = res.data.captchaType || 'graphical'

      // 根据验证码类型加载对应的验证码
      if (captchaEnabled.value) {
        await loadCaptchaByType()
      }
      else {
        loginInfo.value.code = ''
        loginInfo.value.codeKey = ''
      }
    }
  }
  catch (error) {
    console.error('获取登录配置失败:', error)
    // 使用默认配置
    captchaType.value = 'graphical'
    await applyLoginPageConfig(null)
    await refreshCaptcha()
  }
}

// 获取已启用的三方登录平台
async function loadSocialPlatforms() {
  try {
    socialLoading.value = true
    const res = await api.getSocialPlatforms(normalizeTenantId(selectedTenantId.value))
    if (res.code === 200 && res.data) {
      socialPlatforms.value = res.data.filter(p => p.enabled)
    }
  }
  catch (error) {
    console.error('获取三方登录平台失败:', error)
  }
  finally {
    socialLoading.value = false
  }
}

// 处理三方登录
async function handleSocialLogin(platform) {
  try {
    const tenantId = normalizeTenantId(selectedTenantId.value)
    const res = await api.getSocialAuthUrl(platform, tenantId)
    if (res.code === 200 && res.data) {
      rememberSocialTenant(res.data.state, tenantId)
      // 打开授权窗口
      const width = 600
      const height = 500
      const left = (window.innerWidth - width) / 2
      const top = (window.innerHeight - height) / 2

      const authWindow = window.open(
        res.data.authUrl,
        'social_auth',
        `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no,resizable=yes`,
      )

      // 监听授权窗口关闭
      const checkClosed = setInterval(() => {
        if (authWindow.closed) {
          clearInterval(checkClosed)
        }
      }, 500)
    }
    else {
      $message.error(res.msg || '获取授权链接失败')
    }
  }
  catch (error) {
    console.error('获取三方授权链接失败:', error)
    $message.error(localizeLabel('获取授权链接失败'))
  }
}

// 根据验证码类型加载验证码
async function loadCaptchaByType() {
  switch (captchaType.value) {
    case 'slider':
      closeSliderModal()
      break
    case 'sms':
      // 短信验证码不需要预加载，用户点击发送时才获取
      break
    case 'graphical':
    default:
      await refreshCaptcha()
      break
  }
}

// 获取图形验证码
async function refreshCaptcha() {
  try {
    const res = await api.getCaptcha()
    if (res.code === 200 && res.data) {
      loginInfo.value.codeKey = res.data.codeKey // 验证码key
      captchaImage.value = res.data.image || '' // 验证码图片（Base64）
      captchaExpires.value = res.data.expiresIn || 300 // 过期时间

      // 清空验证码输入框
      loginInfo.value.code = ''
    }
  }
  catch (error) {
    console.error('获取验证码失败:', error)
    $message.error(localizeLabel('获取验证码失败'))
  }
}

// 每次打开均由组件获取新的服务端挑战。
function openSliderModal() {
  if (loading.value)
    return
  if (!loginInfo.value.username || !loginInfo.value.password)
    return $message.warning(localizeLabel('请输入用户名和密码'))
  if (tenantOptions.value.length > 0 && normalizeTenantId(selectedTenantId.value) === null)
    return $message.warning(localizeLabel('请选择登录租户'))
  loginInfo.value.code = ''
  loginInfo.value.codeKey = ''
  showSliderModal.value = true
}

function closeSliderModal() {
  if (loading.value)
    return
  showSliderModal.value = false
  loginInfo.value.code = ''
  loginInfo.value.codeKey = ''
}

function onLoginClick() {
  handleLogin()
}

function onSlideSubmit({ code, codeKey }) {
  if (loading.value)
    return
  loginInfo.value.code = code
  loginInfo.value.codeKey = codeKey
  handleLogin()
}

// 处理登录失败
async function handleLoginFailure() {
  if (!captchaEnabled.value)
    return

  // 根据验证码类型处理
  if (captchaType.value === 'slider') {
    loginInfo.value.code = ''
    loginInfo.value.codeKey = ''
    await slideVerifyRef.value?.refresh()
  }
  else if (captchaType.value === 'graphical') {
    // 图形验证码刷新
    await refreshCaptcha()
  }
  // 短信验证码不需要刷新，保持原样
}

// 发送短信验证码
async function sendSmsCode() {
  if (!isValidPhone.value) {
    $message.warning(localizeLabel('请输入正确的手机号'))
    return
  }

  try {
    const res = await api.sendSmsCaptcha(loginInfo.value.phone)
    if (res.code === 200 && res.data) {
      if (res.data.status === 'success') {
        $message.success(localizeLabel('验证码发送成功'))

        // 开始倒计时
        smsCountdown.value = res.data.interval || 60
        startSmsCountdown()
      }
      else {
        $message.error(res.data.message || '验证码发送失败')
      }
    }
  }
  catch (error) {
    console.error('发送短信验证码失败:', error)
    $message.error(localizeLabel('发送短信验证码失败'))
  }
}

// 短信验证码倒计时
function startSmsCountdown() {
  if (smsTimer.value) {
    clearInterval(smsTimer.value)
  }

  smsTimer.value = setInterval(() => {
    if (smsCountdown.value > 0) {
      smsCountdown.value--
    }
    else {
      clearInterval(smsTimer.value)
    }
  }, 1000)
}

async function handleLogin() {
  if (loading.value)
    return
  const { username, password, code, codeKey, phone } = loginInfo.value
  const tenantId = normalizeTenantId(selectedTenantId.value)

  // 基础验证
  if (!username || !password)
    return $message.warning(localizeLabel('请输入用户名和密码'))

  if (tenantOptions.value.length > 0 && tenantId === null) {
    return $message.warning(localizeLabel('请选择登录租户'))
  }

  if (captchaEnabled.value) {
    // 根据验证码类型进行验证
    if (captchaType.value === 'slider') {
      if (!codeKey || !/^\d+$/.test(code)) {
        openSliderModal()
        return
      }
    }
    else if (captchaType.value === 'sms') {
      if (!phone)
        return $message.warning(localizeLabel('请输入手机号'))
      if (!code)
        return $message.warning(localizeLabel('请输入短信验证码'))
    }
    else {
      if (!code)
        return $message.warning(localizeLabel('请输入验证码'))
    }
  }

  try {
    loading.value = true
    if (captchaType.value === 'slider') {
      // A network or login failure must not replay the same proof.
      loginInfo.value.code = ''
      loginInfo.value.codeKey = ''
    }
    $message.loading(localizeLabel('正在验证，请稍后...'), { key: 'login' })

    await loadRuntimeCryptoConfig()

    // 登录密码 RSA 与通用 API 传输加密相互独立，以服务端登录配置为准。
    const passwordEncryptionEnabled = loginConfig.value?.enablePasswordEncryption !== false
    const submittedPassword = await encryptPassword(password, request, passwordEncryptionEnabled)

    // 构造登录参数 - 使用新的后端接口格式
    const params = {
      username,
      password: submittedPassword,
      code,
      codeKey,
      phone, // 短信验证码时需要
      tenantId,
      authType: captchaEnabled.value ? 'password_captcha' : 'password',
      userClient,
      appId: import.meta.env.VITE_APP_ID || 'forge_pc_001', // 客户端AppId
    }

    const res = await api.login(params)

    if (res.code === 200) {
      if (isRemember.value) {
        lStorage.set('loginInfo', { username, password })
      }
      else {
        lStorage.remove('loginInfo')
      }
      showSliderModal.value = false
      onLoginSuccess(res.data)
    }
    else {
      // 登录失败后刷新验证码
      await handleLoginFailure()
    }
  }
  catch (error) {
    $message.destroy('login')
    console.error(error)
    // 登录失败后刷新验证码
    await handleLoginFailure()
  }
  loading.value = false
}

async function onLoginSuccess(data = {}) {
  authStore.resetLoginState({ resetAuth: false })
  const accessToken = data.accessToken || data.token

  // 设置认证信息 - LoginResult 结构
  if (accessToken) {
    authStore.setToken({
      accessToken,
      tokenType: data.tokenType || 'Bearer',
      expiresIn: data.expiresIn,
    })

    // 密钥交换 - 使用 token 作为会话标识
    try {
      await initKeyExchange(request, accessToken)
    }
    catch (error) {
      console.warn('密钥交换失败，将使用降级方案:', error)
    }
  }

  // 如果返回了用户信息，设置到用户存储中
  if (data.userInfo) {
    const loginUser = data.userInfo
    loginUser.forcePasswordChange = data.forcePasswordChange === true || loginUser.forcePasswordChange === true
    userStore.setUser({
      id: loginUser.userId,
      username: loginUser.username,
      nickName: loginUser.realName || loginUser.username,
      email: loginUser.email,
      phone: loginUser.phone,
      avatar: loginUser.avatar,
      userType: loginUser.userType,
      userStatus: loginUser.userStatus,
      forcePasswordChange: loginUser.forcePasswordChange,
      tenantId: loginUser.tenantId,
      tenantName: loginUser.tenantName,
      tenantIds: loginUser.tenantIds || [],
      roleIds: loginUser.roleIds || [],
      roleKeys: loginUser.roleKeys || [],
      permissions: loginUser.permissions || [],
      apiPermissions: loginUser.apiPermissions || [],
      orgIds: loginUser.orgIds || [],
      mainOrgId: loginUser.mainOrgId,
      roles: loginUser.roleKeys ? Array.from(loginUser.roleKeys) : [],
      userInfo: loginUser,
    })

    // 同时存储到localStorage用于持久化
    lStorage.set('userInfo', loginUser)
  }

  const mustChangePassword = data.forcePasswordChange === true || data.userInfo?.forcePasswordChange === true
  if (mustChangePassword) {
    $message.warning(localizeLabel('当前账号必须先修改初始密码'), { key: 'login' })
    router.push('/profile')
    return
  }

  $message.loading(translateUiText('登录中...'), { key: 'login' })
  try {
    // 先获取菜单数据，再跳转
    await loadAndSetMenuData(data.userInfo?.tenantId || selectedTenantId.value)

    $message.success(localizeLabel('登录成功'), { key: 'login' })
    // 使用环境变量中的默认跳转路径
    const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'

    // 处理重定向
    const redirectPath = route.query.redirect
    if (redirectPath && redirectPath !== '/login') {
      // 如果 redirect 不是登录页，则跳转到 redirect
      delete route.query.redirect
      router.push({ path: redirectPath, query: route.query })
    }
    else {
      // 否则跳转到首页
      router.push(defaultRedirectPath)
    }
  }
  catch (error) {
    console.error(error)
    $message.destroy('login')
  }
}

// 监听三方登录子窗口的消息
async function handleSocialLoginMessage(event) {
  if (event.data?.type === 'SOCIAL_LOGIN_SUCCESS') {
    const { data } = event.data
    const accessToken = data?.accessToken || data?.token

    authStore.resetLoginState({ resetAuth: false })

    // 设置 token
    if (accessToken) {
      authStore.setToken({
        accessToken,
        tokenType: data.tokenType || 'Bearer',
        expiresIn: data.expiresIn,
      })
    }

    $message.success(localizeLabel('登录成功'))

    // 使用 window.location.href 强制刷新页面跳转
    const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'
    window.location.href = defaultRedirectPath
  }
  else if (event.data?.type === 'SOCIAL_LOGIN_FAILED') {
    $message.error(localizeLabel('三方登录失败，请重试'))
  }
}

// 页面加载时获取登录配置和验证码
onMounted(() => {
  ;(async () => {
    try {
      tenantConfigApplying.value = true
      await loadLoginTenantOptions()
      await refreshLoginContext()
    }
    finally {
      tenantInitCompleted = true
      tenantConfigApplying.value = false
    }
  })()
  // 监听三方登录消息
  window.addEventListener('message', handleSocialLoginMessage)
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (smsTimer.value) {
    clearInterval(smsTimer.value)
  }
  // 移除消息监听
  window.removeEventListener('message', handleSocialLoginMessage)
})

// 获取并设置菜单数据
async function refreshLoginContext() {
  await Promise.all([
    loadLoginConfig(),
    loadSocialPlatforms(),
  ])
}

async function loadAndSetMenuData(loginTenantId = selectedTenantId.value) {
  try {
    const permissionStore = usePermissionStore()
    const tenantStore = useTenantStore()
    const tenantConfig = await tenantStore.loadTenantConfig(normalizeTenantId(loginTenantId) || userStore.userInfo?.tenantId)
    await applyTenantConfig(tenantConfig, appStore)

    // 获取菜单数据
    const res = await mainApi.getMenu(1)
    if (res.code === 200 && res.data) {
      // 设置菜单数据到store
      permissionStore.setMenuData(res.data)
    }
    else {
      console.error('菜单数据格式不正确:', res)
    }

    // 等待菜单数据加载完成（最多等待5秒）
    let waitCount = 0
    while (!permissionStore.menuDataLoaded && waitCount < 50) {
      await new Promise(resolve => setTimeout(resolve, 100))
      waitCount++
    }
  }
  catch (error) {
    console.error('获取菜单数据失败:', error)
  }
}
</script>

<style scoped>
/* Container */
.login-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 0;
  overflow-x: hidden;
  overflow-y: auto;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* Static background */
.login-bg-animated {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 18%, rgba(37, 99, 235, 0.18) 0%, rgba(37, 99, 235, 0) 26%),
    radial-gradient(circle at 82% 80%, rgba(20, 184, 166, 0.18) 0%, rgba(20, 184, 166, 0) 28%),
    linear-gradient(135deg, #f7fbff 0%, #eef4fb 52%, #f8fafc 100%);
  z-index: 0;
}

.login-bg-animated::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(59, 130, 246, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.035) 1px, transparent 1px);
  background-size: 50px 50px;
  opacity: 0.7;
}

/* Login card */
.login-card {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1fr;
  max-width: none;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  background: #fff;
  border-radius: 0;
  border: none;
  box-shadow: none;
  overflow: hidden;
}

.login-card::before {
  content: none;
}

.login-card::after {
  content: none;
}

@media (min-width: 960px) {
  .login-card {
    grid-template-columns: minmax(0, 48%) minmax(440px, 52%);
  }
}

/* Left side - Branding */
.login-brand {
  background:
    radial-gradient(circle at 24% 18%, rgba(255, 255, 255, 0.22) 0%, rgba(255, 255, 255, 0) 30%),
    radial-gradient(circle at 82% 80%, rgba(191, 219, 254, 0.22) 0%, rgba(191, 219, 254, 0) 34%),
    linear-gradient(135deg, #1e40af, #3b82f6, #60a5fa);
  min-height: 100vh;
  min-height: 100dvh;
  padding: 36px 44px;
  display: none;
  flex-direction: column;
  justify-content: flex-start;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: none;
}

.login-brand::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size:
    44px 44px,
    44px 44px;
  opacity: 0.38;
  mask-image: linear-gradient(180deg, transparent 0%, #000 18%, #000 82%, transparent 100%);
  pointer-events: none;
}

@media (min-width: 960px) {
  .login-brand {
    display: flex;
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  min-height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  text-align: left;
}

.logo-lockup {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 0;
}

.logo-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: 8px;
}

.logo-text {
  color: #fff;
  font-size: 17px;
  font-weight: 800;
  line-height: 1;
}

.banner {
  position: relative;
  display: flex;
  width: 100%;
  min-height: clamp(430px, 62vh, 640px);
  align-items: stretch;
  justify-content: center;
  margin-top: 0;
}

.stack-carousel {
  width: 100%;
  height: clamp(430px, 62vh, 640px);
  margin: 0 auto;
}

.stack-carousel :deep(.n-carousel__slides) {
  width: 100%;
  height: 100%;
  align-items: stretch;
}

.stack-carousel :deep(.n-carousel__slide) {
  width: 100%;
  min-width: 100%;
  display: flex;
  justify-content: center;
}

.carousel-item {
  position: relative;
  width: 100%;
  height: clamp(430px, 62vh, 640px);
  margin: 0 auto;
  padding: 0 0 68px;
  overflow: hidden;
}

.carousel-image {
  position: absolute;
  top: 88px;
  left: 50%;
  width: min(620px, 100%);
  max-width: none;
  max-height: calc(100% - 136px);
  height: auto;
  object-fit: contain;
  transform: translateX(-50%);
  filter: drop-shadow(0 22px 34px rgba(30, 64, 175, 0.22));
  pointer-events: none;
  user-select: none;
}

.arco-carousel-arrow-left,
.arco-carousel-arrow-right {
  position: absolute;
  top: 50%;
  z-index: 5;
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.18);
  color: #fff;
  cursor: pointer;
  transform: translateY(-50%);
  transition:
    background 0.18s ease,
    border-color 0.18s ease;
}

.arco-carousel-arrow-left:hover,
.arco-carousel-arrow-right:hover {
  border-color: rgba(255, 255, 255, 0.58);
  background: rgba(255, 255, 255, 0.22);
}

.arco-carousel-arrow-left {
  left: 8px;
}

.arco-carousel-arrow-right {
  right: 8px;
}

.arco-carousel-dots {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 6;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.arco-carousel-dot {
  width: 16px;
  height: 4px;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.36);
  cursor: pointer;
  transition:
    width 0.18s ease,
    background 0.18s ease;
}

.arco-carousel-dot.active {
  width: 26px;
  background: #fff;
}

.carousel-copy {
  position: relative;
  z-index: 2;
  max-width: 560px;
  margin: 0 auto;
  text-align: center;
}

.carousel-title {
  color: #fff;
  font-size: 1.28rem;
  font-weight: 800;
  line-height: 1.35;
}

.carousel-subtitle {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.94rem;
  line-height: 1.55;
}

/* Right side - Form */
.login-form-wrapper {
  min-height: 100vh;
  min-height: 100dvh;
  padding: 40px 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98)), #fff;
  overflow-y: auto;
}

@media (min-width: 960px) {
  /* Fixed-height internal scroll so a tall form (incl. "其他登录方式")
     stays reachable on short / low-resolution screens instead of being
     clipped below the fold. "safe center" falls back to top alignment
     when content overflows, avoiding the flexbox top-clip bug. */
  .login-form-wrapper {
    height: 100vh;
    height: 100dvh;
    min-height: 0;
    align-items: safe center;
  }
}

@media (max-width: 959px) {
  .login-container {
    align-items: stretch;
    padding: 0;
  }

  .login-card {
    min-height: 100vh;
    min-height: 100dvh;
    max-width: none;
    margin: 0;
    overflow: visible;
  }

  .login-form-wrapper {
    min-height: 100vh;
    min-height: 100dvh;
    align-items: flex-start;
    padding: 22px 22px 72px;
    overflow: visible;
  }

  .login-form {
    max-width: 380px;
    margin: 0 auto;
  }

  .form-header {
    margin-bottom: 16px;
  }

  .form-group {
    margin-bottom: 12px;
  }

  .form-label {
    margin-bottom: 6px;
  }

  .form-options {
    margin-bottom: 14px;
  }

  .login-button {
    margin-top: 2px;
  }

  .login-trust-strip {
    display: none;
  }

  .social-login-section {
    margin-top: 18px;
  }

  .social-divider {
    margin-bottom: 14px;
  }
}

@media (min-width: 960px) and (max-height: 760px) {
  .login-brand {
    padding-top: 28px;
    padding-bottom: 28px;
  }

  .logo-lockup {
    top: 0;
  }

  .banner {
    min-height: 380px;
  }

  .stack-carousel,
  .carousel-item {
    height: 380px;
  }

  .carousel-image {
    top: 76px;
    width: min(560px, 100%);
    max-height: calc(100% - 112px);
  }
}

.login-form {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 22px;
  text-align: left;
}

.form-title {
  font-size: 1.75rem;
  color: #0f172a;
  margin: 0 0 8px;
  letter-spacing: 0;
}

.form-subtitle {
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 400;
  line-height: 1.6;
}

/* Form groups */
.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
  letter-spacing: 0.01em;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.modern-input {
  width: 100%;
}

.tenant-select-wrapper {
  width: 100%;
}

.modern-select {
  width: 100%;
}

/* Override Naive UI input styles */
:deep(.modern-input.n-input) {
  border-radius: 2px;
}

:deep(.modern-select .n-base-selection) {
  border-radius: 2px;
}

:deep(.modern-select .n-base-selection:not(.n-base-selection--disabled):hover) {
  border-color: #3b82f6;
}

:deep(.modern-select .n-base-selection:not(.n-base-selection--disabled).n-base-selection--active) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

:deep(.modern-input .n-input__border),
:deep(.modern-input .n-input__state-border) {
  border-radius: 2px;
}

:deep(.modern-input.n-input:not(.n-input--disabled):hover) {
  border-color: #3b82f6;
}

:deep(.modern-input.n-input:not(.n-input--disabled):focus-within) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-icon {
  color: #94a3b8;
  font-size: 16px;
}

/* Captcha */
.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-image {
  position: relative;
  width: 120px;
  height: 40px;
  padding: 3px;
  border: 1px solid #e0e0e6;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(#fff, #fff) padding-box,
    linear-gradient(135deg, rgba(59, 130, 246, 0.36), rgba(14, 165, 233, 0.18), rgba(148, 163, 184, 0.22)) border-box;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 8px 18px rgba(15, 23, 42, 0.06);
}

.captcha-image::after {
  content: '换';
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.64);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  opacity: 0;
  transform: translateY(-3px);
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
  pointer-events: none;
}

.captcha-image:hover {
  border-color: #3b82f6;
  transform: translateY(-1px);
  box-shadow:
    0 0 0 3px rgba(59, 130, 246, 0.1),
    0 12px 24px rgba(37, 99, 235, 0.14);
}

.captcha-image:hover::after {
  opacity: 1;
  transform: translateY(0);
}

.captcha-image:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow:
    0 0 0 3px rgba(59, 130, 246, 0.12),
    0 12px 24px rgba(37, 99, 235, 0.12);
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 9px;
  background: #f8fafc;
}

.captcha-loading {
  width: 100%;
  height: 100%;
  border-radius: 9px;
  background: linear-gradient(135deg, #f8fafc, #eef2ff);
  color: #94a3b8;
  font-size: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Slider verify trigger */
.slider-verify-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 44px;
  border-radius: 2px;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.25s ease;
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 500;
  user-select: none;
}

.slider-verify-trigger:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #2563eb;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.slider-verify-trigger.verified {
  border-color: #22c55e;
  background: #f0fdf4;
  color: #16a34a;
  cursor: default;
}

.slider-verify-trigger.verified:hover {
  box-shadow: none;
}

.trigger-icon {
  display: flex;
  align-items: center;
  font-size: 1rem;
}

.trigger-arrow {
  margin-left: auto;
  font-size: 0.875rem;
  opacity: 0.5;
  transition: transform 0.2s;
}

.slider-verify-trigger:hover .trigger-arrow {
  transform: translateX(3px);
  opacity: 0.8;
}

/* Slider modal overlay */
.slider-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

/* Slider modal card */
.slider-modal {
  position: relative;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 2rem 1.75rem 1.5rem;
  width: 100%;
  max-width: 400px;
  box-shadow:
    0 24px 48px rgba(15, 23, 42, 0.15),
    0 8px 16px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.slider-modal-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #f1f5f9;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  padding: 0;
}

.slider-modal-close:hover {
  background: #e2e8f0;
  color: #475569;
}

.slider-modal-header {
  text-align: center;
  margin-bottom: 1.25rem;
}

.slider-modal-icon {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.375rem;
  color: #3b82f6;
  margin: 0 auto 12px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.slider-modal-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px;
}

.slider-modal-desc {
  font-size: 0.8125rem;
  color: #94a3b8;
  margin: 0;
}

.slider-modal-body {
  display: flex;
  justify-content: center;
  border-radius: 12px;
  overflow: hidden;
}

.slider-modal-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 14px 0 0;
  font-size: 0.75rem;
  color: #cbd5e1;
}

/* SMS button */
.sms-button {
  min-width: 120px;
  white-space: nowrap;
  border-radius: 2px;
}

.mb-3 {
  margin-bottom: 12px;
}

/* Form options */
.form-options {
  margin-bottom: 24px;
}

.checkbox-label {
  font-size: 0.875rem;
  color: #64748b;
}

/* Login button */
.login-button {
  margin-top: 8px;
  position: relative;
  overflow: hidden;
  isolation: isolate;
  border-radius: 0px !important;
  border: 1px solid rgba(22, 93, 255, 0.22);
  background: linear-gradient(
    135deg,
    var(--primary-400) 0%,
    var(--primary-500) 55%,
    var(--primary-600) 100%
  ) !important;
  font-size: 0.9375rem;
  font-weight: 600;
  height: 52px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 10px 22px rgba(22, 93, 255, 0.24),
    0 2px 6px rgba(22, 93, 255, 0.16);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease,
    background 0.25s ease;
  cursor: pointer;
}

.login-trust-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.login-trust-strip span {
  display: inline-flex;
  min-width: 0;
  height: 34px;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border: 1px solid #e2e8f0;
  border-radius: 2px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.login-trust-strip i {
  color: #3b82f6;
  font-size: 14px;
}

.login-button::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 2px;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.14) 0%,
    rgba(255, 255, 255, 0.04) 34%,
    rgba(255, 255, 255, 0) 62%
  );
  z-index: 1;
  pointer-events: none;
  opacity: 0.95;
  transition: opacity 0.25s ease;
}

.login-button:hover {
  transform: translateY(-2px) scale(1.005);
  border-color: rgba(22, 93, 255, 0.32);
  background: linear-gradient(
    135deg,
    var(--primary-300) 0%,
    var(--primary-500) 58%,
    var(--primary-700) 100%
  ) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.24),
    0 14px 28px rgba(22, 93, 255, 0.3),
    0 4px 10px rgba(22, 93, 255, 0.18);
}

.login-button:hover::after {
  opacity: 1;
}

.login-button:active {
  transform: translateY(0) scale(0.985);
}

.login-button:active::after {
  opacity: 0.86;
}

.login-button :deep(.n-button__content) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
  z-index: 2;
}

.login-button :deep(.n-base-loading),
.login-button :deep(.n-button__icon) {
  position: relative;
  z-index: 2;
}

.button-text {
  letter-spacing: 0.01em;
  color: #fff;
}

.button-icon {
  font-size: 1.125rem;
  color: #fff;
  transition:
    transform 0.25s ease,
    opacity 0.25s ease;
}

.login-button:hover .button-icon {
  transform: translateX(4px);
}

/* Social login section */
.social-login-section {
  margin-top: 28px;
}

.social-divider {
  position: relative;
  text-align: center;
  margin-bottom: 20px;
}

.social-divider::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #e2e8f0 20%, #e2e8f0 80%, transparent);
}

.divider-text {
  position: relative;
  display: inline-block;
  padding: 0 16px;
  background: #fff;
  font-size: 0.8125rem;
  color: #94a3b8;
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.social-button {
  width: 44px;
  height: 44px;
  border-radius: 2px;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  padding: 0;
}

.social-button:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.15);
}

.social-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.social-icon-fallback {
  font-size: 20px;
  color: #64748b;
}

.social-button:hover .social-icon-fallback {
  color: #3b82f6;
}

/* Dark mode */
.dark .login-bg-animated {
  background:
    radial-gradient(circle at 26% 18%, rgba(59, 130, 246, 0.28) 0%, rgba(59, 130, 246, 0) 28%),
    linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%);
}

.dark .login-card {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(255, 255, 255, 0.1);
}

.dark .login-form-wrapper {
  background: transparent;
}

.dark .form-title {
  color: #f1f5f9;
}

.dark .form-subtitle {
  color: #94a3b8;
}

.dark .form-label {
  color: #cbd5e1;
}

.dark .social-divider::before {
  background: linear-gradient(90deg, transparent, #334155 20%, #334155 80%, transparent);
}

.dark .divider-text {
  background: transparent;
}

.dark .slider-modal {
  background: rgba(30, 41, 59, 0.98);
  border-color: rgba(255, 255, 255, 0.1);
}

.dark .slider-modal-title {
  color: #f1f5f9;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }

  .login-bg-animated::before,
  .login-brand::after,
  .login-card::after {
    animation: none;
  }
}

/* ICP Record */
.icp-record {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
  text-align: center;
}

.icp-record a {
  font-size: 0.75rem;
  color: rgba(100, 116, 139, 0.7);
  text-decoration: none;
  transition: color 0.2s ease;
}

.icp-record a:hover {
  color: rgba(59, 130, 246, 0.9);
}

.dark .icp-record a {
  color: rgba(148, 163, 184, 0.6);
}

.dark .icp-record a:hover {
  color: rgba(96, 165, 250, 0.8);
}

/* Responsive */
@media (max-width: 767px) {
  .login-container {
    align-items: stretch;
    padding: 0;
  }

  .form-title {
    font-size: 1.375rem;
  }

  .login-card {
    border-radius: 0;
  }
}
</style>

<style scoped>
.login-form-wrapper {
  position: relative;
}
.login-language-switcher {
  position: absolute;
  top: 20px;
  right: 20px;
}
</style>
