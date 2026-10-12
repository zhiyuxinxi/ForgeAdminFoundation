import { defineStore } from 'pinia'
import type { UserStateType, LoginUserInfo, MenuItem } from './userStore.d'

export const useUserStore = defineStore({
  id: 'useUserStore',
  state: (): UserStateType => ({
    userInfo: null,
    menus: [],
    permissions: [],
    dataLoaded: false
  }),
  getters: {
    userId(): number | string | undefined {
      return this.userInfo?.userId
    },
    username(): string {
      return this.userInfo?.username || ''
    },
    realName(): string {
      return this.userInfo?.realName || this.userInfo?.nickName || ''
    },
    avatar(): string {
      return this.userInfo?.avatar || ''
    },
    tenantId(): number | string | undefined {
      return this.userInfo?.tenantId
    },
    roles(): string[] {
      return this.userInfo?.roleKeys || []
    },
    roleIds(): Array<number | string> {
      return this.userInfo?.roleIds || []
    },
    isAdmin(): boolean {
      return this.userInfo?.admin || false
    },
    isTenantAdmin(): boolean {
      return this.userInfo?.tenantAdmin || false
    }
  },
  actions: {
    setUserInfo(user: LoginUserInfo) {
      this.userInfo = user
    },
    setMenus(menus: MenuItem[]) {
      this.menus = menus
    },
    setPermissions(permissions: string[]) {
      this.permissions = permissions
    },
    setDataLoaded(loaded: boolean) {
      this.dataLoaded = loaded
    },
    reset() {
      this.userInfo = null
      this.menus = []
      this.permissions = []
      this.dataLoaded = false
    }
  }
})
