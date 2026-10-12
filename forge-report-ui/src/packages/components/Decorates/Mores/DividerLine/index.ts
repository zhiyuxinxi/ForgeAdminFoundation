import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const DividerLineConfig: ConfigType = {
  key: 'DividerLine',
  chartKey: 'VDividerLine',
  conKey: 'VCDividerLine',
  title: '发光分割线',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'faguang.png'
}
