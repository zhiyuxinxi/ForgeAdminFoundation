import { computed } from 'vue'
import { LangEnum } from '@/enums/styleEnum'
import { reportLanguage } from '@/i18n/locale'
import { zhCN, enUS, dateEnUS, dateZhCN } from 'naive-ui'


// 语言切换
export const useLang = () => {

  const locale = computed(() => {
    return reportLanguage.value === LangEnum.ZH ? zhCN : enUS
  })

  const dateLocale = computed(() => {
    return reportLanguage.value === LangEnum.ZH ? dateZhCN : dateEnUS
  })

  return {
    locale,
    dateLocale
  }
}
