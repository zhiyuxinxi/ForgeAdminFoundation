import { App } from 'vue'
import VueLazyLoad from 'vue3-lazyload'
import { requireErrorImg } from '@/utils'
import { vPermission } from '@/directives/permission'

export function setupDirectives(app: App) {
  app.use(VueLazyLoad, {
    error: requireErrorImg(),
  })

  app.directive('permission', vPermission)
}
