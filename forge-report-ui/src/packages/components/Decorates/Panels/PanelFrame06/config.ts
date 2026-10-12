import BaseConfig, { option as baseOption } from '../PanelFrame/config'
import { PanelFrame06Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  title: '重角模块',
  styleVariant: 'heavy',
  accentColor: '#ffcf5a',
  borderColor: '#ff9f1c',
  backgroundColor: '#24180588',
  showCorners: true
}

export default class Config extends BaseConfig {
  public key = PanelFrame06Config.key
  public chartConfig = cloneDeep(PanelFrame06Config)
  public option = cloneDeep(option)
}
