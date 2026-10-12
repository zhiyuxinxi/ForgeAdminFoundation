import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const ScreenTitle02Config: ConfigType = {
  key: 'ScreenTitle02',
  chartKey: 'VScreenTitle02',
  conKey: 'VCScreenTitle02',
  title: '光幕标题',
  category: ChatCategoryEnum.TITLE,
  categoryName: ChatCategoryEnumName.TITLE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'title02.png'
}
