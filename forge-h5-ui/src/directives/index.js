import loadingDirective, { loadingService } from './modules/loading'

export function setupDirectives(app) {
  app.directive('loading', loadingDirective)
  app.config.globalProperties.$loading = loadingService
}

export { loadingService }
