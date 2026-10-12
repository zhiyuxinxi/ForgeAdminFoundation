import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import api from '../../api'
import Login from '../../index.vue'
import ServerSliderCaptcha from '../ServerSliderCaptcha.vue'

vi.mock('../../api', () => ({ default: {
  getLoginTenantOptions: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getLoginConfig: vi.fn().mockResolvedValue({ code: 200, data: { captchaType: 'slider', enableCaptcha: true, enablePasswordEncryption: false } }),
  getSocialPlatforms: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getSliderCaptcha: vi.fn(),
  login: vi.fn(),
} }))
vi.mock('@/locales', () => ({ t: key => key, localizeLabel: value => value }))
vi.mock('@vueuse/core', () => ({ useStorage: () => ref(false) }))
vi.mock('@/api', () => ({ default: {} }))
vi.mock('@/components/common/LanguageSwitcher.vue', () => ({ default: { template: '<span />' } }))
vi.mock('@/store', () => ({ useAuthStore: () => ({}), useUserStore: () => ({}), useAppStore: () => ({}), usePermissionStore: () => ({}), useTenantStore: () => ({}) }))
vi.mock('@/utils', () => ({ lStorage: { get: vi.fn(), set: vi.fn(), remove: vi.fn() } }))
vi.mock('@/utils/crypto/crypto-config', () => ({ loadRuntimeCryptoConfig: vi.fn() }))
vi.mock('@/utils/crypto/key-exchange', () => ({ initKeyExchange: vi.fn(), encryptPassword: vi.fn(password => password) }))
vi.mock('@/utils/http', () => ({ request: {} }))
vi.mock('@/utils/page-title', () => ({ normalizePageTitle: value => value }))
vi.mock('@/utils/tenant-config', () => ({ applyTenantConfig: vi.fn(), resolveTenantPublicAssetUrl: vi.fn() }))

const challenge = { codeKey: 'key-one', backgroundImage: 'data:image/png;base64,Ymc=', sliderImage: 'data:image/png;base64,cGllY2U=', backgroundWidth: 320, backgroundHeight: 160, sliderWidth: 50, sliderHeight: 50, notchY: 40, expiresIn: 300 }
let wrapper
beforeEach(() => {
  vi.stubGlobal('useRouter', () => ({}))
  vi.stubGlobal('useRoute', () => ({ query: {} }))
  vi.stubGlobal('$message', { warning: vi.fn(), error: vi.fn(), loading: vi.fn(), destroy: vi.fn() })
  api.getSliderCaptcha.mockReset().mockResolvedValue({ code: 200, data: challenge })
  api.login.mockReset().mockResolvedValue({ code: 401 })
  wrapper = mount(Login, { global: { stubs: ['NCarousel', 'NInput', 'NButton', 'NCheckbox', 'NSelect', 'Transition'] } })
})
afterEach(() => {
  wrapper?.unmount()
  vi.unstubAllGlobals()
})

describe('pc slider login wiring', () => {
  it('does not submit before a server puzzle, then forwards proof and renews it on login failure', async () => {
    await flushPromises()
    wrapper.vm.loginInfo.username = 'alice'
    wrapper.vm.loginInfo.password = 'test-password'
    await wrapper.vm.handleLogin()
    await flushPromises()
    expect(api.login).not.toHaveBeenCalled()
    const slider = wrapper.findComponent(ServerSliderCaptcha)
    await slider.find('input').setValue('123')
    await slider.findAll('button')[1].trigger('click')
    await flushPromises()
    expect(api.login).toHaveBeenCalledTimes(1)
    expect(api.login).toHaveBeenCalledWith(expect.objectContaining({ username: 'alice', code: '123', codeKey: 'key-one', authType: 'password_captcha' }))
    expect(api.getSliderCaptcha).toHaveBeenCalledTimes(2)
    expect(wrapper.vm.loginInfo.code).toBe('')
    expect(wrapper.vm.loginInfo.codeKey).toBe('')
    expect(slider.findAll('button')[1].attributes('disabled')).toBeUndefined()
  })

  it('requires credentials before opening and clears pending proof when the dialog closes', async () => {
    await flushPromises()
    await wrapper.vm.openSliderModal()
    expect(wrapper.findComponent(ServerSliderCaptcha).exists()).toBe(false)
    expect(api.getSliderCaptcha).not.toHaveBeenCalled()
    wrapper.vm.loginInfo.username = 'alice'
    wrapper.vm.loginInfo.password = 'test-password'
    await wrapper.vm.openSliderModal()
    await flushPromises()
    wrapper.vm.loginInfo.code = '42'
    wrapper.vm.loginInfo.codeKey = 'key-one'
    await wrapper.vm.closeSliderModal()
    expect(wrapper.vm.loginInfo.code).toBe('')
    expect(wrapper.vm.loginInfo.codeKey).toBe('')
    expect(api.login).not.toHaveBeenCalled()
  })
})
