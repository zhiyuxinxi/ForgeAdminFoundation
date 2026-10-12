import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const ScreenTitle07Config: ConfigType = {
  key: 'ScreenTitle07',
  chartKey: 'VScreenTitle07',
  conKey: 'VCScreenTitle07',
  title: '晶体标题',
  category: ChatCategoryEnum.TITLE,
  categoryName: ChatCategoryEnumName.TITLE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'title07.png'
}
