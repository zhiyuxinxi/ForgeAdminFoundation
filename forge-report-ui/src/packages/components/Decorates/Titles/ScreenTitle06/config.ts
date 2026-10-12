import BaseConfig, { option as baseOption } from '../ScreenTitle/config'
import { ScreenTitle06Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  dataset: '能源综合管理平台',
  subtitle: 'ENERGY MANAGEMENT',
  styleVariant: 'pulse',
  fontSize: 43,
  accentColor: '#47ffb2',
  secondaryColor: '#18ffff',
  backgroundColor: '#06302466',
  borderColor: '#47ffb2'
}

export default class Config extends BaseConfig {
  public key = ScreenTitle06Config.key
  public chartConfig = cloneDeep(ScreenTitle06Config)
  public option = cloneDeep(option)
}
