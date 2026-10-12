import BaseConfig, { option as baseOption } from '../PanelFrame/config'
import { PanelFrame08Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  title: '卡片模块',
  styleVariant: 'card',
  accentColor: '#93c5fd',
  borderColor: '#2563eb',
  backgroundColor: '#0a173088'
}

export default class Config extends BaseConfig {
  public key = PanelFrame08Config.key
  public chartConfig = cloneDeep(PanelFrame08Config)
  public option = cloneDeep(option)
}
