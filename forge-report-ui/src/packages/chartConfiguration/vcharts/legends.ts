import { uiText } from '@/i18n/display'
export const legendsConfig = {
  // 位置
  orient: [
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
    }
  ],
  // 对齐方式
  position: [
    {
      get label() { return uiText("起始") },
      value: 'start'
    },
    {
      get label() { return uiText("居中") },
      value: 'middle'
    },
    {
      get label() { return uiText("末尾") },
      value: 'end'
    }
  ],
  // 每一项的图例位置
  align: [
    {
      get label() { return uiText("居左") },
      value: 'left'
    },
    {
      get label() { return uiText("居右") },
      value: 'right'
    }
  ]
}

export const fontStyleConfig = {
  // 字重
  fontWeight: [
    {
      label: '100',
      value: 100
    },
    {
      label: '200',
      value: 200
    },
    {
      label: '300',
      value: 300
    },
    {
      label: '400',
      value: 400
    },
    {
      label: '500',
      value: 500
    },
    {
      label: '600',
      value: 600
    },
    {
      get label() { return uiText("正常") },
      value: "normal"
    },
    {
      get label() { return uiText("加粗") },
      value: "bold"
    }
  ],
  fontFamily: [
    {
      get label() { return uiText("宋体") },
      value: 'SimSun'
    },
    {
      get label() { return uiText("黑体") },
      value: 'SimHei'
    },
    {
      get label() { return uiText("楷体") },
      value: '楷体'
    }
  ]
}
