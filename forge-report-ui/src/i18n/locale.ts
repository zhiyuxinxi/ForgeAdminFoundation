import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/en'
import { LangEnum } from '@/enums/styleEnum'
import { StorageEnum } from '@/enums/storageEnum'

export function normalizeLanguage(value: unknown): LangEnum {
  return value === LangEnum.EN || value === 'en' || value === 'en-US' ? LangEnum.EN : LangEnum.ZH
}

function savedLanguage(): LangEnum {
  try {
    return normalizeLanguage(JSON.parse(localStorage.getItem(StorageEnum.GO_LANG_STORE) || '{}').lang)
  } catch {
    return LangEnum.ZH
  }
}

export const reportLanguage = ref(savedLanguage())
export const requestLanguage = () => reportLanguage.value === LangEnum.EN ? 'en-US' : 'zh-CN'

export function changeReportLanguage(value: unknown) {
  reportLanguage.value = normalizeLanguage(value)
  try {
    localStorage.setItem(StorageEnum.GO_LANG_STORE, JSON.stringify({ lang: reportLanguage.value }))
  } catch {
    // Private browsing may disable storage; the current session still switches.
  }
}

watch(reportLanguage, () => {
  document.documentElement.lang = requestLanguage()
  dayjs.locale(reportLanguage.value === LangEnum.EN ? 'en' : 'zh-cn')
}, { immediate: true, flush: 'sync' })

window.addEventListener('storage', event => {
  if (event.key === StorageEnum.GO_LANG_STORE) reportLanguage.value = savedLanguage()
})
