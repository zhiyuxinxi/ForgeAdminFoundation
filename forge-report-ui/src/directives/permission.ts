import type { Directive } from 'vue'
import { useUserStore } from '@/store/modules/userStore/userStore'

const ADMIN_WILDCARD = '*:*:*'

export const vPermission: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const perms = userStore.permissions
    if (perms.includes(ADMIN_WILDCARD)) return
    if (!perms.includes(binding.value)) {
      el.remove()
    }
  }
}
