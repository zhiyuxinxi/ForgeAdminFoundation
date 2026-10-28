import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, withDirectives } from 'vue'
import hasPermi from '../hasPermi'

const { user } = vi.hoisted(() => ({ user: { getDataPermission: [] } }))
vi.mock('@/store', () => ({ useUserStore: () => user }))

const PermissionHost = defineComponent({
  props: ['permissions', 'revision'],
  setup(props) {
    return () => h('div', [withDirectives(
      h('button', { 'data-revision': props.revision }, 'Controlled action'),
      [[hasPermi, props.permissions]],
    )])
  },
})

const wrappers = []
function mountPermission(permissions = ['document:edit']) {
  const errors = []
  const wrapper = mount(PermissionHost, {
    props: { permissions, revision: 0 },
    global: { config: { errorHandler: error => errors.push(error) } },
  })
  wrappers.push(wrapper)
  return { wrapper, errors }
}

beforeEach(() => {
  user.getDataPermission = ['document:edit']
})
afterEach(() => {
  for (const wrapper of wrappers.splice(0)) wrapper.unmount()
  vi.restoreAllMocks()
})

describe('hasPermi directive lifecycle', () => {
  it('rechecks matching and wildcard permissions through Vue updated without a bound this', async () => {
    const { wrapper, errors } = mountPermission()
    expect(wrapper.find('button').exists()).toBe(true)
    await wrapper.setProps({ revision: 1 })
    expect(errors).toEqual([])
    expect(wrapper.find('button').exists()).toBe(true)
    user.getDataPermission = ['**']
    await wrapper.setProps({ permissions: ['document:remove'], revision: 2 })
    expect(errors).toEqual([])
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('removes an unauthorized element during mounted', () => {
    user.getDataPermission = ['document:view']
    const { wrapper, errors } = mountPermission()
    expect(errors).toEqual([])
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('removes an element when its permission no longer matches during updated', async () => {
    const { wrapper, errors } = mountPermission()
    user.getDataPermission = ['document:view']
    await wrapper.setProps({ revision: 1 })
    expect(errors).toEqual([])
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('preserves the loading behavior and checks permissions after they arrive', async () => {
    const warning = vi.spyOn(console, 'warn').mockImplementation(() => {})
    user.getDataPermission = []
    const { wrapper, errors } = mountPermission()
    expect(wrapper.find('button').exists()).toBe(true)
    expect(warning).toHaveBeenCalledWith('权限数据未加载，跳过权限检查')
    user.getDataPermission = ['document:view']
    await wrapper.setProps({ revision: 1 })
    expect(errors).toEqual([])
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('rejects an empty permission binding on both lifecycle hooks', async () => {
    expect(() => mountPermission([])).toThrow('请设置操作权限标签值')
    const updated = mountPermission()
    await updated.wrapper.setProps({ permissions: [] })
    expect(updated.errors).toHaveLength(1)
    expect(updated.errors[0].message).toBe('请设置操作权限标签值')
  })
})
