import { request } from '@/utils'

const useMock = import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true'
export default {
  // 获取用户信息 - 使用新的认证接口
  getUser: () => request.get('/auth/userInfo'),

  // 登出 - 使用新的认证接口
  logout: () => request.post('/auth/logout', {}, { needTip: false }),

  // 获取菜单数据 - 使用新的系统资源接口
  getMenu: async () => {
    if (useMock) {
      const { mockMenuApi } = await import('@/api/mock/menu')
      return mockMenuApi.getMenu()
    }
    else {
      return request.get('/auth/current/menu')
    }
  },

  // 获取租户配置
  getTenantConfig: tenantId => request.post('/system/tenant/userTenantConfig', null, { params: { id: tenantId } }),

  // 当前用户可切换租户
  getCurrentTenantOptions: () => request.get('/system/tenant/current/options'),

  // 切换租户
  switchTenant: tenantId => request.post('/system/tenant/switch', null, { params: { tenantId } }),

  // 当前用户可切换组织
  getCurrentOrgOptions: () => request.get('/system/org/current/options'),

  // 切换当前组织
  switchOrg: orgId => request.post('/system/org/switch', null, { params: { orgId } }),
}
