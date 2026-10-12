import BaseConfig, { option as baseOption } from '../ScreenTitle/config'
import { ScreenTitle05Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  dataset: '智慧城市运营中心',
  subtitle: 'CITY INTELLIGENCE CENTER',
  styleVariant: 'gate',
  fontSize: 44,
  accentColor: '#22d3ee',
  secondaryColor: '#3b82f6',
  backgroundColor: '#041b3588',
  borderColor: '#22d3ee'
}

export default class Config extends BaseConfig {
  public key = ScreenTitle05Config.key
  public chartConfig = cloneDeep(ScreenTitle05Config)
  public option = cloneDeep(option)
}
