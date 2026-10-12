import { finishGlobalLoading, startGlobalLoading } from '@/composables/useGlobalLoading'
import { useAppStore } from '@/store'

export function createPageLoadingGuard(router) {
  let routeLoadingToken = null

  function startRouteLoading() {
    if (routeLoadingToken)
      return

    const appStore = useAppStore()
    if (appStore.routeGuardCompleted !== true)
      return

    routeLoadingToken = startGlobalLoading({
      globalLoadingType: 'route',
      globalLoadingText: '页面加载中，请稍候...',
      globalLoadingDelay: 120,
    })
  }

  function finishRouteLoading() {
    finishGlobalLoading(routeLoadingToken)
    routeLoadingToken = null
  }

  router.beforeEach(() => {
    startRouteLoading()
    $loadingBar.start()
  })

  router.afterEach(() => {
    setTimeout(() => {
      finishRouteLoading()
      $loadingBar.finish()
      // 确保路由守卫完成状态被设置
      const appStore = useAppStore()
      if (!appStore.routeGuardCompleted) {
        appStore.setRouteGuardCompleted(true)
      }
    }, 200)
  })

  router.onError(() => {
    finishRouteLoading()
    $loadingBar.error()
    // 发生错误时也要确保路由守卫完成状态被设置
    const appStore = useAppStore()
    if (!appStore.routeGuardCompleted) {
      appStore.setRouteGuardCompleted(true)
    }
  })
}
