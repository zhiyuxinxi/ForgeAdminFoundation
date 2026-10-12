import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const ScreenTitle05Config: ConfigType = {
  key: 'ScreenTitle05',
  chartKey: 'VScreenTitle05',
  conKey: 'VCScreenTitle05',
  title: '双翼标题',
  category: ChatCategoryEnum.TITLE,
  categoryName: ChatCategoryEnumName.TITLE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'title05.png'
}
