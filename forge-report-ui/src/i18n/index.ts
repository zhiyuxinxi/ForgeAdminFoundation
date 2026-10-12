//语言
import { watch } from 'vue'
import { reportLanguage } from './locale'
import { createI18n } from 'vue-i18n' //引入vue-i18n组件
import { LangEnum } from '@/enums/styleEnum'
import zh from './zh/index'
import en from './en/index'


// 语言数组
export const langList = [
  {
    label: '中文',
    key: LangEnum.ZH
  },
  {
    label: 'English',
    key: LangEnum.EN
  }
]

const i18n = createI18n({
  legacy: false,
  globalInjection:true,
  locale: reportLanguage.value,
  fallbackLocale: LangEnum.ZH,
  messages: {
    [LangEnum.ZH]: zh,
    [LangEnum.EN]: en
  }
})

watch(reportLanguage, value => { i18n.global.locale.value = value }, { flush: 'sync' })

// Keep legacy $t consumers on the same reactive language source as the designer.
// The existing CJS i18n build can otherwise return the new translation without
// invalidating the component render that used the previous language.
export const translate = ((...args: any[]) => {
  reportLanguage.value
  return (i18n.global.t as any)(...args)
}) as typeof i18n.global.t

export default i18n
