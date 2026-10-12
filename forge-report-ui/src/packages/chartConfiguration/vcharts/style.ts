import { uiText } from '@/i18n/display'
export const styleConfig = {
  texture: [
    {
      get label() { return uiText("无纹理") },
      value: ''
    },
    {
      get label() { return uiText("圆形") },
      value: 'circle'
    },
    {
      get label() { return uiText("钻石") },
      value: 'diamond'
    },
    {
      get label() { return uiText("矩形") },
      value: 'rect'
    },
    {
      get label() { return uiText("竖线") },
      value: 'horizontal-line'
    },
    {
      get label() { return uiText("横线") },
      value: 'vertical-line'
    },
    {
      get label() { return uiText("右向左斜线") },
      value: 'bias-rl'
    },
    {
      get label() { return uiText("左向右斜线") },
      value: 'bias-lr'
    },
    {
      get label() { return uiText("格子") },
      value: 'grid'
    }
  ],
  curveType: [
    {
      get label() { return uiText("线性") },
      value: 'linear'
    },
    {
      get label() { return uiText("平滑") },
      value: 'monotone'
    },
    {
      get label() { return uiText("平滑趋近X") },
      value: 'monotoneX'
    },
    {
      get label() { return uiText("台阶") },
      value: 'step'
    },
    {
      get label() { return uiText("连线闭合") },
      value: 'catmullRom'
    },
    {
      get label() { return uiText("顺滑闭合") },
      value: 'catmullRomClosed'
    }
  ],
  lineCap: [
    {
      get label() { return uiText("默认") },
      value: 'butt'
    },
    {
      get label() { return uiText("圆形") },
      value: 'round'
    },
    {
      get label() { return uiText("方形") },
      value: 'square'
    }
  ],
  symbolType: [
    {
      get label() { return uiText("圆形") },
      value: 'circle'
    },
    {
      get label() { return uiText("方形") },
      value: 'rect'
    },
    {
      get label() { return uiText("菱形") },
      value: 'diamond'
    },
    {
      get label() { return uiText("三角形") },
      value: 'square'
    },
    {
      get label() { return uiText("指向向上") },
      value: 'arrow'
    },
    {
      get label() { return uiText("指向向左") },
      value: 'arrow2Left'
    },
    {
      get label() { return uiText("箭头向右") },
      value: 'arrow2Right'
    },
    {
      get label() { return uiText("瘦箭头向上") },
      value: 'wedge'
    },
    {
      get label() { return uiText("箭头向上") },
      value: 'triangle'
    },
    {
      get label() { return uiText("箭头向下") },
      value: 'triangleDown'
    },
    {
      get label() { return uiText("箭头向右") },
      value: 'triangleRight'
    },
    {
      get label() { return uiText("箭头向左") },
      value: 'triangleLeft'
    },
    {
      get label() { return uiText("星星") },
      value: 'star'
    },
    {
      get label() { return uiText("y字形物") },
      value: 'wye'
    },
    {
      get label() { return uiText("矩形") },
      value: 'rect'
    },
    {
      get label() { return uiText("圆角矩形") },
      value: 'rectRound'
    },
    {
      get label() { return uiText("扁平矩形") },
      value: 'roundLine'
    }
  ]
}
