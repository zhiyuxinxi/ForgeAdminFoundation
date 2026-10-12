import { uiText } from '@/i18n/display'
export const axisConfig = {
  // X轴位置
  xposition: [
    {
      get label() { return uiText("顶部") },
      value: 'top'
    },
    {
      get label() { return uiText("底部") },
      value: 'bottom'
    }
  ],
  // Y轴位置
  yposition: [
    {
      get label() { return uiText("左侧") },
      value: 'left'
    },
    {
      get label() { return uiText("右侧") },
      value: 'right'
    }
  ],
  // 线条
  splitLint: {
    lineStyle: {
      type: [
        {
          get label() { return uiText("实线") },
          value: 'solid'
        },
        {
          get label() { return uiText("虚线") },
          value: 'dashed'
        },
        {
          get label() { return uiText("点线") },
          value: 'dotted'
        }
      ]
    }
  },
  // 视觉映射
  visualMap: {
    orient: [
      {
        get label() { return uiText("竖直") },
        value: 'vertical'
      },
      {
        get label() { return uiText("水平") },
        value: 'horizontal'
      }
    ]
  }
}
