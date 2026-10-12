import { uiText } from '@/i18n/display'
export const labelConfig = {
  position: [
    {
      get label() { return uiText("外部") },
      value: 'outside'
    },
    {
      get label() { return uiText("内部") },
      value: 'inside'
    },
    {
      get label() { return uiText("内部-外") },
      value: 'inside-outer'
    },
    {
      get label() { return uiText("内部-里") },
      value: 'inside-inner'
    },
    {
      get label() { return uiText("内部-居中") },
      value: 'inside-center'
    }
  ],
  barPosition: [
    {
      get label() { return uiText("外部") },
      value: 'outside'
    },
    {
      get label() { return uiText("内部") },
      value: 'inside'
    },
    {
      get label() { return uiText("顶部") },
      value: 'top'
    },
    {
      get label() { return uiText("底部") },
      value: 'bottom'
    },
    {
      get label() { return uiText("左侧") },
      value: 'left'
    },
    {
      get label() { return uiText("右侧") },
      value: 'right'
    },
    {
      get label() { return uiText("内部-顶") },
      value: 'inside-top'
    },
    {
      get label() { return uiText("内部-底") },
      value: 'inside-bottom'
    },
    {
      get label() { return uiText("内部-右") },
      value: 'inside-right'
    },
    {
      get label() { return uiText("内部-左") },
      value: 'inside-left'
    },
    {
      get label() { return uiText("顶部-右") },
      value: 'top-right'
    },
    {
      get label() { return uiText("顶部-左") },
      value: 'top-left'
    },
    {
      get label() { return uiText("底部-右") },
      value: 'bottom-right'
    },
    {
      get label() { return uiText("底部-左") },
      value: 'bottom-left'
    }
  ],
  linePosition: [
    {
      get label() { return uiText("顶部") },
      value: 'top'
    },
    {
      get label() { return uiText("底部") },
      value: 'bottom'
    },
    {
      get label() { return uiText("左侧") },
      value: 'left'
    },
    {
      get label() { return uiText("右侧") },
      value: 'right'
    },
    {
      get label() { return uiText("顶部-右") },
      value: 'top-right'
    },
    {
      get label() { return uiText("顶部-左") },
      value: 'top-left'
    },
    {
      get label() { return uiText("底部-右") },
      value: 'bottom-right'
    },
    {
      get label() { return uiText("底部-左") },
      value: 'bottom-left'
    },
    {
      get label() { return uiText("居中") },
      value: 'center'
    }
  ]
}
