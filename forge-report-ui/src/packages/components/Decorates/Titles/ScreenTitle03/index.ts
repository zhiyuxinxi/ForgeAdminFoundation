import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const ScreenTitle03Config: ConfigType = {
  key: 'ScreenTitle03',
  chartKey: 'VScreenTitle03',
  conKey: 'VCScreenTitle03',
  title: '星环标题',
  category: ChatCategoryEnum.TITLE,
  categoryName: ChatCategoryEnumName.TITLE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'title03.png'
}
