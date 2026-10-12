import { defineStore } from 'pinia'
import { defaultPrimaryColor } from '@/settings.js'

export const useAppStore = defineStore('app', {
  state: () => ({
    isDark: false,
    primaryColor: defaultPrimaryColor,
    keepAliveNames: [] // 需要缓存的页面名称列表
  }),
  actions: {
    toggleDark() {
      this.isDark = !this.isDark
    },
    setPrimaryColor(color) {
      this.primaryColor = color
    },
    // 添加需要缓存的页面
    addKeepAliveName(name) {
      !this.keepAliveNames.includes(name) && this.keepAliveNames.push(name)
    },
    // 删除需要缓存的页面
    removeKeepAliveName(name) {
      this.keepAliveNames = this.keepAliveNames.filter((n) => n !== name)
    },
    // 设置需要缓存的页面列表
    setKeepAliveNames(names = []) {
      this.keepAliveNames = names
    }
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_app`,
    pick: ['primaryColor', 'keepAliveNames'],
    storage: sessionStorage,
  },
})
