import { uiText } from '@/i18n/display'
export const labelConfig = {
  position: [
    { get label() { return uiText("外侧") }, value: 'outside' },
    { get label() { return uiText("内部") }, value: 'inside' },
    { get label() { return uiText("中心") }, value: 'center' }
  ],
  fontWeight: [
    { get label() { return uiText("正常") }, value: 'normal' },
    { get label() { return uiText("普通加粗") }, value: 'bold' },
    { get label() { return uiText("加粗") }, value: 'bolder' },
    { get label() { return uiText("细体") }, value: 'lighter' }
  ]
}
