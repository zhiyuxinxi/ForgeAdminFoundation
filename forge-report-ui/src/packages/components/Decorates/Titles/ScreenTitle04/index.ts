import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const ScreenTitle04Config: ConfigType = {
  key: 'ScreenTitle04',
  chartKey: 'VScreenTitle04',
  conKey: 'VCScreenTitle04',
  title: '锋刃标题',
  category: ChatCategoryEnum.TITLE,
  categoryName: ChatCategoryEnumName.TITLE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'title04.png'
}
