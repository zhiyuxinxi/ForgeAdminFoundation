import type { uiText } from '../src/i18n/display'

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $ui: typeof uiText
  }
}

export {}
