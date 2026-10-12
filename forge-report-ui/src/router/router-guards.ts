import { Router } from 'vue-router'
import { watch } from 'vue'
import { reportLanguage } from '@/i18n/locale'
import { uiText } from '@/i18n/display'
import { PageEnum } from '@/enums/pageEnum'
import { loginCheck } from '@/utils'
import { useUserStore } from '@/store/modules/userStore/userStore'
import { loadUserData } from '@/utils/auth'

export function createRouterGuards(router: Router) {
  watch([() => router.currentRoute.value.meta.title, reportLanguage], ([title]) => {
    if (typeof title === 'string') document.title = uiText(title)
  }, { immediate: true })
  router.beforeEach(async (to, from, next) => {
    if (!window.route) window.route = { params: {} }
    Object.assign(window.route.params, to.query)

    const Loading = window['$loading']
    Loading && Loading.start()
    const isErrorPage = router.getRoutes().findIndex((item) => item.name === to.name)
    if (isErrorPage === -1) {
      next({ name: PageEnum.ERROR_PAGE_NAME_404 })
      return
    }

    if (!loginCheck()) {
      if (to.name === PageEnum.BASE_LOGIN_NAME || to.name === PageEnum.BASE_SSO_LOGIN_NAME) {
        next()
      } else {
        next({ name: PageEnum.BASE_LOGIN_NAME })
      }
      return
    }

    const userStore = useUserStore()
    if (!userStore.dataLoaded) {
      try {
        await loadUserData()
      } catch {
        userStore.reset()
        next({ name: PageEnum.BASE_LOGIN_NAME })
        return
      }
    }

    next()
  })

  router.afterEach((to, _, failure) => {
    const Loading = window['$loading']
    document.title = uiText(to?.meta?.title as string) || document.title
    Loading && Loading.finish()
  })

  router.onError((error) => {
    console.log(error, '路由错误')
  })
}
