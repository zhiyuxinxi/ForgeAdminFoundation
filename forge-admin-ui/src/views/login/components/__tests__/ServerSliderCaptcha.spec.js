import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/locales/en-US'
import zh from '@/locales/zh-CN'
import api from '../../api'
import ServerSliderCaptcha from '../ServerSliderCaptcha.vue'

vi.mock('../../api', () => ({ default: { getSliderCaptcha: vi.fn() } }))
vi.mock('@/locales', () => ({ t: key => key }))
const data = { codeKey: 'server-key', backgroundImage: 'data:image/png;base64,Ymc=', sliderImage: 'data:image/png;base64,cGllY2U=', backgroundWidth: 320, backgroundHeight: 160, sliderWidth: 50, sliderHeight: 50, notchY: 40, expiresIn: 300 }
let wrapper
beforeEach(() => api.getSliderCaptcha.mockReset().mockResolvedValue({ code: 200, data: { ...data } }))
afterEach(() => {
  wrapper?.unmount()
  vi.restoreAllMocks()
})

describe('server slider challenge', () => {
  it('renders server images and submits original-image X with its key, without claiming success', async () => {
    wrapper = mount(ServerSliderCaptcha)
    await flushPromises()
    expect(wrapper.find('.background').attributes('src')).toBe(data.backgroundImage)
    expect(wrapper.find('.piece').attributes('src')).toBe(data.sliderImage)
    await wrapper.find('input').setValue('135')
    expect(wrapper.find('.piece').element.style.left).toBe('42.1875%')
    expect(wrapper.find('input').attributes('max')).toBe('270')
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('submit')).toEqual([[{ codeKey: 'server-key', code: '135' }]])
    expect(wrapper.emitted('success')).toBeUndefined()
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('submit')).toHaveLength(1)
  })

  it.each([{ ...data, codeKey: '' }, { ...data, backgroundWidth: 0 }, { ...data, backgroundImage: 'https://external.invalid/image' }])('refuses an unusable server challenge', async (invalid) => {
    api.getSliderCaptcha.mockResolvedValue({ code: 200, data: invalid })
    wrapper = mount(ServerSliderCaptcha)
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toBe('login.slider.loadFailed')
    expect(wrapper.findAll('button')[1].attributes('disabled')).toBeDefined()
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('rejects expired challenges and refreshes with a new key', async () => {
    const clock = vi.spyOn(Date, 'now').mockReturnValue(1000)
    wrapper = mount(ServerSliderCaptcha)
    await flushPromises()
    clock.mockReturnValue(302000)
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.find('[role="alert"]').text()).toBe('login.slider.expired')
    api.getSliderCaptcha.mockResolvedValue({ code: 200, data: { ...data, codeKey: 'fresh-key' } })
    await wrapper.findAll('button')[0].trigger('click')
    await flushPromises()
    await wrapper.find('input').setValue('99')
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('submit')[0][0]).toEqual({ codeKey: 'fresh-key', code: '99' })
  })

  it('keeps a late previous fetch from replacing the refreshed key', async () => {
    let resolveOld
    api.getSliderCaptcha.mockImplementationOnce(() => new Promise((resolve) => {
      resolveOld = resolve
    }))
    wrapper = mount(ServerSliderCaptcha)
    api.getSliderCaptcha.mockResolvedValue({ code: 200, data: { ...data, codeKey: 'new-key' } })
    await wrapper.vm.refresh()
    resolveOld({ code: 200, data })
    await flushPromises()
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('submit')[0][0].codeKey).toBe('new-key')
  })

  it('blocks duplicate login interaction while busy and recovers from challenge fetch failure', async () => {
    api.getSliderCaptcha.mockRejectedValueOnce(new Error('network'))
    wrapper = mount(ServerSliderCaptcha)
    await flushPromises()
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    await wrapper.findAll('button')[0].trigger('click')
    await flushPromises()
    await wrapper.setProps({ busy: true })
    expect(wrapper.find('input').attributes('disabled')).toBeDefined()
    expect(wrapper.findAll('button').every(button => button.attributes('disabled') !== undefined)).toBe(true)
    expect(Object.keys(en.login.slider)).toEqual(Object.keys(zh.login.slider))
    expect(en.login.slider.submit).toBe('Confirm and sign in')
    expect(zh.login.slider.submit).toBe('确认并登录')
  })
})
