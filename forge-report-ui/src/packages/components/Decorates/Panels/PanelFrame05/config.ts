import BaseConfig, { option as baseOption } from '../PanelFrame/config'
import { PanelFrame05Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  title: '辉光模块',
  styleVariant: 'glow',
  accentColor: '#58a6ff',
  borderColor: '#7dd3fc',
  backgroundColor: '#071d3a99'
}

export default class Config extends BaseConfig {
  public key = PanelFrame05Config.key
  public chartConfig = cloneDeep(PanelFrame05Config)
  public option = cloneDeep(option)
}
