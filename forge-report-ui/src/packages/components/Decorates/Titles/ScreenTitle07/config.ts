import BaseConfig, { option as baseOption } from '../ScreenTitle/config'
import { ScreenTitle07Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  dataset: '安全态势感知中心',
  subtitle: 'SECURITY SITUATION',
  styleVariant: 'crystal',
  fontSize: 44,
  accentColor: '#f472b6',
  secondaryColor: '#a78bfa',
  gradientFrom: '#fff1f8',
  gradientTo: '#f472b6',
  backgroundColor: '#39143366',
  borderColor: '#f472b6'
}

export default class Config extends BaseConfig {
  public key = ScreenTitle07Config.key
  public chartConfig = cloneDeep(ScreenTitle07Config)
  public option = cloneDeep(option)
}
