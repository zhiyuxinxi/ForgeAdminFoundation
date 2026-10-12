import { defineStore } from 'pinia'
import { computed } from 'vue'
import { LangEnum } from '@/enums/styleEnum'
import { changeReportLanguage, reportLanguage } from '@/i18n/locale'

// One preference for i18n, Naive UI, dates and requests. Never reload the designer.
export const useLangStore = defineStore('useLangStore', () => ({
  lang: reportLanguage,
  getLang: computed(() => reportLanguage.value),
  changeLang: (language: LangEnum) => changeReportLanguage(language)
}))
