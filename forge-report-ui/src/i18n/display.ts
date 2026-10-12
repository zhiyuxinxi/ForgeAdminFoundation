import { LangEnum } from '@/enums/styleEnum'
import { reportLanguage } from './locale'
import messages from './display-messages.json'

// Only call for authored interface text. Never apply this lookup to report data,
// user input, API responses, component values or persisted identifiers.
export function uiText(text: string | undefined | null, values: Record<string, unknown> = {}, context?: string): string {
  if (!text) return text || ''
  const translated = reportLanguage.value === LangEnum.EN
    ? (messages as Record<string, string>)[context ? `${context}:${text}` : text] ?? text
    : text
  return translated.replace(/\{(\d+)\}/g, (token, key) => Object.prototype.hasOwnProperty.call(values, key) ? String(values[key]) : token)
}
