import { uiText } from '@/i18n/display'
export const legendConfig = {
  // X轴位置
  lengendX: [
    {
      get label() { return uiText("靠左") },
      value: 'left'
    },
    {
      get label() { return uiText("居中") },
      value: 'center'
    },
    {
      get label() { return uiText("靠右") },
      value: 'right'
    }
  ],
  // y轴位置
  lengendY: [
    {
      get label() { return uiText("靠上") },
      value: 'top'
    },
    {
      get label() { return uiText("居中") },
      value: 'center'
    },
    {
      get label() { return uiText("靠下") },
      value: 'bottom'
    }
  ],
  // 排列方向
  orient: [
    {
      get label() { return uiText("水平") },
      value: 'horizontal'
    },
    {
      get label() { return uiText("垂直") },
      value: 'vertical'
    }
  ],
  // 形状
  shape: [
    {
      get label() { return uiText("圆形") },
      value: 'circle'
    },
    {
      get label() { return uiText("方形") },
      value: 'rect'
    },
    {
      get label() { return uiText("圆角方形") },
      value: 'roundRect'
    },
    {
      get label() { return uiText("三角形") },
      value: 'triangle'
    },
    {
      get label() { return uiText("钢笔形") },
      value: 'pin'
    },
    {
      get label() { return uiText("箭头形") },
      value: 'arrow'
    }
  ]
}
