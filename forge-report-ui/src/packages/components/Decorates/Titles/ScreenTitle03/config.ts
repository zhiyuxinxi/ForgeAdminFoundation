import BaseConfig, { option as baseOption } from '../ScreenTitle/config'
import { ScreenTitle03Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  dataset: '全域数据驾驶舱',
  subtitle: 'GLOBAL DATA COMMAND',
  styleVariant: 'halo',
  fontSize: 44,
  accentColor: '#58a6ff',
  secondaryColor: '#b45cff',
  backgroundColor: '#111d4c88',
  borderColor: '#58a6ff'
}

export default class Config extends BaseConfig {
  public key = ScreenTitle03Config.key
  public chartConfig = cloneDeep(ScreenTitle03Config)
  public option = cloneDeep(option)
}
