import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const ScreenTitleConfig: ConfigType = {
  key: 'ScreenTitle',
  chartKey: 'VScreenTitle',
  conKey: 'VCScreenTitle',
  title: '大屏标题',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'text_static.png'
}
