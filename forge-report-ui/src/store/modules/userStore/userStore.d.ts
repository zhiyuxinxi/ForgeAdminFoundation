export interface LoginUserInfo {
  userId: number | string
  username: string
  realName: string
  nickName: string
  avatar: string
  email: string
  phone: string
  userType: number
  userStatus: number
  tenantId: number | string
  roleIds: Array<number | string>
  roleKeys: string[]
  permissions: string[]
  apiPermissions: string[]
  orgIds: Array<number | string>
  mainOrgId: number | string
  deptName: string
  admin: boolean
  tenantAdmin: boolean
  loginTime: string
  loginIp: string
  [key: string]: unknown
}

export interface MenuItem {
  id: number
  parentId: number
  resourceName: string
  resourceNameEn?: string
  resourceType: number
  path: string
  component: string
  icon: string
  sort: number
  visible: number
  menuStatus: number
  keepAlive: number
  alwaysShow: number
  redirect: string
  isExternal: number
  perms: string
  clientCode: string
  ssoEnabled: number
  ssoTargetClient: string
  openTarget: string
  children: MenuItem[]
}

export interface UserStateType {
  userInfo: LoginUserInfo | null
  menus: MenuItem[]
  permissions: string[]
  dataLoaded: boolean
}
