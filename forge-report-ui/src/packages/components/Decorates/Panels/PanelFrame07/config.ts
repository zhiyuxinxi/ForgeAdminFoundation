import BaseConfig, { option as baseOption } from '../PanelFrame/config'
import { PanelFrame07Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  title: '切角模块',
  styleVariant: 'cut',
  accentColor: '#f472b6',
  borderColor: '#a78bfa',
  backgroundColor: '#25112988'
}

export default class Config extends BaseConfig {
  public key = PanelFrame07Config.key
  public chartConfig = cloneDeep(PanelFrame07Config)
  public option = cloneDeep(option)
}
