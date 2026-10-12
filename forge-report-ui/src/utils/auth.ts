import { getUserInfoApi, getUserMenuApi, getUserPermissionsApi } from '@/api/auth'
import { useUserStore } from '@/store/modules/userStore/userStore'

export async function loadUserData() {
  const userStore = useUserStore()

  const [userRes, menuRes, permRes] = await Promise.all([
    getUserInfoApi(),
    getUserMenuApi(),
    getUserPermissionsApi()
  ])

  if (userRes.code === 200 && userRes.data) {
    const user = userRes.data
    userStore.setUserInfo({
      userId: user.userId || user.id,
      username: user.username,
      realName: user.realName || user.nickName,
      nickName: user.nickName || user.realName || user.username,
      avatar: user.avatar || '',
      email: user.email || '',
      phone: user.phone || '',
      userType: user.userType || 0,
      userStatus: user.userStatus || 0,
      tenantId: user.tenantId,
      roleIds: user.roleIds || [],
      roleKeys: user.roleKeys || [],
      permissions: user.permissions || [],
      apiPermissions: user.apiPermissions || [],
      orgIds: user.orgIds || [],
      mainOrgId: user.mainOrgId,
      deptName: user.deptName || '',
      admin: user.admin || false,
      tenantAdmin: user.tenantAdmin || false,
      loginTime: user.loginTime || '',
      loginIp: user.loginIp || ''
    })
  }

  if (menuRes.code === 200 && menuRes.data) {
    userStore.setMenus(menuRes.data)
  }

  if (permRes.code === 200 && permRes.data) {
    userStore.setPermissions(permRes.data)
  }

  userStore.setDataLoaded(true)
}
